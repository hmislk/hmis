package com.divudi.core.util;

import com.divudi.core.entity.channel.SessionInstance;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Named
@ApplicationScoped
public class CommonFunctionsProxy {

    public double abs(double value) {
        return CommonFunctions.abs(value);
    }

    public String convertToWordJSF(double number) {
        return CommonFunctions.convertToWord(number);
    }

    public Double reverseSign(Double value) {
        return CommonFunctions.reverseSign(value);
    }

    public static double roundToTwoDecimals(double value) {
        return CommonFunctions.roundToTwoDecimals(value);
    }

    public static double roundToTwoDecimalsBigDecimal(double value) {
        return CommonFunctions.roundToTwoDecimalsBigDecimal(value);
    }

    public String formatToLongDate(Date date, String dateFormat) {
        return CommonFunctions.formatToLongDate(date, dateFormat);
    }

    public Date getCurrentDateTime() {
        return CommonFunctions.getCurrentDateTime();
    }

    public Date getDateBeforeThreeMonthsCurrentDateTime() {
        return CommonFunctions.getDateBeforeThreeMonthsCurrentDateTime();
    }

    public Date getEndOfDayOnCurrentDate() {
        return CommonFunctions.getEndOfDay(new Date());
    }

    public Date getStartOfDayOnCurrentDate() {
        return CommonFunctions.getStartOfDay(new Date());
    }

    public boolean renderPaginator(List<Object> list, int count) {
        return CommonFunctions.renderPaginator(list, count);
    }

    public Date getDateAfterThreeMonthsCurrentDateTime() {
        return CommonFunctions.getDateAfterThreeMonthsCurrentDateTime();
    }

    public boolean sameDate(Date date1, Date date2) {
        return CommonFunctions.sameDate(date1, date2);
    }

    public Date getEndOfDay() {
        return CommonFunctions.getEndOfDay(new Date());
    }

    public SessionInstance convertToSessionInstance(Object ob) {
        if (ob instanceof SessionInstance) {
            return (SessionInstance) ob;
        }
        return null;
    }

    public Date retiermentDate(Date dob) {
        return CommonFunctions.retiermentDate(dob);
    }

    public double dateDifferenceInMinutes(Date fromDate, Date toDate) {
        return CommonFunctions.dateDifferenceInMinutes(fromDate, toDate);
    }


    public static Date getRoundedHourAfter60Minutes() {

        LocalDateTime d = LocalDateTime.now()
                .plusMinutes(60)
                .plusMinutes(30) // Add 30 minutes to round to nearest
                .truncatedTo(ChronoUnit.HOURS);

        return CommonFunctions.convertLocalDateTimeToDate(d);
    }

    /**
     * Delegates to {@link CommonFunctions#escapeHtml(String)} to sanitise text
     * before rendering with <code>escape="false"</code> in JSF pages.
     *
     * @param input raw string value
     * @return escaped string safe for HTML output
     */
    public String escapeHtml(String input) {
        return CommonFunctions.escapeHtml(input);
    }

    /**
     * Adds the specified number of days to the given date.
     *
     * @param date the base date
     * @param days the number of days to add (can be negative to subtract)
     * @return the resulting date after adding the days
     */
    public Date addDaysToDate(Date date, Long days) {
        if (date == null || days == null) {
            return date;
        }
        return CommonFunctions.getAddedDate(date, days.intValue());
    }

    /**
     * PrimeFaces {@code filterFunction} that matches a numeric column by value.
     * The built-in "exact" mode compares strings, so typing {@code 100} never
     * matches a stored {@code 100.0} and {@code 12.50} never matches {@code 12.5}.
     * The typed text is read with the view locale's grouping and decimal
     * separators. It matches when it equals the stored value, or the stored
     * value rounded to two decimals the way {@code f:convertNumber} displays it.
     *
     * @param value the row's numeric value
     * @param filter the text typed into the column filter
     * @param locale the view locale, used for the separators
     * @return true if the filter is blank or equals the value
     */
    public boolean filterByNumberEquals(Object value, Object filter, Locale locale) {
        String text = filter == null ? "" : filter.toString().trim();
        if (text.isEmpty()) {
            return true;
        }
        if (!(value instanceof Number)) {
            return false;
        }
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale == null ? Locale.getDefault() : locale);
        String normalised = text.replace(String.valueOf(symbols.getGroupingSeparator()), "")
                .replace(symbols.getDecimalSeparator(), '.');
        try {
            BigDecimal typed = new BigDecimal(normalised);
            BigDecimal actual = new BigDecimal(value.toString());
            return typed.compareTo(actual) == 0
                    || typed.compareTo(actual.setScale(2, RoundingMode.HALF_EVEN)) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
