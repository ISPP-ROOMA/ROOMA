package com.example.demo.ApartmentMatch.DTOs;

public class CandidateFilterDTO {
    private Integer minAge;
    private Integer maxAge;
    private String requiredProfession;
    private Boolean allowedSmoker;
    private String requiredSchedule;

    public CandidateFilterDTO() {
    }

    public Integer getMinAge() {
        return minAge;
    }

    public void setMinAge(Integer minAge) {
        this.minAge = minAge;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    public String getRequiredProfession() {
        return requiredProfession;
    }

    public void setRequiredProfession(String requiredProfession) {
        this.requiredProfession = requiredProfession;
    }

    public Boolean getAllowedSmoker() {
        return allowedSmoker;
    }

    public void setAllowedSmoker(Boolean allowedSmoker) {
        this.allowedSmoker = allowedSmoker;
    }

    public String getRequiredSchedule() {
        return requiredSchedule;
    }

    public void setRequiredSchedule(String requiredSchedule) {
        this.requiredSchedule = requiredSchedule;
    }
}
