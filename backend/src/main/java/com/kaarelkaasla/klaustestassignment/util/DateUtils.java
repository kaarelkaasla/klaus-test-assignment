package com.kaarelkaasla.klaustestassignment.util;

import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Utility class for various date operations.
 */
@Component
public class DateUtils {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Validates if the given date string matches the expected date format.
     *
     * @param dateStr
     *            The date string to validate.
     *
     * @return True if the date string is valid, false otherwise.
     */
    public static boolean isValidDate(String dateStr) {
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
    public static Date parseDate(String dateStr) throws ParseException {
        return dateFormat.parse(dateStr);
    }

    /**
     * Formats the given Date object into a string.
     *
     * @param date
     *            The Date object to format.
     *
     * @return The formatted date string.
     */
    public static String formatDate(Date date) {
        return dateFormat.format(date);
    }

    /**
     * Parses the given date-time string into a LocalDateTime object.
     *
     * @param dateStr
     *            The date-time string to parse.
     *
     * @return The parsed LocalDateTime object.
     */
    public static LocalDateTime parseDateTime(String dateStr) {
        return LocalDateTime.parse(dateStr, dateTimeFormatter);
    }

    /**
     * Calculates the number of days between two date strings.
     *
     * @param startDateStr
     *            The start date string.
     * @param endDateStr
     *            The end date string.
     *
     * @return The number of days between the two dates.
     */
    public static long getDaysBetween(String startDateStr, String endDateStr) {
        LocalDate startDate = LocalDate.parse(startDateStr.substring(0, 10));
        LocalDate endDate = LocalDate.parse(endDateStr.substring(0, 10));
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Checks if the two date strings belong to different months or years.
     *
     * @param startDateStr
     *            The start date string.
     * @param endDateStr
     *            The end date string.
     *
     * @return True if the dates are in different months or years, false otherwise.
     */
    public static boolean isDifferentMonthOrYear(String startDateStr, String endDateStr) {
        LocalDate startDate = LocalDate.parse(startDateStr.substring(0, 10));
        LocalDate endDate = LocalDate.parse(endDateStr.substring(0, 10));
        return startDate.getMonthValue() != endDate.getMonthValue() || startDate.getYear() != endDate.getYear();
    }
}
