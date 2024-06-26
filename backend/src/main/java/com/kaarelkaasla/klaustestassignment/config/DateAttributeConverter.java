package com.kaarelkaasla.klaustestassignment.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * JPA attribute converter to handle the conversion between {@link java.util.Date} and its {@link String} representation
 * in the database. Applies the converter automatically to all entity attributes of type {@link Date}.
 */
@Converter(autoApply = true)
public class DateAttributeConverter implements AttributeConverter<Date, String> {

    /**
     * Date format pattern used for conversion.
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * Formatter instance for date conversion.
     */
    private static final SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);

    /**
     * Converts {@link Date} to its {@link String} representation for database storage.
     *
     * @param date
     *            the date to be converted, may be null
     *
     * @return the string representation of the date, or null if the date is null
     */
    @Override
    public String convertToDatabaseColumn(Date date) {
        return date == null ? null : formatter.format(date);
    }

    /**
     * Converts {@link String} from the database to a {@link Date} entity attribute.
     *
     * @param dbData
     *            the string representation of the date from the database, may be null
     *
     * @return the parsed date, or null if the string is null
     *
     * @throws RuntimeException
     *             if the string cannot be parsed to a date
     */
    @Override
    public Date convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : formatter.parse(dbData);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse date: " + dbData, e);
        }
    }
}
