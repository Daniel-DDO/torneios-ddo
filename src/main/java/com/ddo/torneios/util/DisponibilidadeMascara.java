package com.ddo.torneios.util;

import com.ddo.torneios.model.DiaSemana;
import com.ddo.torneios.model.TurnoDisponibilidade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Conversão entre a grade (dia -> turnos) e a máscara de 28 bits guardada no banco.
 * bit = diaOrdinal * 4 + turnoOrdinal.
 */
public final class DisponibilidadeMascara {

    private static final int TURNOS_POR_DIA = TurnoDisponibilidade.values().length;
    private static final int BITS_TURNOS_QUE_CONTAM = calcularBitsQueContam();

    private DisponibilidadeMascara() {}

    public static int deGrade(Map<DiaSemana, ? extends Collection<TurnoDisponibilidade>> grade) {
        int mascara = 0;
        if (grade == null) return mascara;

        for (Map.Entry<DiaSemana, ? extends Collection<TurnoDisponibilidade>> entrada : grade.entrySet()) {
            if (entrada.getKey() == null || entrada.getValue() == null) continue;
            for (TurnoDisponibilidade turno : entrada.getValue()) {
                if (turno == null) continue;
                mascara |= 1 << posicao(entrada.getKey(), turno);
            }
        }
        return mascara;
    }

    /** Sempre devolve os 7 dias (os sem disponibilidade vêm com lista vazia). */
    public static Map<DiaSemana, List<TurnoDisponibilidade>> paraGrade(int mascara) {
        Map<DiaSemana, List<TurnoDisponibilidade>> grade = new EnumMap<>(DiaSemana.class);
        for (DiaSemana dia : DiaSemana.values()) {
            List<TurnoDisponibilidade> turnos = new ArrayList<>();
            for (TurnoDisponibilidade turno : TurnoDisponibilidade.values()) {
                if ((mascara & (1 << posicao(dia, turno))) != 0) {
                    turnos.add(turno);
                }
            }
            grade.put(dia, turnos);
        }
        return grade;
    }

    /** Dias com pelo menos um turno que conta (manhã, tarde ou noite). Madrugada sozinha não conta. */
    public static int contarDiasValidos(int mascara) {
        int dias = 0;
        for (DiaSemana dia : DiaSemana.values()) {
            int bitsDoDia = (mascara >> (dia.ordinal() * TURNOS_POR_DIA)) & ((1 << TURNOS_POR_DIA) - 1);
            if ((bitsDoDia & BITS_TURNOS_QUE_CONTAM) != 0) {
                dias++;
            }
        }
        return dias;
    }

    private static int posicao(DiaSemana dia, TurnoDisponibilidade turno) {
        return dia.ordinal() * TURNOS_POR_DIA + turno.ordinal();
    }

    private static int calcularBitsQueContam() {
        int bits = 0;
        for (TurnoDisponibilidade turno : TurnoDisponibilidade.values()) {
            if (turno.isContaParaMinimo()) {
                bits |= 1 << turno.ordinal();
            }
        }
        return bits;
    }
}
