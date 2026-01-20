package com.divudi.core.data.dto;

import java.util.Date;

import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import com.divudi.core.data.Sex;

public class InwardAdmissionDemographicDataDTO {

    // referring consultant data
    private Long id;
    private String doctorName;
    private String specialityName;

    // patient demographic data
    private int patientAge;
    private Sex patientSex;
    
    // gender-wise counters
    private int maleCount;
    private int femaleCount;
    private int otherCount;
    private int unknownCount;

    // age group-wise counters
    private int noAgeCount;
    private int belowOneYearCount;
    private int oneToFiveYearsCount;
    private int fiveToTenYearsCount;
    private int tenToFifteenYearsCount;
    private int fifteenToTwentyYearsCount;
    private int twentyToThirtyYearsCount;
    private int thirtyToFortyYearsCount;
    private int fortyToFiftyYearsCount;
    private int aboveFiftyYearsCount;

    private int totalCount;

    // Constructor for processing data
    public InwardAdmissionDemographicDataDTO(String specialityName, String doctorName) {
        this.specialityName = specialityName;
        
        if (doctorName != null) {
            this.doctorName = doctorName;
        }
        
        this.maleCount = 0;
        this.femaleCount = 0;
        this.otherCount = 0;
        this.unknownCount = 0;
        this.noAgeCount = 0;
        this.belowOneYearCount = 0;
        this.oneToFiveYearsCount = 0;
        this.fiveToTenYearsCount = 0;
        this.tenToFifteenYearsCount = 0;
        this.fifteenToTwentyYearsCount = 0;
        this.twentyToThirtyYearsCount = 0;
        this.thirtyToFortyYearsCount = 0;
        this.fortyToFiftyYearsCount = 0;
        this.aboveFiftyYearsCount = 0;
        this.totalCount = 0;
    }

    // Constructor for fetching data from database (Doctor Wise)
    public InwardAdmissionDemographicDataDTO(Long id, String doctorName, String specialityName, Date dob, Sex sex) {
        this.id = id;
        this.doctorName = doctorName;
        this.specialityName = specialityName;
        this.patientSex = (sex != null ? sex : Sex.Unknown);
        calShortAgeFromDob(dob);
    }
    
    // Constructor for fetching data from database (Doctor Wise)
    public InwardAdmissionDemographicDataDTO(Long id, String specialityName, Date dob, Sex sex) {
        this.id = id;
        this.specialityName = specialityName;
        this.patientSex = (sex != null ? sex : Sex.Unknown);
        calShortAgeFromDob(dob);
    }

    // Calculation of age from date of birth
    public void calShortAgeFromDob(Date dob) {
        if (dob == null) {
            this.patientAge = -1;
            return;
        }

        LocalDate ldDob = new LocalDate(dob);
        LocalDate currentDate = LocalDate.now();

        Period period = new Period(ldDob, currentDate, PeriodType.years());
        this.patientAge = period.getYears();
    }

    public void incrementGenderCount(Sex patientSex) {
        if (patientSex == null) {
            return;
        }
        switch (patientSex) {
            case Male:
                maleCount++;
                break;  
            case Female:
                femaleCount++;
                break;  
            case Other:
                otherCount++;
                break;
            case Unknown:
                unknownCount++;
                break;
            default:
                break;
        }
    }

    public void incrementAgeGroupCount(int age) {
        if (age == -1) {
            noAgeCount++;
        } else if (age < 1) {
            belowOneYearCount++;
        } else if (age < 5) {
            oneToFiveYearsCount++;
        } else if (age < 10) {
            fiveToTenYearsCount++;
        } else if (age < 15) {
            tenToFifteenYearsCount++;
        } else if (age < 20) {
            fifteenToTwentyYearsCount++;
        } else if (age < 30) {
            twentyToThirtyYearsCount++;
        } else if (age < 40) {
            thirtyToFortyYearsCount++;
        } else if (age < 50) {
            fortyToFiftyYearsCount++;
        } else {
            aboveFiftyYearsCount++;
        }
    }

    public void incrementTotalCount() {
        totalCount++;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getSpecialityName() {
        return specialityName;
    }

    public void setSpecialityName(String specialityName) {
        this.specialityName = specialityName;
    }

    public int getPatientAge() {
        return patientAge;
    }

    public void setPatientAge(int patientAge) {
        this.patientAge = patientAge;
    }

    public Sex getPatientSex() {
        return patientSex;
    }

    public void setPatientSex(Sex patientSex) {
        this.patientSex = patientSex;
    }

    public int getMaleCount() {
        return maleCount;
    }

    public void setMaleCount(int maleCount) {
        this.maleCount = maleCount;
    }

    public int getFemaleCount() {
        return femaleCount;
    }

    public void setFemaleCount(int femaleCount) {
        this.femaleCount = femaleCount;
    }

    public int getOtherCount() {
        return otherCount;
    }

    public void setOtherCount(int otherCount) {
        this.otherCount = otherCount;
    }
    
    public int getUnknownCount() {
        return unknownCount;
    }

    public void setUnknownCount(int unknownCount) {
        this.unknownCount = unknownCount;
    }
    
    public int getNoAgeCount() {
        return noAgeCount;
    }

    public void setNoAgeCount(int noAgeCount) {
        this.noAgeCount = noAgeCount;
    }

    public int getBelowOneYearCount() {
        return belowOneYearCount;
    }

    public void setBelowOneYearCount(int belowOneYearCount) {
        this.belowOneYearCount = belowOneYearCount;
    }

    public int getOneToFiveYearsCount() {
        return oneToFiveYearsCount;
    }

    public void setOneToFiveYearsCount(int oneToFiveYearsCount) {
        this.oneToFiveYearsCount = oneToFiveYearsCount;
    }

    public int getFiveToTenYearsCount() {
        return fiveToTenYearsCount;
    }

    public void setFiveToTenYearsCount(int fiveToTenYearsCount) {
        this.fiveToTenYearsCount = fiveToTenYearsCount;
    }

    public int getTenToFifteenYearsCount() {
        return tenToFifteenYearsCount;
    }

    public void setTenToFifteenYearsCount(int tenToFifteenYearsCount) {
        this.tenToFifteenYearsCount = tenToFifteenYearsCount;
    }

    public int getFifteenToTwentyYearsCount() {
        return fifteenToTwentyYearsCount;
    }

    public void setFifteenToTwentyYearsCount(int fifteenToTwentyYearsCount) {
        this.fifteenToTwentyYearsCount = fifteenToTwentyYearsCount;
    }

    public int getTwentyToThirtyYearsCount() {
        return twentyToThirtyYearsCount;
    }

    public void setTwentyToThirtyYearsCount(int twentyToThirtyYearsCount) {
        this.twentyToThirtyYearsCount = twentyToThirtyYearsCount;
    }

    public int getThirtyToFortyYearsCount() {
        return thirtyToFortyYearsCount;
    }

    public void setThirtyToFortyYearsCount(int thirtyToFortyYearsCount) {
        this.thirtyToFortyYearsCount = thirtyToFortyYearsCount;
    }

    public int getFortyToFiftyYearsCount() {
        return fortyToFiftyYearsCount;
    }

    public void setFortyToFiftyYearsCount(int fortyToFiftyYearsCount) {
        this.fortyToFiftyYearsCount = fortyToFiftyYearsCount;
    }

    public int getAboveFiftyYearsCount() {
        return aboveFiftyYearsCount;
    }

    public void setAboveFiftyYearsCount(int aboveFiftyYearsCount) {
        this.aboveFiftyYearsCount = aboveFiftyYearsCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }


}
