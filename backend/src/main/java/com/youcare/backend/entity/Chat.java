package com.youcare.backend.entity;

import java.time.Instant;

import com.youcare.backend.entity.enums.EstadoEnEmpresa;
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
 * Chat entre un {@link Cliente} y quien ofrece el servicio: un
 * {@link Cuidador} autónomo o una {@link Empresa}. Siempre hay exactamente un
 * proveedor (lo garantiza también el CHECK de la BD).
 * <p>
 * No tiene relación con las reservas: el cliente puede preguntar antes de
 * reservar o sin llegar a reservar nunca.
 * </p>
 */
@Entity
@Table(name = "chat")
public class Chat {

    /** Identificador único (PK). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    /** Relleno solo si el proveedor es un cuidador autónomo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuidador_id", updatable = false)
    private Cuidador cuidador;

    /** Relleno solo si el proveedor es una empresa. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", updatable = false)
    private Empresa empresa;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    /** {@code null} mientras no haya mensajes. Sirve para ordenar la lista de chats. */
    @Column(name = "fecha_ultimo_mensaje")
    private Instant fechaUltimoMensaje;

    // ========== CONSTRUCTORES ===========

    /** Constructor sin argumentos requerido por JPA. No debe usarse directamente. */
    protected Chat() {
    }

    private Chat(Cliente cliente, Cuidador cuidador, Empresa empresa) {
        this.cliente = cliente;
        this.cuidador = cuidador;
        this.empresa = empresa;
    }

    /**
     * Crea un chat con un cuidador autónomo.
     *
     * @throws ValidacionException si falta algún participante o el cuidador
     *                             trabaja activo en una empresa
     */
    public static Chat conCuidador(Cliente cliente, Cuidador cuidador) {
        validarCliente(cliente);
        if (cuidador == null) {
            throw new ValidacionException("cuidador", "El cuidador es obligatorio");
        }
        if (cuidador.getEmpresa() != null && cuidador.getEstadoEnEmpresa() == EstadoEnEmpresa.ACTIVO) {
            throw new ValidacionException("cuidador",
                    "El cuidador trabaja para una empresa: hay que contactar con la empresa");
        }
        return new Chat(cliente, cuidador, null);
    }

    /**
     * Crea un chat con una empresa.
     *
     * @throws ValidacionException si falta algún participante
     */
    public static Chat conEmpresa(Cliente cliente, Empresa empresa) {
        validarCliente(cliente);
        if (empresa == null) {
            throw new ValidacionException("empresa", "La empresa es obligatoria");
        }
        return new Chat(cliente, null, empresa);
    }

    private static void validarCliente(Cliente cliente) {
        if (cliente == null) {
            throw new ValidacionException("cliente", "El cliente es obligatorio");
        }
    }

    @PrePersist
    private void alCrear() {
        this.fechaCreacion = Instant.now();
    }

    // ============= LÓGICA =============

    /** @return el cuidador o la empresa, el que no sea {@code null} */
    public Usuario getProveedor() {
        return cuidador != null ? cuidador : empresa;
    }

    /**
     * Compara por id y no con {@code equals}: con LAZY, Hibernate puede
     * devolver un proxy en lugar del objeto real.
     *
     * @return {@code true} si el usuario es el cliente o el proveedor
     */
    public boolean esParticipante(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            return false;
        }
        return usuario.getId().equals(cliente.getId())
                || usuario.getId().equals(getProveedor().getId());
    }

    /** Actualiza la fecha del último mensaje. Lo llama el Service al guardar un mensaje. */
    public void registrarMensaje(Instant fechaEnvio) {
        this.fechaUltimoMensaje = fechaEnvio;
    }

    // ============= GETTERS =============

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    /** @return el cuidador, o {@code null} si el proveedor es una empresa */
    public Cuidador getCuidador() {
        return cuidador;
    }

    /** @return la empresa, o {@code null} si el proveedor es un cuidador */
    public Empresa getEmpresa() {
        return empresa;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public Instant getFechaUltimoMensaje() {
        return fechaUltimoMensaje;
    }
}