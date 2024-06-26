package com.kaarelkaasla.klaustestassignment.service;

import com.kaarelkaasla.klaustestassignment.*;
import com.kaarelkaasla.klaustestassignment.repository.RatingRepository;
import com.kaarelkaasla.klaustestassignment.util.RatingUtils;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RatingServiceImpl is a gRPC service implementation that provides methods for aggregating category rating scores over
 * specified time periods.
 */
@GrpcService
@Slf4j
public class RatingServiceImpl extends RatingServiceGrpc.RatingServiceImplBase {

    private final RatingRepository ratingRepository;
    private final RatingUtils ratingUtils;

    @Autowired
    public RatingServiceImpl(RatingRepository ratingRepository, RatingUtils ratingUtils) {
        this.ratingRepository = ratingRepository;
        this.ratingUtils = ratingUtils;
    }

    /**
     * Aggregates category rating scores over specified time periods.
     *
     * @param request
     *            The request containing start and end dates.
     * @param responseObserver
     *            The response observer to send the aggregated scores.
     */
    @Override
    public void getAggregatedScores(AggregatedScoresRequest request,
            StreamObserver<AggregatedScoresResponse> responseObserver) {
        try {
            String startDateStr = request.getStartDate();
            String endDateStr = request.getEndDate();

            log.info("Received a gRPC request to get aggregated scores with startDate: {} and endDate: {}",
                    startDateStr, endDateStr);

            if (!ratingUtils.isValidDate(startDateStr) || !ratingUtils.isValidDate(endDateStr)) {
                log.warn("Invalid date format for startDate: {} or endDate: {}", startDateStr, endDateStr);
                responseObserver
                        .onError(Status.INVALID_ARGUMENT.withDescription("Invalid date format").asRuntimeException());
                return;
            }

            long daysBetween;
            try {
                daysBetween = ratingUtils.getDaysBetween(startDateStr, endDateStr);
            } catch (DateTimeParseException e) {
                log.warn("DateTimeParseException while calculating days between dates: {}", e.getMessage());
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid date format").withCause(e)
                        .asRuntimeException());
                return;
            }

            List<Object[]> aggregatedRatingsRaw;
            try {
                aggregatedRatingsRaw = (daysBetween > 31
                        || ratingUtils.isDifferentMonthOrYear(startDateStr, endDateStr))
                                ? ratingRepository.findWeeklyAggregatedRatingsBetween(startDateStr, endDateStr)
                                : ratingRepository.findAggregatedRatingsBetween(startDateStr, endDateStr);
            } catch (Exception e) {
                log.error("Database query failed", e);
                responseObserver.onError(Status.INTERNAL.withDescription("Failed to retrieve data from database")
                        .withCause(e).asRuntimeException());
                return;
            }

            Map<Long, String> categoryIdToNameMap = ratingUtils.getCategoryIdToNameMap();

            Map<String, CategoryRatingResult> categoryResultsMap = processAggregatedRatings(aggregatedRatingsRaw,
                    categoryIdToNameMap, endDateStr);

            AggregatedScoresResponse response = buildAggregatedScoresResponse(categoryResultsMap);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("Successfully sent aggregated scores response");
        } catch (Exception e) {
            log.error("Unexpected error occurred", e);
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Internal server error").withCause(e).asRuntimeException());
        }
    }

    /**
     * Processes raw aggregated ratings into a map of category rating results.
     *
     * @param aggregatedRatingsRaw
     *            The raw aggregated ratings from the database.
     * @param categoryIdToNameMap
     *            A map from category IDs to category names.
     * @param endDateString
     *            The end date string for adjusting periods.
     *
     * @return A map from category names to their rating results.
     */
    private Map<String, CategoryRatingResult> processAggregatedRatings(List<Object[]> aggregatedRatingsRaw,
            Map<Long, String> categoryIdToNameMap, String endDateString) {
        return aggregatedRatingsRaw.stream().map(row -> processRow(row, categoryIdToNameMap, endDateString)).collect(
                Collectors.toMap(CategoryRatingResult::getCategoryName, result -> result, this::mergeCategoryResults));
    }

    /**
     * Processes a single row of raw aggregated ratings.
     *
     * @param row
     *            A single row from the raw aggregated ratings.
     * @param categoryIdToNameMap
     *            A map from category IDs to category names.
     * @param endDateString
     *            The end date string for adjusting periods.
     *
     * @return A CategoryRatingResult representing the processed row.
     */
    private CategoryRatingResult processRow(Object[] row, Map<Long, String> categoryIdToNameMap, String endDateString) {
        String period = adjustPeriodForEndDate(row[0].toString(), endDateString);
        Long categoryId = ((Number) row[1]).longValue();
        int frequency = ((Number) row[2]).intValue();
        double averageRating = ((Number) row[3]).doubleValue();
        String categoryName = categoryIdToNameMap.getOrDefault(categoryId, "Unknown Category");
        if ("Unknown Category".equals(categoryName)) {
            log.warn("Category ID {} not found in categoryIdToNameMap", categoryId);
        }
        double averageScorePercentage = RatingUtils.roundToTwoDecimalPlaces((averageRating / 5) * 100);

        PeriodScore periodScore = PeriodScore.newBuilder().setPeriod(period)
                .setAverageScorePercentage(averageScorePercentage).build();
        return CategoryRatingResult.newBuilder().setCategoryName(categoryName).setFrequency(frequency)
                .setOverallAverageScorePercentage(averageScorePercentage).addPeriodScores(periodScore).build();
    }

    /**
     * Adjusts the period string for the end date if necessary.
     *
     * @param period
     *            The original period string.
     * @param endDateString
     *            The end date string.
     *
     * @return The adjusted period string.
     */
    private String adjustPeriodForEndDate(String period, String endDateString) {
        if (period.contains(" to ")) {
            String[] dates = period.split(" to ");
            String endOfWeek = dates[1];
            if (endOfWeek.equals(endDateString)) {
                return dates[0] + " to " + endDateString;
            }
        }
        return period;
    }

    /**
     * Merges two CategoryRatingResults into one.
     *
     * @param result1
     *            The first CategoryRatingResult.
     * @param result2
     *            The second CategoryRatingResult.
     *
     * @return The merged CategoryRatingResult.
     */
    private CategoryRatingResult mergeCategoryResults(CategoryRatingResult result1, CategoryRatingResult result2) {
        List<PeriodScore> mergedScores = new ArrayList<>(result1.getPeriodScoresList());
        mergedScores.addAll(result2.getPeriodScoresList());

        int totalFrequency = result1.getFrequency() + result2.getFrequency();
        double totalScoreSum = result1.getOverallAverageScorePercentage() * result1.getFrequency()
                + result2.getOverallAverageScorePercentage() * result2.getFrequency();
        double overallAverageScorePercentage = RatingUtils.roundToTwoDecimalPlaces(totalScoreSum / totalFrequency);

        return CategoryRatingResult.newBuilder().setCategoryName(result1.getCategoryName()).setFrequency(totalFrequency)
                .setOverallAverageScorePercentage(overallAverageScorePercentage).addAllPeriodScores(mergedScores)
                .build();
    }

    /**
     * Builds the final AggregatedScoresResponse.
     *
     * @param categoryResultsMap
     *            A map from category names to their rating results.
     *
     * @return The AggregatedScoresResponse.
     */
    private AggregatedScoresResponse buildAggregatedScoresResponse(
            Map<String, CategoryRatingResult> categoryResultsMap) {
        AggregatedScoresResponse.Builder responseBuilder = AggregatedScoresResponse.newBuilder();

        categoryResultsMap.values().forEach(responseBuilder::addCategoryRatingResults);

        return responseBuilder.build();
    }
}
