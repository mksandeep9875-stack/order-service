package com.menon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    @Autowired
    @Qualifier("inventory-service-check-availability")
    WebClient webClient;

    public boolean checkInventoryAvailability(Map<String, Integer> productsItems, String token) {

        String products = productsItems.values().stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        log.info("Triggering inventory call from order to check product availability for products: {}", products);

        return Boolean.TRUE.equals(webClient.post()
                .header("Authorization", token)
                .body(BodyInserters.fromValue(productsItems))
                .retrieve()
                .bodyToMono(Boolean.class)
                .block());

    }

}
