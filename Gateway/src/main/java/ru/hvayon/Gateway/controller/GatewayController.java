package ru.hvayon.Gateway.controller;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.hvayon.Gateway.auth.AuthService;
import ru.hvayon.Gateway.kafka.LogMessage;
import ru.hvayon.Gateway.kafka.MyKafkaProducer;
import ru.hvayon.Gateway.request.AddTicketRequest;
import ru.hvayon.Gateway.request.PrivilegeHistoryRequest;
import ru.hvayon.Gateway.request.TicketRequest;
import ru.hvayon.Gateway.response.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@PropertySource("classpath:application.properties")
public class GatewayController {
    @Value("${flight_service.host}")
    private String FLIGHT_SERVICE;

    @Value("${ticket_service.host}")
    private String TICKET_SERVICE;

    @Value("${bonus_service.host}")
    private String BONUS_SERVICE;

    @Value("${stats_service.host}")
    private String STATS_SERVICE;

    private static String GET_FLIGHTS_URL = "/api/v1/flights?page={page}&size={size}";
    private static String GET_FLIGHT_BY_NUMBER_URL = "/api/v1/flights/{flightNumber}";
    private static String GET_TICKETS_URL = "/api/v1/tickets";
    private static String GET_TICKET_BY_UID = "/api/v1/tickets/{ticketUid}";
    private static String GET_PRIVILEGE_URL = "/api/v1/privilege";
    private static String GET_PRIVILEGE_HISTORY_URL = "/api/v1/privilege/history";
    private static String GET_PRIVILEGE_HISTORY_BY_TICKET_UID_URL = "/api/v1/privilege/history/{ticketUid}";
    private static String GET_STATS_URL = "/api/v1/stats";

    @Autowired
    private CircuitBreakerFactory circuitBreakerFactory;

    @Autowired
    private RetryTemplate retryTemplate;

    @Autowired
    private AuthService authService;

    @Autowired
    private MyKafkaProducer kafkaProducer;

//    @GetMapping("/authOnly")
//    public ResponseEntity<UserInfoResponse> test(@RequestHeader("Authorization") String authHeader) throws Exception {
//        // for test
//        LocalDateTime startDttm = LocalDateTime.now();
//
//        String username = authService.auth(authHeader).getPrincipal();
////        kafkaProducer.send(new LogMessageDto(UUID.randomUUID(), startDttm, LocalDateTime.now(), username, "AUTH", "GATEWAY"));
//        return new ResponseEntity<>(authService.auth("Bearer " + authService.getAuthToken()), HttpStatus.OK);
//    }

    @GetMapping("/stats")
    @SneakyThrows
    public LogMessage[] getStatistics(@RequestHeader("Authorization") String authHeader) {
        String username = authService.auth(authHeader).getPrincipal();
        if (!username.equals("admin")) {
            throw new HttpClientErrorException(
                    HttpStatus.FORBIDDEN,
                    "Only admins can access stats"
            );
        }
        LocalDateTime startDttm = LocalDateTime.now();
        LogMessage[] response = new RestTemplate().exchange(
                STATS_SERVICE + GET_STATS_URL,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                LogMessage[].class).getBody();
        // send to kafka
        kafkaProducer.send(new LogMessage(UUID.randomUUID(), startDttm, LocalDateTime.now(), username, "GET_STATS", "STATS_SERVICE"));
        return response;
    }

