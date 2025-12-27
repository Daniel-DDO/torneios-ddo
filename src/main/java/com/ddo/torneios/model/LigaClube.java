package com.ddo.torneios.model;

public enum LigaClube {
    LALIGA("La Liga - ESP"),
    PREMIER_LEAGUE("Premier League - ING"),
    SERIEA("Serie A - ITA"),
    BUNDESLIGA("Bundesliga - ALE"),
    LIGUEONE("Ligue One - FRA"),
    BRASILEIRAO("Brasileirão - BRA"),
    ARGENTINA("Liga Argentina - ARG"),
    MLS("Major League Soccer - EUA"),
    SAUDI_PRO_LEAGUE("Saudi Pro League - ARA"),
    ERIDIVISE("Eridivise - HOL"),
    SELECAO("Seleção"),
    OUTROS("Outros");

    private final String liga;

    LigaClube(String liga) {
        this.liga = liga;
    }

    public String getLiga() {
        return liga;
    }
}

