package com.youcare.backend.entity.embeddable;

import java.time.Instant;
import com.youcare.backend.config.Config;
import com.youcare.backend.entity.enums.EstadoDocumento;
import com.youcare.backend.entity.enums.TipoDocumento;
import com.youcare.backend.exception.ValidacionException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Documento que un cuidador o empresa sube para verificar su identidad o
 * su cualificación.
 * <p>
 * No tiene {@code id} y esta en una tabla de relacionada con su propietario.
 * Del archivo solo se guarda su clave en el almacenamiento privado; la URL se
 * genera en cada petición.
 * </p>
 */
@Embeddable
public class Documento {

    /** Tipo de documento. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = Config.MAX_LONGITUD_TIPO_DOCUMENTO)
    private TipoDocumento tipo;

    /** Estado de revisión por parte del admin. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = Config.MAX_LONGITUD_ESTADO)
    private EstadoDocumento estado;

    /** Clave del archivo en el almacenamiento. No es una URL. */
    @Column(name = "clave_archivo", nullable = false, length = Config.MAX_LONGITUD_CLAVE_DOCUMENTO)
    private String claveArchivo;

    /** Motivo del rechazo, en caso de que el estado es {@code RECHAZADO}. */
    @Column(name = "motivo_rechazo", length = Config.MAX_LONGITUD_MOTIVO_RECHAZO)
    private String motivoRechazo;

    /** Fecha y hora de subida. */
    @Column(name = "fecha_subida", nullable = false)
    private Instant fechaSubida;

    /**
     * Constructor sin argumentos requerido por JPA. No debe usarse directamente.
     */
    protected Documento() {
    }

    /**
     * Crea un documento recién subido, pendiente de revisión.
     *
     * @param tipo         tipo de documento
     * @param claveArchivo clave del archivo en el almacenamiento
     */
    public Documento(TipoDocumento tipo, String claveArchivo) {
        this.tipo = tipo;
        this.estado = EstadoDocumento.PENDIENTE;
        this.claveArchivo = claveArchivo;
        this.fechaSubida = Instant.now();
    }

    // ============= MÉTODOS =============

    /**
     * Marca el documento como válido y borra cualquier motivo de rechazo previo.
     */
    public void aprobar() {
        this.estado = EstadoDocumento.APROBADO;
        this.motivoRechazo = null;
    }

    /**
     * Marca el documento como no válido. El usuario deberá subirlo de nuevo.
     *
     * @param motivo la explicación del rechazo es obligatoria
     * @throws ValidacionException Manda excepción si falta el motivo o si supera el
     *                             máximo de longitud.
     */
    public void rechazar(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ValidacionException("motivoRechazo", "Falta indicar el motivo del rechazo");
        }

        if (motivo.length() > Config.MAX_LONGITUD_MOTIVO_RECHAZO) {
            throw new ValidacionException("motivoRechazo",
                    "No se debe superar la longitud de " + Config.MAX_LONGITUD_MOTIVO_RECHAZO);
        }
        this.estado = EstadoDocumento.RECHAZADO;
        this.motivoRechazo = motivo.trim();
    }

    // ============= GETTERS =============

    /** @return tipo de documento */
    public TipoDocumento getTipo() {
        return tipo;
    }

    /** @return estado de revisión */
    public EstadoDocumento getEstado() {
        return estado;
    }

    /** @return clave del archivo en el almacenamiento */
    public String getClaveArchivo() {
        return claveArchivo;
    }

    /** @return motivo del rechazo, o {@code null} si no está rechazado */
    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    /** @return fecha y hora de subida */
    public Instant getFechaSubida() {
        return fechaSubida;
    }
}
