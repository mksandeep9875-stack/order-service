package com.menon;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "orders")
@Getter
@Setter
public class Order {

    @Id
    private String orderId;           // Unique order identifier
    private String customerPhone;     // Link to the customer
    private String vendorPhone;       // Link to the vendor selling the product - OPTIONAL
    private Map<String, Integer> productsItems; // <productId -, quantity>
    private BigDecimal totalPrice;
    private String currency;          // e.g., USD, EUR
    private String deliveryAddress;  // Full address for delivery
    private String status;            // PROCESSING, CONFIRMED, DELIVERED, CANCELLED
    private String paymentId;         // Optional: link to payment service
    private LocalDateTime createdAt;  // Timestamp for order creation
    private LocalDateTime updatedAt;  // Timestamp for status updates - updated for each stage change

}
