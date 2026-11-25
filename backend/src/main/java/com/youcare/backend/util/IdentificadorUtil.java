package com.youcare.backend.util;

import java.util.Locale;

/**
 * Utilidades para documentos identificativos (DNI, NIE y CIF).
 */
public final class IdentificadorUtil {

    private IdentificadorUtil() {
        throw new UnsupportedOperationException("Clase de utilidades, no se instancia");
    }

    /**
     * Normaliza un DNI, NIE o CIF: quita todos los espacios y guiones y lo pasa a
     * mayúsculas. Así {@code "12345678-z"} y {@code " 12345678Z "} se guardan
     * igual: {@code "12345678Z"}.
     * <p>
     * Solo transforma, no valida. El formato y la letra de control se comprueban
     * en el DTO.
     * </p>
     *
     * @param valor documento recibido, puede ser {@code null}
     * @return el documento normalizado, o {@code null}
     */
    public static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        // Mayúsculas iguales en cualquier SO 
        return valor.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }
}