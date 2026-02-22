package com.example.demo.Vivienda;

import com.example.demo.User.UserEntity;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "miembros_vivienda",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_miembro_vivienda_usuario", columnNames = {"vivienda_id", "usuario_id"})
        }
)
public class MiembroViviendaEntity {

    @Id
    @SequenceGenerator(name = "miembro_vivienda_seq", sequenceName = "miembro_vivienda_seq", initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "miembro_vivienda_seq")
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "vivienda_id", nullable = false)
    private ViviendaEntity vivienda;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UserEntity usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MiembroRol rol;

    @Column(nullable = false)
    private LocalDate fechaIngreso;

    public MiembroViviendaEntity() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ViviendaEntity getVivienda() {
        return vivienda;
    }

    public void setVivienda(ViviendaEntity vivienda) {
        this.vivienda = vivienda;
    }

    public UserEntity getUsuario() {
        return usuario;
    }

    public void setUsuario(UserEntity usuario) {
        this.usuario = usuario;
    }

    public MiembroRol getRol() {
        return rol;
    }

    public void setRol(MiembroRol rol) {
        this.rol = rol;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }
}




