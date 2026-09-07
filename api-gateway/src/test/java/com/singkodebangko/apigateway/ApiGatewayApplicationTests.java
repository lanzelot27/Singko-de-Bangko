package com.singkodebangko.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void contextLoads() {
    }

    @Test
    void whenAccessingSecuredEndpointWithoutToken_thenReturns401() {
        webTestClient.get()
                .uri("/accounts/2001/balance")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("Unauthorized")
                .jsonPath("$.message").isEqualTo("Missing authorization token")
                .jsonPath("$.path").isEqualTo("/accounts/2001/balance");
    }
}
