package com.example.demo.Review;

import java.time.LocalDateTime;

import com.example.demo.MemberApartment.ApartmentMemberEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"review_member_id", "reviewed_member_id"})
})
public class Review {

    @Id
    @SequenceGenerator(name = "reviews_seq", sequenceName = "reviews_seq", initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reviews_seq")
    private Integer id;

    @NotNull
    @Max(5)
    @Min(1)
    @Column(nullable = false)
    private Integer rating;

    @NotNull
    @Size(max = 500)
    @Column(length = 500)
    private String comment;

    @NotNull
    @Size(max = 500)
    @Column(length = 500)
    private String response;

    @NotNull
    @OneToOne
    private ApartmentMemberEntity reviewMember;

    @NotNull
    @OneToOne
    private ApartmentMemberEntity reviewedMember;

    @NotNull
    @Column(nullable = false)
    private Boolean published;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime reviewDate; 

    public Review() {
    }

    public Review(Integer id, Integer rating, String comment, String response, ApartmentMemberEntity reviewMember,
            ApartmentMemberEntity reviewedMember, Boolean published, LocalDateTime reviewDate) {
        this.id = id;
        this.rating = rating;
        this.comment = comment;
        this.response = response;
        this.reviewMember = reviewMember;
        this.reviewedMember = reviewedMember;
        this.published = published;
        this.reviewDate = reviewDate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public ApartmentMemberEntity getReviewMember() {
        return reviewMember;
    }

    public void setReviewMember(ApartmentMemberEntity reviewMember) {
        this.reviewMember = reviewMember;
    }

    public ApartmentMemberEntity getReviewedMember() {
        return reviewedMember;
    }

    public void setReviewedMember(ApartmentMemberEntity reviewedMember) {
        this.reviewedMember = reviewedMember;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }

    public LocalDateTime getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(LocalDateTime reviewDate) {
        this.reviewDate = reviewDate;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

}
