package com.example.demo.MemberApartment;

import com.example.demo.Apartment.ApartmentEntity;

import jakarta.persistence.*;

@Entity
@Table(name = "apartment_rules")
public class ApartmentRuleEntity {

    @Id
    @SequenceGenerator(name = "apartment_rule_seq", sequenceName = "apartment_rule_seq", initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "apartment_rule_seq")
    private Integer id;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "apartment_id", nullable = false, unique = true)
    private ApartmentEntity apartment;

    @Column(nullable = false)
    private boolean allowsPets;

    @Column(nullable = false)
    private boolean allowsSmokers;

    @Column(nullable = false)
    private boolean partiesAllowed;

    public ApartmentRuleEntity() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ApartmentEntity getApartment() {
        return apartment;
    }

    public void setApartment(ApartmentEntity apartment) {
        this.apartment = apartment;
    }

    public boolean isAllowsPets() {
        return allowsPets;
    }

    public void setAllowsPets(boolean allowsPets) {
        this.allowsPets = allowsPets;
    }

    public boolean isAllowsSmokers() {
        return allowsSmokers;
    }

    public void setAllowsSmokers(boolean allowsSmokers) {
        this.allowsSmokers = allowsSmokers;
    }

    public boolean isPartiesAllowed() {
        return partiesAllowed;
    }

    public void setPartiesAllowed(boolean partiesAllowed) {
        this.partiesAllowed = partiesAllowed;
    }
}




