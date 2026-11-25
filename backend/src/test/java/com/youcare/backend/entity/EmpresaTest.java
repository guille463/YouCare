package com.youcare.backend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.youcare.backend.entity.embeddable.Documento;
import com.youcare.backend.entity.enums.EstadoVerificacion;
import com.youcare.backend.entity.enums.TipoDocumento;
import com.youcare.backend.exception.ValidacionException;

class EmpresaTest {

    private Empresa empresa;

    @BeforeEach
    void setUp() {
        // CIF en minúscula y con guion a proposito: comprueba la normalizacion
        empresa = new Empresa("info@cuidasur.com", "hash", "Luis", "Pérez Ruiz", "Cuidados Sur SL", "b-12345678",
                "958000000");
    }

    @Test
    void normalizaElCifAlCrear() {
        assertEquals("B12345678", empresa.getCif());
    }

    @Test
    void subirCifPasaAPendiente() {
        empresa.aniadirDocumento(new Documento(TipoDocumento.CIF, "docs/cif.pdf"));

        assertEquals(EstadoVerificacion.PENDIENTE, empresa.getEstadoVerificacion());
    }

    @Test
    void subirOtroDocumentoNoCambiaElEstado() {
        empresa.aniadirDocumento(new Documento(TipoDocumento.OTRO, "docs/otro.pdf"));

        assertEquals(EstadoVerificacion.SIN_SUBIR, empresa.getEstadoVerificacion());
    }

    @Test
    void subirCertificadoLanzaExcepcionYNoLoAnade() {
        Documento cert = new Documento(TipoDocumento.CERTIFICADO_PROFESIONAL, "docs/cert.pdf");

        assertThrows(ValidacionException.class, () -> empresa.aniadirDocumento(cert));
        assertTrue(empresa.getDocumentos().isEmpty());
    }

    @Test
    void laListaDeDocumentosEsDeSoloLectura() {
        Documento otro = new Documento(TipoDocumento.OTRO, "docs/otro.pdf");

        assertThrows(UnsupportedOperationException.class, () -> empresa.getDocumentos().add(otro));
    }

        @Test
    void verificarConCifAprobadoPasaAVerificado() {
        Documento cif = new Documento(TipoDocumento.CIF, "docs/cif.pdf");
        empresa.aniadirDocumento(cif);
        cif.aprobar();

        empresa.verificar();

        assertEquals(EstadoVerificacion.VERIFICADO, empresa.getEstadoVerificacion());
    }

    @Test
    void verificarConCifRechazadoLanzaExcepcion() {
        Documento cif = new Documento(TipoDocumento.CIF, "docs/cif.pdf");
        empresa.aniadirDocumento(cif);
        cif.rechazar("Documento ilegible");

        assertThrows(ValidacionException.class, () -> empresa.verificar());
    }

    @Test
    void rechazarVerificacionPasaARechazado() {
        empresa.aniadirDocumento(new Documento(TipoDocumento.CIF, "docs/cif.pdf"));

        empresa.rechazarVerificacion();

        assertEquals(EstadoVerificacion.RECHAZADO, empresa.getEstadoVerificacion());
    }
}