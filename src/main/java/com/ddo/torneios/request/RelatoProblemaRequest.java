package com.ddo.torneios.request;

public record RelatoProblemaRequest(
        String nomeMandante,
        String timeMandante,
        String nomeVisitante,
        String timeVisitante,
        String relato
) {}