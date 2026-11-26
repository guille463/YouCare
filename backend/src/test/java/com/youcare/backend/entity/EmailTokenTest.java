package com.youcare.backend.entity;

import java.time.Duration;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.youcare.backend.config.Config;
import static com.youcare.backend.entity.EntidadesTestUtil.HASH_VALIDO;
import static com.youcare.backend.entity.EntidadesTestUtil.cliente;
import com.youcare.backend.entity.enums.TipoEmailToken;
import com.youcare.backend.exception.ValidacionException;

class EmailTokenTest {

    private Cliente usuario;
    private EmailToken verificacion;

    @BeforeEach
    void setUp() {
        usuario = cliente(1L);
        verificacion = new EmailToken(usuario, HASH_VALIDO, TipoEmailToken.VERIFICACION);
    }

    // ========== CREACIÓN ==========

    @Test
    void guardaUsuarioHashYTipo() {
        assertSame(usuario, verificacion.getUsuario());
        assertEquals(HASH_VALIDO, verificacion.getHashToken());
        assertEquals(TipoEmailToken.VERIFICACION, verificacion.getTipo());
        assertNull(verificacion.getFechaUso());
    }

    @Test
    void verificacionCaducaEnLasHorasDeEmail() {
        Duration validez = Duration.between(verificacion.getFechaCreacion(), verificacion.getFechaExpiracion());

        assertEquals(Duration.ofHours(Config.HORAS_VALIDEZ_EMAIL), validez);
    }

    @Test
    void recuperacionCaducaEnLasHorasDeRecuperacion() {
        EmailToken recuperacion = new EmailToken(usuario, HASH_VALIDO, TipoEmailToken.RECUPERACION);
        Duration validez = Duration.between(recuperacion.getFechaCreacion(), recuperacion.getFechaExpiracion());

        assertEquals(Duration.ofHours(Config.HORAS_VALIDEZ_RECUPERACION_CONTRASENIA), validez);
    }

    @Test
    void sinUsuarioLanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> new EmailToken(null, HASH_VALIDO, TipoEmailToken.VERIFICACION));
        assertEquals("usuario", ex.getCampo());
    }

    @Test
    void hashInvalidoLanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> new EmailToken(usuario, "corto", TipoEmailToken.VERIFICACION));
        assertEquals("hashToken", ex.getCampo());
    }

    @Test
    void sinTipoLanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> new EmailToken(usuario, HASH_VALIDO, null));
        assertEquals("tipo", ex.getCampo());
    }

    // ========== VALIDEZ ==========

    @Test
    void recienCreadoEsValido() {
        assertTrue(verificacion.esValido(Instant.now()));
    }

    @Test
    void enElInstanteDeExpiracionYaNoEsValido() {
        assertFalse(verificacion.esValido(verificacion.getFechaExpiracion()));
    }

    // ========== USO ÚNICO ==========

    @Test
    void marcarUsadoGuardaLaFechaYDejaDeSerValido() {
        verificacion.marcarUsado();

        assertNotNull(verificacion.getFechaUso());
        assertFalse(verificacion.esValido(Instant.now()));
    }

    @Test
    void noSePuedeUsarDosVeces() {
        verificacion.marcarUsado();

        ValidacionException ex = assertThrows(ValidacionException.class, verificacion::marcarUsado);
        assertEquals("token", ex.getCampo());
    }

    @Test
    void noSePuedeUsarSiHaCaducado() {
        // Simula que ya pasó la fecha de expiración
        ReflectionTestUtils.setField(verificacion, "fechaExpiracion", Instant.now().minusSeconds(1));

        ValidacionException ex = assertThrows(ValidacionException.class, verificacion::marcarUsado);
        assertEquals("token", ex.getCampo());
        assertNull(verificacion.getFechaUso());
    }
}