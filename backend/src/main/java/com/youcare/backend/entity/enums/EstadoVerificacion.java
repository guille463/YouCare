package com.youcare.backend.entity.enums;

/**
 * Estado de verificacion de la cuenta de un cuidador o una empresa
 * <p>
 * Solo las cuentas {@Link #VERIFICADO} aparecen en las busquedas publicas
 * <p>
 */
public enum EstadoVerificacion {
    /**
     * Cuenta registrada sin documentos.
     */
    SIN_SUBIR,
    /**
     * Cuenta registrada con documentos sin revisar. No aparece en las busquedas
     */
    PENDIENTE,
    /**
     * Todos los documentos obligatorios estan aprobados. Puede aparecer en las
     * busquedas y recibir reservas
     */
    VERIFICADO,
    /**
     * La verificacion ha sido denegada. No aparece en las busquedas
     */
    RECHAZADO
}
