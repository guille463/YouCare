package com.youcare.backend.entity.embeddable;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.youcare.backend.exception.ValidacionException;

class FranjaHorariaTest {

    /** Atajo para crear franjas con horas enteras. */
    private FranjaHoraria franja(DayOfWeek dia, int inicio, int fin) {
        return new FranjaHoraria(dia, LocalTime.of(inicio, 0), LocalTime.of(fin, 0));
    }

    @Test
    void finAntesDelInicioLanzaExcepcion() {
        assertThrows(ValidacionException.class, () -> franja(DayOfWeek.MONDAY, 12, 9));
    }

    @Test
    void finIgualAlInicioLanzaExcepcion() {
        assertThrows(ValidacionException.class, () -> franja(DayOfWeek.MONDAY, 9, 9));
    }

    @Test
    void diaNullLanzaExcepcion() {
        assertThrows(ValidacionException.class, () -> franja(null, 9, 12));
    }

    @Test
    void mismoDiaYHorasCruzadasSeSolapan() {
        assertTrue(franja(DayOfWeek.MONDAY, 9, 12).seSolapan(franja(DayOfWeek.MONDAY, 11, 14)));
    }

    @Test
    void franjasContiguasNoSeSolapan() {
        assertFalse(franja(DayOfWeek.MONDAY, 9, 12).seSolapan(franja(DayOfWeek.MONDAY, 12, 14)));
    }

    @Test
    void distintoDiaNoSeSolapan() {
        assertFalse(franja(DayOfWeek.MONDAY, 9, 12).seSolapan(franja(DayOfWeek.TUESDAY, 9, 12)));
    }

    @Test
    void franjasConLosMismosDatosSonIguales() {
        FranjaHoraria a = franja(DayOfWeek.MONDAY, 9, 12);
        FranjaHoraria b = franja(DayOfWeek.MONDAY, 9, 12);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}