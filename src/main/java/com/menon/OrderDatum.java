package com.menon;


import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class OrderDatum {

    private String orderId;
    private String paymentId;
    private String type;
    private String description;
    private String status;
    private Map<String, Integer> productsItems;
}
