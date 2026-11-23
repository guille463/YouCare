package com.youcare.backend.util;
import java.util.Locale; 

public final class EmailUtil {
    private EmailUtil(){
        throw new UnsupportedOperationException("Clase de utilidades, no se instancia");
    }

    /**
     * Normaliza un email: quita espacios al principio y al final y lo pasa a minusculas
     * GARANTIZA QUE "Ejemplo@gmail.com" y "ejemplo@gmail.com" se traten como el mismo email
     * @param email recibido, puede ser {@code null}
     * @return el email normalizado, o {@code null}
     */

    public static String normalizar(String email){
        if (email == null){
            return null;
        }
        // Transforma a miniscula para todo tipo de SO
        return email.trim().toLowerCase(Locale.ROOT); 
    }


    
}
