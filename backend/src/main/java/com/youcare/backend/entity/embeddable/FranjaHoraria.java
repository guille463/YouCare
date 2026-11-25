package com.youcare.backend.entity.embeddable;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

import com.youcare.backend.config.Config;
import com.youcare.backend.exception.ValidacionException;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Franja horaria semanal en la que un cuidador está disponible.
 * <p>
 * Para cambiar una franja se elimina y se añade otra.
 * </p>
 */
@Embeddable
public class FranjaHoraria {

    /** Día de la semana de la franja. */
    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = Config.MAX_LONGITUD_DIA_SEMANA)
    private DayOfWeek diaSemana;

    /** Hora de inicio (incluida). */
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    /** Hora de fin (excluida). */
    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    /**
     * Constructor sin argumentos requerido por JPA. No debe usarse directamente.
     */
    protected FranjaHoraria() {
    }

    /**
     * @param diaSemana  día de la semana (MONDAY, TUESDAY, ...)
     * @param horaInicio hora de inicio
     * @param horaFin    hora de fin, posterior a la de inicio
     */
    public FranjaHoraria(DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFin) {
        if (diaSemana == null) {
            throw new ValidacionException("diaSemana", "El día de la semana es obligatorio");
        }
        if (horaInicio == null || horaFin == null) {
            throw new ValidacionException("horaInicio", "Las horas de inicio y de fin son obligatorias");
        }
        if (!horaFin.isAfter(horaInicio)) {
            throw new ValidacionException("horaFin", "La hora de fin debe ser posterior a la de inicio");
        }
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    /**
     * @param franjaAComprobar otra franja
     * @return {@code true} si es el mismo día y los horarios se pisan
     */
    public boolean seSolapan(FranjaHoraria franjaAComprobar) {
        return this.diaSemana == franjaAComprobar.getDiaSemana()
                && this.horaInicio.isBefore(franjaAComprobar.getHoraFin())
                && franjaAComprobar.getHoraInicio().isBefore(this.horaFin);
    }

    // ============= GETTERS =============

    /** @return día de la semana */
    public DayOfWeek getDiaSemana() {
        return diaSemana;
    }

    /** @return hora de inicio */
    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    /** @return hora de fin */
    public LocalTime getHoraFin() {
        return horaFin;
    }

          // ============= EQUALS & HASHCODE =============

    /** Dos franjas son iguales si tienen el mismo día y las mismas horas. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FranjaHoraria otra)) {
            return false;
        }
        return diaSemana == otra.diaSemana
                && Objects.equals(horaInicio, otra.horaInicio)
                && Objects.equals(horaFin, otra.horaFin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(diaSemana, horaInicio, horaFin);
    }
    
}