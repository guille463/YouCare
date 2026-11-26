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

import com.youcare.backend.config.Config;
import static com.youcare.backend.entity.EntidadesTestUtil.HASH_VALIDO;
import static com.youcare.backend.entity.EntidadesTestUtil.cliente;
import com.youcare.backend.exception.ValidacionException;

class RefreshTokenTest {

    private Cliente usuario;
    private RefreshToken token;

    @BeforeEach
    void setUp() {
        usuario = cliente(1L);
        token = new RefreshToken(usuario, HASH_VALIDO);
    }

    // ========== CREACIÓN ==========

    @Test
    void guardaUsuarioYHash() {
        assertSame(usuario, token.getUsuario());
        assertEquals(HASH_VALIDO, token.getHashToken());
        assertNotNull(token.getFechaCreacion());
    }

    @Test
    void caducaALosDiasConfigurados() {
        Duration validez = Duration.between(token.getFechaCreacion(), token.getFechaExpiracion());

        assertEquals(Duration.ofDays(Config.DIAS_VALIDEZ_REFRESH_TOKEN), validez);
    }

    @Test
    void sinUsuarioLanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> new RefreshToken(null, HASH_VALIDO));
        assertEquals("usuario", ex.getCampo());
    }

    @Test
    void hashNullLanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> new RefreshToken(usuario, null));
        assertEquals("hashToken", ex.getCampo());
    }

    @Test
    void hashCortoLanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> new RefreshToken(usuario, "a".repeat(Config.LONGITUD_HASH_TOKEN - 1)));
        assertEquals("hashToken", ex.getCampo());
    }

    @Test
    void hashLargoLanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> new RefreshToken(usuario, "a".repeat(Config.LONGITUD_HASH_TOKEN + 1)));
        assertEquals("hashToken", ex.getCampo());
    }

    // ========== ACTIVO / CADUCADO ==========

    @Test
    void recienCreadoEstaActivo() {
        assertTrue(token.estaActivo(Instant.now()));
        assertNull(token.getFechaRevocacion());
    }

    @Test
    void unSegundoAntesDeCaducarSigueActivo() {
        assertTrue(token.estaActivo(token.getFechaExpiracion().minusSeconds(1)));
    }

    @Test
    void enElInstanteDeExpiracionYaNoEstaActivo() {
        assertFalse(token.estaActivo(token.getFechaExpiracion()));
    }

    // ========== REVOCACIÓN ==========

    @Test
    void revocadoNoEstaActivo() {
        token.revocar();

        assertNotNull(token.getFechaRevocacion());
        assertFalse(token.estaActivo(Instant.now()));
    }

    @Test
    void revocarDosVecesConservaLaPrimeraFecha() {
        token.revocar();
        Instant primera = token.getFechaRevocacion();

        token.revocar();

        assertEquals(primera, token.getFechaRevocacion());
    }
}