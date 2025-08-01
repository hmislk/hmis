/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.util;

import com.divudi.core.data.dataStructure.DateRange;
import com.divudi.core.data.dataStructure.YearMonthDay;

import java.text.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 *
 * @author buddhika
 */
public class CommonFunctions {

    public static double abs(double value) {
        return Math.abs(value);
    }

    public static Double reverseSign(Double value) {
        if (value == null) {
            return null;
        }
        return -value;
    }

    public static String changeTextCases(String nm, String tc) {
        if (tc == null) {
            return nm;
        }
        switch (tc.toUpperCase()) {
            case "UPPERCASE":
                return nm.toUpperCase();
            case "LOWERCASE":
                return nm.toLowerCase();
            case "CAPITALIZE":
                return capitalizeFirstLetter(nm);
            default:
                return nm;
        }
    }

    public static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        String[] words = str.split("\\s");
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return result.toString().trim();
    }

    public static String getDigitsOnlyByRemovingWhitespacesAndNonDigitCharacters(String membershipNumber) {
        if (membershipNumber == null) {
            return null;
        }
        return membershipNumber.replaceAll("[^\\d]", "");
    }

    private static final String[] units = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine"};
    private static final String[] teens = {"Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
    private static final String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

    public static Long removeSpecialCharsInPhonenumber(String phonenumber) {
        try {
            if (phonenumber == null || phonenumber.trim().isEmpty()) {
                return null;
            }
            String cleandPhoneNumber = phonenumber.replaceAll("[\\s+\\-()]", "");
            return Long.parseLong(cleandPhoneNumber);
        } catch (Exception e) {
            return null;
        }
    }

    // ChatGPT Contribution: Safe rounding method for financial calculations
    public static double roundToTwoDecimals(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    // ChatGPT Contribution
    public static double roundToTwoDecimalsBigDecimal(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return new java.math.BigDecimal(value)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static boolean checkOnlyNumeric(String text) {
        if (text == null) {
            return false;
        }
        String cleandtext = text.replaceAll("[\\s+\\-()]", "");
        String regex = "^[0-9]+$";
        // Check if the text matches the pattern
        boolean onlyNumeric = cleandtext.matches(regex);

        return onlyNumeric;
    }

    public static String generateUuid() {
        return UUID.randomUUID().toString();
    }

    public static List<Integer> convertStringToIntegerList(String text) {
        List<Integer> numbers = new ArrayList<>();

        // Check if the input string is null or empty
        if (text == null || text.trim().isEmpty()) {
            return numbers; // Return an empty list
        }

        // Split the input string by commas
        String[] parts = text.split(",");

        for (String part : parts) {
            try {
                // Attempt to convert each part to an integer and add it to the list
                numbers.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException e) {
                // Handle parts of the string that cannot be converted to an integer
                // For example, log the error or add a specific handling code here
                // For now, we'll just ignore the invalid part and continue
            }
        }

        return numbers;
    }

    public static String generateRandomNumericHIN(int length) {
        Random random = new Random();
        StringBuilder hinId = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            hinId.append(random.nextInt(10)); // Append a random digit (0-9)
        }
        return hinId.toString();
    }

    public static String convertToWord(double number) {
        if (number == 0) {
            return "Zero";
        }

        int intPart = (int) number;

        // Correctly round to two decimal places and extract the cents
        int decimalPart = (int) Math.round((number - intPart) * 100);

        StringBuilder result = new StringBuilder();

        if (intPart >= 1000000) {
            result.append(convert(intPart / 1000000)).append(" Million ");
            intPart %= 1000000;
        }

        if (intPart >= 1000) {
            result.append(convert(intPart / 1000)).append(" Thousand ");
            intPart %= 1000;
        }

        if (intPart > 0) {
            result.append(convert(intPart));
        }

        if (decimalPart > 0) {
            result.append(" and ").append(convert(decimalPart)).append(" Cents");
        }

        return result.toString().trim();
    }

    private static String convert(int number) {
        if (number < 10) {
            return units[number];
        } else if (number < 20) {
            return teens[number - 10];
        } else if (number < 100) {
            return tens[number / 10] + " " + units[number % 10];
        } else {
            return units[number / 100] + " Hundred " + convert(number % 100);
        }
    }

    public static Date convertLocalDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static String getDateFormat(Date date, String formatString) {
        if (date == null) {
            date = new Date();
        }
        if (formatString == null || formatString.trim().isEmpty()) {
            formatString = "dd MMMM yyyy";
        }
        return formatDate(date, formatString);
    }

    public static LocalDateTime convertDateToLocalDateTime(Date date) {
        if (date == null) {
            date = new Date();
        }
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public static Date convertDateToDbType(String argDate) {
        if (argDate == null) {
            return null; // Handle null input
        }

        SimpleDateFormat originalFormat = new SimpleDateFormat("MMMM d, yyyy, hh:mm a");
        SimpleDateFormat desiredFormat = new SimpleDateFormat("yyyy-MM-dd");

        try {
            Date date = originalFormat.parse(argDate);
            String formattedDateString = desiredFormat.format(date);
            return desiredFormat.parse(formattedDateString);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String sanitizeStringForDatabase(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[\"'/\\\\]", "");

    }

    /**
     * Escape HTML special characters to safely render dynamic text.
     * <p>
     * This method should be used when outputting user-provided content in JSF
     * components with <code>escape="false"</code> to avoid XSS issues. It will
     * convert characters such as <code>&lt;</code> and <code>&gt;</code> to
     * their HTML entity equivalents.
     *
     * @param input Raw string
     * @return Sanitised string safe for HTML output
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public static Long convertStringToLongByRemoveSpecialChars(String phonenumber) {
        if (phonenumber == null || phonenumber.trim().isEmpty()) {
            return null;
        }
        try {
            String cleandPhoneNumber = phonenumber.replaceAll("[\\s+\\-()]", "");
            return Long.parseLong(cleandPhoneNumber);
        } catch (Exception e) {
            return null;
        }
    }

    public static String removeNonUnicodeChars(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[^\\u0000-\\uFFFF]", "");
    }

    public static double stringToDouble(String str) {
        if (str == null || str.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0.0; // Return 0.0 if the string cannot be parsed to a double
        }
    }

    public static double round(double numberToRound, int decimalPlaces) {
        if (numberToRound == 0) {
            return 0.0;
        }
        Double m = Math.pow(10, decimalPlaces);
        Double n = m * numberToRound;
        Long l = Math.round(n);
        return l / m;
    }

    public static double round(double numberToRound) {
        return round(numberToRound, 2);
    }

// ChatGPT contributed - 2025-05
    public static long stringToLong(String string) {
        if (string == null || string.trim().isEmpty()) {
            return 0l;
        }
        try {
            return Long.parseLong(string.trim());
        } catch (NumberFormatException e) {
            return 0l;
        }
    }

    public static long calTimePeriod(Date frDate, Date tDate) {
        if (frDate == null || tDate == null) {
            return 0;
        }
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(tDate);
        cal2.setTime(frDate);
        Long minCount = (cal1.getTimeInMillis() - cal2.getTimeInMillis()) / (1000 * 60 * 60);
        return minCount;
    }

    public static Date deductMinutesFromCurrentTime(int minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("Minutes must be non-negative");
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MINUTE, -minutes);
        return calendar.getTime();
    }

    public static LocalDateTime getLocalDateTime(Date dateTime) {
        Date input = dateTime;
        LocalDateTime date = input.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return date;
    }

    public static Date getStartOfMonth() {
        return getStartOfMonth(new Date());
    }

    public static Date getStartOfMonth(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("IST"));
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        calendar.set(year, month, 1, 0, 0, 0);
        return calendar.getTime();
    }

    public static long calculateAgeInDays(Date dob, Date toDate) {
        if (dob == null || toDate == null) {
            return 0L;
        }
        long ageInDays;
        ageInDays = (toDate.getTime() - dob.getTime()) / (1000 * 60 * 60 * 24);
        if (ageInDays < 0) {
            ageInDays = 0;
        }
        return ageInDays;
    }

    public static Date getEndOfMonth() {
        return getEndOfMonth(new Date());
    }

    public static Date getEndOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        calendar.set(year, month, 1, 0, 0, 0);
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.SECOND, -1);
        return calendar.getTime();
    }

    public static List<Date> getDateList(Date fromDate, Date toDate) {
        List<Date> dateList = new ArrayList<>();
        Date currentDate = new Date();

        // Handle null cases
        if (fromDate == null) {
            fromDate = currentDate;
        }
        if (toDate == null) {
            toDate = currentDate;
        }

        // Swap dates if fromDate is after toDate
        if (fromDate.after(toDate)) {
            Date temp = fromDate;
            fromDate = toDate;
            toDate = temp;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fromDate);

        while (!calendar.getTime().after(toDate)) {
            dateList.add(calendar.getTime());
            calendar.add(Calendar.DATE, 1);
        }

        return dateList;
    }

    public static Date getStartOfDay() {
        return getStartOfDay(new Date());
    }

    public static Date getStartOfDay(Date date) {
        if (date == null) {
            date = new Date();
        }
        // Get a Calendar instance using the default time zone and locale.
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.setTime(date);

        // Reset hour, minutes, seconds and millis
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }

    public static Date getEndOfDay() {
        return getEndOfDay(new Date());
    }

    public static Date getEndOfDay(Date d) {
        if (d == null) {
            d = new Date();
        }
        // Get a Calendar instance using the default time zone and locale.
        Calendar c = Calendar.getInstance(TimeZone.getDefault());
        c.setTime(d);

        // Set hour, minute, second, and millisecond to the last possible values for the day.
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);

        return c.getTime();
    }

    public static String nameToCode(String name) {
        if (name == null) {
            return "";
        }
        String code;
        code = name.trim().replaceAll("\\s+", "_");
        code = code.toLowerCase();
        return code;
    }

    public static String dateToString(Date date, String ddMMyyyy) {
        return formatDate(date, ddMMyyyy);
    }

    public static String getBaseUrl() {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String url = req.getRequestURL().toString();
        return url.substring(0, url.length() - req.getRequestURI().length()) + req.getContextPath() + "/";
    }

    public static String getTimeFormat24(Date date) {
        String s;
        DateFormat d = new SimpleDateFormat("HH:mm:ss");
        s = d.format(date);
        return s;
    }

    public static String getDateTimeFormat24(Date date) {
        String s;
        DateFormat d = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        s = d.format(date);
        return s;
    }

    public static String getDateTimeFormat(Date date) {
        String s;
        DateFormat d = new SimpleDateFormat("YYYY-MM-dd hh:mm:ss a");
        s = d.format(date);
        return s;
    }

    public static Date getConvertDateTimeFormat24(String dateString) throws ParseException {
        DateFormat d = new SimpleDateFormat("yyyy-MM-dd");
        return d.parse(dateString);
    }

    public static Date getCurrentDateTime() {
        return new Date();
    }

    public static String formatNumber(Double number, String format) {
        if (number == null) {
            return "";
        }
        DecimalFormat decimalFormat = new DecimalFormat(format);
        return decimalFormat.format(number);
    }

    /**
     * Formats a Double value according to the given format string and returns
     * it as a double.
     *
     * @param number The Double value to be formatted.
     * @param format The format string specifying the desired format.
     * @return The formatted double value.
     */
    public static double formatDouble(Double number, String format) {
        if (number == null) {
            return 0.0; // Handle null input gracefully by returning 0.0
        }

        DecimalFormat decimalFormat = new DecimalFormat(format);
        try {
            String formattedValue = decimalFormat.format(number);
            return decimalFormat.parse(formattedValue).doubleValue();
        } catch (ParseException e) {
            return 0.0; // Handle any parsing errors gracefully by returning 0.0
        }
    }

    public static String shortDate(Date date) {
        SimpleDateFormat dt1 = new SimpleDateFormat("dMMMyy");
        return (dt1.format(date));
    }

    public static double dateDifferenceInSeconds(Date fromDate, Date toDate) {
        long timeInMs = toDate.getTime() - fromDate.getTime();
        return (double) timeInMs / 1000;
    }

    public static boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\."
                + "[a-zA-Z0-9_+&*-]+)*@"
                + "(?:[a-zA-Z0-9-]+\\.)+[a-z"
                + "A-Z]{2,7}$";

        Pattern pat = Pattern.compile(emailRegex);
        if (email == null) {
            return false;
        }
        return pat.matcher(email).matches();
    }

    public static Long convertStringToLongOrZero(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public static double extractDoubleValue(String input) {
        String cleanedInput = input.replaceAll(",", ""); // Remove commas
        cleanedInput = cleanedInput.trim(); // Trim leading and trailing whitespace

        if (cleanedInput.isEmpty()) {
            return 0.0;
        }

        try {
            return Double.parseDouble(cleanedInput);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static String getDouble(double d) {
        String s;
        NumberFormat myFormatter = new DecimalFormat("##0.00");
        s = myFormatter.format(d);
        return s;
    }

    public static Double getDouble(String s) {
        Double d = null;
        if (s == null) {
            return d;
        }
        try {
            d = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            d = 0.0;
        }
        return d;
    }

    public static double dateDifferenceInMinutes(Date fromDate, Date toDate) {
        if (fromDate == null || toDate == null) {
            return 0;
        }
        long timeInMs = toDate.getTime() - fromDate.getTime();
        return (double) timeInMs / (1000 * 60);
    }

    public static String formatToLongDate(Date date, String dateFormat) {
        if (date == null) {
            return "";
        }

        SimpleDateFormat d = new SimpleDateFormat(dateFormat);

        return d.format(date);
    }

    public static Date getDateBeforeThreeMonthsCurrentDateTime() {
        Calendar calendar = Calendar.getInstance();
        // Subtract three months from the current date
        calendar.add(Calendar.MONTH, -3);
        // Set time to the beginning of the day (optional, based on your requirement)
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static boolean renderPaginator(List<Object> list, int count) {
        boolean render = false;
        if (list == null) {
            return render;
        }
        if (list.size() > count) {
            render = true;
        }
        return render;
    }

    public static Date getDateAfterThreeMonthsCurrentDateTime() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(CommonFunctions.getEndOfDay(new Date()));
        cal.add(Calendar.MONTH, 3);
        return cal.getTime();
    }

    public static boolean sameDate(Date date1, Date date2) {
        Calendar d1 = Calendar.getInstance();
        d1.setTime(date1);
        DateTime first = new DateTime(date1);
        DateTime second = new DateTime(date2);
        LocalDate firstDate = first.toLocalDate();
        LocalDate secondDate = second.toLocalDate();
        return firstDate.equals(secondDate);
    }

    public static Date dateAfter24Hours(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, 24);
        return calendar.getTime();
    }

    public static Date retiermentDate(Date dob) {
        if (dob == null) {
            dob = new Date();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(dob);
        cal.add(Calendar.YEAR, 50);
        return cal.getTime();
    }

    public static Date getBeginningOfMonth(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        calendar.set(year, month, 1, 0, 0, 0);
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.MINUTE, -1);
        return calendar.getTime();
    }

    public static Date getFirstDayOfYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        //////// // System.out.println("First : " + cal.getTime());
        return cal.getTime();
    }

    public static Date getLastDayOfYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MONTH, 11);
        cal.set(Calendar.DATE, 31);
        //////// // System.out.println("Last : " + cal.getTime());
        return cal.getTime();
    }

    public Date getFirstDayOfWeek(Date date) {
        // get today and clear time of day
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the hour of day !
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

// get start of this week in milliseconds
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());

// start of the next week
//        cal.add(Calendar.WEEK_OF_YEAR, 1);
        return cal.getTime();
    }

    public Date getLastDayOfWeek(Date date) {
        // get today and clear time of day
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the hour of day !
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

// get start of this week in milliseconds
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());

        cal.add(Calendar.DATE, 7);

// start of the next week
//        cal.add(Calendar.WEEK_OF_YEAR, 1);
        return cal.getTime();
    }

    public static Date removeTime(Date date) {
        Calendar cal;
        cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        //      System.err.println("Only Date " + cal.getTime());
        return cal.getTime();
    }

    public static double roundToTwoDecimalPlaces(double num) {
        return roundToTwoDecimalPlaces(num, 2);
    }

    public static double roundToTwoDecimalPlaces(double num, int decimalPlaces) {
        double mul = Math.pow(10, decimalPlaces);
        return (double) Math.round(num * mul) / mul;
    }

    public DateRange getDateRangeForOT(Date date) {
        DateRange dateRange = new DateRange();
        Date startOfThisMonth = CommonFunctions.getStartOfMonth(date);
        Calendar cal = Calendar.getInstance();
        cal.setTime(startOfThisMonth);
        cal.add(Calendar.DAY_OF_WEEK, -1);
        Date startOfPrevMonth = CommonFunctions.getStartOfMonth(cal.getTime());
        Date from = getFirstDayOfWeek(startOfPrevMonth);
        Date endOfPrevMonth = CommonFunctions.getEndOfMonth(cal.getTime());
        Date to = getFirstDayOfWeek(endOfPrevMonth);
        cal.setTime(endOfPrevMonth);
        if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            cal.setTime(to);
            cal.add(Calendar.DAY_OF_WEEK, -1);
        }

        to = cal.getTime();

        dateRange.setFromDate(from);
        dateRange.setToDate(to);

        return dateRange;
    }

    public DateRange getDateRange(Date from, int range) {
        DateRange dateRange = new DateRange();
        Calendar cal = Calendar.getInstance();
        cal.setTime(from);
        cal.add(Calendar.DATE, range);
        Date to = cal.getTime();

        dateRange.setFromDate(from);
        dateRange.setToDate(to);

        return dateRange;
    }

    public static List<String> extractPlaceholders(String htmlContent) {
        List<String> placeholders = new ArrayList<>();
        if (htmlContent != null) {
            Pattern pattern = Pattern.compile("\\{(.*?)\\}");
            Matcher matcher = pattern.matcher(htmlContent);
            while (matcher.find()) {
                placeholders.add(matcher.group(1));
            }
        }
        return placeholders;
    }

    public static String replaceText(Map<String, String> m, String template) {
        String output = template;
        if (m == null || m.isEmpty()) {
            return output;
        }
        for (Map.Entry<String, String> entry : m.entrySet()) {
            output = output.replaceAll(entry.getKey(), entry.getValue());
        }
        return output;
    }

    public static String formatDate(Date date, String pattern) {
        String dateString = "";
        if (date == null) {
            return dateString;
        }
        if (pattern == null || pattern.trim().isEmpty()) {
            pattern = "dd-MM-yyyy";
        }
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            dateString = simpleDateFormat.format(date);
        } catch (Exception e) {
            dateString = "";
        }
        return dateString;
    }

    public static Date parseDate(String dateInString, String format) {
        if (format == null || format.trim().isEmpty()) {
            format = "dd MM yyyy";
        }
        SimpleDateFormat formatter = new SimpleDateFormat(format, Locale.ENGLISH);
        Date date;
        try {
            date = formatter.parse(dateInString);
        } catch (ParseException ex) {
            date = null;
        }
        return date;
    }

    public static boolean checkToDateAreInSameDay(Date firstDate, Date secondDate) {

        Date startOfDay = getStartOfDay(firstDate);
        Date endOfDay = getEndOfDay(firstDate);

        return startOfDay.before(secondDate) && endOfDay.after(secondDate);
    }

    public static Date getAddedDate(Date date, int range) {

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, range);

        return cal.getTime();
    }

    public static Long getDayCountTillNow(Date date) {
        if (date == null) {
            return 0L;
        }

        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date);

        return (cal1.getTimeInMillis() - cal2.getTimeInMillis()) / (1000 * 60 * 60 * 24);

    }

    public static Long getDayCount(Date frm, Date to) {
        if (frm == null) {
            return 0L;
        }

        if (to == null) {
            to = new Date();
        }

        to = getEndOfDay(to);
        frm = getStartOfDay(frm);

        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(to);
        cal2.setTime(frm);

        Long inDays = (cal1.getTimeInMillis() - cal2.getTimeInMillis()) / (1000 * 60 * 60 * 24);

        //we need to 1 because date rangs is missing one day as it between days
        inDays++;
        return inDays;
    }

    public static Date guessDob(String docStr) {
        try {
            int years = Integer.parseInt(docStr);
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("IST"));
            now.add(Calendar.YEAR, -years);
            return now.getTime();
        } catch (Exception e) {
            return new Date();
        }
    }

    public static Date guessDob(YearMonthDay yearMonthDay) {
        int years;
        int month;
        int day;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("IST"));
        try {
            if (yearMonthDay.getYear() != null && !yearMonthDay.getYear().isEmpty()) {
                years = Integer.parseInt(yearMonthDay.getYear());
                now.add(Calendar.YEAR, -years);
            }

            if (yearMonthDay.getMonth() != null && !yearMonthDay.getMonth().isEmpty()) {
                month = Integer.parseInt(yearMonthDay.getMonth());
                now.add(Calendar.MONTH, -month);
            }

            if (yearMonthDay.getDay() != null && !yearMonthDay.getDay().isEmpty()) {
                day = Integer.parseInt(yearMonthDay.getDay());
                now.add(Calendar.DATE, -day);
            }

            return now.getTime();
        } catch (Exception e) {
            return new Date();

        }
    }

    public Date guessDobFromMonth(String docStr) {
        try {
            int month = Integer.parseInt(docStr);
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("IST"));
            now.add(Calendar.MONTH, -month);
            return now.getTime();
        } catch (Exception e) {
            return new Date();

        }
    }

    public String calculateAge(Date dob, Date toDate) {
        if (dob == null || toDate == null) {
            return "";
        }
        long ageInDays;
        ageInDays = (toDate.getTime() - dob.getTime()) / (1000 * 60 * 60 * 24);
        if (ageInDays < 0) {
            return "";
        }
        if (ageInDays < 60) {
            return ageInDays + " days";
        } else if (ageInDays < 366) {
            return (ageInDays / 30) + " months";
        } else {
            return (ageInDays / 365) + " years";
        }
    }

    public long calculateDurationTime(Date dob, Date toDate) {
        if (dob == null || toDate == null) {
            return 0;
        }
        long durationHours;
        durationHours = (toDate.getTime() - dob.getTime()) / (1000 * 60 * 60);
        return durationHours;
    }

    public static long calculateDurationMin(Date dob, Date toDate) {
        if (dob == null || toDate == null || dob.getTime() > toDate.getTime()) {
            return 0;
        }
        long dMin;
        dMin = (toDate.getTime() - dob.getTime()) / (1000 * 60);
        return dMin;
    }

    public static Date getFirstDayOfYear() {
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(cal.get(Calendar.YEAR), 0, 1, 0, 0, 0);
        return cal.getTime();
    }

    public static Date getNextDateStart(Date fromDate) {
        if (fromDate == null) {
            fromDate = new Date();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(fromDate);
        cal.add(Calendar.DAY_OF_MONTH, 1); // Move to the next day
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public Date getLastDayOfYear() {
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MONTH, 11);
        cal.set(Calendar.DATE, 31);
        cal.set(cal.get(Calendar.YEAR), 11, 31, 23, 59, 59);
        return cal.getTime();
    }

    public static Date getStartOfBeforeDay(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, -1);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, day, 0, 0, 0);
        ////// // System.out.println("calendar.getTime() = " + calendar.getTime());
        return calendar.getTime();
    }

    public static YearMonthDay guessAge(Date dofb) {
        YearMonthDay yearMonthDay = new YearMonthDay();

//        Calendar cal=Calendar.getInstance();
//        cal.setTime(dob);
//        //// // System.out.println("cal.get(Calendar.YEAR) = " + cal.get(Calendar.YEAR));
//        //// // System.out.println("cal.get(Calendar.MONTH) = " + cal.get(Calendar.MONTH)+1);
//        //// // System.out.println("cal.get(Calendar.DATE) = " + cal.get(Calendar.DATE));
//        LocalDate birthDay=new LocalDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DATE));
//        LocalDate now=new LocalDate();
        LocalDate dob = new LocalDate(dofb);
        LocalDate date = new LocalDate(new Date());

        Period period = new Period(dob, date, PeriodType.yearMonthDay());
        int ageYears = period.getYears();
        int ageMonths = period.getMonths();
        int ageDays = period.getDays();

//        Years years=Years.yearsBetween(birthDay, now);
//        Months months=Months.monthsBetween(birthDay, now);
//        Days days=Days.daysBetween(birthDay, now);
//        //// // System.out.println("years.getYears() = " + years.getYears());
        yearMonthDay.setYear(Integer.toString(ageYears));
//        //// // System.out.println("months.getMonths() = " + months.getMonths());
        yearMonthDay.setMonth(Integer.toString(ageMonths));
//        //// // System.out.println("days.getDays() = " + days.getDays());
        yearMonthDay.setDay(Integer.toString(ageDays));

        return yearMonthDay;
    }

    public static Long convertDoubleToLong(Double value) {
        if (value == null) {
            return null;
        }

        return value.longValue();
    }

    public static Long convertStringToLongOrNull(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Date fetchDateOfBirthFromAge(Integer ageInt) {
        if (ageInt == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date()); // Sets the calendar to the current date
        cal.add(Calendar.YEAR, -ageInt); // Subtracts the age from the current year to get the birth year
        return cal.getTime(); // Returns the computed date
    }

    public static String convertDoubleToString(Double value) {
        if (value == null) {
            return null; // or throw an IllegalArgumentException depending on your requirements
        }

        // Convert Double to String
        return String.valueOf(value);
    }
}
