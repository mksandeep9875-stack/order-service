package com.menon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class CustomerService
{
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    @Qualifier("customer-service-validate")
    WebClient webClient;

    public boolean validateToken(String token) {


        log.info("Validating token within the AuthService: {}", token);
        log.info("Sending request to auth service to validate token: {}", token);

        Principal authResponse = webClient.get()
                .header("Authorization", token)
                .retrieve()
                .bodyToMono(Principal.class)
                .block(); // Thread is Blocked until the response is received | SYNC

        log.info("Response from auth service: {}", authResponse);
        return authResponse.getState().equalsIgnoreCase("valid");
    }
}
