package com.kaarelkaasla.klaustestassignment.service;

import com.kaarelkaasla.klaustestassignment.*;
import com.kaarelkaasla.klaustestassignment.entity.RatingCategory;
import com.kaarelkaasla.klaustestassignment.repository.RatingCategoryRepository;
import com.kaarelkaasla.klaustestassignment.repository.RatingRepository;
import com.kaarelkaasla.klaustestassignment.util.DateUtils;
import com.kaarelkaasla.klaustestassignment.util.MathUtils;
import com.kaarelkaasla.klaustestassignment.util.RatingCategoryUtils;
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
 * Unit tests for the TicketScoreServiceImpl class.
 */
public class TicketScoreServiceImplTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private RatingCategoryRepository ratingCategoryRepository;

    @Mock
    private DateUtils dateUtils;

    @Mock
    private RatingCategoryUtils ratingCategoryUtils;

    @InjectMocks
    private TicketScoreServiceImpl ticketService;

    private SimpleDateFormat requestDateFormat;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        requestDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    }

    @AfterEach
    public void tearDown() {
        Mockito.reset(ratingRepository, ratingCategoryRepository, dateUtils, ratingCategoryUtils);
    }

    /**
     * Tests that getTicketCategoryScores handles invalid date formats.
     */
    @Test
    public void testGetTicketCategoryScores_InvalidDateFormat() {
        TicketCategoryScoresRequest request = TicketCategoryScoresRequest.newBuilder().setStartDate("invalid-date")
                .setEndDate("2023-12-31T23:59:59").build();

        StreamObserver<TicketCategoryScoresResponse> responseObserver = mock(StreamObserver.class);

        ticketService.getTicketCategoryScores(request, responseObserver);

        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(responseObserver).onError(errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof StatusRuntimeException);
        assertEquals(Status.INVALID_ARGUMENT.getCode(),
                ((StatusRuntimeException) errorCaptor.getValue()).getStatus().getCode());
    }

    /**
     * Tests that getTicketCategoryScores handles database query exceptions.
     */
    @Test
    public void testGetTicketCategoryScores_DatabaseError() {
        TicketCategoryScoresRequest request = TicketCategoryScoresRequest.newBuilder()
                .setStartDate("2023-01-01T00:00:00").setEndDate("2023-12-31T23:59:59").build();

        StreamObserver<TicketCategoryScoresResponse> responseObserver = mock(StreamObserver.class);

        when(ratingRepository.findRatingsWithinPeriod(anyString(), anyString()))
                .thenThrow(new RuntimeException("Database error"));

        ticketService.getTicketCategoryScores(request, responseObserver);

        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(responseObserver).onError(errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof StatusRuntimeException);
        assertEquals(Status.INTERNAL.getCode(),
                ((StatusRuntimeException) errorCaptor.getValue()).getStatus().getCode());
    }

    /**
     * Tests that getTicketCategoryScores retrieves and processes data correctly.
     */
    @Test
    public void testGetTicketCategoryScores_Success() throws ParseException {
        TicketCategoryScoresRequest request = TicketCategoryScoresRequest.newBuilder()
                .setStartDate("2023-01-01T00:00:00").setEndDate("2023-12-31T23:59:59").build();

        StreamObserver<TicketCategoryScoresResponse> responseObserver = mock(StreamObserver.class);

        List<Object[]> ratingsRaw = Arrays.asList(new Object[] { 1, 1L, 4 }, new Object[] { 1, 2L, 3 },
                new Object[] { 2, 1L, 5 }, new Object[] { 2, 2L, 2 });

        List<RatingCategory> ratingCategories = Arrays.asList(new RatingCategory(1L, "Category 1", 1.0),
                new RatingCategory(2L, "Category 2", 1.0));

        when(ratingRepository.findRatingsWithinPeriod(anyString(), anyString())).thenReturn(ratingsRaw);
        when(ratingCategoryRepository.findAll()).thenReturn(ratingCategories);
        when(ratingCategoryUtils.getCategoryIdToNameMap()).thenReturn(Map.of(1L, "Category 1", 2L, "Category 2"));

        ticketService.getTicketCategoryScores(request, responseObserver);

        ArgumentCaptor<TicketCategoryScoresResponse> responseCaptor = ArgumentCaptor
                .forClass(TicketCategoryScoresResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();

        TicketCategoryScoresResponse response = responseCaptor.getValue();
        assertNotNull(response);
        assertEquals(2, response.getTicketCategoryScoresCount());

        TicketCategoryScore score1 = response.getTicketCategoryScoresList().stream()
                .filter(tcs -> tcs.getTicketId() == 1).findFirst().orElse(null);
        assertNotNull(score1);
        assertEquals(80.00, score1.getCategoryScoresMap().get("Category 1"));
        assertEquals(60.00, score1.getCategoryScoresMap().get("Category 2"));

        TicketCategoryScore score2 = response.getTicketCategoryScoresList().stream()
                .filter(tcs -> tcs.getTicketId() == 2).findFirst().orElse(null);
        assertNotNull(score2);
        assertEquals(100.00, score2.getCategoryScoresMap().get("Category 1"));
        assertEquals(40.00, score2.getCategoryScoresMap().get("Category 2"));
    }

    /**
     * Tests that getTicketCategoryScores handles no ratings found for the specified period.
     */
    @Test
    public void testGetTicketCategoryScores_NoRatingsFound() {
        TicketCategoryScoresRequest request = TicketCategoryScoresRequest.newBuilder()
                .setStartDate("2023-01-01T00:00:00").setEndDate("2023-12-31T23:59:59").build();

        StreamObserver<TicketCategoryScoresResponse> responseObserver = mock(StreamObserver.class);

        when(ratingRepository.findRatingsWithinPeriod(anyString(), anyString())).thenReturn(Collections.emptyList());

        ticketService.getTicketCategoryScores(request, responseObserver);

        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(responseObserver).onError(errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof StatusRuntimeException);
        assertEquals(Status.NOT_FOUND.getCode(),
                ((StatusRuntimeException) errorCaptor.getValue()).getStatus().getCode());
    }

    /**
     * Tests that processTicketCategoryScores processes the response correctly.
     */
    @Test
    public void testProcessTicketCategoryScores() {
        List<RatingCategory> ratingCategories = Arrays.asList(new RatingCategory(1L, "Category 1", 1.0),
                new RatingCategory(2L, "Category 2", 1.0));

        TicketCategoryScore score1 = TicketCategoryScore.newBuilder().setTicketId(1)
                .putCategoryScores("Category 1", 80.0).putCategoryScores("Category 2", 60.0).build();

        TicketCategoryScore score2 = TicketCategoryScore.newBuilder().setTicketId(2)
                .putCategoryScores("Category 1", 100.0).putCategoryScores("Category 2", 40.0).build();

        TicketCategoryScoresResponse response = TicketCategoryScoresResponse.newBuilder()
                .addAllTicketCategoryScores(Arrays.asList(score1, score2)).build();

        when(ratingCategoryRepository.findAll()).thenReturn(ratingCategories);

        List<Map<String, Object>> result = ticketService.processTicketCategoryScores(response);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).get("TicketId"));
        assertEquals(80.0, ((Map<String, Double>) result.get(0).get("CategoryScores")).get("Category 1"));
        assertEquals(60.0, ((Map<String, Double>) result.get(0).get("CategoryScores")).get("Category 2"));
        assertEquals(2, result.get(1).get("TicketId"));
        assertEquals(100.0, ((Map<String, Double>) result.get(1).get("CategoryScores")).get("Category 1"));
        assertEquals(40.0, ((Map<String, Double>) result.get(1).get("CategoryScores")).get("Category 2"));
    }
}
