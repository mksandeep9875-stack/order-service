package com.menon;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Bean(name = "customer-service-validate")
    public WebClient webClientCustomerService(WebClient.Builder webClientBuilder)
    {
        return webClientBuilder
                .baseUrl("http://localhost:8085/customer/v1/validate")
                .filter(new LoggingWebClientFilter())
                .build();
    }

    @Bean(name = "payment-service-create-payment")
    public WebClient webClientPaymentService(WebClient.Builder webClientBuilder)
    {
        return webClientBuilder
                .baseUrl("http://localhost:8095/payment/v1/create")
                .filter(new LoggingWebClientFilter())
                .build();
    }

    @Bean(name = "inventory-service-check-availability")
    public WebClient webClientInventoryService(WebClient.Builder webClientBuilder)
    {
        return webClientBuilder
                .baseUrl("http://localhost:8092/inventory/v1/checkInventoryAvailability")
                .filter(new LoggingWebClientFilter())
                .build();
    }


}
