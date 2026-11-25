package com.youcare.backend.entity;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.youcare.backend.entity.embeddable.Documento;
import com.youcare.backend.entity.embeddable.FranjaHoraria;
import com.youcare.backend.entity.enums.EstadoEnEmpresa;
import com.youcare.backend.entity.enums.EstadoVerificacion;
import com.youcare.backend.entity.enums.TipoDocumento;
import com.youcare.backend.exception.ValidacionException;

class CuidadorTest {

    private Cuidador cuidador;

    @BeforeEach
    void setUp() {
        // DNI en minúscula a propósito: comprueba la normalización del paso 1
        cuidador = new Cuidador("ana@test.com", "hash", "Ana", "García López", "12345678z", "600000000");
    }

        /** Crea una empresa que ha pasado la verificación por el camino real. */
    private Empresa empresaVerificada() {
        Empresa empresa = new Empresa("info@cuidasur.com", "hash", "Luis", "Pérez Ruiz",
                "Cuidados Sur SL", "B12345678", "958000000");
        Documento cif = new Documento(TipoDocumento.CIF, "docs/cif.pdf");
        empresa.aniadirDocumento(cif);
        cif.aprobar();
        empresa.verificar();
        return empresa;
    }

    @Test
    void normalizaElDniAlCrear() {
        assertEquals("12345678Z", cuidador.getDniNie());
    }

    @Test
    void empiezaSinSubir() {
        assertEquals(EstadoVerificacion.SIN_SUBIR, cuidador.getEstadoVerificacion());
    }

    @Test
    void subirCertificadoNoCambiaElEstado() {
        cuidador.aniadirDocumento(new Documento(TipoDocumento.CERTIFICADO_PROFESIONAL, "docs/cert.pdf"));

        assertEquals(EstadoVerificacion.SIN_SUBIR, cuidador.getEstadoVerificacion());
        assertEquals(1, cuidador.getDocumentos().size());
    }

    @Test
    void subirDniPasaAPendiente() {
        cuidador.aniadirDocumento(new Documento(TipoDocumento.DNI_NIE, "docs/dni.pdf"));

        assertEquals(EstadoVerificacion.PENDIENTE, cuidador.getEstadoVerificacion());
    }

       @Test
    void volverASubirDniTrasRechazoPasaAPendiente() {
        cuidador.aniadirDocumento(new Documento(TipoDocumento.DNI_NIE, "docs/dni.pdf"));
        cuidador.rechazarVerificacion();

        cuidador.aniadirDocumento(new Documento(TipoDocumento.DNI_NIE, "docs/dni2.pdf"));

        assertEquals(EstadoVerificacion.PENDIENTE, cuidador.getEstadoVerificacion());
    }

    @Test
    void subirCifLanzaExcepcionYNoLoAnade() {
        Documento cif = new Documento(TipoDocumento.CIF, "docs/cif.pdf");

        assertThrows(ValidacionException.class, () -> cuidador.aniadirDocumento(cif));
        assertTrue(cuidador.getDocumentos().isEmpty());
    }

    @Test
    void documentoNullLanzaExcepcion() {
        assertThrows(ValidacionException.class, () -> cuidador.aniadirDocumento(null));
    }

    @Test
    void laListaDeDocumentosEsDeSoloLectura() {
        Documento otro = new Documento(TipoDocumento.OTRO, "docs/otro.pdf");

        assertThrows(UnsupportedOperationException.class, () -> cuidador.getDocumentos().add(otro));
    }

        @Test
    void verificarConDniAprobadoPasaAVerificado() {
        Documento dni = new Documento(TipoDocumento.DNI_NIE, "docs/dni.pdf");
        cuidador.aniadirDocumento(dni);
        dni.aprobar();

        cuidador.verificar();

        assertEquals(EstadoVerificacion.VERIFICADO, cuidador.getEstadoVerificacion());
    }

