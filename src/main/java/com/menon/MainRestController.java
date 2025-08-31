package com.menon;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("order/v1")
public class MainRestController {

    private static final Logger log = LoggerFactory.getLogger(MainRestController.class);
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    CustomerService customerService;
    @Autowired
    PaymentService paymentService;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    RedisTemplate<String, Object> redisTemplate;
    @Autowired
    Producer producer;

    @GetMapping("get/details/{orderid}")
    public ResponseEntity<?> getOrder(@PathVariable("orderid") String orderid)
    {
        Order order = orderRepository.findById(orderid).get();
        return ResponseEntity.ok(order);
    }


    @PostMapping("create/booking")
    public ResponseEntity<?> createOrder(@RequestBody Order order,
                                         @RequestHeader("Authorization") String token,
                                         HttpServletRequest request,
                                         HttpServletResponse response) throws JsonProcessingException {

        // COOKIE VALIDATION LOGIC
        List<Cookie> cookieList = getCookies(request);
        log.info("cookie check complete");
        if( cookieList.stream().filter(cookie -> cookie.getName().equals("order-service-stage-1")).findAny().isEmpty()) // COOKIE_CHECK
        {
            log.info("Received request to create order, no cookie present: {}", order);
            if(customerService.validateToken(token))
            {
                // first check if the products are available in inventory
                if(inventoryService.checkInventoryAvailability(order.getProductsItems(), token)){
                    log.info("Products are available in inventory: {}", order.getProductsItems());
                } else {
                    log.info("Products are not available in inventory: {}", order.getProductsItems());
                    return ResponseEntity.badRequest().body("Products are not available in inventory, please continue with other products");
                }
                log.info("Token is valid: {}", token);
                log.info("Proceeding to create order: {}", order);
                order.setOrderId("ORDER-" + new Random().nextInt(1000000));
                order.setStatus("PROCESSING");
                producer.publishOrderDatum(order.getOrderId(),
                                                "CREATE",
                        "Order Created Successfully with Order ID: " + order.getOrderId(),
                        "PROCESSING",
                        "PAYMENT ID PENDING",
                        order.getProductsItems());
                // Once published to kafka, we can save the order to the database
                orderRepository.save(order);

                log.info("Order saved successfully: {}", order);
                log.info("Creating a New Payment Request");
                PaymentRequest paymentRequest = createPaymentRequest(order);

                log.info("Sending request to Payment Service");
                // create payment
                String responseKey = paymentService.createPayment(paymentRequest, token, order.getProductsItems());
                log.info("Received the ResponseKey which will be sent as a Cookie to the Front-end");

                log.info("Setting up the Cookie for the Front-end");
                Cookie cookieStage1 = new Cookie("order-service-stage-1", responseKey);
                cookieStage1.setMaxAge(300);
                log.info("Cookie set up successfully");

                response.addCookie(cookieStage1);
                log.info("Cookie added to the outgoing response");
                log.info("Order created successfully: {} and request forwarded to Payment Service", order);
                return ResponseEntity.ok("STAGE 1: We have started processing your Order with Order ID: " + order.getOrderId() + ". Please wait for the payment to be completed.");
            }
            else
            {
                log.info("Token is invalid: {}", token);
                return ResponseEntity.badRequest().body("Invalid token");
            }


        }
        else
        {

            // FOLLOW UP LOGIC
            log.info("found a relevant cookie.. initiating follow up logic");

            Cookie followup_cookie =  cookieList.stream().
                    filter(cookie -> cookie.getName().equals("order-service-stage-1")).findAny().get();

            String followup_cookie_key = followup_cookie.getValue();
            String cacheResponse = (String)redisTemplate.opsForValue().get(followup_cookie_key);
            log.info("cacheResponse from cookie/redis cache: {}", cacheResponse);

            String[] cacheResponseArray = cacheResponse.split(" ");

            if(cacheResponseArray[0].equals("stage1"))
            {
                log.info("Request still under process...");

                return ResponseEntity.ok("Request still under process...");
            }
            else if(cacheResponseArray[0].equals("paymentId:orderId"))
            {
                String[] parts = cacheResponseArray[1].split(":");
                return ResponseEntity.ok("Order Created Successfully with Order ID: " + parts[1] + " and Payment ID: " + parts[0]);
            }
            else
            {
                return ResponseEntity.ok("Error Processing the Order");
            }


        }

    }

    private static PaymentRequest createPaymentRequest(Order order) {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(order.getOrderId());
        //paymentRequest.setAmount(menuService.calculateOrder(order));
        paymentRequest.setAmount(BigDecimal.TEN);
        log.info("Payment Request created successfully: {}", paymentRequest);
        return paymentRequest;
    }

    private List<Cookie> getCookies(HttpServletRequest request) {
        List<Cookie> cookieList = null;
        log.info("initiating cookie check");

        //Optional<String> healthStatusCookie = Optional.ofNullable(request.getHeader("health_status_cookie"));
        Cookie[] cookies = request.getCookies();
        if(cookies == null)
        {
            cookieList = new ArrayList<>();
        }
        else
        {
            // REFACTOR TO TAKE NULL VALUES INTO ACCOUNT
            cookieList = List.of(cookies);
        }
        return cookieList;
    }

}
