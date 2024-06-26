package com.kaarelkaasla.klaustestassignment.controller;

import com.kaarelkaasla.klaustestassignment.*;
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

/**
 * Controller for handling rating-related API requests.
 */
@RestController
@RequestMapping("/api/v1/scores")
@Slf4j
public class RatingController {

    @Value("${grpc.server.host}")
    private String grpcServerHost;

    @Value("${grpc.server.port}")
    private int grpcServerPort;

    @Value("${api.key-header}")
    private String apiKeyHeader;

    @Value("${api.key}")
    private String apiKey;

    private final RatingUtils ratingUtils;

    @Autowired
    public RatingController(RatingUtils ratingUtils) {
        this.ratingUtils = ratingUtils;
    }

    /**
     * Retrieves aggregated scores within the specified date range.
     *
     * @param requestApiKey
     *            The API key for authentication.
     * @param startDate
     *            The start date of the period in ISO 8601 format.
     * @param endDate
     *            The end date of the period in ISO 8601 format.
     *
     * @return The aggregated scores.
     */
    @GetMapping("/aggregated")
    public ResponseEntity<Object> getAggregatedScores(
            @RequestHeader(value = "${api.key-header}", required = false) String requestApiKey,
            @RequestParam String startDate, @RequestParam String endDate) {

        log.info("Received an API request to get aggregated scores with startDate: {} and endDate: {}", startDate,
                endDate);

        if (requestApiKey == null || !requestApiKey.equals(apiKey)) {
            log.warn("Unauthorized access attempt with invalid API key.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid API key.");
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
                metadata.put(apiKeyHeader, requestApiKey);

                RatingServiceGrpc.RatingServiceBlockingStub stub = RatingServiceGrpc.newBlockingStub(channel);
                stub = MetadataUtils.attachHeaders(stub, metadata);

                AggregatedScoresRequest request = AggregatedScoresRequest.newBuilder()
                        .setStartDate(startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .setEndDate(endDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).build();

                AggregatedScoresResponse response = stub.getAggregatedScores(request);
                log.info("Successfully retrieved aggregated scores");
                return ResponseEntity.ok(response);

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