    @PostMapping("/tickets")
    public ResponseEntity<TicketPurchaseResponse> addTicket(@RequestHeader(name = "X-User-Name") String username, @RequestBody TicketRequest ticket) throws Exception {
        // get flight by flightNumber and check if exist
        FlightResponse flight = (FlightResponse)
                circuitBreakerFactory.create("FlightServiceCircuitBreaker")
                        .run(() -> new RestTemplate().getForObject(
                                        FLIGHT_SERVICE + GET_FLIGHT_BY_NUMBER_URL,
                                        FlightResponse.class,
                                        ticket.getFlightNumber()),
                                this::flightServiceFallback);

        // add ticket to Tickets of user
        AddTicketRequest request = AddTicketRequest.build(
                username,
                ticket.getFlightNumber(),
                ticket.getPrice(),
                "PAID"
        );
        ResponseEntity<UUID> addedTicket = (ResponseEntity<UUID>)
                circuitBreakerFactory.create("TicketServiceCircuitBreaker")
                        .run(() -> new RestTemplate().postForEntity(
                                        TICKET_SERVICE + GET_TICKETS_URL,
                                        request,
                                        UUID.class),
                                this::ticketServiceFallback);

        // get ticketUid of added ticket
        UUID ticketUid = addedTicket.getBody();

        try {
            PrivilegeResponse privilege = getPrivilegeInfo(username).getBody();
            int bonusBalance = privilege.getBalance();

            int paidByMoney = ticket.getPrice();
            int paidByBonuses = 0;

            if (ticket.isPaidFromBalance()) {
                // debit all bonuses
                addHistoryRecord(ticketUid, bonusBalance, "DEBIT_THE_ACCOUNT", username);
                updateBalance(0, username);
                paidByBonuses = bonusBalance;
                paidByMoney = paidByMoney - bonusBalance;
            } else {
                // add bonus = 10% of ticket price
                addHistoryRecord(ticketUid, ticket.getPrice() / 10, "FILL_IN_BALANCE", username);
                updateBalance(bonusBalance + (ticket.getPrice() / 10), username);
            }

            // get updated balance info
            PrivilegeResponse updatedPrivilege = getPrivilegeInfo(username).getBody();

            // create response
            TicketPurchaseResponse response = TicketPurchaseResponse.build(
                    ticketUid,
                    flight.getFlightNumber(),
                    flight.getFromAirport(),
                    flight.getToAirport(),
                    flight.getDate(),
                    flight.getPrice(),
                    paidByMoney,
                    paidByBonuses,
                    "PAID",
                    updatedPrivilege
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            if (e.getClass() == HttpClientErrorException.class) {
                throw new Exception("Privilege of user " + username + " not found");
            } else if (e.getClass() == ResponseStatusException.class) {
                // delete recently added ticket
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-User-Name", username);
                RestTemplate rest = new RestTemplate();
                rest.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
                rest.exchange(
                        TICKET_SERVICE + GET_TICKET_BY_UID,
                        HttpMethod.DELETE,
                        new HttpEntity<>(headers),
                        Void.class,
                        ticketUid,
                        username
                );
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Bonus Service unavailable");
            } else {
                throw new Exception(e.getMessage());
            }
        }
    }

    @GetMapping("/flights")
    public ResponseEntity<FlightListResponse> getFlights(@RequestParam int page, @RequestParam int size) {
        return (ResponseEntity<FlightListResponse>) circuitBreakerFactory.create("FlightServiceCircuitBreaker")
                .run(() -> new RestTemplate().getForEntity(
                                FLIGHT_SERVICE + GET_FLIGHTS_URL,
                                FlightListResponse.class,
                                page - 1,
                                size),
                        this::flightServiceFallback);
    }

    private ResponseEntity<Void> flightServiceFallback(Throwable throwable) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Flight Service unavailable");
    }

    @GetMapping("/tickets")
    public TicketResponse[] getTickets(@RequestHeader(name = "X-User-Name") String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Name", username);
        TicketResponse[] tickets = (TicketResponse[])
                circuitBreakerFactory.create("TicketServiceCircuitBreaker")
                        .run(() -> new RestTemplate().exchange(
                                        TICKET_SERVICE + GET_TICKETS_URL,
                                        HttpMethod.GET,
                                        new HttpEntity<>(headers),
                                        TicketResponse[].class),
                                this::ticketServiceFallback).getBody();
        for (TicketResponse ticket : tickets) {
            FlightResponse flight = circuitBreakerFactory.create("FlightResponseCircuitBreaker")
                    .run(() -> new RestTemplate().getForEntity(
                                    FLIGHT_SERVICE + GET_FLIGHT_BY_NUMBER_URL,
                                    FlightResponse.class,
                                    ticket.getFlightNumber()).getBody(),
                            throwable -> new FlightResponse());
            ticket.setDate(flight.getDate());
            ticket.setFromAirport(flight.getFromAirport());
            ticket.setToAirport(flight.getToAirport());
        }
        return tickets;
    }

