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
    public static final int MAX_LONGITUD_HASH_CONTRASENIA = 100;

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

    // ====ADMIN====
    public static final int MAX_LONGITUD_DEPARTAMENTO_ADMIN = 100;

    // =====NOMBRE Y APELLIDOS ========
    public static final String PATRON_NOMBRE_Y_APELLIDOS = "^[\\p{L}]+(?:[ '-][\\p{L}]+)*$";
    public static final int MAX_LONGITUD_NOMBRE = 100;
    public static final int MAX_LONGITUD_APELLIDOS = 150;

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
    public static final int MAX_LONGITUD_DNI_NIE = 9;
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

    // ======= ROL========
    public static final int MAX_LONGITUD_ROL = 20;

    // ==== CIUDAD====
    public static final String PATRON_CIUDAD = "^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ]+(?:[ '/-][a-zA-ZáéíóúÁÉÍÓÚüÜñÑ]+)*$";
    public static final int MAX_LONGITUD_CIUDAD = 100;

    // ==== DIRECCION ===
    public static final String PATRON_DIRECCION = "^(?=.*[a-zA-Z0-9áéíóúÁÉÍÓÚüÜñÑ])[a-zA-Z0-9áéíóúÁÉÍÓÚüÜñÑºª.,/#' -]+$";
    public static final int MAX_LONGITUD_DIRECCION = 200;

    // ========= STRIKE ===========
    public static final int MAX_STRIKES = 3;

    // ========= CUIDADOR ============

    /** Longitud de las columnas que guardan un enum de estado */
    public static final int MAX_LONGITUD_ESTADO = 20;
    public static final int MAX_LONGITUD_TIPO_DOCUMENTO = 30;
    /** Clave del documento en el almacenamiento privado no es URL */
    public static final int MAX_LONGITUD_CLAVE_DOCUMENTO = 255;
    public static final int MAX_LONGITUD_MOTIVO_RECHAZO = 500;
    public static final int MAX_LONGITUD_DIA_SEMANA = 10;

    // ========= PERSONA CUIDADA ============
    public static final int MAX_LONGITUD_NECESIDADES = 1000;

    // ========= EMPRESA ============
    public static final int MAX_LONGITUD_RAZON_SOCIAL = 150;
    public static final int MAX_LONGITUD_CIF = 9;
}