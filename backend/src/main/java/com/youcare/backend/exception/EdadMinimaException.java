package com.youcare.backend.exception;

import com.youcare.backend.config.Config;

/**
 * Se lanza cuando se intenta crear un usuario que no alcanza la edad mínima.
 *
 */
public class EdadMinimaException extends RuntimeException {

    /** @param edadMinima edad mínima exigida para registrarse */
    public EdadMinimaException() {
        super("Debes tener al menos " + Config.EDAD_MINIMA_REGISTRO + " años para registrarte");
    }
}
