package com.youcare.backend.entity;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.youcare.backend.entity.EntidadesTestUtil.cliente;
import static com.youcare.backend.entity.EntidadesTestUtil.cuidador;
import static com.youcare.backend.entity.EntidadesTestUtil.empresaVerificada;
import com.youcare.backend.exception.ValidacionException;

class ChatTest {

    private Cliente cliente;
    private Cuidador cuidador;
    private Empresa empresa;

    @BeforeEach
    void setUp() {
        cliente = cliente(1L);
        cuidador = cuidador(2L);
        empresa = empresaVerificada(3L);
    }

    // ========== CREACIÓN ==========

    @Test
    void conCuidadorAutonomoGuardaSoloElCuidador() {
        Chat chat = Chat.conCuidador(cliente, cuidador);

        assertSame(cliente, chat.getCliente());
        assertSame(cuidador, chat.getCuidador());
        assertNull(chat.getEmpresa());
        assertSame(cuidador, chat.getProveedor());
    }

    @Test
    void conEmpresaGuardaSoloLaEmpresa() {
        Chat chat = Chat.conEmpresa(cliente, empresa);

        assertSame(empresa, chat.getEmpresa());
        assertNull(chat.getCuidador());
        assertSame(empresa, chat.getProveedor());
    }

    @Test
    void sinMensajesNoTieneFechaDeUltimoMensaje() {
        assertNull(Chat.conEmpresa(cliente, empresa).getFechaUltimoMensaje());
    }

    @Test
    void conCuidadorSinClienteLanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> Chat.conCuidador(null, cuidador));
        assertEquals("cliente", ex.getCampo());
    }

    @Test
    void conEmpresaSinClienteLanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> Chat.conEmpresa(null, empresa));
        assertEquals("cliente", ex.getCampo());
    }

    @Test
    void conCuidadorNullLanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> Chat.conCuidador(cliente, null));
        assertEquals("cuidador", ex.getCampo());
    }

    @Test
    void conEmpresaNullLanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> Chat.conEmpresa(cliente, null));
        assertEquals("empresa", ex.getCampo());
    }

    // ========== CUIDADOR DE EMPRESA ==========

    @Test
    void cuidadorActivoEnEmpresaNoPuedeChatearDirectamente() {
        cuidador.vincularAEmpresa(empresa);
        cuidador.aceptarEnEmpresa();

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> Chat.conCuidador(cliente, cuidador));
        assertEquals("cuidador", ex.getCampo());
    }

    @Test
    void cuidadorPendienteEnEmpresaSigueSiendoAutonomo() {
        cuidador.vincularAEmpresa(empresa);

        assertSame(cuidador, Chat.conCuidador(cliente, cuidador).getProveedor());
    }

    @Test
    void cuidadorDeBajaEnEmpresaSigueSiendoAutonomo() {
        cuidador.vincularAEmpresa(empresa);
        cuidador.aceptarEnEmpresa();
        cuidador.darDeBajaEnEmpresa();

        assertSame(cuidador, Chat.conCuidador(cliente, cuidador).getProveedor());
    }

    // ========== PARTICIPANTES ==========

    @Test
    void clienteYCuidadorSonParticipantes() {
        Chat chat = Chat.conCuidador(cliente, cuidador);

        assertTrue(chat.esParticipante(cliente));
        assertTrue(chat.esParticipante(cuidador));
    }

    @Test
    void empresaEsParticipanteDeSuChat() {
        assertTrue(Chat.conEmpresa(cliente, empresa).esParticipante(empresa));
    }

    @Test
    void otroUsuarioNoEsParticipante() {
        Chat chat = Chat.conCuidador(cliente, cuidador);

        assertFalse(chat.esParticipante(cliente(99L)));
        assertFalse(chat.esParticipante(empresa));
    }

    @Test
    void compararPorIdYNoPorReferencia() {
        // Otro objeto con el mismo id: es lo que pasa con un proxy LAZY de Hibernate
        Cliente mismoClienteOtraInstancia = cliente(1L);

        assertTrue(Chat.conCuidador(cliente, cuidador).esParticipante(mismoClienteOtraInstancia));
    }

    @Test
    void usuarioNullOSinIdNoEsParticipante() {
        Chat chat = Chat.conCuidador(cliente, cuidador);
        Cliente sinGuardar = new Cliente("nuevo@test.com", "hash", "Eva", "Ruiz Mora",
                LocalDate.of(1990, 5, 5), "600000009");

        assertFalse(chat.esParticipante(null));
        assertFalse(chat.esParticipante(sinGuardar));
    }

    // ========== ÚLTIMO MENSAJE ==========

    @Test
    void registrarMensajeActualizaLaFecha() {
        Chat chat = Chat.conEmpresa(cliente, empresa);
        Instant fecha = Instant.parse("2026-11-26T10:00:00Z");

        chat.registrarMensaje(fecha);

        assertEquals(fecha, chat.getFechaUltimoMensaje());
    }
}