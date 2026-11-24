package com.youcare.backend.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.youcare.backend.config.Config;
import com.youcare.backend.entity.embeddable.Documento;
import com.youcare.backend.entity.embeddable.FranjaHoraria;
import com.youcare.backend.entity.enums.EstadoEnEmpresa;
import com.youcare.backend.entity.enums.EstadoVerificacion;
import com.youcare.backend.entity.enums.Rol;
import com.youcare.backend.entity.enums.TipoDocumento;
import com.youcare.backend.exception.ValidacionException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Profesional que ofrece servicios de cuidados, de forma autónoma o a través de
 * una {@link Empresa}.
 * <p>
 * Hereda los atributos comunes de {@link Usuario}. Se guarda en la tabla
 * {@code cuidador}, que comparte PK con {@code usuario}. Solo debería aparecer
 * en las búsquedas cuando su verificación es {@code VERIFICADO}.
 * </p>
 */
@Entity
@Table(name = "cuidador")
public class Cuidador extends Usuario {

    /** DNI o NIE, único. Solo se pide a los cuidadores. */
    @Column(name = "dni_nie", nullable = false, unique = true, length = Config.MAX_LONGITUD_DNI_NIE)
    private String dniNie;

    /** Empresa a la que pertenece, o {@code null} si es autónomo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    /** Estado dentro de la empresa. {@code null} si es autónomo. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_en_empresa", length = Config.MAX_LONGITUD_ESTADO)
    private EstadoEnEmpresa estadoEnEmpresa;

    /** Estado de verificación de la cuenta por parte del admin. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_verificacion", nullable = false, length = Config.MAX_LONGITUD_ESTADO)
    private EstadoVerificacion estadoVerificacion;

    /** Documentos subidos para la verificación. */
    @ElementCollection
    @CollectionTable(name = "cuidador_documento", joinColumns = @JoinColumn(name = "cuidador_id"))
    private List<Documento> documentos = new ArrayList<>();

    /** Franjas semanales en las que está disponible. */
    @ElementCollection
    @CollectionTable(name = "cuidador_franja_horaria", joinColumns = @JoinColumn(name = "cuidador_id"))
    private List<FranjaHoraria> disponibilidad = new ArrayList<>();

    // ========== CONSTRUCTORES ===========

    /**
     * Constructor sin argumentos requerido por JPA. No debe usarse directamente.
     */
    protected Cuidador() {
    }

    /**
     * Crea un cuidador autónomo y sin verificar.
     *
     * @param email           correo electrónico, único
     * @param hashContrasenia hash BCrypt de la contraseña, nunca la contraseña en
     *                        si
     * @param nombre          nombre cuidador
     * @param apellidos       apellidos
     * @param dniNie          DNI o NIE
     * @param telefono        teléfono de contacto
     */
    public Cuidador(String email, String hashContrasenia, String nombre, String apellidos, String dniNie,
            String telefono) {
        super(email, hashContrasenia, nombre, apellidos, Rol.CUIDADOR, telefono);
        this.dniNie = dniNie;
        this.estadoVerificacion = EstadoVerificacion.SIN_SUBIR;
    }

    // ============= GETTERS & SETTERS =============
    /** @return DNI o NIE */
    public String getDniNie() {
        return dniNie;
    }

    /** @param dniNie DNI o NIE, único */
    public void setDniNie(String dniNie) {
        this.dniNie = dniNie;
    }

    /** @return empresa a la que pertenece, o {@code null} si es autónomo */
    public Empresa getEmpresa() {
        return empresa;
    }

    /** @return estado en la empresa, o {@code null} si es autónomo */
    public EstadoEnEmpresa getEstadoEnEmpresa() {
        return estadoEnEmpresa;
    }

    /**
     * @param estadoEnEmpresa nuevo estado; solo se puede cambiar si pertenece a una
     *                        empresa
     */
    public void setEstadoEnEmpresa(EstadoEnEmpresa estadoEnEmpresa) {
        if (this.empresa == null) {
            throw new ValidacionException("estadoEnEmpresa", "Un cuidador autónomo no tiene estado en ninguna empresa");
        }
        if (estadoEnEmpresa == null) {
            throw new ValidacionException("estadoEnEmpresa", "El estado en la empresa es obligatorio si tiene estado");
        }
        this.estadoEnEmpresa = estadoEnEmpresa;
    }

    /** @return estado de verificación */
    public EstadoVerificacion getEstadoVerificacion() {
        return estadoVerificacion;
    }

    /**
     * @param estadoVerificacion nuevo estado de verificación (decisión del
     *                           administrador)
     */
    public void setEstadoVerificacion(EstadoVerificacion estadoVerificacion) {
        this.estadoVerificacion = estadoVerificacion;
    }

    /** @return documentos subidos */
    public List<Documento> getDocumentos() {
        return documentos;
    }

    /** @return franjas de disponibilidad (lista de solo lectura) */
    public List<FranjaHoraria> getDisponibilidad() {
        return disponibilidad;
    }

    // =========== MÉTODOS ===============

    /**
     * Vincula al cuidador con una empresa. Queda en estado {@code PENDIENTE}
     * hasta que la empresa lo acepte.
     *
     * @param empresa empresa a la que se une
     */
    public void vincularAEmpresa(Empresa empresa) {
        if (empresa == null) {
            throw new ValidacionException("empresa", "La empresa es obligatoria");
        }
        this.empresa = empresa;
        this.estadoEnEmpresa = EstadoEnEmpresa.PENDIENTE;
    }

    /** Convierte al cuidador en autónomo, borra la empresa y su estado en ella. */
    public void desvincularDeEmpresa() {
        this.empresa = null;
        this.estadoEnEmpresa = null;
    }

    /** @param documento documento recién subido */
    public void aniadirDocumento(Documento documento) {
        this.documentos.add(documento);
        // Pone el estado a pendiente en caso de que se haya subido un documento de tipo
        // certificado profesional
        if (documento.getTipo() == TipoDocumento.CERTIFICADO_PROFESIONAL
                && this.estadoVerificacion == EstadoVerificacion.SIN_SUBIR) {
            this.estadoVerificacion = EstadoVerificacion.PENDIENTE;
        }
    }

    /**
     * @param franja nueva franja de disponibilidad; no puede solaparse con otra del
     *               mismo día
     */
    public void aniadirFranja(FranjaHoraria franja) {
        for (FranjaHoraria existente : disponibilidad) {
            if (existente.seSolapan(franja)) {
                throw new ValidacionException("disponibilidad", "La franja se solapa con otra ya existente");
            }
        }
        this.disponibilidad.add(franja);
    }
}