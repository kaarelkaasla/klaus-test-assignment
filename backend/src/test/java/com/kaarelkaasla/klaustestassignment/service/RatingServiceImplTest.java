package com.kaarelkaasla.klaustestassignment.service;

import com.kaarelkaasla.klaustestassignment.AggregatedScoresRequest;
import com.kaarelkaasla.klaustestassignment.AggregatedScoresResponse;
import com.kaarelkaasla.klaustestassignment.repository.RatingRepository;
import com.kaarelkaasla.klaustestassignment.util.RatingUtils;
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

/**
 * Unit tests for the RatingServiceImpl class.
 */
public class RatingServiceImplTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private RatingUtils ratingUtils;

    @InjectMocks
    private RatingServiceImpl ratingService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void tearDown() {
        Mockito.reset(ratingRepository, ratingUtils);
    }

    /**
     * Tests that getAggregatedScores handles an invalid start date.
     */
    @Test
    public void testGetAggregatedScores_InvalidStartDate() {
        AggregatedScoresRequest request = AggregatedScoresRequest.newBuilder().setStartDate("invalid-date")
                .setEndDate("2023-12-31").build();

        StreamObserver<AggregatedScoresResponse> responseObserver = mock(StreamObserver.class);

        when(ratingUtils.isValidDate("invalid-date")).thenReturn(false);

        ratingService.getAggregatedScores(request, responseObserver);

        verify(responseObserver).onError(any(Throwable.class));
    }

    /**
     * Tests that getAggregatedScores handles an invalid end date.
     */
    @Test
    public void testGetAggregatedScores_InvalidEndDate() {
        AggregatedScoresRequest request = AggregatedScoresRequest.newBuilder().setStartDate("2023-01-01")
                .setEndDate("invalid-date").build();

        StreamObserver<AggregatedScoresResponse> responseObserver = mock(StreamObserver.class);

        when(ratingUtils.isValidDate("2023-01-01")).thenReturn(true);
        when(ratingUtils.isValidDate("invalid-date")).thenReturn(false);

        ratingService.getAggregatedScores(request, responseObserver);

        verify(responseObserver).onError(any(Throwable.class));
    }

    /**
     * Tests that getAggregatedScores handles a DateTimeParseException.
     */
    @Test
    public void testGetAggregatedScores_DateParseException() {
        AggregatedScoresRequest request = AggregatedScoresRequest.newBuilder().setStartDate("2023-01-01")
                .setEndDate("2023-12-31").build();

        StreamObserver<AggregatedScoresResponse> responseObserver = mock(StreamObserver.class);

        when(ratingUtils.isValidDate(anyString())).thenReturn(true);
        when(ratingUtils.getDaysBetween(anyString(), anyString())).thenThrow(DateTimeParseException.class);

        ratingService.getAggregatedScores(request, responseObserver);

        verify(responseObserver).onError(any(Throwable.class));
    }

    /**
     * Tests that getAggregatedScores handles a database query exception.
     */
    @Test
    public void testGetAggregatedScores_DatabaseQueryException() {
        AggregatedScoresRequest request = AggregatedScoresRequest.newBuilder().setStartDate("2023-01-01")
                .setEndDate("2023-01-15").build();

        StreamObserver<AggregatedScoresResponse> responseObserver = mock(StreamObserver.class);

        when(ratingUtils.isValidDate(anyString())).thenReturn(true);
        when(ratingUtils.getDaysBetween(anyString(), anyString())).thenReturn(14L);
        when(ratingRepository.findAggregatedRatingsBetween(anyString(), anyString())).thenThrow(RuntimeException.class);

        ratingService.getAggregatedScores(request, responseObserver);

        verify(responseObserver).onError(any(Throwable.class));
    }

    /**
     * Tests that getAggregatedScores successfully retrieves and processes data.
     */
    @Test
    public void testGetAggregatedScores_Success() {
        AggregatedScoresRequest request = AggregatedScoresRequest.newBuilder().setStartDate("2023-01-01")
                .setEndDate("2023-01-15").build();

        StreamObserver<AggregatedScoresResponse> responseObserver = mock(StreamObserver.class);

        when(ratingUtils.isValidDate(anyString())).thenReturn(true);
        when(ratingUtils.getDaysBetween(anyString(), anyString())).thenReturn(14L);

        List<Object[]> aggregatedRatingsRaw = Arrays.asList(new Object[] { "2023-01-01 to 2023-01-07", 1L, 10, 4.5 },
                new Object[] { "2023-01-08 to 2023-01-14", 1L, 5, 3.5 });

        when(ratingRepository.findAggregatedRatingsBetween(anyString(), anyString())).thenReturn(aggregatedRatingsRaw);
        when(ratingUtils.getCategoryIdToNameMap()).thenReturn(Collections.singletonMap(1L, "Category 1"));

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
        AggregatedScoresRequest request = AggregatedScoresRequest.newBuilder().setStartDate("2023-01-01")
                .setEndDate("2023-02-15").build();

        StreamObserver<AggregatedScoresResponse> responseObserver = mock(StreamObserver.class);

        when(ratingUtils.isValidDate(anyString())).thenReturn(true);
        when(ratingUtils.getDaysBetween(anyString(), anyString())).thenReturn(45L);
        when(ratingUtils.isDifferentMonthOrYear(anyString(), anyString())).thenReturn(true);

        List<Object[]> aggregatedRatingsRaw = Arrays.asList(new Object[] { "2023-01-01 to 2023-01-07", 1L, 10, 4.5 },
                new Object[] { "2023-01-08 to 2023-01-14", 1L, 5, 3.5 });

        when(ratingRepository.findWeeklyAggregatedRatingsBetween(anyString(), anyString()))
                .thenReturn(aggregatedRatingsRaw);
        when(ratingUtils.getCategoryIdToNameMap()).thenReturn(Collections.singletonMap(1L, "Category 1"));

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
     * Tests private methods indirectly through public methods.
     */
    @Test
    public void testPrivateMethodsThroughPublicInterface() {
        AggregatedScoresRequest request = AggregatedScoresRequest.newBuilder().setStartDate("2023-01-01")
                .setEndDate("2023-01-15").build();

        StreamObserver<AggregatedScoresResponse> responseObserver = mock(StreamObserver.class);

        when(ratingUtils.isValidDate(anyString())).thenReturn(true);
        when(ratingUtils.getDaysBetween(anyString(), anyString())).thenReturn(14L);

        List<Object[]> aggregatedRatingsRaw = Arrays.asList(new Object[] { "2023-01-01 to 2023-01-07", 1L, 10, 4.5 },
                new Object[] { "2023-01-08 to 2023-01-14", 1L, 5, 3.5 });

        when(ratingRepository.findAggregatedRatingsBetween(anyString(), anyString())).thenReturn(aggregatedRatingsRaw);
        when(ratingUtils.getCategoryIdToNameMap()).thenReturn(Collections.singletonMap(1L, "Category 1"));

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
