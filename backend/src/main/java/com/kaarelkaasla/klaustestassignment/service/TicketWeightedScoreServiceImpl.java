package com.kaarelkaasla.klaustestassignment.service;

import com.kaarelkaasla.klaustestassignment.*;
import com.kaarelkaasla.klaustestassignment.entity.RatingCategory;
import com.kaarelkaasla.klaustestassignment.repository.RatingCategoryRepository;
import com.kaarelkaasla.klaustestassignment.repository.RatingRepository;
import com.kaarelkaasla.klaustestassignment.util.DateUtils;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TicketWeightedScoreServiceImpl is a gRPC service implementation that provides methods for aggregating overall
 * weighted ratings for both the specified period and an equal length period preceding it.
 */
@GrpcService
@Slf4j
public class TicketWeightedScoreServiceImpl extends TicketWeightedScoreServiceGrpc.TicketWeightedScoreServiceImplBase {

    private static final DecimalFormat df = new DecimalFormat("0.00");
    private final RatingRepository ratingRepository;
    private final RatingCategoryRepository ratingCategoryRepository;
    private final ScoreService scoreService;
    private final DateUtils dateUtils;

    @Autowired
    public TicketWeightedScoreServiceImpl(RatingRepository ratingRepository,
            RatingCategoryRepository ratingCategoryRepository, ScoreService scoreService, DateUtils dateUtils) {
        this.ratingRepository = ratingRepository;
        this.ratingCategoryRepository = ratingCategoryRepository;
        this.scoreService = scoreService;
        this.dateUtils = dateUtils;
    }

    /**
     * Retrieves weighted scores for a specified period and optionally for the preceding period.
     *
     * @param request
     *            The request containing start and end dates and the flag to include the previous period.
     * @param responseObserver
     *            The response observer to send the weighted scores.
     */
    @Override
    public void getWeightedScores(WeightedScoresRequest request,
            StreamObserver<WeightedScoresResponse> responseObserver) {
        try {
            String startDateStr = request.getStartDate();
            String endDateStr = request.getEndDate();

            log.info("Received a gRPC request to get weighted scores with startDate: {} and endDate: {}", startDateStr,
                    endDateStr);

            Date startDate;
            Date endDate;
            try {
                startDate = dateUtils.parseDate(startDateStr);
                endDate = dateUtils.parseDate(endDateStr);
            } catch (ParseException e) {
                log.warn("Error parsing dates: {}", e.getMessage());
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid date format").withCause(e)
                        .asRuntimeException());
                return;
            }

            String startDateString = dateUtils.formatDate(startDate);
            String endDateString = dateUtils.formatDate(endDate);

            double averageScore = calculateAverageScore(startDateString, endDateString);
            String currentPeriod = startDateString + " to " + endDateString;

            PeriodScore.Builder currentPeriodScoreBuilder = PeriodScore.newBuilder().setPeriod(currentPeriod)
                    .setAverageScorePercentage(Double.parseDouble(df.format(averageScore)));

            if (averageScore == 0) {
                currentPeriodScoreBuilder.setMessage("N/A");
            }

            WeightedScoresResponse.Builder responseBuilder = WeightedScoresResponse.newBuilder()
                    .setCurrentPeriodScore(currentPeriodScoreBuilder.build());

            if (request.getIncludePreviousPeriod()) {
                LocalDate startLocalDate = LocalDate.parse(startDateStr.substring(0, 10));
                LocalDate endLocalDate = LocalDate.parse(endDateStr.substring(0, 10));
                long daysBetween = ChronoUnit.DAYS.between(startLocalDate, endLocalDate);

                LocalDate previousStartLocalDate = startLocalDate.minusDays(daysBetween + 1);
                LocalDate previousEndLocalDate = startLocalDate.minusDays(1);

                String previousStartDateString = previousStartLocalDate.atStartOfDay()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                String previousEndDateString = previousEndLocalDate.atTime(23, 59, 59)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

                double previousPeriodAverageScore = calculateAverageScore(previousStartDateString,
                        previousEndDateString);
                String previousPeriod = previousStartDateString + " to " + previousEndDateString;

                PeriodScore.Builder previousPeriodScoreBuilder = PeriodScore.newBuilder().setPeriod(previousPeriod)
                        .setAverageScorePercentage(Double.parseDouble(df.format(previousPeriodAverageScore)));

                if (previousPeriodAverageScore == 0) {
                    previousPeriodScoreBuilder.setMessage("N/A");
                }

                responseBuilder.setPreviousPeriodScore(previousPeriodScoreBuilder.build());

                ScoreChange.Builder scoreChangeBuilder = ScoreChange.newBuilder();
                if (averageScore != 0 && previousPeriodAverageScore != 0) {
                    double scoreChange = averageScore - previousPeriodAverageScore;
                    scoreChangeBuilder.setValue(Double.parseDouble(df.format(scoreChange)));
                } else {
                    scoreChangeBuilder.setMessage("N/A");
                }
                responseBuilder.setScoreChange(scoreChangeBuilder.build());
            }
            WeightedScoresResponse response = responseBuilder.build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("Successfully sent weighted scores response");
        } catch (Exception e) {
            log.error("Unexpected error occurred", e);
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Internal server error").withCause(e).asRuntimeException());
        }
    }

    /**
     * Calculates the average score for the specified period.
     *
     * @param startDate
     *            The start date of the period.
     * @param endDate
     *            The end date of the period.
     *
     * @return The average score for the period.
     */
    private double calculateAverageScore(String startDate, String endDate) {
        List<Object[]> ratingsRaw;
        try {
            ratingsRaw = ratingRepository.findRatingsWithinPeriod(startDate, endDate);
        } catch (Exception e) {
            log.error("Database query failed", e);
            throw new RuntimeException("Failed to retrieve data from database", e);
        }

        if (ratingsRaw.isEmpty()) {
            return 0;
        }

        Map<Long, String> categoryIdToNameMap;
        try {
            categoryIdToNameMap = ratingCategoryRepository.findAll().stream()
                    .collect(Collectors.toMap(RatingCategory::getId, RatingCategory::getName));
        } catch (Exception e) {
            log.error("Failed to retrieve rating categories from the database", e);
            throw new RuntimeException("Failed to retrieve rating categories from the database", e);
        }

        Map<Integer, Map<String, Integer>> ticketCategoryRatingsMap = new HashMap<>();

        for (Object[] row : ratingsRaw) {
            Integer ticketId = ((Number) row[0]).intValue();
            Long categoryId = ((Number) row[1]).longValue();
            Integer rating = ((Number) row[2]).intValue();

            String categoryName = categoryIdToNameMap.get(categoryId);
            if (categoryName == null) {
                log.warn("Category ID {} not found in the map", categoryId);
                continue;
            }

            ticketCategoryRatingsMap.computeIfAbsent(ticketId, k -> new HashMap<>()).put(categoryName, rating);
        }

        List<Double> ticketScores = ticketCategoryRatingsMap.values().stream().map(scoreService::calculateScore)
                .toList();

        return ticketScores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }
}
