package com.ddo.torneios.dto;

import com.ddo.torneios.model.Punicao;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PunicaoDTO {
    private String id;
    private Integer pontos;
    private String motivo;
    private LocalDateTime dataAplicacao;
    private String nomeJogador;

    public PunicaoDTO(Punicao punicao) {
        this.id = punicao.getId();
        this.pontos = punicao.getPontos();
        this.motivo = punicao.getMotivo();
        this.dataAplicacao = punicao.getDataAplicacao();
        if (punicao.getParticipacaoFase() != null && punicao.getParticipacaoFase().getJogadorClube() != null) {
            this.nomeJogador = punicao.getParticipacaoFase().getJogadorClube().getJogador().getNome();
        }
    }
}