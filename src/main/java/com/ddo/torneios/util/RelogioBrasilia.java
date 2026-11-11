package com.ddo.torneios.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class RelogioBrasilia {

    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    private RelogioBrasilia() {
    }

    public static OffsetDateTime agora() {
        return OffsetDateTime.now(ZONA);
    }

    public static LocalDateTime agoraLocal() {
        return LocalDateTime.now(ZONA);
    }

    public static OffsetDateTime paraBrasilia(OffsetDateTime data) {
        if (data == null) {
            return null;
        }
        return data.atZoneSameInstant(ZONA).toOffsetDateTime();
    }
}
