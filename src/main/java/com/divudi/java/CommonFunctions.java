/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.java;

import com.divudi.bean.common.SessionController;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.data.dataStructure.DateRange;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.entity.Bill;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

/**
 *
 * @author buddhika
 */
public class CommonFunctions {

    private SessionController sessionController;

    private static final String[] units = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine"};
    private static final String[] teens = {"Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
    private static final String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

    
    public static Long removeSpecialCharsInPhonenumber(String phonenumber) {
        try {
            if (phonenumber == null || phonenumber.trim().equals("")) {
                return null;
            }
            String cleandPhoneNumber = phonenumber.replaceAll("[\\s+\\-()]", "");
            Long convertedPhoneNumber = Long.parseLong(cleandPhoneNumber);
            return convertedPhoneNumber;
        } catch (Exception e) {
            return null;
        }
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
        int decimalPart = (int) (Double.parseDouble(String.format("%.2f", number % 1)) * 100);
        //System.out.println(number);
        //System.out.println(number - intPart);
        //System.out.println(String.format("%.2f", number%1));
        //System.out.println(number);
        //System.out.println(number - intPart);

        // System.out.println(String.format("%.2f", decimalPart));
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

    //----------Date Time Formats
    public static String getDateFormat(Date date) {
        String s = "";
        DateFormat d = new SimpleDateFormat("YYYY-MM-dd");
        s = d.format(date);
        return s;
    }

    public static String getDateFormat(Date date, String formatString) {
        if (date == null) {
            date = new Date();
        }
        if (formatString == null || formatString.trim().equals("")) {
            formatString = "dd MMMM yyyy";
        }
        String s;
        DateFormat d = new SimpleDateFormat(formatString);
        s = d.format(date);
        return s;
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

    public static Long convertStringToLongByRemoveSpecialChars(String phonenumber) {
        if (phonenumber == null || phonenumber.trim().equals("")) {
            return null;
        }
        try {
            String cleandPhoneNumber = phonenumber.replaceAll("[\\s+\\-()]", "");
            Long convertedPhoneNumber = Long.parseLong(cleandPhoneNumber);
            return convertedPhoneNumber;
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

    public static LocalDateTime getLocalDateTime(Date dateTime) {
        Date input = dateTime;
        LocalDateTime date = input.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return date;
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
            return 0l;
        }
        long ageInDays;
        ageInDays = (toDate.getTime() - dob.getTime()) / (1000 * 60 * 60 * 24);
        if (ageInDays < 0) {
            ageInDays = 0;
        }
        return ageInDays;
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

    public static Boolean checkAgeSex(Date dob, Sex sex, Title title) {
        Boolean result = true;
        Date toDate = Calendar.getInstance().getTime();

        long age;

        if ((toDate.getTime() - dob.getTime()) / (1000 * 60 * 60 * 24) == 0) {
            return false;
        }

        age = ((toDate.getTime() - dob.getTime()) / (1000 * 60 * 60 * 24)) / 365;

        if (title == Title.Baby || title == Title.Baby_Of) {
            if (age > 6) {
                result = false;
            }
        } else if ((title == Title.Master)) {
            if (age > 13) {
                result = false;
            }
        }

        if (title == Title.Mrs
                || title == Title.Mrs
                || title == Title.Ms
                || title == Title.Miss
                || title == Title.DrMrs
                || title == Title.DrMiss) {

            if (sex == Sex.Male) {
                result = false;
            }
        }

        if (title == Title.Mr
                || title == Title.Master
                || title == Title.Dr) {
            if (sex == Sex.Female) {
                result = false;
            }
        }

        return result;
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

    public static Date getStartOfMonth() {
        return getStartOfMonth(new Date());
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
        code = name.replaceAll(" ", "_");
        code = code.toLowerCase();
        return code;
    }

    public static String dateToString(Date date, String ddMMyyyy) {
        return formatDate(date, ddMMyyyy);
    }

    public Date getFirstDayOfYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        //////// // System.out.println("First : " + cal.getTime());
        return cal.getTime();
    }

    public Date getLastDayOfYear(Date date) {
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
        //      //////// // System.out.println("Start of this week:       " + cal.getTime());
        //       //////// // System.out.println("... in milliseconds:      " + cal.getTimeInMillis());

// start of the next week
//        cal.add(Calendar.WEEK_OF_YEAR, 1);
//        //////// // System.out.println("Start of the next week:   " + cal.getTime());
//        //////// // System.out.println("... in milliseconds:      " + cal.getTimeInMillis());
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
        //   //////// // System.out.println("Start of this week:       " + cal.getTime());
        //     //////// // System.out.println("... in milliseconds:      " + cal.getTimeInMillis());

        cal.add(Calendar.DATE, 7);

// start of the next week
//        cal.add(Calendar.WEEK_OF_YEAR, 1);
//        //////// // System.out.println("Start of the next week:   " + cal.getTime());
//        //////// // System.out.println("... in milliseconds:      " + cal.getTimeInMillis());
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
        double roundOff = (double) Math.round(num * mul) / mul;
        return roundOff;
    }

    public DateRange getDateRangeForOT(Date date) {
        DateRange dateRange = new DateRange();
        Date startOfThisMonth = com.divudi.java.CommonFunctions.getStartOfMonth(date);
        Calendar cal = Calendar.getInstance();
        cal.setTime(startOfThisMonth);
        cal.add(Calendar.DAY_OF_WEEK, -1);
        Date startOfPrevMonth = com.divudi.java.CommonFunctions.getStartOfMonth(cal.getTime());
        Date from = getFirstDayOfWeek(startOfPrevMonth);
        Date endOfPrevMonth = com.divudi.java.CommonFunctions.getEndOfMonth(cal.getTime());
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

        //System.err.println("From " + dateRange.getFromDate());
        //System.err.println("To " + dateRange.getToDate());
        return dateRange;
    }

    public static Map<String, String> getReplaceables(Bill b) {
        Map<String, String> m = new HashMap<>();
        if (b == null) {
            return m;
        }

        if (b.getInsId() != null) {
            m.put("{ins_id}", b.getInsId());
        }

        if (b.getDeptId() != null) {
            m.put("{dept_id}", b.getDeptId());
        }

        if (b.getId() != null) {
            m.put("{id}", b.getId() + "");
        }

        if (b.getDepartment() != null) {
            m.put("{department}", b.getDepartment().getName());
        }

        if (b.getInstitution() != null) {
            m.put("{institution}", b.getInstitution().getName());
        }

        if (b.getFromDepartment() != null) {
            m.put("{from_department}", b.getFromDepartment().getName());
        }

        if (b.getFromInstitution() != null) {
            m.put("{from_institution}", b.getFromInstitution().getName());
        }

        if (b.getToDepartment() != null) {
            m.put("{to_department}", b.getToDepartment().getName());
        }

        if (b.getToInstitution() != null) {
            m.put("{to_institution}", b.getToInstitution().getName());
        }
        DecimalFormat df = new DecimalFormat("0.00");

        m.put("{grand_total}", df.format(b.getGrantTotal()));
        m.put("{net_total}", df.format(b.getNetTotal()));
        m.put("{discount}", df.format(b.getDiscount()));
        m.put("{balance}", df.format(b.getBalance()));
        m.put("{paid}", df.format(b.getPaidAmount()));

        if (b.getBillDate() != null) {
            m.put("{date}", formatDate(b.getBillDate(), "dd/MMMM/yyyy"));
        }

        if (b.getBillTime() != null) {
            m.put("{time}", formatDate(b.getBillTime(), "hh:mm a"));
        }

        if (b.getCreater() != null) {
            m.put("{user}", b.getCreater().getName());
        }

        if (b.getPatientEncounter() != null) {
            Map<String, String> me = getReplaceables(b.getPatientEncounter());
            me.forEach(m::putIfAbsent);
        } else if (b.getPatient() != null) {
            Map<String, String> mp = getReplaceables(b.getPatient());
            mp.forEach(m::putIfAbsent);
        }
        return m;
    }

    public static Map<String, String> getReplaceables(Patient p) {
        Map<String, String> m = new HashMap<>();
        if (p == null) {
            return m;
        }
        m.put("{age}", p.getAge());
        m.put("{phn}", p.getPhn());
        if (p.getPerson() != null) {
            m.put("{name}", p.getPerson().getName());
            m.put("{sex}", p.getPerson().getSex().toString());
            m.put("{phone}", p.getPerson().getPhone());
            m.put("{address}", p.getPerson().getAddress());

        }
        return m;
    }

    public static Map<String, String> getReplaceables(PatientEncounter e) {
        Map<String, String> m = new HashMap<>();
        if (e == null) {
            return m;
        }
        if (e.getBhtNo() != null) {
            m.put("{bht}", e.getBhtNo());
        }
        if (e.getBhtNo() != null) {
            m.put("{bht}", e.getBhtNo());
        }
        if (e.getDateOfAdmission() != null) {
            m.put("{date_of_admission}", formatDate(e.getDateOfAdmission(), "dd/MMMM/yyyy"));
        }
        if (e.getDateOfDischarge() != null) {
            m.put("{date_of_discharge}", formatDate(e.getDateOfDischarge(), "dd/MMMM/yyyy"));
        }
        if (e.getBhtNo() != null) {
            m.put("{bht}", e.getBhtNo());
        }
        if (e.getOpdDoctor() != null) {
            m.put("{opd_doctor}", e.getOpdDoctor().getPerson().getNameWithTitle());
        }
        if (e.getReferringDoctor() != null) {
            m.put("{referring_doctor}", e.getReferringDoctor().getPerson().getNameWithTitle());
        }
        if (e.getPatient() != null) {
            Map<String, String> p = getReplaceables(e.getPatient());
            p.forEach(m::putIfAbsent);
        }
        return m;
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
        if (pattern == null || pattern.trim().equals("")) {
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
        if (format == null || format.trim().equals("")) {
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

        if (startOfDay.before(secondDate) && endOfDay.after(secondDate)) {
            return true;
        } else {
            return false;
        }
    }

    public Date getAddedDate(Date date, int range) {

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, range);
        Date returnDate = cal.getTime();

        return returnDate;
    }

    public Long getDayCountTillNow(Date date) {
        if (date == null) {
            return 0l;
        }

        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date);

        Long inDays = (cal1.getTimeInMillis() - cal2.getTimeInMillis()) / (1000 * 60 * 60 * 24);
        //System.err.println("INDAYS "+inDays);
        return inDays;

    }

    public static Long getDayCount(Date frm, Date to) {
        if (frm == null) {
            return 0l;
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

//        System.err.println("cal 1 " + cal1.getTimeInMillis());
//        System.err.println("cal 2 " + cal2.getTimeInMillis());
//        System.err.println("Frm " + frm);
//        System.err.println("TO " + to);
        Long inDays = (cal1.getTimeInMillis() - cal2.getTimeInMillis()) / (1000 * 60 * 60 * 24);
//        System.err.println("INDAYS " + inDays);

        //we need to 1 because date rangs is missing one day as it between days
        inDays++;
        return inDays;

    }

    public Date guessDob(String docStr) {
        //////// // System.out.println("year string is " + docStr);
        try {
            int years = Integer.valueOf(docStr);
            //////// // System.out.println("int year is " + years);
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("IST"));
            //////// // System.out.println("now before is " + now);
            now.add(Calendar.YEAR, -years);
            //////// // System.out.println("now after is " + now);
            //////// // System.out.println("now time is " + now.getTime());
            return now.getTime();
        } catch (Exception e) {
            //////// // System.out.println("Error is " + e.getMessage());
            return new Date();

        }
    }

    public Date guessDob(YearMonthDay yearMonthDay) {
        // //////// // System.out.println("year string is " + docStr);
        int years = 0;
        int month = 0;
        int day = 0;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("IST"));
        try {
            if (yearMonthDay.getYear() != null && !yearMonthDay.getYear().isEmpty()) {
                years = Integer.valueOf(yearMonthDay.getYear());
                now.add(Calendar.YEAR, -years);
            }

            if (yearMonthDay.getMonth() != null && !yearMonthDay.getMonth().isEmpty()) {
                month = Integer.valueOf(yearMonthDay.getMonth());
                now.add(Calendar.MONTH, -month);
            }

            if (yearMonthDay.getDay() != null && !yearMonthDay.getDay().isEmpty()) {
                day = Integer.valueOf(yearMonthDay.getDay());
                now.add(Calendar.DATE, -day);
            }

            return now.getTime();
        } catch (Exception e) {
            //////// // System.out.println("Error is " + e.getMessage());
            return new Date();

        }
    }

    public Date guessDobFromMonth(String docStr) {
        //////// // System.out.println("year string is " + docStr);
        try {
            int month = Integer.valueOf(docStr);
            //////// // System.out.println("int month is " + month);
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("IST"));
            //////// // System.out.println("now before is " + now);
            now.add(Calendar.MONTH, -month);
            //////// // System.out.println("now after is " + now);
            //////// // System.out.println("now time is " + now.getTime());
            return now.getTime();
        } catch (Exception e) {
            //////// // System.out.println("Error is " + e.getMessage());
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
        //////// // System.out.println("Age in days " + ageInDays);
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

    public static Date getEndOfMonth() {
        return getEndOfMonth(new Date());
    }

    public Date getFirstDayOfYear() {
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(cal.get(Calendar.YEAR), 0, 1, 0, 0, 0);
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

    public YearMonthDay guessAge(Date dofb) {
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

        return Double.valueOf(value).longValue();
    }

    public static Long convertStringToLong(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {

            return null;
        }
    }

    public static String convertDoubleToString(Double value) {
        if (value == null) {
            return null; // or throw an IllegalArgumentException depending on your requirements
        }

        // Convert Double to String
        return String.valueOf(value);
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

}
