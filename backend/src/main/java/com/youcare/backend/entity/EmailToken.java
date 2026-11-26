package com.youcare.backend.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.youcare.backend.config.Config;
import com.youcare.backend.entity.enums.TipoEmailToken;
import com.youcare.backend.exception.ValidacionException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Token de un solo uso enviado por email, para verificar la cuenta o
 * recuperar la contraseña. Se guarda el hash SHA-256, igual que en
 * {@link RefreshToken}.
 */
@Entity
@Table(name = "email_token")
public class EmailToken {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = Config.MAX_LONGITUD_TIPO_EMAIL_TOKEN)
    private TipoEmailToken tipo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false, updatable = false)
    private Instant fechaExpiracion;

    /** {@code null} mientras no se haya usado. */
    @Column(name = "fecha_uso")
    private Instant fechaUso;

    // ========== CONSTRUCTORES ===========

    /** Constructor sin argumentos requerido por JPA. No debe usarse directamente. */
    protected EmailToken() {
    }

    /**
     * La caducidad depende del tipo: 24 h para verificación y 1 h para
     * recuperación de contraseña.
     *
     * @throws ValidacionException si falta algún dato o el hash no es válido
     */
    public EmailToken(Usuario usuario, String hashToken, TipoEmailToken tipo) {
        if (usuario == null) {
            throw new ValidacionException("usuario", "El usuario es obligatorio");
        }
        if (hashToken == null || hashToken.length() != Config.LONGITUD_HASH_TOKEN) {
            throw new ValidacionException("hashToken", "El hash del token no es válido");
        }
        if (tipo == null) {
            throw new ValidacionException("tipo", "El tipo de token es obligatorio");
        }
        this.usuario = usuario;
        this.hashToken = hashToken;
        this.tipo = tipo;
        this.fechaCreacion = Instant.now();
        this.fechaExpiracion = fechaCreacion.plus(horasValidez(tipo), ChronoUnit.HOURS);
    }

    private static long horasValidez(TipoEmailToken tipo) {
        return switch (tipo) {
            case VERIFICACION -> Config.HORAS_VALIDEZ_EMAIL;
            case RECUPERACION -> Config.HORAS_VALIDEZ_RECUPERACION_CONTRASENIA;
        };
    }

    // ============= LOGICA =============

    /** @return {@code true} si no se ha usado ni ha caducado */
    public boolean esValido(Instant ahora) {
        return fechaUso == null && ahora.isBefore(fechaExpiracion);
    }

    /**
     * Marca el token como usado. Solo se puede usar una vez.
     *
     * @throws ValidacionException si no es válido
     */
    public void marcarUsado() {
        Instant ahora = Instant.now();
        if (!esValido(ahora)) {
            throw new ValidacionException("token", "El enlace ha caducado o ya se ha usado");
        }
        fechaUso = ahora;
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

    public TipoEmailToken getTipo() {
        return tipo;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public Instant getFechaExpiracion() {
        return fechaExpiracion;
    }

    public Instant getFechaUso() {
        return fechaUso;
    }
}