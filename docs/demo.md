# Ten-minute demonstration

Preparation: use JDK 21, run each service's `./mvnw clean verify`, and start the
built JARs using [README commands](../README.md). Use a fresh Inventory process
with the dev profile for the expected starting quantities. Keep both logs visible.

| Time | Demonstration | What to explain |
| --- | --- | --- |
| 0:00–1:00 | Show service diagram and separate poms; both health/info endpoints | Data and runtime ownership; local health does not assert Inventory availability |
| 1:00–2:00 | GET JAVA-BOOK → 20 | Development-only seed and Inventory ownership |
| 2:00–3:00 | POST order quantity 2, key demo-1, correlation demo-order-1 → 201 | RestClient request, IDs, Location, stock now 18 |
| 3:00–4:00 | Repeat exact POST; then change quantity under demo-1 | Original order/no decrement; changed fingerprint gives 409 |
| 4:00–5:00 | New key, quantity 999 → 422; inspect rejected order | Business rejection differs from technical uncertainty; stock remains 18 |
| 5:00–6:00 | Stop Inventory; post several orders with new keys | Controlled 503, one retry per permitted operation; no fake confirmed/rejected result |
| 6:00–7:00 | Observe CLOSED → OPEN; post another order | Fast 503, no retry log / outbound call while open; WireMock supplies exact call-count proof |
| 7:00–8:00 | Restart Inventory; retry a failed key after the five-second open wait | HALF_OPEN trial restores service; explicitly explain stock/history reset on restart |
| 8:00–9:00 | Find demo-order-1 in both service logs | Same correlation value, service, HTTP duration/status and operation IDs |
| 9:00–10:00 | Show clean suite results and evidence; run slow-response focused test if desired | Real one-second response timeout, exactly two attempts, independent WireMock testing |

Focused timing demonstration from order-service:

```sh
./mvnw -Dtest=OrderApiTest#givenSlowInventory_whenCreatingOrder_thenRealReadTimeoutRetriesWithinBoundedTime test
```

For repeatable real-process verification, run `python3 scripts/verify-e2e.py`
from the root. It uses temporary ports, stops only the processes it creates,
and leaves raw output under ignored `.local/e2e/`. It restarts Inventory to
exercise recovery, so its final stock assertion uses the reseeded stock.

The demo outline is prepared; a live presentation to the user is not recorded
as completed. [Learning notes](microservices-notes.md) contain review answers.
