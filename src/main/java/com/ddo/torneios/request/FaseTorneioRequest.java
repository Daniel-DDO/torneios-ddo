package com.ddo.torneios.request;

import com.ddo.torneios.model.AlgoritmoGeracaoLiga;
import com.ddo.torneios.model.AlgoritmoGeracaoMataMata;
import com.ddo.torneios.model.FaseMataMata;
import com.ddo.torneios.model.TipoTorneio;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class FaseTorneioRequest {

    @NotNull(message = "O ID do torneio é obrigatório")
    private String torneioId;

    @NotNull(message = "O nome da fase é obrigatório")
    private String nome;

    @NotNull(message = "A ordem da fase é obrigatória")
    @Positive
    private Integer ordem;

    @NotNull(message = "O tipo de torneio é obrigatório")
    private TipoTorneio tipoTorneio;

    private Integer numeroRodadas; //PONTOS_CORRIDOS
    private FaseMataMata faseInicialMataMata; //MATA_MATA
    private Boolean temJogoVolta; //MATA_MATA
    private AlgoritmoGeracaoLiga algoritmoLiga;
    private AlgoritmoGeracaoMataMata algoritmoMataMata;
    private Integer maxJogosEmCasa;
}