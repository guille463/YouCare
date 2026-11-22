package com.youcare.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Period;

import com.youcare.backend.config.Config;
import com.youcare.backend.entity.enums.Rol;
import com.youcare.backend.exception.EdadMinimaException;

/**
 * Usuario que contrata servicios de cuidados para sí mismo o para un familiar.
 * <p>
 * Hereda los datos comunes de {@link Usuario} y añade su fecha de nacimiento
 * y su dirección postal. Se guarda en la tabla {@code cliente}, que comparte
 * clave primaria con {@code usuario}.
 * </p>
 *
 */
@Entity
@Table(name = "cliente")
public class Cliente extends Usuario {

    /** Fecha de nacimiento, usada para comprobar la mayoría de edad. */
    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    /** Dirección del cliente. */
    private String direccion;

    /** Ciudad de residencia. */
    private String ciudad;

    // ========== CONSTRUCTORES ===========

    /**
     * Constructor sin argumentos requerido por JPA. No debe usarse directamente.
     */
    protected Cliente() {
    }

    /**
     * Crea un cliente con los datos obligatorios. La dirección y el resto de
     * datos opcionales se asignan después con sus setters.
     *
     * @param email           correo electrónico, único en la plataforma
     * @param contrasenia     hash BCrypt de la contraseña, nunca la contraseña en
     *                        claro
     * @param nombre          nombre completo del cliente
     * @param fechaNacimiento fecha de nacimiento del cliente
     */
    public Cliente(String email, String contrasenia, String nombre, LocalDate fechaNacimiento) {
        super(email, contrasenia, nombre, Rol.CLIENTE);
        this.fechaNacimiento = fechaNacimiento;
    }

    // ============= GETTERS & SETTERS =============

    /** @return fecha de nacimiento */
    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    /**
     * @param fechaNacimiento fecha de nacimiento; el cliente debe ser mayor de edad
     */
    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        validarMayorDeEdad(fechaNacimiento);
        this.fechaNacimiento = fechaNacimiento;
    }

    /** @return dirección postal, o {@code null} si no la ha indicado */
    public String getDireccion() {
        return direccion;
    }

    /** @param direccion dirección postal del cliente */
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    /** @return ciudad de residencia, o {@code null} si no la ha indicado */
    public String getCiudad() {
        return ciudad;
    }

    /** @param ciudad ciudad de residencia */
    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    // ============= MÉTODOS ==========
    /**
     * Comprueba que la fecha existe y que el cliente alcanza la edad mínima.
     *
     * @param fechaNacimiento fecha a comprobar
     * @throws IllegalArgumentException si la fecha es {@code null}
     * @throws EdadMinimaException      si el cliente no alcanza la edad mínima
     */
    private static void validarMayorDeEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new IllegalArgumentException("La fecha de nacimiento es obligatoria");
        }
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < Config.EDAD_MINIMA_REGISTRO) {
            throw new EdadMinimaException();
        }
    }
}