package com.youcare.backend.entity;

import java.time.LocalDate;

import org.springframework.test.util.ReflectionTestUtils;

import com.youcare.backend.entity.embeddable.Documento;
import com.youcare.backend.entity.enums.TipoDocumento;

/**
 * Datos de prueba compartidos por los tests de Chat, Mensaje y tokens.
 * <p>
 * Los ids normalmente los asigna la BD al guardar. En un test unitario no hay
 * BD, así que se asignan por reflexión con {@link ReflectionTestUtils}.
 * </p>
 */
final class EntidadesTestUtil {

    /** Hash SHA-256 válido: 64 caracteres hexadecimales en minúscula. */
    static final String HASH_VALIDO = "a".repeat(64);

    private EntidadesTestUtil() {
    }

    static <T extends Usuario> T conId(T usuario, long id) {
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    static Cliente cliente(long id) {
        return conId(new Cliente("cliente" + id + "@test.com", "hash", "Ana", "García López",
                LocalDate.of(1980, 1, 1), "600000000"), id);
    }

    static Cuidador cuidador(long id) {
        return conId(new Cuidador("cuidador" + id + "@test.com", "hash", "Luis", "Pérez Ruiz",
                "12345678Z", "600000001"), id);
    }

    /** Empresa que ha pasado la verificacion*/
    static Empresa empresaVerificada(long id) {
        Empresa empresa = new Empresa("empresa" + id + "@test.com", "hash", "Marta", "Sánchez Gil",
                "Cuidados Sur SL", "B12345678", "958000000");
        Documento cif = new Documento(TipoDocumento.CIF, "docs/cif.pdf");
        empresa.aniadirDocumento(cif);
        cif.aprobar();
        empresa.verificar();
        return conId(empresa, id);
    }
}