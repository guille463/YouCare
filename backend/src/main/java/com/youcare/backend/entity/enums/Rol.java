package com.youcare.backend.entity.enums;

/**
 * Roles posibles de un usuario en la plataforma YouCare.
 *
 * @author Jorge Martín Llaneli
 */
public enum Rol {
    /** Persona (o familiar) que contrata servicios de cuidados. */
    CLIENTE,
    /** Profesional que ofrece servicios de cuidados. */
    CUIDADOR,
    /** Persona que gestiona la plataforma y valida perfiles. */
    ADMIN,
    /** Empresa de cuidados que se comercializa a traves de la app */
    EMPRESA
}
