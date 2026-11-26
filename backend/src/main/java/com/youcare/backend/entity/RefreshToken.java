package com.youcare.backend.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.youcare.backend.config.Config;
import com.youcare.backend.exception.ValidacionException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Refresh token de larga duración para renovar el JWT de acceso.
 * <p>
 * Se guarda el hash SHA-256, nunca el token: si se filtra la BD, los hashes
 * no sirven para iniciar sesión. No se usa BCrypt porque hay que buscar por
 * el hash, y BCrypt genera uno distinto cada vez por la sal.
 * </p>
 * <p>
 * Rotación: en cada renovación se revoca el token usado y se crea uno nuevo.
 * </p>
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    /** Identificador único (PK). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    /** SHA-256 del token en hexadecimal. */
    @Column(name = "hash_token", nullable = false, unique = true, updatable = false,
            length = Config.LONGITUD_HASH_TOKEN)
    private String hashToken;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false, updatable = false)
    private Instant fechaExpiracion;

    /** {@code null} mientras el token esté activo. */
    @Column(name = "fecha_revocacion")
    private Instant fechaRevocacion;

    // ========== CONSTRUCTORES ===========

    /** Constructor sin argumentos requerido por JPA. No debe usarse directamente. */
    protected RefreshToken() {
    }

    /**
     * @param usuario   propietario del token
     * @param hashToken SHA-256 del token en hexadecimal (64 caracteres)
     * @throws ValidacionException si falta el usuario o el hash no es válido
     */
    public RefreshToken(Usuario usuario, String hashToken) {
        if (usuario == null) {
            throw new ValidacionException("usuario", "El usuario es obligatorio");
        }
        if (hashToken == null || hashToken.length() != Config.LONGITUD_HASH_TOKEN) {
            throw new ValidacionException("hashToken", "El hash del token no es válido");
        }
        this.usuario = usuario;
        this.hashToken = hashToken;
        this.fechaCreacion = Instant.now();
        this.fechaExpiracion = fechaCreacion.plus(Config.DIAS_VALIDEZ_REFRESH_TOKEN, ChronoUnit.DAYS);
    }

    // ============= LÓGICA =============

    /** @return {@code true} si no esta revocado ni caducado */
    public boolean estaActivo(Instant ahora) {
        return fechaRevocacion == null && ahora.isBefore(fechaExpiracion);
    }

    /** Revoca el token (logout, rotacion o cambio de contraseña). */
    public void revocar() {
        if (fechaRevocacion == null) {
            fechaRevocacion = Instant.now();
        }
    }

    // ============= GETTERS =============

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getHashToken() {
        return hashToken;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public Instant getFechaExpiracion() {
        return fechaExpiracion;
    }

    public Instant getFechaRevocacion() {
        return fechaRevocacion;
    }
}