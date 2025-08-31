package com.menon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class CustomerService {
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    @Qualifier("customer-service-validate")
    WebClient webClient;

    public boolean validateToken(String token, String customerPhone) {
        log.info("Validating token within the CustomerService for order-service: {}", token);

        Principal authResponse = webClient.get()
                .header("Authorization", token)
                .retrieve()
                .bodyToMono(Principal.class)
                .block();

        log.info("Response from auth service: {}", authResponse);
        return authResponse.getUsername().equalsIgnoreCase(customerPhone) && authResponse.getState().equalsIgnoreCase("valid");
    }
}
