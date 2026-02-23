package com.example.demo.MemberApartment;

import com.example.demo.Apartment.ApartmentEntity;

import jakarta.persistence.*;

@Entity
@Table(name = "reglas_vivienda")
public class ReglaViviendaEntity {

    @Id
    @SequenceGenerator(name = "regla_vivienda_seq", sequenceName = "regla_vivienda_seq", initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "regla_vivienda_seq")
    private Integer id;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "vivienda_id", nullable = false, unique = true)
    private ApartmentEntity vivienda;

    @Column(nullable = false)
    private boolean permiteMascotas;

    @Column(nullable = false)
    private boolean permiteFumadores;

    @Column(nullable = false)
    private boolean fiestasPermitidas;

    public ReglaViviendaEntity() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ApartmentEntity getVivienda() {
        return vivienda;
    }

    public void setVivienda(ApartmentEntity vivienda) {
        this.vivienda = vivienda;
    }

    public boolean isPermiteMascotas() {
        return permiteMascotas;
    }

    public void setPermiteMascotas(boolean permiteMascotas) {
        this.permiteMascotas = permiteMascotas;
    }

    public boolean isPermiteFumadores() {
        return permiteFumadores;
    }

    public void setPermiteFumadores(boolean permiteFumadores) {
        this.permiteFumadores = permiteFumadores;
    }

    public boolean isFiestasPermitidas() {
        return fiestasPermitidas;
    }

    public void setFiestasPermitidas(boolean fiestasPermitidas) {
        this.fiestasPermitidas = fiestasPermitidas;
    }
}




