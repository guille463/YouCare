package com.youcare.backend.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.youcare.backend.config.Config;
import com.youcare.backend.entity.embeddable.Documento;
import com.youcare.backend.entity.embeddable.FranjaHoraria;
import com.youcare.backend.entity.enums.EstadoDocumento;
import com.youcare.backend.entity.enums.EstadoEnEmpresa;
import com.youcare.backend.entity.enums.EstadoVerificacion;
import com.youcare.backend.entity.enums.Rol;
import com.youcare.backend.entity.enums.TipoDocumento;
import com.youcare.backend.exception.ValidacionException;
import com.youcare.backend.util.IdentificadorUtil;

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
       private Set<FranjaHoraria> disponibilidad = new HashSet<>();

    // ========== CONSTRUCTORES ===========

    /**
     * Constructor sin argumentos requerido por JPA. No debe usarse directamente.
     */
    protected Cuidador() {
    }

    /**
     * Crea un cuidador autónomo y sin documentos subidos.
     *
     * @param email           correo electrónico, único
     * @param hashContrasenia hash BCrypt de la contraseña, nunca la contraseña en
     *                        claro
     * @param nombre          nombre del cuidador
     * @param apellidos       apellidos
     * @param dniNie          DNI o NIE
     * @param telefono        teléfono de contacto
     */
    public Cuidador(String email, String hashContrasenia, String nombre, String apellidos, String dniNie,
            String telefono) {
        super(email, hashContrasenia, nombre, apellidos, Rol.CUIDADOR, telefono);
        this.dniNie = IdentificadorUtil.normalizar(dniNie) ;
        this.estadoVerificacion = EstadoVerificacion.SIN_SUBIR;
    }

    // ============= GETTERS & SETTERS =============

    /** @return DNI o NIE */
    public String getDniNie() {
        return dniNie;
    }

    /** @param dniNie DNI o NIE, único */
    public void setDniNie(String dniNie) {
        this.dniNie = IdentificadorUtil.normalizar(dniNie);
    }

    /** @return empresa a la que pertenece, o {@code null} si es autónomo */
    public Empresa getEmpresa() {
        return empresa;
    }

    /** @return estado en la empresa, o {@code null} si es autónomo */
    public EstadoEnEmpresa getEstadoEnEmpresa() {
        return estadoEnEmpresa;
    }

    
    /** @return estado de verificación */
    public EstadoVerificacion getEstadoVerificacion() {
        return estadoVerificacion;
    }

    /** @return documentos subidos (lista de solo lectura) */
    public List<Documento> getDocumentos() {
        return Collections.unmodifiableList(documentos);
    }

        /** @return franjas de disponibilidad (conjunto de solo lectura) */
    public Set<FranjaHoraria> getDisponibilidad() {
        return Collections.unmodifiableSet(disponibilidad);
    }

    // =========== MÉTODOS ===============

       /**
     * Solicita unirse a una empresa. Queda en {@code PENDIENTE} hasta que la
     * empresa lo acepte.
     *
     * @param empresa empresa a la que se une
     * @throws ValidacionException si la empresa es {@code null}, no está
     *                             verificada o el cuidador ya pertenece a otra
     */
    public void vincularAEmpresa(Empresa empresa) {
        if (empresa == null) {
            throw new ValidacionException("empresa", "La empresa es obligatoria");
        }
        if (empresa.getEstadoVerificacion() != EstadoVerificacion.VERIFICADO) {
            throw new ValidacionException("empresa", "La empresa no está verificada");
        }
        if (this.empresa != null && this.estadoEnEmpresa != EstadoEnEmpresa.BAJA) {
            throw new ValidacionException("empresa",
                    "Ya pertenece a una empresa; primero debe darse de baja");
        }
        this.empresa = empresa;
        this.estadoEnEmpresa = EstadoEnEmpresa.PENDIENTE;
    }

        /**
     * La empresa acepta al cuidador.
     *
     * @throws ValidacionException si no tiene una solicitud pendiente
     */
    public void aceptarEnEmpresa() {
        if (this.estadoEnEmpresa != EstadoEnEmpresa.PENDIENTE) {
            throw new ValidacionException("estadoEnEmpresa",
                    "Solo se puede aceptar una solicitud pendiente");
        }
        this.estadoEnEmpresa = EstadoEnEmpresa.ACTIVO;
    }

    /**
     * Baja en la empresa (la pide el cuidador o la decide la empresa). Se
     * conserva la empresa para saber dónde estuvo.
     *
     * @throws ValidacionException si es autónomo o ya estaba de baja
     */
    public void darDeBajaEnEmpresa() {
        if (this.estadoEnEmpresa == null || this.estadoEnEmpresa == EstadoEnEmpresa.BAJA) {
            throw new ValidacionException("estadoEnEmpresa",
                    "No pertenece a ninguna empresa activa");
        }
        this.estadoEnEmpresa = EstadoEnEmpresa.BAJA;
    }

        /** Convierte al cuidador en autónomo: borra la empresa y su estado en ella. */
    public void desvincularDeEmpresa() {
        this.empresa = null;
        this.estadoEnEmpresa = null;
    }

        /**
     * Añade un documento recién subido.
     * <p>
     * Solo el {@code DNI_NIE} reactiva la verificación de la cuenta: si estaba
     * {@code SIN_SUBIR} o {@code RECHAZADO}, pasa a {@code PENDIENTE}. Los demás
     * documentos (certificados) se revisan uno a uno con su propio estado.
     * </p>
     *
     * @param documento documento recién subido
     * @throws ValidacionException si el documento es {@code null} o es un CIF
     */
    public void aniadirDocumento(Documento documento) {
        if (documento == null) {
            throw new ValidacionException("documento", "El documento es obligatorio");
        }
        if (documento.getTipo() == TipoDocumento.CIF) {
            throw new ValidacionException("tipo", "Un cuidador no puede subir un CIF");
        }

        this.documentos.add(documento);

        if (documento.getTipo() == TipoDocumento.DNI_NIE
                && (this.estadoVerificacion == EstadoVerificacion.SIN_SUBIR
                        || this.estadoVerificacion == EstadoVerificacion.RECHAZADO)) {
            this.estadoVerificacion = EstadoVerificacion.PENDIENTE;
        }
    }

    /**
     * @param franja nueva franja de disponibilidad; no puede solaparse con otra del
     *               mismo día
     */
    public void aniadirFranja(FranjaHoraria franja) {
        if (franja == null){
            throw new ValidacionException("disponibilidad","La franja es obligatoria");
        }
        for (FranjaHoraria existente : disponibilidad) {
            if (existente.seSolapan(franja)) {
                throw new ValidacionException("disponibilidad", "La franja se solapa con otra ya existente");
            }
        }
        this.disponibilidad.add(franja);
    }

        /**
     * El administrador verifica la cuenta. Desde ese momento, el cuidador aparece
     * en las búsquedas.
     *
     * @throws ValidacionException si la cuenta no está pendiente de revisión o
     *                             no tiene un DNI/NIE aprobado
     */
    public void verificar() {
        if (this.estadoVerificacion != EstadoVerificacion.PENDIENTE) {
            throw new ValidacionException("estadoVerificacion",
                    "Solo se puede verificar una cuenta pendiente de revisión");
        }
        boolean dniAprobado = documentos.stream()
                .anyMatch(d -> d.getTipo() == TipoDocumento.DNI_NIE
                        && d.getEstado() == EstadoDocumento.APROBADO);
        if (!dniAprobado) {
            throw new ValidacionException("documentos",
                    "No se puede verificar sin un DNI/NIE aprobado");
        }
        this.estadoVerificacion = EstadoVerificacion.VERIFICADO;
    }

    /**
     * El administrador rechaza la verificación. El motivo concreto va en el
     * documento rechazado ({@link Documento#rechazar(String)}). El cuidador
     * vuelve a {@code PENDIENTE} cuando sube un nuevo DNI/NIE.
     *
     * @throws ValidacionException si la cuenta no está pendiente de revisión
     */
    public void rechazarVerificacion() {
        if (this.estadoVerificacion != EstadoVerificacion.PENDIENTE) {
            throw new ValidacionException("estadoVerificacion",
                    "Solo se puede rechazar una cuenta pendiente de revisión");
        }
        this.estadoVerificacion = EstadoVerificacion.RECHAZADO;
    }
}