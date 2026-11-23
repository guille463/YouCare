package com.youcare.backend.entity;

import java.time.Instant;

import com.youcare.backend.config.Config;
import com.youcare.backend.entity.enums.Rol;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Entidad base de todos los usuarios de la plataforma YouCare.
 * <p>
 * Agrupa los datos comunes a clientes, cuidadores, administradores y empresas,
 * incluida la imagen de perfil y la ubicación usada en la búsqueda por radio.
 * Usa herencia {@code JOINED}: cada subclase tiene su propia tabla, que
 * comparte clave primaria con {@code usuario}.
 * </p>
 *
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
    @Column(nullable = false, unique = true, length = Config.MAX_LONGITUD_EMAIL)
    private String email;

    /** Hash BCrypt de la contraseña. La contraseña en claro nunca se almacena. */
    @Column(name = "hash_contrasenia", nullable = false, length = Config.MAX_LONGITUD_HASH_CONTRASENIA)
    private String hashContrasenia;

    /** Nombre completo del usuario o razón social si es una empresa. */
    @Column(nullable = false, length = Config.MAX_LONGITUD_NOMBRE)
    private String nombre;

    /** Apellidos del usuario (en una empresa, de la persona de contacto). */
    @Column(nullable = false, length = Config.MAX_LONGITUD_APELLIDOS)
    private String apellidos;

    /** Teléfono de contacto. */
    @Column(length = Config.MAX_LONGITUD_TELEFONO, nullable = false)
    private String telefono;

    /**
     * Clave del objeto de la imagen de perfil en el almacenamiento (R2).
     * No es una URL: la URL firmada la genera {@code StorageService} en cada
     * petición.
     */
    @Column(name = "clave_imagen_perfil", length = Config.MAX_LONGITUD_CLAVE_IMG)
    private String claveImagenPerfil;

    /** Rol del usuario en la plataforma. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = Config.MAX_LONGITUD_ROL)
    private Rol rol;

    /** Indica si la cuenta está activa. Una cuenta bloqueada no puede acceder. */
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

    /**
     * Cantidad de advertencias que tiene la cuenta. Si alcanza la cantidad de
     * {@link Config#MAX_STRIKES} la cuenta queda inactiva
     */
    @Column(name = "cantidad_strikes", nullable = false)
    private int strikes;

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
     * @param email           correo electrónico, único en la plataforma
     * @param hashContrasenia hash BCrypt de la contraseña, nunca la contraseña en
     *                        claro
     * @param nombre          nombre de pila
     * @param apellidos       apellidos
     * @param rol             rol del usuario, fijado por cada subclase
     */
    protected Usuario(String email, String hashContrasenia, String nombre, String apellidos, Rol rol, String telefono) {
        this.email = email;
        this.hashContrasenia = hashContrasenia;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.rol = rol;
        this.telefono = telefono;
        this.strikes = 0;
    }

    // ============= GETTERS & SETTERS =============

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

    /** @return hash BCrypt de la contraseña */
    public String getHashContrasenia() {
        return hashContrasenia;
    }

    /** @param hashContrasenia hash BCrypt, nunca la contraseña en claro */
    public void setHashContrasenia(String hashContrasenia) {
        this.hashContrasenia = hashContrasenia;
    }

    /** @return nombre de pila */
    public String getNombre() {
        return nombre;
    }

    /** @param nombre nombre de pila */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /** @return apellidos */
    public String getApellidos() {
        return apellidos;
    }

    /** @param apellidos apellidos */
    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    /** @return teléfono de contacto, o {@code null} si no lo ha indicado */
    public String getTelefono() {
        return telefono;
    }

    /** @param telefono teléfono de contacto */
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    /**
     * @return clave de la imagen de perfil en el almacenamiento, o {@code null} si
     *         no tiene
     */
    public String getClaveImagenPerfil() {
        return claveImagenPerfil;
    }

    /** @param claveImagenPerfil clave del objeto en el almacenamiento */
    public void setClaveImagenPerfil(String claveImagenPerfil) {
        this.claveImagenPerfil = claveImagenPerfil;
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
     *               bloquearla
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

    /** @return Cantidad de advertencias que tiene la cuenta */
    public int getStrikes() {
        return strikes;
    }

    // =========== MÉTODOS ===============
    /** Asigna la fecha de registro justo antes de guardar por primera vez. */
    @PrePersist
    private void alCrear() {
        this.fechaRegistro = Instant.now();
    }

    /** Añade 1 strike y si supera a {@link Config#MAX_STRIKES} se pone la cuenta en inactivo */
    public void aniadirStrike(){
        if ((this.strikes + 1) >= Config.MAX_STRIKES) {
            setActivo(false);
            this.strikes = Config.MAX_STRIKES;
        } else {
            this.strikes += 1;
        }
    }
    /** Resta 1 strike solo si es > {@code 0} */
    public void quitarStrike(){
        if (this.strikes > 0) {
            this.strikes -= 1;
        }
    }

}