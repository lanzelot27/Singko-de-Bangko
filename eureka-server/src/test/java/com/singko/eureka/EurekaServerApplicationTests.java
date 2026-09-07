package com.singko.eureka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = EurekaServerApplication.class)
class EurekaServerApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("Unit Test 1: Eureka Server Application context loads successfully")
    void testContextLoads() {
        assertNotNull(environment, "Spring Environment must not be null");
        assertTrue(EurekaServerApplication.class.isAnnotationPresent(EnableEurekaServer.class),
                "EurekaServerApplication must be annotated with @EnableEurekaServer");
    }

    @Test
    @DisplayName("Unit Test 2: Verify configured port and application name")
    void testEurekaServerProperties() {
        String serverPort = environment.getProperty("server.port");
        String appName = environment.getProperty("spring.application.name");

        assertEquals("8761", serverPort, "Eureka server port must default to 8761");
        assertEquals("EUREKA-SERVER", appName, "Application name must be EUREKA-SERVER");
    }

    @Test
    @DisplayName("Unit Test 3: Verify standalone registry settings")
    void testEurekaServerStandaloneSettings() {
        String registerWithEureka = environment.getProperty("eureka.client.register-with-eureka");
        String fetchRegistry = environment.getProperty("eureka.client.fetch-registry");

        assertEquals("false", registerWithEureka, "register-with-eureka should be false for standalone server");
        assertEquals("false", fetchRegistry, "fetch-registry should be false for standalone server");
    }
}
