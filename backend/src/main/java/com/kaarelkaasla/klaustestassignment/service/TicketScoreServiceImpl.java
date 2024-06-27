package com.kaarelkaasla.klaustestassignment.service;

import com.kaarelkaasla.klaustestassignment.TicketCategoryScore;
import com.kaarelkaasla.klaustestassignment.TicketCategoryScoresRequest;
import com.kaarelkaasla.klaustestassignment.TicketCategoryScoresResponse;
import com.kaarelkaasla.klaustestassignment.TicketScoreServiceGrpc;
import com.kaarelkaasla.klaustestassignment.entity.RatingCategory;
import com.kaarelkaasla.klaustestassignment.repository.RatingCategoryRepository;
import com.kaarelkaasla.klaustestassignment.repository.RatingRepository;
import com.kaarelkaasla.klaustestassignment.util.DateUtils;
import com.kaarelkaasla.klaustestassignment.util.MathUtils;
import com.kaarelkaasla.klaustestassignment.util.RatingCategoryUtils;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TicketScoreServiceImpl is a gRPC service implementation that provides methods for retrieving ticket category scores
 * over specified time periods.
 */
@GrpcService
@Slf4j
public class TicketScoreServiceImpl extends TicketScoreServiceGrpc.TicketScoreServiceImplBase {

    private final RatingRepository ratingRepository;
    private final RatingCategoryRepository ratingCategoryRepository;
    private final DateUtils dateUtils;
    private final RatingCategoryUtils ratingCategoryUtils;

    @Autowired
    public TicketScoreServiceImpl(RatingRepository ratingRepository, RatingCategoryRepository ratingCategoryRepository,
            DateUtils dateUtils, RatingCategoryUtils ratingCategoryUtils) {
        this.ratingRepository = ratingRepository;
        this.ratingCategoryRepository = ratingCategoryRepository;
        this.dateUtils = dateUtils;
        this.ratingCategoryUtils = ratingCategoryUtils;
    }

    /**
     * Retrieves ticket category scores for a specified period.
     *
     * @param request
     *            The request containing start and end dates.
     * @param responseObserver
     *            The response observer to send the ticket category scores.
     */
    @Override
    public void getTicketCategoryScores(TicketCategoryScoresRequest request,
            StreamObserver<TicketCategoryScoresResponse> responseObserver) {
        try {
            String startDateStr = request.getStartDate();
            String endDateStr = request.getEndDate();

            log.info("Received a gRPC request to get ticket category scores with startDate: {} and endDate: {}",
                    startDateStr, endDateStr);

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

            List<Object[]> ratingsRaw;
            try {
                ratingsRaw = ratingRepository.findRatingsWithinPeriod(dateUtils.formatDate(startDate),
                        dateUtils.formatDate(endDate));
            } catch (Exception e) {
                log.error("Database query failed", e);
                responseObserver.onError(Status.INTERNAL.withDescription("Failed to retrieve data from database")
                        .withCause(e).asRuntimeException());
                return;
            }

            if (ratingsRaw.isEmpty()) {
                log.info("No ratings found for the specified period.");
                responseObserver.onError(Status.NOT_FOUND.withDescription("No ratings found for the specified period.")
                        .asRuntimeException());
                return;
            }

            Map<Long, String> categoryIdToNameMap = ratingCategoryUtils.getCategoryIdToNameMap();
            Map<Integer, Map<String, List<Integer>>> ticketCategoryRatingsMap = getTicketCategoryRatingsMap(ratingsRaw,
                    categoryIdToNameMap);

            List<TicketCategoryScore> ticketCategoryScoresList = buildTicketCategoryScoresList(
                    ticketCategoryRatingsMap);

            TicketCategoryScoresResponse response = TicketCategoryScoresResponse.newBuilder()
                    .addAllTicketCategoryScores(ticketCategoryScoresList).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("Successfully sent ticket category scores response");
        } catch (Exception e) {
            log.error("Unexpected error occurred", e);
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Internal server error").withCause(e).asRuntimeException());
        }
    }

    /**
     * Creates a map of ticket IDs to category ratings from raw rating data.
     *
     * @param ratingsRaw
     *            The raw rating data.
     * @param categoryIdToNameMap
     *            A map from category IDs to category names.
     *
     * @return A map from ticket IDs to category ratings.
     */
    private Map<Integer, Map<String, List<Integer>>> getTicketCategoryRatingsMap(List<Object[]> ratingsRaw,
            Map<Long, String> categoryIdToNameMap) {
        return ratingsRaw.stream()
                .collect(Collectors.groupingBy(row -> ((Number) row[0]).intValue(),
                        Collectors.groupingBy(row -> categoryIdToNameMap.get(((Number) row[1]).longValue()),
                                Collectors.mapping(row -> ((Number) row[2]).intValue(), Collectors.toList()))));
    }

    /**
     * Builds a list of TicketCategoryScore objects from the given ticket category ratings map.
     *
     * @param ticketCategoryRatingsMap
     *            A map from ticket IDs to category ratings.
     *
     * @return A list of TicketCategoryScore objects.
     */
    private List<TicketCategoryScore> buildTicketCategoryScoresList(
            Map<Integer, Map<String, List<Integer>>> ticketCategoryRatingsMap) {
        return ticketCategoryRatingsMap.entrySet().stream().map(entry -> {
            Integer ticketId = entry.getKey();
            Map<String, Double> averageScores = entry.getValue().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> MathUtils.roundToTwoDecimalPlaces(
                            e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0) * 20)));

            return TicketCategoryScore.newBuilder().setTicketId(ticketId).putAllCategoryScores(averageScores).build();
        }).toList();
    }

    /**
     * Processes the TicketCategoryScoresResponse into a list of maps for each ticket.
     *
     * @param response
     *            The response containing ticket category scores.
     *
     * @return A list of maps containing ticket data.
     */
    public List<Map<String, Object>> processTicketCategoryScores(TicketCategoryScoresResponse response) {
        List<String> allCategoryNames = ratingCategoryRepository.findAll().stream().map(RatingCategory::getName)
                .toList();

        return response.getTicketCategoryScoresList().stream()
                .sorted(Comparator.comparingInt(TicketCategoryScore::getTicketId)).map(score -> {
                    Map<String, Object> ticketData = new HashMap<>();
                    ticketData.put("TicketId", score.getTicketId());

                    Map<String, Double> categoryScores = allCategoryNames.stream()
                            .collect(Collectors.toMap(category -> category,
                                    category -> score.getCategoryScoresMap().getOrDefault(category, 0.0)));

                    ticketData.put("CategoryScores", categoryScores);
                    return ticketData;
                }).toList();
    }
}
