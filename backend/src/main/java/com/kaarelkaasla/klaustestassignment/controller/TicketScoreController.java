package com.kaarelkaasla.klaustestassignment.controller;

import com.kaarelkaasla.klaustestassignment.TicketCategoryScoresRequest;
import com.kaarelkaasla.klaustestassignment.TicketCategoryScoresResponse;
import com.kaarelkaasla.klaustestassignment.TicketScoreServiceGrpc;
import com.kaarelkaasla.klaustestassignment.service.TicketScoreServiceImpl;
import com.kaarelkaasla.klaustestassignment.util.RatingUtils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Controller for handling ticket-related API requests.
 */
@RestController
@RequestMapping("/api/v1/tickets")
@Slf4j
public class TicketScoreController {

    @Value("${grpc.server.host}")
    private String grpcServerHost;

    @Value("${grpc.server.port}")
    private int grpcServerPort;

    @Value("${api.key-header}")
    private String apiKeyHeader;

    @Value("${api.key}")
    private String apiKey;

    private final TicketScoreServiceImpl ticketService;
    private final RatingUtils ratingUtils;

    @Autowired
    public TicketScoreController(TicketScoreServiceImpl ticketService, RatingUtils ratingUtils) {
        this.ticketService = ticketService;
        this.ratingUtils = ratingUtils;
    }

    /**
     * Retrieves category scores for tickets created within the specified date range.
     *
     * @param startDate
     *            The start date of the period in ISO 8601 format.
     * @param endDate
     *            The end date of the period in ISO 8601 format.
     * @param requestApiKey
     *            The API key for authentication.
     *
     * @return A list of maps containing ticket IDs and their respective category scores.
     */
    @GetMapping("/category-scores")
    public ResponseEntity<Object> getTicketCategoryScores(@RequestParam String startDate, @RequestParam String endDate,
            @RequestHeader(value = "${api.key-header}", required = false) String requestApiKey) {

        log.info("Received an API request to get ticket category scores with startDate: {} and endDate: {}", startDate,
                endDate);

        if (requestApiKey == null || !requestApiKey.equals(apiKey)) {
            log.warn("Unauthorized access attempt with invalid API key.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing API key.");
        }

        try {
            LocalDateTime startDateTime = ratingUtils.parseDateTime(startDate);
            LocalDateTime endDateTime = ratingUtils.parseDateTime(endDate);

            if (startDateTime.isAfter(endDateTime)) {
                log.warn("Start date {} is after end date {}", startDateTime, endDateTime);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Start date must be earlier than or equal to end date.");
            }

            ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcServerHost, grpcServerPort).usePlaintext()
                    .build();

            try {
                Metadata metadata = new Metadata();
                Metadata.Key<String> apiKeyHeader = Metadata.Key.of(this.apiKeyHeader,
                        Metadata.ASCII_STRING_MARSHALLER);
                metadata.put(apiKeyHeader, apiKey);

                TicketScoreServiceGrpc.TicketScoreServiceBlockingStub stub = TicketScoreServiceGrpc
                        .newBlockingStub(channel);
                stub = MetadataUtils.attachHeaders(stub, metadata);

                TicketCategoryScoresRequest request = TicketCategoryScoresRequest.newBuilder()
                        .setStartDate(startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .setEndDate(endDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).build();

                TicketCategoryScoresResponse response = stub.getTicketCategoryScores(request);

                if (response.getTicketCategoryScoresList().isEmpty()) {
                    log.info("No ratings found for the specified period.");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("No ratings found for the specified period.");
                }

                List<Map<String, Object>> result = ticketService.processTicketCategoryScores(response);
                log.info("Successfully retrieved ticket category scores");
                return ResponseEntity.ok(result);

            } finally {
                channel.shutdown();
                log.debug("gRPC channel shut down");
            }
        } catch (DateTimeParseException e) {
            log.warn("Invalid date format: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid date format. Please use the format yyyy-MM-dd'T'HH:mm:ss.");
        } catch (Exception e) {
            log.error("An unexpected error occurred", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", e);
        }
    }
}