    private ResponseEntity<Void> ticketServiceFallback(Throwable throwable) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Ticket Service unavailable");
    }


    @GetMapping("/me")
    public ResponseEntity getUserInfo(@RequestHeader(name = "X-User-Name") String username) throws Exception {
        TicketResponse[] tickets = circuitBreakerFactory.create("TicketServiceCircuitBreaker")
                .run(() -> getTickets(username), throwable -> null);
        PrivilegeResponse privilege = circuitBreakerFactory.create("BonusServiceCircuitBreaker")
                .run(() -> getPrivilegeInfo(username).getBody(), throwable -> null);
        Map<String, Object> body = new HashMap<>();
        body.put("tickets", tickets);
        if (privilege != null) {
            body.put("privilege", privilege);
        } else {
            body.put("privilege", "");
        }
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    @GetMapping("/tickets/{ticketUid}")
    public TicketResponse getTicketByUid(@PathVariable UUID ticketUid, @RequestHeader(name = "X-User-Name") String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Name", username);
        TicketResponse ticket = (TicketResponse)
                circuitBreakerFactory.create("TicketServiceCircuitBreaker")
                        .run(() -> new RestTemplate().exchange(
                                TICKET_SERVICE + GET_TICKET_BY_UID,
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                TicketResponse.class,
                                ticketUid,
                                username
                        ), this::ticketServiceFallback).getBody();
        FlightResponse flight = circuitBreakerFactory.create("FlightResponseCircuitBreaker")
                .run(() -> new RestTemplate().getForEntity(
                                FLIGHT_SERVICE + GET_FLIGHT_BY_NUMBER_URL,
                                FlightResponse.class,
                                ticket.getFlightNumber()).getBody(),
                        throwable -> new FlightResponse());
        ticket.setDate(flight.getDate());
        ticket.setFromAirport(flight.getFromAirport());
        ticket.setToAirport(flight.getToAirport());
        return ticket;
    }

    @DeleteMapping("/tickets/{ticketUid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteTicketByUid(@PathVariable UUID ticketUid, @RequestHeader(name = "X-User-Name") String username) throws Exception {
        try {
            // get ticket to be deleted
            TicketResponse ticket = (TicketResponse)
                    circuitBreakerFactory.create("TicketServiceCircuitBreaker")
                            .run(() -> getTicketByUid(ticketUid, username), this::ticketServiceFallback);

            // change status to "CANCELED"
            HttpHeaders ticketHeaders = new HttpHeaders();
            ticketHeaders.set("X-User-Name", username);
            Map<String, Object> fields = new HashMap<>();
            fields.put("status", "CANCELED");
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
            circuitBreakerFactory.create("TicketServiceCircuitBreaker")
                    .run(() -> restTemplate.exchange(
                            TICKET_SERVICE + GET_TICKET_BY_UID,
                            HttpMethod.PATCH,
                            new HttpEntity<>(fields, ticketHeaders),
                            TicketResponse.class,
                            ticketUid
                    ), this::ticketServiceFallback);

            // call Bonus Service to update data in separate thread
            new Thread(() -> {
                try {
                    retryTemplate.execute(obj -> {
                        System.out.println("Call bonus service...");
                        updateDataInBonusService(ticketUid, username);
                        System.out.println("Bonus service updated!");
                        return null;
                    });
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Bonus service unavailable");
                }
            }).start();

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatusCode.valueOf(404)) {
                throw new Exception("Ticket with of user " + username + " not found");
            } else {
                throw new Exception("Unknown error" + e.getMessage());
            }
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private void updateDataInBonusService(UUID ticketUid, String username) throws Exception {
        // find privilege status
        PrivilegeResponse privilege = getPrivilegeInfo(username).getBody();
        int currentBalance = privilege.getBalance();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Name", username);

        // find history record of this ticketUid
        PrivilegeHistoryResponse privilegeHistoryResponse =
                new RestTemplate().exchange(
                        BONUS_SERVICE + GET_PRIVILEGE_HISTORY_BY_TICKET_UID_URL,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        PrivilegeHistoryResponse.class,
                        ticketUid
                ).getBody();
        String operationType = privilegeHistoryResponse.getOperationType();
        int balanceDiff = privilegeHistoryResponse.getBalanceDiff();
        int newBalance;
        // refund bonuses
        if (operationType.equals("FILL_IN_BALANCE")) {
            newBalance = currentBalance - balanceDiff;
            operationType = "DEBIT_THE_ACCOUNT";
        } else if (operationType.equals("DEBIT_THE_ACCOUNT")) {
            newBalance = currentBalance + balanceDiff;
            operationType = "FILL_IN_BALANCE";
        } else {
            throw new Exception("Unknown type of operation");
        }
        // add history record
        addHistoryRecord(ticketUid, balanceDiff, operationType, username);

        // change bonus balance of user
        Map<String, Object> balanceUpdate = new HashMap<>();
        balanceUpdate.put("balance", newBalance);
        RestTemplate rest = new RestTemplate();
        rest.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        ResponseEntity<PrivilegeResponse> updatedPrivilege = rest.exchange(
                BONUS_SERVICE + GET_PRIVILEGE_URL,
                HttpMethod.PATCH,
                new HttpEntity<>(balanceUpdate, headers),
                PrivilegeResponse.class
        );
    }

    @GetMapping("/privilege")
    public ResponseEntity<PrivilegeWithHistoryResponse> getPrivilegeWithHistory(@RequestHeader(name = "X-User-Name") String username) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Name", username);
        ResponseEntity<PrivilegeResponse> privilege = getPrivilegeInfo(username);
        ResponseEntity<PrivilegeHistoryResponse[]> historyList = getPrivilegeHistory(username);
        PrivilegeWithHistoryResponse response = PrivilegeWithHistoryResponse.build(privilege.getBody().getBalance(), privilege.getBody().getStatus(), Arrays.asList(historyList.getBody()));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/manage/health")
    public ResponseEntity<Void> status() {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private ResponseEntity<PrivilegeHistoryResponse[]> getPrivilegeHistory(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Name", username);
        return (ResponseEntity<PrivilegeHistoryResponse[]>)
                circuitBreakerFactory.create("BonusServiceCircuitBreaker")
                        .run(() -> new RestTemplate().exchange(
                                BONUS_SERVICE + GET_PRIVILEGE_HISTORY_URL,
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                PrivilegeHistoryResponse[].class
                        ), this::bonusServiceFallback);

    }

    private ResponseEntity<PrivilegeResponse> getPrivilegeInfo(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Name", username);
        return (ResponseEntity<PrivilegeResponse>)
                circuitBreakerFactory.create("BonusServiceCircuitBreaker")
                        .run(() -> new RestTemplate().exchange(
                                BONUS_SERVICE + GET_PRIVILEGE_URL,
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                PrivilegeResponse.class
                        ), this::bonusServiceFallback);
    }

    private ResponseEntity<Void> bonusServiceFallback(Throwable throwable) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Bonus Service unavailable");
    }

    private String addHistoryRecord(UUID ticketUid, int bonusAmount, String operationType, String username) {
        PrivilegeHistoryRequest request = PrivilegeHistoryRequest.build(
                ticketUid,
                bonusAmount,
                operationType
        );
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Name", username);
        HttpEntity<PrivilegeHistoryRequest> historyRecord = new HttpEntity<>(request, headers);
        ResponseEntity<Void> historyResponseEntity = new RestTemplate().postForEntity(
                BONUS_SERVICE + GET_PRIVILEGE_HISTORY_URL,
                historyRecord,
                Void.class
        );
        return historyResponseEntity.getHeaders().get("Location").toString();
    }

    private void updateBalance(int balance, String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Name", username);
        Map<String, Object> fields = new HashMap<>();
        fields.put("balance", balance);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        restTemplate.exchange(
                BONUS_SERVICE + GET_PRIVILEGE_URL,
                HttpMethod.PATCH,
                new HttpEntity<>(fields, headers),
                PrivilegeResponse.class
        );
    }
}
