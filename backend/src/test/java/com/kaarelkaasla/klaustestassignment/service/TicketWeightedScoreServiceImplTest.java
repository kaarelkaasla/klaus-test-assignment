package com.kaarelkaasla.klaustestassignment.service;

import com.kaarelkaasla.klaustestassignment.*;
import com.kaarelkaasla.klaustestassignment.entity.RatingCategory;
import com.kaarelkaasla.klaustestassignment.repository.RatingCategoryRepository;
import com.kaarelkaasla.klaustestassignment.repository.RatingRepository;
import com.kaarelkaasla.klaustestassignment.util.DateUtils;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the TicketWeightedScoreServiceImpl class.
 */
public class TicketWeightedScoreServiceImplTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private RatingCategoryRepository ratingCategoryRepository;

    @Mock
    private ScoreService scoreService;

    @InjectMocks
    private TicketWeightedScoreServiceImpl ticketWeightedScoreService;

    private MockedStatic<DateUtils> dateUtilsMockedStatic;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        dateUtilsMockedStatic = mockStatic(DateUtils.class);
    }

    @AfterEach
    public void tearDown() {
        dateUtilsMockedStatic.close();
        Mockito.reset(ratingRepository, ratingCategoryRepository, scoreService);
    }

    /**
     * Tests that getWeightedScores handles invalid date formats.
     */
    @Test
    public void testGetWeightedScores_InvalidDateFormat() throws ParseException {
        WeightedScoresRequest request = WeightedScoresRequest.newBuilder().setStartDate("invalid-date")
                .setEndDate("2023-12-31T23:59:59").build();

        StreamObserver<WeightedScoresResponse> responseObserver = mock(StreamObserver.class);

        dateUtilsMockedStatic.when(() -> DateUtils.parseDate("invalid-date"))
                .thenThrow(new ParseException("Unparseable date: \"invalid-date\"", 0));

        ticketWeightedScoreService.getWeightedScores(request, responseObserver);

        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(responseObserver).onError(errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof StatusRuntimeException);
        assertEquals(Status.INVALID_ARGUMENT.getCode(),
                ((StatusRuntimeException) errorCaptor.getValue()).getStatus().getCode());
    }

    /**
     * Tests that getWeightedScores handles database query exceptions.
     */
    @Test
    public void testGetWeightedScores_DatabaseError() {
        WeightedScoresRequest request = WeightedScoresRequest.newBuilder().setStartDate("2023-01-01T00:00:00")
                .setEndDate("2023-12-31T23:59:59").build();

        StreamObserver<WeightedScoresResponse> responseObserver = mock(StreamObserver.class);

        dateUtilsMockedStatic.when(() -> DateUtils.parseDate(anyString())).thenAnswer(invocation -> {
            String dateStr = invocation.getArgument(0);
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(dateStr);
        });

        dateUtilsMockedStatic.when(() -> DateUtils.formatDate(any(Date.class))).thenReturn("2023-01-01T00:00:00");
        when(ratingRepository.findRatingsWithinPeriod(anyString(), anyString()))
                .thenThrow(new RuntimeException("Database error"));

        ticketWeightedScoreService.getWeightedScores(request, responseObserver);

        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(responseObserver).onError(errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof StatusRuntimeException);
        assertEquals(Status.INTERNAL.getCode(),
                ((StatusRuntimeException) errorCaptor.getValue()).getStatus().getCode());
    }

    /**
     * Tests that getWeightedScores retrieves and processes data correctly.
     */
    @Test
    public void testGetWeightedScores_Success() throws ParseException {
        WeightedScoresRequest request = WeightedScoresRequest.newBuilder().setStartDate("2023-01-01T00:00:00")
                .setEndDate("2023-12-31T23:59:59").build();

        StreamObserver<WeightedScoresResponse> responseObserver = mock(StreamObserver.class);

        List<Object[]> ratingsRaw = Arrays.asList(new Object[] { 1, 1L, 4 }, new Object[] { 1, 2L, 3 },
                new Object[] { 2, 1L, 5 }, new Object[] { 2, 2L, 2 });

        List<RatingCategory> ratingCategories = Arrays.asList(new RatingCategory(1L, "Category 1", 1.0),
                new RatingCategory(2L, "Category 2", 1.0));

        dateUtilsMockedStatic.when(() -> DateUtils.parseDate("2023-01-01T00:00:00"))
                .thenReturn(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse("2023-01-01T00:00:00"));
        dateUtilsMockedStatic.when(() -> DateUtils.parseDate("2023-12-31T23:59:59"))
                .thenReturn(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse("2023-12-31T23:59:59"));
        dateUtilsMockedStatic.when(() -> DateUtils.formatDate(any(Date.class))).thenAnswer(
                invocation -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(invocation.getArgument(0)));
        when(ratingRepository.findRatingsWithinPeriod(anyString(), anyString())).thenReturn(ratingsRaw);
        when(ratingCategoryRepository.findAll()).thenReturn(ratingCategories);
        when(scoreService.calculateScore(anyMap())).thenReturn(70.0);

        ticketWeightedScoreService.getWeightedScores(request, responseObserver);

        ArgumentCaptor<WeightedScoresResponse> responseCaptor = ArgumentCaptor.forClass(WeightedScoresResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();

        WeightedScoresResponse response = responseCaptor.getValue();
        assertNotNull(response);

        PeriodScore currentPeriodScore = response.getCurrentPeriodScore();
        assertEquals("2023-01-01T00:00:00 to 2023-12-31T23:59:59", currentPeriodScore.getPeriod());
        assertEquals(70.0, currentPeriodScore.getAverageScorePercentage());

        // Assuming includePreviousPeriod is false in this test
        assertFalse(response.hasPreviousPeriodScore());
        assertFalse(response.hasScoreChange());
    }

    /**
     * Tests that getWeightedScores calculates and includes the previous period score correctly.
     */
    @Test
    public void testGetWeightedScores_SuccessWithPreviousPeriod() throws ParseException {
        WeightedScoresRequest request = WeightedScoresRequest.newBuilder().setStartDate("2023-01-01T00:00:00")
                .setEndDate("2023-12-31T23:59:59").setIncludePreviousPeriod(true).build();

        StreamObserver<WeightedScoresResponse> responseObserver = mock(StreamObserver.class);

        List<Object[]> currentRatingsRaw = Arrays.asList(new Object[] { 1, 1L, 4 }, new Object[] { 1, 2L, 3 },
                new Object[] { 2, 1L, 5 }, new Object[] { 2, 2L, 2 });

        List<Object[]> previousRatingsRaw = Arrays.asList(new Object[] { 1, 1L, 3 }, new Object[] { 1, 2L, 2 },
                new Object[] { 2, 1L, 4 }, new Object[] { 2, 2L, 1 });

        List<RatingCategory> ratingCategories = Arrays.asList(new RatingCategory(1L, "Category 1", 1.0),
                new RatingCategory(2L, "Category 2", 1.0));

        dateUtilsMockedStatic.when(() -> DateUtils.parseDate("2023-01-01T00:00:00"))
                .thenReturn(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse("2023-01-01T00:00:00"));
        dateUtilsMockedStatic.when(() -> DateUtils.parseDate("2023-12-31T23:59:59"))
                .thenReturn(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse("2023-12-31T23:59:59"));
        dateUtilsMockedStatic.when(() -> DateUtils.formatDate(any(Date.class))).thenAnswer(
                invocation -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(invocation.getArgument(0)));
        when(ratingRepository.findRatingsWithinPeriod(eq("2023-01-01T00:00:00"), eq("2023-12-31T23:59:59")))
                .thenReturn(currentRatingsRaw);
        when(ratingRepository.findRatingsWithinPeriod(eq("2022-01-01T00:00:00"), eq("2022-12-31T23:59:59")))
                .thenReturn(previousRatingsRaw);
        when(ratingCategoryRepository.findAll()).thenReturn(ratingCategories);
        when(scoreService.calculateScore(anyMap())).thenAnswer(invocation -> {
            Map<String, Integer> ratings = invocation.getArgument(0);
            double average = ratings.values().stream().mapToInt(Integer::intValue).average().orElse(0);
            return (average / 5) * 100;
        });

        ticketWeightedScoreService.getWeightedScores(request, responseObserver);

        ArgumentCaptor<WeightedScoresResponse> responseCaptor = ArgumentCaptor.forClass(WeightedScoresResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();

        WeightedScoresResponse response = responseCaptor.getValue();
        assertNotNull(response);

        PeriodScore currentPeriodScore = response.getCurrentPeriodScore();
        assertEquals("2023-01-01T00:00:00 to 2023-12-31T23:59:59", currentPeriodScore.getPeriod());
        assertEquals(70.0, currentPeriodScore.getAverageScorePercentage());

        PeriodScore previousPeriodScore = response.getPreviousPeriodScore();
        assertNotNull(previousPeriodScore);
        assertEquals("2022-01-01T00:00:00 to 2022-12-31T23:59:59", previousPeriodScore.getPeriod());
        assertEquals(50.0, previousPeriodScore.getAverageScorePercentage());

        ScoreChange scoreChange = response.getScoreChange();
        assertNotNull(scoreChange);
        assertEquals(20.0, scoreChange.getValue());
    }
}
