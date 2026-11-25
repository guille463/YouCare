package com.youcare.backend.entity.embeddable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.youcare.backend.config.Config;
import com.youcare.backend.entity.enums.EstadoDocumento;
import com.youcare.backend.entity.enums.TipoDocumento;
import com.youcare.backend.exception.ValidacionException;

class DocumentoTest {

    private Documento documento;

    @BeforeEach
    void setUp() {
        documento = new Documento(TipoDocumento.DNI_NIE, "docs/dni.pdf");
    }

    @Test
    void empiezaPendiente() {
        assertEquals(EstadoDocumento.PENDIENTE, documento.getEstado());
    }

    @Test
    void rechazarGuardaElMotivoRecortado() {
        documento.rechazar("  Imagen borrosa  ");

        assertEquals(EstadoDocumento.RECHAZADO, documento.getEstado());
        assertEquals("Imagen borrosa", documento.getMotivoRechazo());
    }

    @Test
    void rechazarSinMotivoLanzaExcepcion() {
        assertThrows(ValidacionException.class, () -> documento.rechazar("   "));
    }

    @Test
    void motivoEnElLimiteConEspaciosSeAcepta() {
        String motivo = "a".repeat(Config.MAX_LONGITUD_MOTIVO_RECHAZO) + "   ";

        documento.rechazar(motivo);

        assertEquals(Config.MAX_LONGITUD_MOTIVO_RECHAZO, documento.getMotivoRechazo().length());
    }

    @Test
    void motivoDemasiadoLargoLanzaExcepcion() {
        String motivo = "a".repeat(Config.MAX_LONGITUD_MOTIVO_RECHAZO + 1);

        assertThrows(ValidacionException.class, () -> documento.rechazar(motivo));
    }

    @Test
    void aprobarBorraElMotivoAnterior() {
        documento.rechazar("Imagen borrosa");

        documento.aprobar();

        assertEquals(EstadoDocumento.APROBADO, documento.getEstado());
        assertNull(documento.getMotivoRechazo());
    }
}