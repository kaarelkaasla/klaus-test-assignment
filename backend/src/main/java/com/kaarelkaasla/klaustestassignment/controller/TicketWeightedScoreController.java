package com.kaarelkaasla.klaustestassignment.controller;

import com.kaarelkaasla.klaustestassignment.TicketWeightedScoreServiceGrpc;
import com.kaarelkaasla.klaustestassignment.WeightedScoresRequest;
import com.kaarelkaasla.klaustestassignment.WeightedScoresResponse;
import com.kaarelkaasla.klaustestassignment.util.DateUtils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Controller for handling weighted score-related API requests.
 */
@RestController
@RequestMapping("/api/v1/tickets")
@Slf4j
public class TicketWeightedScoreController {

    @Value("${grpc.server.host}")
    private String grpcServerHost;

    @Value("${grpc.server.port}")
    private int grpcServerPort;

    @Value("${api.key-header}")
    private String apiKeyHeader;

    @Value("${api.key}")
    private String apiKey;

    private final DateUtils dateUtils;

    @Autowired
    public TicketWeightedScoreController(DateUtils dateUtils) {
        this.dateUtils = dateUtils;
    }

    @GetMapping("/weighted-scores")
    public ResponseEntity<Object> getWeightedScores(@RequestParam String startDate, @RequestParam String endDate,
            @RequestParam(required = false, defaultValue = "false") String includePreviousPeriod,
            @RequestHeader(value = "${api.key-header}", required = false) String requestApiKey) {

        log.info(
                "Received an API request to get weighted scores with startDate: {} and endDate: {}, includePreviousPeriod: {}",
                startDate, endDate, includePreviousPeriod);

        if (!includePreviousPeriod.equals("true") && !includePreviousPeriod.equals("false")) {
            log.warn("Invalid value for includePreviousPeriod: {}", includePreviousPeriod);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid value for includePreviousPeriod. Must be true or false.");
        }

        boolean includePrevious = Boolean.parseBoolean(includePreviousPeriod);

        if (requestApiKey == null || !requestApiKey.equals(apiKey)) {
            log.warn("Unauthorized access attempt with invalid API key.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing API key.");
        }

        try {
            LocalDateTime startDateTime = dateUtils.parseDateTime(startDate);
            LocalDateTime endDateTime = dateUtils.parseDateTime(endDate);

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

                TicketWeightedScoreServiceGrpc.TicketWeightedScoreServiceBlockingStub stub = TicketWeightedScoreServiceGrpc
                        .newBlockingStub(channel);
                stub = MetadataUtils.attachHeaders(stub, metadata);

                WeightedScoresRequest request = WeightedScoresRequest.newBuilder()
                        .setStartDate(startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .setEndDate(endDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .setIncludePreviousPeriod(includePrevious).build();

                WeightedScoresResponse response = stub.getWeightedScores(request);

                if (response.getCurrentPeriodScore().getAverageScorePercentage() == 0
                        && response.getPreviousPeriodScore().getAverageScorePercentage() == 0) {
                    log.info("No ratings found for the specified periods.");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("No ratings found for the specified periods.");
                }

                log.info("Successfully retrieved weighted scores");
                return ResponseEntity.ok(response);
            } catch (StatusRuntimeException e) {
                Status status = e.getStatus();
                return switch (status.getCode()) {
                case NOT_FOUND -> {
                    log.info("No ratings found for the specified periods.");
                    yield ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("No ratings found for the specified periods.");
                }
                case INVALID_ARGUMENT -> {
                    log.warn("Invalid argument: {}", e.getMessage());
                    yield ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid argument.");
                }
                case UNAUTHENTICATED -> {
                    log.warn("Unauthenticated request: {}", e.getMessage());
                    yield ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthenticated request.");
                }
                case INTERNAL -> {
                    log.error("Internal server error: {}", e.getMessage());
                    yield ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error.");
                }
                default -> {
                    log.error("Unexpected gRPC error: {}", e.getMessage());
                    yield ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error.");
                }
                };
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }
}
