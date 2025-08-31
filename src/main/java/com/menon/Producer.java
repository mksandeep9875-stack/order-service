package com.menon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;


@Service
public class Producer
{
    private static final Logger logger = LoggerFactory.getLogger(Producer.class);
    private static final String TOPIC = "order-events";

    @Autowired //DEPENDENCY INJECTION PROMISE FULFILLED AT RUNTIME
    private KafkaTemplate<String, String> kafkaTemplate ;
    @Qualifier("payment-service-create-payment")
    @Autowired
    private WebClient payment;

    public void publishOrderDatum(String orderId,
                                  String type,
                                  String description,
                                  String status,
                                  String paymentId, Map<String, Integer> productsItems) throws JsonProcessingException
    {
        OrderDatum orderDatum = getOrderDatum(orderId, type, description, status, paymentId, productsItems);

        ObjectMapper objectMapper = new ObjectMapper();
        String datum =  objectMapper.writeValueAsString(orderDatum);
        logger.info(String.format("#### -> Producing message, order-events -> %s", datum));
        this.kafkaTemplate.send(TOPIC, datum);
    }

    private OrderDatum getOrderDatum(String orderId, String type, String description, String status, String paymentId, Map<String, Integer> productsItems) {
        OrderDatum orderDatum = new OrderDatum();
        orderDatum.setOrderId(orderId);
        orderDatum.setType(type);
        orderDatum.setDescription(description);
        orderDatum.setStatus(status);
        orderDatum.setPaymentId(paymentId);
        orderDatum.setProductsItems(productsItems);
        return orderDatum;
    }

}
