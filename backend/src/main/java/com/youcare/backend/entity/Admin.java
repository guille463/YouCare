package com.youcare.backend.entity;

import com.youcare.backend.entity.enums.Rol;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Usuario con permisos de administración y moderación en la plataforma YouCare.
 * <p>
 * Hereda los datos comunes de {@link Usuario}. Se guarda en la tabla
 * {@code admin}, que comparte clave primaria con {@code usuario}.
 * </p>
 */
@Entity
@Table(name = "admin")
public class Admin extends Usuario {

    /**
     * Cargo o departamento interno del administrador (opcional).
     * Ejemplo: "Soporte", "Moderación", "Superadmin".
     */
    @Column(length = 100)
    private String departamento;

    /**
     * Constructor sin argumentos requerido por JPA. No debe usarse directamente.
     */
    protected Admin() {
    }

    /**
     * Crea un administrador con los datos obligatorios.
     *
     * @param email           correo electrónico, único en la plataforma
     * @param hashContrasenia hash BCrypt de la contraseña, nunca la contraseña en claro
     * @param nombre          nombre de pila del administrador
     * @param apellidos       apellidos del administrador
     */
    public Admin(String email, String hashContrasenia, String nombre, String apellidos) {
        super(email, hashContrasenia, nombre, apellidos, Rol.ADMIN);
    }

    // ============= GETTERS/SETTERS =============

    /** @return departamento o cargo interno, o {@code null} si no se ha indicado */
    public String getDepartamento() {
        return departamento;
    }

    /** @param departamento departamento o cargo del administrador */
    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }
}