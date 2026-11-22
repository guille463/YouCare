package com.youcare.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

import com.youcare.backend.entity.enums.Rol;

/**
 * Entidad base de todos los usuarios de la plataforma YouCare.
 * <p>
 * Agrupa los datos comunes a clientes, cuidadores, administradores y empresas,
 * incluida la imagen de perfil y la ubicación usada en la búsqueda por radio.
 * Usa herencia {@code JOINED}: cada subclase tiene su propia tabla, que
 * comparte clave primaria con {@code usuario}.
 * </p>
 *
 * @author Jorge Martín Llaneli
 */
@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Usuario {

    /** Identificador único del usuario (PK autogenerada). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Correo electrónico, único y usado para iniciar sesión. */
    @Column(nullable = false, unique = true)
    private String email;

    /** Hash BCrypt de la contraseña. La contraseña real nunca se almacena. */
    @Column(name = "contrasenia", nullable = false)
    private String contrasenia;

    /** Nombre completo del usuario o razón social si es una empresa. */
    @Column(nullable = false, unique = true)
    private String nombre;

    /** Teléfono de contacto. */
    private String telefono;

    /** URL de la imagen de perfil, común a todos los tipos de usuario. */
    @Column(name = "imagen_perfil_url")
    private String imagenPerfilUrl;

    /**
     * Latitud de la ubicación en grados (WGS84), o {@code null} si no la ha
     * indicado.
     */
    private Double latitud;

    /**
     * Longitud de la ubicación en grados (WGS84), o {@code null} si no la ha
     * indicado.
     */
    private Double longitud;

    /** Rol del usuario en la plataforma. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    /** Indica si la cuenta está activa. Una cuenta inactiva no puede acceder. */
    @Column(nullable = false)
    private boolean activo = true;

    /** Indica si el usuario ha confirmado que su correo existe y es suyo. */
    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado = false;

    /**
     * Fecha y hora de registro (UTC), asignada automáticamente al crear la cuenta.
     */
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Instant fechaRegistro;

    // ========== CONSTRUCTORES ===========

    /**
     * Constructor sin argumentos requerido por JPA. No debe usarse directamente.
     */
    protected Usuario() {
    }

    /**
     * Crea un usuario con los datos obligatorios. Los demás campos se asignan
     * con sus setters o toman su valor por defecto.
     *
     * @param email       correo electrónico, único en la plataforma
     * @param contrasenia hash BCrypt de la contraseña, nunca la contraseña en claro
     * @param nombre      nombre completo o razón social si es una empresa
     * @param rol         rol del usuario, fijado por cada subclase
     */
    protected Usuario(String email, String contrasenia, String nombre, Rol rol) {
        this.email = email;
        this.contrasenia = contrasenia;
        this.nombre = nombre;
        this.rol = rol;
    }

    // ============= MÉTODOS =============

    /** Asigna la fecha de registro justo antes de guardar por primera vez. */
    @PrePersist
    void alCrear() {
        this.fechaRegistro = Instant.now();
    }

    /** @return identificador único del usuario */
    public Long getId() {
        return id;
    }

    /** @return correo electrónico del usuario */
    public String getEmail() {
        return email;
    }

    /** @param email correo electrónico, único en la plataforma */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return hash de la contraseña */
    public String getContrasenia() {
        return contrasenia;
    }

    /**
     * @param contrasenia hash BCrypt de la contraseña, nunca la contraseña en claro
     */
    public void setContrasenia(String contrasenia) {
        this.contrasenia = contrasenia;
    }

    /** @return nombre completo o razón social */
    public String getNombre() {
        return nombre;
    }

    /**
     * @param nombre nombre completo del usuario o razón social si es una empresa
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /** @return teléfono de contacto, o {@code null} si no lo ha indicado */
    public String getTelefono() {
        return telefono;
    }

    /** @param telefono teléfono de contacto */
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    /** @return URL de la imagen de perfil, o {@code null} si no tiene */
    public String getImagenPerfilUrl() {
        return imagenPerfilUrl;
    }

    /** @param imagenPerfilUrl URL de la imagen de perfil */
    public void setImagenPerfilUrl(String imagenPerfilUrl) {
        this.imagenPerfilUrl = imagenPerfilUrl;
    }

    /** @return latitud en grados, o {@code null} si no tiene ubicación */
    public Double getLatitud() {
        return latitud;
    }

    /** @param latitud latitud en grados, entre -90 y 90 */
    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    /** @return longitud en grados, o {@code null} si no tiene ubicación */
    public Double getLongitud() {
        return longitud;
    }

    /** @param longitud longitud en grados, entre -180 y 180 */
    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    /** @return rol del usuario */
    public Rol getRol() {
        return rol;
    }

    /** @return {@code true} si la cuenta está activa */
    public boolean isActivo() {
        return activo;
    }

    /**
     * @param activo {@code true} para activar la cuenta, {@code false} para
     *               desactivarla
     */
    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    /** @return {@code true} si el correo ya ha sido verificado */
    public boolean isEmailVerificado() {
        return emailVerificado;
    }

    /** @param emailVerificado {@code true} cuando el usuario confirma su correo */
    public void setEmailVerificado(boolean emailVerificado) {
        this.emailVerificado = emailVerificado;
    }

    /** @return fecha y hora de registro (UTC) */
    public Instant getFechaRegistro() {
        return fechaRegistro;
    }
}