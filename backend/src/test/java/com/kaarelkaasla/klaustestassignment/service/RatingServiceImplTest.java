package com.kaarelkaasla.klaustestassignment.service;

import com.kaarelkaasla.klaustestassignment.*;
import com.kaarelkaasla.klaustestassignment.repository.RatingRepository;
import com.kaarelkaasla.klaustestassignment.util.DateUtils;
import com.kaarelkaasla.klaustestassignment.util.RatingCategoryUtils;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class RatingServiceImplTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private RatingCategoryUtils ratingCategoryUtils;

    @InjectMocks
    private RatingServiceImpl ratingService;

    private MockedStatic<DateUtils> dateUtilsMockedStatic;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        dateUtilsMockedStatic = mockStatic(DateUtils.class);
    }

    @AfterEach
    public void tearDown() {
        dateUtilsMockedStatic.close();
        Mockito.reset(ratingRepository, ratingCategoryUtils);
    }

    /**
     * Tests that getAggregatedScores handles an invalid start date.
     */
    @Test
    public void testGetAggregatedScores_InvalidStartDate() {
        AggregatedScoresRequest request = AggregatedScoresRequest.newBuilder().setStartDate("invalid-date")
                .setEndDate("2023-12-31T23:59:59").build();

        StreamObserver<AggregatedScoresResponse> responseObserver = mock(StreamObserver.class);

        dateUtilsMockedStatic.when(() -> DateUtils.isValidDate("invalid-date")).thenReturn(false);

        ratingService.getAggregatedScores(request, responseObserver);

        verify(responseObserver).onError(any(Throwable.class));
    }

    /**
     * Tests that getAggregatedScores handles an invalid end date.
     */
    @Test
    public void testGetAggregatedScores_InvalidEndDate() {
        AggregatedScoresRequest request = AggregatedScoresRequest.newBuilder().setStartDate("2023-01-01T00:00:00")
                .setEndDate("invalid-date").build();

        StreamObserver<AggregatedScoresResponse> responseObserver = mock(StreamObserver.class);

        dateUtilsMockedStatic.when(() -> DateUtils.isValidDate("2023-01-01T00:00:00")).thenReturn(true);
        dateUtilsMockedStatic.when(() -> DateUtils.isValidDate("invalid-date")).thenReturn(false);

        ratingService.getAggregatedScores(request, responseObserver);

        verify(responseObserver).onError(any(Throwable.class));
    }

    /**
     * Tests that getAggregatedScores handles a DateTimeParseException.
     */
    @Test
    public void testGetAggregatedScores_DateParseException() {
        AggregatedScoresRequest request = AggregatedScoresRequest.newBuilder().setStartDate("2023-01-01T00:00:00")
                .setEndDate("2023-12-31T23:59:59").build();

        StreamObserver<AggregatedScoresResponse> responseObserver = mock(StreamObserver.class);

        dateUtilsMockedStatic.when(() -> DateUtils.isValidDate(anyString())).thenReturn(true);
        dateUtilsMockedStatic.when(() -> DateUtils.getDaysBetween(anyString(), anyString()))
                .thenThrow(DateTimeParseException.class);

        ratingService.getAggregatedScores(request, responseObserver);

        verify(responseObserver).onError(any(Throwable.class));
    }

    /**
     * Tests that getAggregatedScores handles a database query exception.
     */
    @Test
    public void testGetAggregatedScores_DatabaseQueryException() {
        AggregatedScoresRequest request = AggregatedScoresRequest.newBuilder().setStartDate("2023-01-01T00:00:00")
                .setEndDate("2023-01-15T23:59:59").build();

        StreamObserver<AggregatedScoresResponse> responseObserver = mock(StreamObserver.class);

        dateUtilsMockedStatic.when(() -> DateUtils.isValidDate(anyString())).thenReturn(true);
        dateUtilsMockedStatic.when(() -> DateUtils.getDaysBetween(anyString(), anyString())).thenReturn(14L);
        when(ratingRepository.findAggregatedRatingsBetween(anyString(), anyString())).thenThrow(RuntimeException.class);

        ratingService.getAggregatedScores(request, responseObserver);

        verify(responseObserver).onError(any(Throwable.class));
    }

    /**
     * Tests that getAggregatedScores successfully retrieves and processes data.
     */
    @Test
    public void testGetAggregatedScores_Success() {
        AggregatedScoresRequest request = AggregatedScoresRequest.newBuilder().setStartDate("2023-01-01T00:00:00")
                .setEndDate("2023-01-15T23:59:59").build();

        StreamObserver<AggregatedScoresResponse> responseObserver = mock(StreamObserver.class);

        dateUtilsMockedStatic.when(() -> DateUtils.isValidDate(anyString())).thenReturn(true);
        dateUtilsMockedStatic.when(() -> DateUtils.getDaysBetween(anyString(), anyString())).thenReturn(14L);

        List<Object[]> aggregatedRatingsRaw = Arrays.asList(new Object[] { "2023-01-01 to 2023-01-07", 1L, 10, 4.5 },
                new Object[] { "2023-01-08 to 2023-01-14", 1L, 5, 3.5 });

        when(ratingRepository.findAggregatedRatingsBetween(anyString(), anyString())).thenReturn(aggregatedRatingsRaw);
        when(ratingCategoryUtils.getCategoryIdToNameMap()).thenReturn(Collections.singletonMap(1L, "Category 1"));

        ratingService.getAggregatedScores(request, responseObserver);

        ArgumentCaptor<AggregatedScoresResponse> responseCaptor = ArgumentCaptor
                .forClass(AggregatedScoresResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();

        AggregatedScoresResponse response = responseCaptor.getValue();
        assertNotNull(response);
        assertEquals(1, response.getCategoryRatingResultsCount());
        assertEquals("Category 1", response.getCategoryRatingResults(0).getCategoryName());
    }

    /**
     * Tests that getAggregatedScores handles different months or years.
     */
    @Test
    public void testGetAggregatedScores_SuccessWithDifferentMonthsOrYears() {
        AggregatedScoresRequest request = AggregatedScoresRequest.newBuilder().setStartDate("2023-01-01T00:00:00")
                .setEndDate("2023-02-15T23:59:59").build();

        StreamObserver<AggregatedScoresResponse> responseObserver = mock(StreamObserver.class);

        dateUtilsMockedStatic.when(() -> DateUtils.isValidDate(anyString())).thenReturn(true);
        dateUtilsMockedStatic.when(() -> DateUtils.getDaysBetween(anyString(), anyString())).thenReturn(45L);
        dateUtilsMockedStatic.when(() -> DateUtils.isDifferentMonthOrYear(anyString(), anyString())).thenReturn(true);

        List<Object[]> aggregatedRatingsRaw = Arrays.asList(new Object[] { "2023-01-01 to 2023-01-07", 1L, 10, 4.5 },
                new Object[] { "2023-01-08 to 2023-01-14", 1L, 5, 3.5 });

        when(ratingRepository.findWeeklyAggregatedRatingsBetween(anyString(), anyString()))
                .thenReturn(aggregatedRatingsRaw);
        when(ratingCategoryUtils.getCategoryIdToNameMap()).thenReturn(Collections.singletonMap(1L, "Category 1"));

        ratingService.getAggregatedScores(request, responseObserver);

        ArgumentCaptor<AggregatedScoresResponse> responseCaptor = ArgumentCaptor
                .forClass(AggregatedScoresResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();

        AggregatedScoresResponse response = responseCaptor.getValue();
        assertNotNull(response);
        assertEquals(1, response.getCategoryRatingResultsCount());
        assertEquals("Category 1", response.getCategoryRatingResults(0).getCategoryName());
    }

    /**
     * Tests that getAggregatedScores handles an empty result set.
     */
    @Test
    public void testGetAggregatedScores_EmptyResultSet() {
        AggregatedScoresRequest request = AggregatedScoresRequest.newBuilder().setStartDate("2023-01-01T00:00:00")
                .setEndDate("2023-01-15T23:59:59").build();

        StreamObserver<AggregatedScoresResponse> responseObserver = mock(StreamObserver.class);

        dateUtilsMockedStatic.when(() -> DateUtils.isValidDate(anyString())).thenReturn(true);
        dateUtilsMockedStatic.when(() -> DateUtils.getDaysBetween(anyString(), anyString())).thenReturn(14L);
        when(ratingRepository.findAggregatedRatingsBetween(anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        ratingService.getAggregatedScores(request, responseObserver);

        verify(responseObserver).onError(any(Throwable.class));
    }

    /**
     * Tests private methods indirectly through public methods.
     */
    @Test
    public void testPrivateMethodsThroughPublicInterface() {
        AggregatedScoresRequest request = AggregatedScoresRequest.newBuilder().setStartDate("2023-01-01T00:00:00")
                .setEndDate("2023-01-15T23:59:59").build();

        StreamObserver<AggregatedScoresResponse> responseObserver = mock(StreamObserver.class);

        dateUtilsMockedStatic.when(() -> DateUtils.isValidDate(anyString())).thenReturn(true);
        dateUtilsMockedStatic.when(() -> DateUtils.getDaysBetween(anyString(), anyString())).thenReturn(14L);

        List<Object[]> aggregatedRatingsRaw = Arrays.asList(new Object[] { "2023-01-01 to 2023-01-07", 1L, 10, 4.5 },
                new Object[] { "2023-01-08 to 2023-01-14", 1L, 5, 3.5 });

        when(ratingRepository.findAggregatedRatingsBetween(anyString(), anyString())).thenReturn(aggregatedRatingsRaw);
        when(ratingCategoryUtils.getCategoryIdToNameMap()).thenReturn(Collections.singletonMap(1L, "Category 1"));

        ratingService.getAggregatedScores(request, responseObserver);

        ArgumentCaptor<AggregatedScoresResponse> responseCaptor = ArgumentCaptor
                .forClass(AggregatedScoresResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();

        AggregatedScoresResponse response = responseCaptor.getValue();
        assertNotNull(response);
        assertEquals(1, response.getCategoryRatingResultsCount());
        assertEquals("Category 1", response.getCategoryRatingResults(0).getCategoryName());
    }
}
