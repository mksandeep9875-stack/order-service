# Order Service

**Port:** 8096

---

## Overview
`order-service` manages order-related operations for the Ecommerce platform.  
It allows creating orders (bookings) and retrieving order details.

This service will call the payment service in asynchronous manner and uses redis cache to implement the saga choreographed architecture.
Once the payment is completed the webflux reactive style of communication will update the redis such that the endpoint now returns success/fail depending on the payment response.
Also after the order completion the service also pushed order-events produces to the queue, such that the inventory service will consume it and update the inventory stock details accordingly.

The service also have a reactive communication with inventory service to see if the orders selected by customer is in stock before proceeding with order creation.

---

## Endpoints

| Endpoint                      | Method | Description                        |
|--------------------------------|--------|------------------------------------|
| /order/v1/create/booking       | POST   | Create a new order (booking)       |
| /order/v1/get/details/{orderid} | GET   | Retrieve details of a specific order by order ID |

---
## Configuration

Configuration file: src/main/resources/application*.properties or application.yml.

The application properties will be taken from the profile from https://github.com/mksandeep9875-stack/config-server-properties.git using spring cloud config server

---
## Dependencies

-Spring Boot Starter Web

-Spring Boot Starter Actuator

-Spring Boot Starter MongoDB (depending on your database)

-Spring Cloud Config Client

-Eureka Client

-Spring cloud Webflux


---

## How to Run

```bash
git clone <your-repo-url>
cd customer-service
mvn clean install
mvn spring-boot:run
