package com.youcare.backend.entity.enums;

/**
 * Estado de revision de un documento concreto por parte del administrador
 * 
 */
public enum EstadoDocumento {
   /**
    * Subido y a la espera de revision 
    */ 
   PENDIENTE,

   /**
    * Revisado y valido
    */
   APROBADO,

   /**
    * Revisado y no valido. El motivo se guarda en el ndocuemnto y el usuario debe subirlo de nuevo
    */
   RECHAZADO
}
