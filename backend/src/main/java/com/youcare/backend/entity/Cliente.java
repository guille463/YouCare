package com.youcare.backend.entity;

import java.time.LocalDate;
import com.youcare.backend.entity.enums.Rol;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

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
    public Cliente(String email, String contrasenia, String nombre, String apellidos, LocalDate fechaNacimiento) {
        super(email, contrasenia, nombre, apellidos, Rol.CLIENTE);
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


}