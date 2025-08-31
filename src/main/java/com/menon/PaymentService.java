package com.menon;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PaymentService
{
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    RedisTemplate<String, Object> redisTemplate;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    Producer producer;
    @Autowired
    @Qualifier("payment-service-create-payment")
    WebClient webClient;

            public String createPayment(PaymentRequest paymentRequest, String token, Map<String, Integer> productsItems)
            {
                log.info("Received request to create payment: {}", paymentRequest);

                AtomicInteger retryCounter = new AtomicInteger(0);

                Mono<String> paymentServiceResponse = webClient.post()
                        .header("Authorization", token)
                        .body(BodyInserters.fromValue(paymentRequest))
                        .retrieve()
                        .bodyToMono(String.class).
                        retryWhen(Retry.fixedDelay(5, Duration.ofSeconds(10))
                                .doBeforeRetry(retrySignal -> {retryCounter.incrementAndGet(); log.info("Retrying..."+retryCounter);})
                                .filter(throwable -> throwable instanceof RuntimeException));

                try{
                    producer.publishOrderDatum(paymentRequest.getOrderId(),
                            "UPDATE",
                            "Order Status updated to PAYMENT PENDING with Order ID: " + paymentRequest.getOrderId(),
                            "ORDER CREATED | PAYMENT PENDING",
                            "PAYMENT PENDING",
                            productsItems);
                }catch (JsonProcessingException e)
                {
                    throw new RuntimeException(e);
                }


                log.info("Payment Request sent to the payment service");

                String responseKey = paymentRequest.getOrderId()+(new Random().nextInt(1000)); // this is the key that we will return from this method

                log.info("Response Key generated: {}", responseKey);
                // eventual response from the payment service will  come a bit later but we will  proceed with our thread execution

                // here goes the handler for the eventual response and one of the primary responsibilities of this handler is to put the
                // response in the cache so that any one else with the right cache key can retrieve the response from the cache and we then return the key from this method

                // SECOND PART OF ASYNC REQUEST - TO SET UP THE HANDLER FOR THE EVENTUAL RESPONSE

                redisTemplate.opsForValue().set(responseKey,"stage1 orderid:"+paymentRequest.getOrderId()); // this is the response from the payment service
                log.info("Response Key set in the cache: {}", responseKey);


                paymentServiceResponse.subscribe(
                        (response) ->
                        {
                            log.info(response+" from the payment service");
                            // MENU CREATION LOGIC TO BE IMPLEMENTED HERE
                            // AND PUT THE RESPONSE IN REDIS

                            log.info("Updating status of the Order to PAID");
                            String stage1response  = (String)redisTemplate.opsForValue().get(responseKey);
                            String[] stage1responseArray = stage1response.split(" ");
                            String[] orderidarray = stage1responseArray[1].split(":");
                            String orderid = orderidarray[1];
                            Order order = orderRepository.findById(orderid).get();
                            order.setStatus("PAYMENT SUCCESSFUL");
                            order.setPaymentId(response);
                            orderRepository.save(order);
                            try
                            {
                                producer.publishOrderDatum(order.getOrderId(),
                                        "UPDATE",
                                        "Order Status updated to PAYMENT SUCCESSFUL and Payment ID: " + response + " with Order ID: " + order.getOrderId(),
                                        "CONFIRMED",
                                        order.getPaymentId(), order.getProductsItems());
                            }
                            catch (JsonProcessingException e)
                            {
                                throw new RuntimeException(e);
                            }
                            log.info("Updated status of the Order to PAID successfully");
                            redisTemplate.opsForValue().set(responseKey,"paymentId:orderId "+response+":"+orderid);
                        },
                        error ->
                        {
                            log.info("error processing the response "+error.getMessage());

                            log.info("Updating status of the Order to FAILED");
                            String stage1response  = (String)redisTemplate.opsForValue().get(responseKey);
                            String[] stage1responseArray = stage1response.split(" ");
                            String[] orderidarray = stage1responseArray[1].split(":");
                            String orderid = orderidarray[1];
                            Order order = orderRepository.findById(orderid).get();
                            order.setStatus("PAYMENT CREATION FAILED");
                            orderRepository.save(order);
                            try
                            {
                                producer.publishOrderDatum(order.getOrderId(),
                                        "UPDATE",
                                        "Order Status updated to PAYMENT CREATION FAILED with Order ID: " + order.getOrderId(),
                                        "CANCELLED",
                                        order.getPaymentId(),
                                        order.getProductsItems());
                            }
                            catch (JsonProcessingException e)
                            {
                                throw new RuntimeException(e);
                            }
                            log.info("Updated status of the Order to FAILED successfully");

                            redisTemplate.opsForValue().set(responseKey,"error "+error.getMessage());
                        });


                //redisTemplate.opsForValue().set(responseKey, response.block()); // this is the response from the payment service


                log.info("Returning the response key: {}", responseKey);
                return responseKey;
            }

}