    @Test
    void verificarConDniSinAprobarLanzaExcepcion() {
        cuidador.aniadirDocumento(new Documento(TipoDocumento.DNI_NIE, "docs/dni.pdf"));

        assertThrows(ValidacionException.class, () -> cuidador.verificar());
        assertEquals(EstadoVerificacion.PENDIENTE, cuidador.getEstadoVerificacion());
    }

    @Test
    void verificarSinDocumentosLanzaExcepcion() {
        assertThrows(ValidacionException.class, () -> cuidador.verificar());
    }

    @Test
    void rechazarSinDocumentosLanzaExcepcion() {
        assertThrows(ValidacionException.class, () -> cuidador.rechazarVerificacion());
    }

        @Test
    void vincularAEmpresaVerificadaQuedaPendiente() {
        Empresa empresa = empresaVerificada();

        cuidador.vincularAEmpresa(empresa);

        assertSame(empresa, cuidador.getEmpresa());
        assertEquals(EstadoEnEmpresa.PENDIENTE, cuidador.getEstadoEnEmpresa());
    }

    @Test
    void vincularAEmpresaSinVerificarLanzaExcepcion() {
        Empresa sinVerificar = new Empresa("otra@test.com", "hash", "Eva", "Ruiz Gil",
                "Otra SL", "B87654321", "958111111");

        assertThrows(ValidacionException.class, () -> cuidador.vincularAEmpresa(sinVerificar));
        assertNull(cuidador.getEmpresa());
    }

    @Test
    void vincularEstandoActivoEnOtraLanzaExcepcion() {
        cuidador.vincularAEmpresa(empresaVerificada());
        cuidador.aceptarEnEmpresa();
        Empresa otra = empresaVerificada();

        assertThrows(ValidacionException.class, () -> cuidador.vincularAEmpresa(otra));
    }

    @Test
    void aceptarPasaAActivo() {
        cuidador.vincularAEmpresa(empresaVerificada());

        cuidador.aceptarEnEmpresa();

        assertEquals(EstadoEnEmpresa.ACTIVO, cuidador.getEstadoEnEmpresa());
    }

    @Test
    void aceptarSiendoAutonomoLanzaExcepcion() {
        assertThrows(ValidacionException.class, () -> cuidador.aceptarEnEmpresa());
    }

    @Test
    void trasLaBajaPuedeVincularseAOtra() {
        cuidador.vincularAEmpresa(empresaVerificada());
        cuidador.darDeBajaEnEmpresa();
        Empresa otra = empresaVerificada();

        cuidador.vincularAEmpresa(otra);

        assertSame(otra, cuidador.getEmpresa());
        assertEquals(EstadoEnEmpresa.PENDIENTE, cuidador.getEstadoEnEmpresa());
    }

    @Test
    void darDeBajaSiendoAutonomoLanzaExcepcion() {
        assertThrows(ValidacionException.class, () -> cuidador.darDeBajaEnEmpresa());
    }

    @Test
    void desvincularVuelveAAutonomo() {
        cuidador.vincularAEmpresa(empresaVerificada());

        cuidador.desvincularDeEmpresa();

        assertNull(cuidador.getEmpresa());
        assertNull(cuidador.getEstadoEnEmpresa());
    }

        @Test
    void aniadirFranjaSolapadaLanzaExcepcionYNoLaAnade() {
        cuidador.aniadirFranja(new FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));
        FranjaHoraria solapada = new FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(14, 0));

        assertThrows(ValidacionException.class, () -> cuidador.aniadirFranja(solapada));
        assertEquals(1, cuidador.getDisponibilidad().size());
    }

    @Test
    void aniadirFranjasContiguasSeAceptan() {
        cuidador.aniadirFranja(new FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));
        cuidador.aniadirFranja(new FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(14, 0)));

        assertEquals(2, cuidador.getDisponibilidad().size());
    }

    @Test
    void laDisponibilidadEsDeSoloLectura() {
        FranjaHoraria franja = new FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));

        assertThrows(UnsupportedOperationException.class, () -> cuidador.getDisponibilidad().add(franja));
    }
}