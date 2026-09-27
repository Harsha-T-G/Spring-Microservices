package org.example.inventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    public OpenAPI serviceOpenApi(@Value("${info.app.version}") String version) {
        return new OpenAPI().info(new Info().title("Inventory API").version(version)
                .description("Inspect stock and reserve it atomically. Use the dev profile for sample stock."));
    }
    @Bean
    public OperationCustomizer correlationHeader() {
        return (operation, handlerMethod) -> operation.addParametersItem(new Parameter()
                .name("X-Correlation-Id").in("header").required(false)
                .description("Optional tracing ID. A UUID is generated when absent or invalid.")
                .schema(new StringSchema().pattern("[A-Za-z0-9._-]{1,64}"))
                .example("swagger-demo-1"));
    }

}
