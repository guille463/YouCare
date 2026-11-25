package com.youcare.backend.entity.enums;

/**
 * Representa el estado de un cuidador dentro de la empresa en la que está.
 * <p>
 * Solo tiene sentido si el cuidador pertenece a una empresa; un cuidador
 * no perteneciente a una empresa no tiene estado en empresa.
 * </p>
 */
public enum EstadoEnEmpresa {
    /** Ha solicitado entrar en la empresa pero aun no se ha aceptado */
    PENDIENTE,
    /** Trabaja para la empresa */
    ACTIVO,
    /** Ya no trabaja para la empresa */
    BAJA
}