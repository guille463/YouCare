package com.youcare.backend.entity;

import java.time.Instant;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Mensaje de una {@link chat}. El receptor no se guarda: es el otro
 * participante de la conversación.
 */
@Entity
@Table(name = "mensaje")
public class Mensaje {

    /** Identificador único (PK). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false, updatable = false)
    private Chat chat;

    /** Cliente, cuidador o empresa que lo envía. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emisor_id", nullable = false, updatable = false)
    private Usuario emisor;

    @Column(nullable = false, updatable = false, length = Config.MAX_LONGITUD_CONTENIDO_MENSAJE)
    private String contenido;

    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private Instant fechaEnvio;

    /** {@code null} mientras el receptor no lo haya leído. */
    @Column(name = "fecha_lectura")
    private Instant fechaLectura;

    // ========== CONSTRUCTORES ===========

    /** Constructor sin argumentos requerido por JPA. No debe usarse directamente. */
    protected Mensaje() {
    }

    /**
     * @throws ValidacionException si falta la conversación, el emisor no
     *                             participa en ella o el contenido no es válido
     */
    public Mensaje(Chat chat, Usuario emisor, String contenido) {
        if (chat == null) {
            throw new ValidacionException("chat", "La conversación es obligatoria");
        }
        if (!chat.esParticipante(emisor)) {
            throw new ValidacionException("emisor", "El emisor no participa en la conversación");
        }
        if (contenido == null || contenido.isBlank()) {
            throw new ValidacionException("contenido", "El mensaje no puede estar vacío");
        }
        String limpio = contenido.strip();
        if (limpio.length() > Config.MAX_LONGITUD_CONTENIDO_MENSAJE) {
            throw new ValidacionException("contenido",
                    "El mensaje no puede superar " + Config.MAX_LONGITUD_CONTENIDO_MENSAJE + " caracteres");
        }
        this.chat = chat;
        this.emisor = emisor;
        this.contenido = limpio;
    }

    @PrePersist
    private void alCrear() {
        this.fechaEnvio = Instant.now();
    }

    // ============= LÓGICA =============

    /** Marca el mensaje como leído. Si ya lo estaba, conserva la primera fecha. */
    public void marcarLeido() {
        if (fechaLectura == null) {
            fechaLectura = Instant.now();
        }
    }

    public boolean estaLeido() {
        return fechaLectura != null;
    }

    // ============= GETTERS =============

    public Long getId() {
        return id;
    }

    public Chat getChat() {
        return chat;
    }

    public Usuario getEmisor() {
        return emisor;
    }

    public String getContenido() {
        return contenido;
    }

    public Instant getFechaEnvio() {
        return fechaEnvio;
    }

    public Instant getFechaLectura() {
        return fechaLectura;
    }
}