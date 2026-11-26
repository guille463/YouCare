package com.youcare.backend.entity;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.youcare.backend.config.Config;
import static com.youcare.backend.entity.EntidadesTestUtil.cliente;
import static com.youcare.backend.entity.EntidadesTestUtil.cuidador;
import static com.youcare.backend.entity.EntidadesTestUtil.empresaVerificada;
import com.youcare.backend.exception.ValidacionException;

class MensajeTest {

    private Cliente cliente;
    private Cuidador cuidador;
    private Chat chat;

    @BeforeEach
    void setUp() {
        cliente = cliente(1L);
        cuidador = cuidador(2L);
        chat = Chat.conCuidador(cliente, cuidador);
    }

    // ========== CREACIÓN ==========

    @Test
    void creaMensajeDelCliente() {
        Mensaje mensaje = new Mensaje(chat, cliente, "Hola");

        assertSame(chat, mensaje.getChat());
        assertSame(cliente, mensaje.getEmisor());
        assertEquals("Hola", mensaje.getContenido());
    }

    @Test
    void elProveedorTambienPuedeEscribir() {
        assertSame(cuidador, new Mensaje(chat, cuidador, "Buenas").getEmisor());
    }

    @Test
    void laEmpresaPuedeEscribirEnSuChat() {
        Empresa empresa = empresaVerificada(3L);
        Chat chatEmpresa = Chat.conEmpresa(cliente, empresa);

        assertSame(empresa, new Mensaje(chatEmpresa, empresa, "Hola").getEmisor());
    }

    @Test
    void quitaLosEspaciosDeLosExtremos() {
        assertEquals("Hola", new Mensaje(chat, cliente, "   Hola  \n").getContenido());
    }

    // ========== VALIDACIONES ==========

    @Test
    void sinChatLanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> new Mensaje(null, cliente, "Hola"));
        assertEquals("chat", ex.getCampo());
    }

    @Test
    void emisorQueNoParticipaLanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> new Mensaje(chat, cliente(99L), "Hola"));
        assertEquals("emisor", ex.getCampo());
    }

    @Test
    void emisorNullLanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> new Mensaje(chat, null, "Hola"));
        assertEquals("emisor", ex.getCampo());
    }

    @Test
    void contenidoNullLanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> new Mensaje(chat, cliente, null));
        assertEquals("contenido", ex.getCampo());
    }

    @Test
    void contenidoSoloEspaciosLanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> new Mensaje(chat, cliente, "   \n\t "));
        assertEquals("contenido", ex.getCampo());
    }

    @Test
    void contenidoConLongitudMaximaEsValido() {
        String maximo = "x".repeat(Config.MAX_LONGITUD_CONTENIDO_MENSAJE);

        assertEquals(maximo, new Mensaje(chat, cliente, maximo).getContenido());
    }

    @Test
    void contenidoQueSuperaElMaximoLanzaExcepcion() {
        String demasiado = "x".repeat(Config.MAX_LONGITUD_CONTENIDO_MENSAJE + 1);

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> new Mensaje(chat, cliente, demasiado));
        assertEquals("contenido", ex.getCampo());
    }

    @Test
    void losEspaciosDeLosExtremosNoCuentanParaElMaximo() {
        String maximo = "x".repeat(Config.MAX_LONGITUD_CONTENIDO_MENSAJE);

        assertEquals(maximo, new Mensaje(chat, cliente, "  " + maximo + "  ").getContenido());
    }

    // ========== LECTURA ==========

    @Test
    void unMensajeNuevoNoEstaLeido() {
        Mensaje mensaje = new Mensaje(chat, cliente, "Hola");

        assertFalse(mensaje.estaLeido());
        assertNull(mensaje.getFechaLectura());
    }

    @Test
    void marcarLeidoGuardaLaFecha() {
        Mensaje mensaje = new Mensaje(chat, cliente, "Hola");

        mensaje.marcarLeido();

        assertTrue(mensaje.estaLeido());
        assertNotNull(mensaje.getFechaLectura());
    }

    @Test
    void marcarLeidoDosVecesConservaLaPrimeraFecha() {
        Mensaje mensaje = new Mensaje(chat, cliente, "Hola");
        mensaje.marcarLeido();
        Instant primera = mensaje.getFechaLectura();

        mensaje.marcarLeido();

        assertEquals(primera, mensaje.getFechaLectura());
    }
}