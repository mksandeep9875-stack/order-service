package com.menon;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class PaymentRequest {

    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
}
