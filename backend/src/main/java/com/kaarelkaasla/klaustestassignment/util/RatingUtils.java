package com.kaarelkaasla.klaustestassignment.util;

import com.kaarelkaasla.klaustestassignment.entity.RatingCategory;
import com.kaarelkaasla.klaustestassignment.repository.RatingCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for handling rating-related operations.
 */
@Component
public class RatingUtils {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private final RatingCategoryRepository ratingCategoryRepository;

    @Autowired
    public RatingUtils(RatingCategoryRepository ratingCategoryRepository) {
        this.ratingCategoryRepository = ratingCategoryRepository;
    }

    /**
     * Validates if the given date string is in a valid format.
     *
     * @param dateStr
     *            The date string to validate.
     *
     * @return True if the date string is valid, false otherwise.
     */
    public boolean isValidDate(String dateStr) {
        try {
            dateFormat.parse(dateStr);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Parses the given date string into a Date object.
     *
     * @param dateStr
     *            The date string to parse.
     *
     * @return The parsed Date object.
     *
     * @throws ParseException
     *             If the date string is invalid.
     */
    public Date parseDate(String dateStr) throws ParseException {
        return dateFormat.parse(dateStr);
    }

    /**
     * Formats the given Date object into a date string.
     *
     * @param date
     *            The Date object to format.
     *
     * @return The formatted date string.
     */
    public String formatDate(Date date) {
        return dateFormat.format(date);
    }

    /**
     * Parses the given date string into a LocalDateTime object.
     *
     * @param dateStr
     *            The date string to parse.
     *
     * @return The parsed LocalDateTime object.
     *
     * @throws DateTimeParseException
     *             If the date string is invalid.
     */
    public LocalDateTime parseDateTime(String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return LocalDateTime.parse(dateStr, formatter);
    }

    /**
     * Calculates the number of days between two dates.
     *
     * @param startDateStr
     *            The start date string.
     * @param endDateStr
     *            The end date string.
     *
     * @return The number of days between the start and end dates.
     */
    public long getDaysBetween(String startDateStr, String endDateStr) {
        LocalDate startDate = LocalDate.parse(startDateStr.substring(0, 10));
        LocalDate endDate = LocalDate.parse(endDateStr.substring(0, 10));
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Checks if the start and end dates are in different months or years.
     *
     * @param startDateStr
     *            The start date string.
     * @param endDateStr
     *            The end date string.
     *
     * @return True if the dates are in different months or years, false otherwise.
     */
    public boolean isDifferentMonthOrYear(String startDateStr, String endDateStr) {
        LocalDate startDate = LocalDate.parse(startDateStr.substring(0, 10));
        LocalDate endDate = LocalDate.parse(endDateStr.substring(0, 10));
        return startDate.getMonthValue() != endDate.getMonthValue() || startDate.getYear() != endDate.getYear();
    }

    /**
     * Retrieves a map of category IDs to category names from the repository.
     *
     * @return A map where the key is the category ID and the value is the category name.
     */
    public Map<Long, String> getCategoryIdToNameMap() {
        return ratingCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(RatingCategory::getId, RatingCategory::getName));
    }

    /**
     * Rounds a double value to two decimal places.
     *
     * @param value
     *            The value to round.
     *
     * @return The rounded value.
     */
    public static double roundToTwoDecimalPlaces(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
