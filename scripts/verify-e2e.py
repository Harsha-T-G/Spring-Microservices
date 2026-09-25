import argparse
import json
import os
from pathlib import Path
import socket
import subprocess
import threading
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]


def free_port():
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        return sock.getsockname()[1]


def request(port, path, body=None, key=None, correlation='e2e-check'):
    headers = {'X-Correlation-Id': correlation}
    data = None
    if body is not None:
        headers['Content-Type'] = 'application/json'
        headers['Idempotency-Key'] = key
        data = json.dumps(body).encode()
    incoming = urllib.request.Request(f'http://127.0.0.1:{port}{path}', data=data, headers=headers)
    start = time.monotonic()
    try:
        response = urllib.request.urlopen(incoming, timeout=5)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        return response.status, json.load(response), dict(response.headers), time.monotonic() - start


def eventually(operation, timeout):
    deadline = time.monotonic() + timeout
    last_error = None
    while time.monotonic() < deadline:
        try:
            return operation()
        except (AssertionError, OSError) as error:
            last_error = error
            threading.Event().wait(0.1)
    raise AssertionError(f'Condition did not succeed in {timeout}s: {last_error}')


def stop(process):
    if process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=8)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=3)


def main():
    parser = argparse.ArgumentParser(description='Verify the two built services in isolated local processes')
    parser.add_argument('--java', default=str(Path(os.environ['JAVA_HOME']) / 'bin/java') if 'JAVA_HOME' in os.environ else 'java')
    args = parser.parse_args()
    inventory_port, order_port = free_port(), free_port()
    while inventory_port == order_port:
        order_port = free_port()
    logs = ROOT / '.local' / 'e2e' / time.strftime('%Y%m%d-%H%M%S')
    logs.mkdir(parents=True, exist_ok=False)
    processes, handles, evidence = [], [], []

    def record(scenario, **values):
        item = {'scenario': scenario, **values}
        evidence.append(item)
        print(json.dumps(item), flush=True)

    def launch(service, port, extra):
        jar = ROOT / f'{service}-service/target/{service}-service-0.0.1-SNAPSHOT.jar'
        if not jar.is_file():
            raise FileNotFoundError(f'Build {service}-service with ./mvnw clean verify first')
        output = (logs / f'{service}.log').open('a')
        handles.append(output)
        process = subprocess.Popen([args.java, '-jar', str(jar), f'--server.port={port}', *extra],
                                   cwd=ROOT, stdout=output, stderr=subprocess.STDOUT)
        processes.append(process)
        def ready():
            assert process.poll() is None, f'{service} exited; inspect {logs}'
            status, body, _, _ = request(port, '/actuator/health')
            assert status == 200 and body['status'] == 'UP'
        eventually(ready, 30)
        return process

    def stock(expected):
        status, body, _, _ = request(inventory_port, '/api/v1/inventory/JAVA-BOOK')
        assert status == 200 and body['availableQuantity'] == expected, body

    payload = {'customerId': 'CUST-E2E', 'sku': 'JAVA-BOOK', 'quantity': 2}
    try:
        inventory = launch('inventory', inventory_port, ['--spring.profiles.active=dev'])
        launch('order', order_port, [f'--inventory.base-url=http://127.0.0.1:{inventory_port}'])
        record('independent-startup', order_port=order_port, inventory_port=inventory_port)
        stock(20)
        status, original, headers, elapsed = request(order_port, '/api/v1/orders', payload, 'e2e-success', 'e2e-success')
        assert status == 201 and original['status'] == 'CONFIRMED', original
        assert headers['X-Correlation-Id'] == 'e2e-success'
        assert headers['Location'] == '/api/v1/orders/' + original['id']
        stock(18)
        record('success', http=status, stock=18, order_id=original['id'], reservation_id=original['reservationId'], seconds=round(elapsed, 3))
        status, duplicate, _, _ = request(order_port, '/api/v1/orders', payload, 'e2e-success', 'e2e-duplicate')
        assert status == 201 and duplicate == original
        stock(18)
        record('duplicate', http=status, same_order=True, stock=18)
        status, rejected, _, _ = request(order_port, '/api/v1/orders', {**payload, 'quantity': 999}, 'e2e-rejected')
        assert status == 422 and rejected['code'] == 'INSUFFICIENT_STOCK', rejected
        stock(18)
        record('insufficient-stock', http=status, code=rejected['code'], stock=18)
        for service in ('order', 'inventory'):
            body = (logs / f'{service}.log').read_text()
            assert f'[{service}-service,e2e-success]' in body and original['reservationId'] in body
        record('correlation', id='e2e-success', present_in_both_service_logs=True)
        stop(inventory)
        for index in range(4):
            status, error, _, elapsed = request(order_port, '/api/v1/orders', payload, f'e2e-outage-{index}')
            assert status == 503 and error['code'] == 'INVENTORY_UNAVAILABLE' and elapsed < 4, error
            record('unavailable', request=index + 1, http=status, seconds=round(elapsed, 3))
        order_log = (logs / 'order.log').read_text()
        assert 'State transition from CLOSED to OPEN' in order_log
        retries_before = order_log.count('inventoryRetry=')
        status, _, _, elapsed = request(order_port, '/api/v1/orders', payload, 'e2e-open')
        assert status == 503 and elapsed < 0.5
        assert (logs / 'order.log').read_text().count('inventoryRetry=') == retries_before
        record('circuit-open', http=status, seconds=round(elapsed, 3), new_retry_logs=0)
        status, orders, _, _ = request(order_port, '/api/v1/orders')
        assert status == 200 and len(orders) == 2
        launch('inventory', inventory_port, ['--spring.profiles.active=dev'])
        def recovered():
            status, body, _, _ = request(order_port, '/api/v1/orders', payload, 'e2e-outage-0', 'e2e-recovery')
            assert status == 201, body
            assert body['status'] == 'CONFIRMED'
            return body
        recovery = eventually(recovered, 12)
        stock(18)
        assert 'State transition from HALF_OPEN to CLOSED' in (logs / 'order.log').read_text()
        record('recovery-after-inventory-restart', http=201, stock=18, order_id=recovery['id'])
        record('result', status='PASS', note='Inventory restart resets its in-memory stock and reservation history.')
        (logs / 'results.json').write_text(json.dumps(evidence, indent=2) + '\n')
        print(f'Local evidence: {logs}', flush=True)
    finally:
        for process in reversed(processes):
            stop(process)
        for output in handles:
            output.close()


if __name__ == '__main__':
    main()
