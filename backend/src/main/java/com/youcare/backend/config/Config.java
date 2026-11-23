package com.youcare.backend.config;

import java.util.List;

public final class Config {

    // Constructor privado para evitar que se creen objetos de esta clase
    private Config() {
        throw new UnsupportedOperationException("Esta es una clase de constantes y no se puede instanciar");
    }

    // ============== CONTRASENIA ==============
    public static final int MAX_INTENTOS_CONTRASENIA = 3;
    public static final int MIN_LONGITUD_CONTRASENIA = 8;

    /** Para evitar ataques de denegación */
    public static final int MAX_LONGITUD_CONTRASENIA = 64;

    public static final int HORAS_VALIDEZ_RECUPERACION_CONTRASENIA = 1;
    /**
     * Tras superar {@link #MAX_INTENTOS_CONTRASENIA} intentos al meter la
     * contrasenia
     */
    public static final int MINUTOS_BLOQUEO_CUENTA = 15;
    /**
     * Expresión regular que valida la complejidad y los caracteres permitidos
     * en una contrasenia:
     * <ul>
     * <li>Al menos una letra minúscula ({@code a-z}).</li>
     * <li>Al menos una letra mayúscula ({@code A-Z}).</li>
     * <li>Al menos un dígito ({@code 0-9}).</li>
     * <li>Solo permite letras sin tilde, números y los símbolos
     * {@code @ $ ! % * ? & . _ -} (sin espacios ni {@code ñ}).</li>
     * </ul>
     * <p>
     * No comprueba la longitud mínima; debe combinarse con
     * {@link #MIN_LONGITUD_CONTRASENIA}.
     * </p>
     */

    public static final String PATRON_CONTRASENIA = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d@$!%*?&._-]+$";
   

    //===== NOMBRE========
    // public static final String 
    public static final int MAX_LONGITUD_NOMBRE = 255;


    // ================ EMAIL ===================
    public static final int HORAS_VALIDEZ_EMAIL = 24;
    public static final String PATRON_EMAIL = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final int MAX_LONGITUD_EMAIL = 255;

    // =============== TELEFONO ==============
    /** Patron tanto de móviles como de teléfonos de empresa (España) */
    public static final String PATRON_TELEFONO = "^(\\+34|0034|34)?[6789]\\d{8}$";
    public static final int MAX_LONGITUD_TELEFONO = 20;

    // ============= DNI ============
    public static final String PATRON_DNI_NIE = "^([0-9]{8}|[XYZ][0-9]{7})[TRWAGMYFPDXBNJZSQVHLCKE]$";

    // =========== CIF ============
    public static final String PATRON_CIF = "^[ABCDEFGHJNPQRSUVW][0-9]{7}[0-9A-J]$";

    // ========= IMAGENES ============
    /** El limite de subida de una imagen son 5 MB */
    public static final long MAX_TAMANIO_IMAGEN_BYTES = 5L * 1024 * 1024;
    public static final List<String> EXTENSIONES_IMAGEN_PERMITIDAS = List.of("jpg", "jpeg", "png", "webp");
    public static final String IMAGEN_PERFIL_POR_DEFECTO = "/assets/img/default-avatar.png";
    public static final int MAX_LONGITUD_CLAVE_IMG = 255; 

    // =========== EDAD ============
    public static final int EDAD_MINIMA_REGISTRO = 18;

    //======= ROL========
    public static final int MAX_LONGITUD_ROL = 20;

}