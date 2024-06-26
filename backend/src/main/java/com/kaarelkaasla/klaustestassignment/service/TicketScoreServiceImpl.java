package com.kaarelkaasla.klaustestassignment.service;

import com.kaarelkaasla.klaustestassignment.TicketCategoryScore;
import com.kaarelkaasla.klaustestassignment.TicketCategoryScoresRequest;
import com.kaarelkaasla.klaustestassignment.TicketCategoryScoresResponse;
import com.kaarelkaasla.klaustestassignment.TicketScoreServiceGrpc;
import com.kaarelkaasla.klaustestassignment.entity.RatingCategory;
import com.kaarelkaasla.klaustestassignment.repository.RatingCategoryRepository;
import com.kaarelkaasla.klaustestassignment.repository.RatingRepository;
import com.kaarelkaasla.klaustestassignment.util.RatingUtils;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TicketScoreServiceImpl is a gRPC service implementation that provides methods for aggregating rating category scores
 * for tickets by IDs.
 */
@GrpcService
@Slf4j
public class TicketScoreServiceImpl extends TicketScoreServiceGrpc.TicketScoreServiceImplBase {

    private final SimpleDateFormat requestDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private final RatingRepository ratingRepository;
    private final RatingCategoryRepository ratingCategoryRepository;

    @Autowired
    public TicketScoreServiceImpl(RatingRepository ratingRepository,
            RatingCategoryRepository ratingCategoryRepository) {
        this.ratingRepository = ratingRepository;
        this.ratingCategoryRepository = ratingCategoryRepository;
    }

    /**
     * Processes the ticket category scores response.
     *
     * @param response
     *            The gRPC response containing ticket category scores.
     *
     * @return A list of maps containing ticket IDs and their respective category scores.
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

    /**
     * Retrieves ticket category scores for the specified period.
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
                startDate = parseDate(startDateStr);
                endDate = parseDate(endDateStr);
            } catch (ParseException e) {
                log.warn("Error parsing dates: {}", e.getMessage());
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid date format").withCause(e)
                        .asRuntimeException());
                return;
            }

            List<Object[]> ratingsRaw;
            try {
                ratingsRaw = ratingRepository.findRatingsWithinPeriod(formatDate(startDate), formatDate(endDate));
            } catch (Exception e) {
                log.error("Database query failed", e);
                responseObserver.onError(Status.INTERNAL.withDescription("Failed to retrieve data from database")
                        .withCause(e).asRuntimeException());
                return;
            }

            Map<Long, String> categoryIdToNameMap = getCategoryIdToNameMap();
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
     * Parses a date string to a Date object.
     *
     * @param date
     *            The date string to parse.
     *
     * @return The parsed Date object.
     *
     * @throws ParseException
     *             If the date string is invalid.
     */
    private Date parseDate(String date) throws ParseException {
        return requestDateFormat.parse(date);
    }

    /**
     * Formats a Date object to a string.
     *
     * @param date
     *            The Date object to format.
     *
     * @return The formatted date string.
     */
    private String formatDate(Date date) {
        return dbDateFormat.format(date);
    }

    /**
     * Retrieves a map from category IDs to category names.
     *
     * @return The map from category IDs to category names.
     */
    private Map<Long, String> getCategoryIdToNameMap() {
        return ratingCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(RatingCategory::getId, RatingCategory::getName));
    }

    /**
     * Retrieves a map from ticket IDs to their category ratings.
     *
     * @param ratingsRaw
     *            The raw ratings data from the database.
     * @param categoryIdToNameMap
     *            The map from category IDs to category names.
     *
     * @return The map from ticket IDs to their category ratings.
     */
    private Map<Integer, Map<String, List<Integer>>> getTicketCategoryRatingsMap(List<Object[]> ratingsRaw,
            Map<Long, String> categoryIdToNameMap) {
        return ratingsRaw.stream()
                .collect(Collectors.groupingBy(row -> ((Number) row[0]).intValue(),
                        Collectors.groupingBy(row -> categoryIdToNameMap.get(((Number) row[1]).longValue()),
                                Collectors.mapping(row -> ((Number) row[2]).intValue(), Collectors.toList()))));
    }

    /**
     * Builds a list of TicketCategoryScores from the given map.
     *
     * @param ticketCategoryRatingsMap
     *            The map from ticket IDs to their category ratings.
     *
     * @return The list of TicketCategoryScores.
     */
    private List<TicketCategoryScore> buildTicketCategoryScoresList(
            Map<Integer, Map<String, List<Integer>>> ticketCategoryRatingsMap) {
        return ticketCategoryRatingsMap.entrySet().stream().map(entry -> {
            Integer ticketId = entry.getKey();
            Map<String, Double> averageScores = entry.getValue().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> RatingUtils.roundToTwoDecimalPlaces(
                            e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0) * 20)));

            return TicketCategoryScore.newBuilder().setTicketId(ticketId).putAllCategoryScores(averageScores).build();
        }).toList();
    }
}
