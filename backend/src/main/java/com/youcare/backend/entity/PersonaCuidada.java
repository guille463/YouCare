package com.youcare.backend.entity;

import java.time.LocalDate;
import com.youcare.backend.config.Config;
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
 * Persona mayor o dependiente que recibe los cuidados. Pertenece a un
 * {@link Cliente}, que puede tener a su cargo a varias personas.
 * <p>
 * El servicio se presta en su domicilio, no en el del cliente que paga. La
 * ubicación geográfica se añadirá posteriormente.
 * </p>
 */
@Entity
@Table(name = "persona_cuidada")
public class PersonaCuidada {

    /** Identificador único (PK). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cliente que la tiene a su cargo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = Config.MAX_LONGITUD_NOMBRE)
    private String nombre;

    @Column(nullable = false, length = Config.MAX_LONGITUD_APELLIDOS)
    private String apellidos;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    /** Dirección donde se presta el servicio. */
    @Column(nullable = false, length = Config.MAX_LONGITUD_DIRECCION)
    private String direccion;

    @Column(nullable = false, length = Config.MAX_LONGITUD_CIUDAD)
    private String ciudad;

    /**
     * Necesidades de cuidado en texto libre.
     * Opcional.
     */
    @Column(length = Config.MAX_LONGITUD_NECESIDADES)
    private String necesidades;

    // ========== CONSTRUCTORES ===========

    /**
     * Constructor sin argumentos requerido por JPA. No debe usarse directamente.
     */
    protected PersonaCuidada() {
    }

    /**
     * @param cliente         cliente responsable
     * @param nombre          nombre de pila
     * @param apellidos       apellidos
     * @param fechaNacimiento fecha de nacimiento
     * @param direccion       dirección donde se presta el servicio
     * @param ciudad          ciudad del servicio
     */
    public PersonaCuidada(Cliente cliente, String nombre, String apellidos, LocalDate fechaNacimiento,
            String direccion, String ciudad) {
        this.cliente = cliente;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.fechaNacimiento = fechaNacimiento;
        this.direccion = direccion;
        this.ciudad = ciudad;
    }

    // ============= GETTERS & SETTERS =============

    /** @return identificador único */
    public Long getId() {
        return id;
    }

    /** @return cliente responsable */
    public Cliente getCliente() {
        return cliente;
    }

    /** @return nombre */
    public String getNombre() {
        return nombre;
    }

    /** @param nombre nombre */
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

    /** @return fecha de nacimiento */
    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    /** @param fechaNacimiento fecha de nacimiento */
    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    /** @return dirección donde se presta el servicio */
    public String getDireccion() {
        return direccion;
    }

    /** @param direccion dirección donde se presta el servicio */
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    /** @return ciudad del servicio */
    public String getCiudad() {
        return ciudad;
    }

    /** @param ciudad ciudad del servicio */
    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    /** @return necesidades de cuidado, o {@code null} si no se han indicado */
    public String getNecesidades() {
        return necesidades;
    }

    /** @param necesidades necesidades de cuidado en texto libre */
    public void setNecesidades(String necesidades) {
        this.necesidades = necesidades;
    }
}