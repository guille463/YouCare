package com.youcare.backend.entity.enums;
/**
 * 
 * Tipos de documentos que un ciudador o una empresa sube para verificar su identidad o su cualificacion
 */
public enum TipoDocumento {
    /**
     * Doucmento de identidad del ciudador DNI o NIE
     */
    DNI_NIE,

    /**
     * Titulo o certificado que acredita la formacion en ciudados
     */
    CERTIFICADO_PROFESIONAL,

    /**
     * Documento que acredita el CIF de la empresa
     */
    CIF,

    /**
     * Cualquier otro documento que el administrador pida para completar la verificacion
     */
    OTRO
       
}
