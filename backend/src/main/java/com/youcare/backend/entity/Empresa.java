package com.youcare.backend.entity;

import java.util.ArrayList;
import java.util.List;
import com.youcare.backend.config.Config;
import com.youcare.backend.entity.embeddable.Documento;
import com.youcare.backend.entity.enums.EstadoVerificacion;
import com.youcare.backend.entity.enums.Rol;
import com.youcare.backend.entity.enums.TipoDocumento;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * Empresa de cuidados que ofrece sus servicios a través de la plataforma.
 * <p>
 * Hereda los datos comunes de {@link Usuario}: el {@code nombre} y los
 * {@code apellidos} son los de la persona de contacto. Los datos fiscales
 * (razón social y CIF) se guardan aquí. Se guarda en la tabla {@code empresa},
 * que comparte clave primaria con {@code usuario}.
 * </p>
 */
@Entity
@Table(name = "empresa")
public class Empresa extends Usuario {

    /** Razón social de la empresa. */
    @Column(name = "razon_social", nullable = false, length = Config.MAX_LONGITUD_RAZON_SOCIAL)
    private String razonSocial;

    /** CIF de la empresa, único. */
    @Column(nullable = false, unique = true, length = Config.MAX_LONGITUD_CIF)
    private String cif;

    /** Estado de verificación de la empresa por parte del administrador. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_verificacion", nullable = false, length = Config.MAX_LONGITUD_ESTADO)
    private EstadoVerificacion estadoVerificacion;

    /** Documentos subidos para la verificación. */
    @ElementCollection
    @CollectionTable(name = "empresa_documento", joinColumns = @JoinColumn(name = "empresa_id"))
    private List<Documento> documentos = new ArrayList<>();

    // ========== CONSTRUCTORES ===========

    /**
     * Constructor sin argumentos requerido por JPA. No debe usarse directamente.
     */
    protected Empresa() {
    }

    /**
     * Crea una empresa sin verificar.
     *
     * @param email           correo electrónico, único en la plataforma
     * @param hashContrasenia hash BCrypt de la contraseña, nunca la contraseña en
     *                        claro
     * @param nombre          nombre de la persona de contacto
     * @param apellidos       apellidos de la persona de contacto
     * @param razonSocial     razón social de la empresa
     * @param cif             CIF de la empresa
     * @param telefono        teléfono de contacto
     */
    public Empresa(String email, String hashContrasenia, String nombre, String apellidos, String razonSocial,
            String cif, String telefono) {
        super(email, hashContrasenia, nombre, apellidos, Rol.EMPRESA, telefono);
        this.razonSocial = razonSocial;
        this.cif = cif;
        this.estadoVerificacion = EstadoVerificacion.SIN_SUBIR;
    }

    // ============= GETTERS & SETTERS =============

    /** @return razón social */
    public String getRazonSocial() {
        return razonSocial;
    }

    /** @param razonSocial razón social de la empresa */
    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    /** @return CIF */
    public String getCif() {
        return cif;
    }

    /** @param cif CIF de la empresa, único */
    public void setCif(String cif) {
        this.cif = cif;
    }

    /** @return estado de verificación */
    public EstadoVerificacion getEstadoVerificacion() {
        return estadoVerificacion;
    }

    /** @param estadoVerificacion nuevo estado (decisión del administrador) */
    public void setEstadoVerificacion(EstadoVerificacion estadoVerificacion) {
        this.estadoVerificacion = estadoVerificacion;
    }

    /** @return documentos subidos (lista de solo lectura) */
    public List<Documento> getDocumentos() {
        return documentos;
    }

    // =========== MÉTODOS ===============

    /** @param documento documento recién subido */
    public void aniadirDocumento(Documento documento) {
        this.documentos.add(documento);
        // Si estaba rechazado o sin subir, se pone a pendiente
        if (documento.getTipo() == TipoDocumento.CIF && (this.estadoVerificacion == EstadoVerificacion.SIN_SUBIR
                || this.estadoVerificacion == EstadoVerificacion.RECHAZADO)) {
            this.estadoVerificacion = EstadoVerificacion.PENDIENTE;
        }
    }
}