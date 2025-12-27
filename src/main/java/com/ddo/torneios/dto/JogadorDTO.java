package com.ddo.torneios.dto;

import com.ddo.torneios.model.Cargo;
import com.ddo.torneios.model.Insignia;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.model.StatusJogador;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record JogadorDTO(
        String id,
        String nome,
        String discord,
        Integer finais,
        Integer titulos,
        Integer golsMarcados,
        Integer golsSofridos,
        Integer partidasJogadas,
        Integer vitorias,
        Integer empates,
        Integer derrotas,
        LocalDateTime criacaoConta,
        LocalDateTime modificacaoConta,
        StatusJogador statusJogador,
        boolean contaReivindicada,
        Cargo cargo,
        String imagem,
        String descricao,
        LocalDateTime suspensoAte,
        Long cartoesAmarelos,
        Long cartoesVermelhos,
        BigDecimal saldoVirtual,
        Set<Insignia> insignias,
        BigDecimal pontosCoeficiente
) {
    public JogadorDTO(Jogador jogador) {
        this(
                jogador.getId(),
                jogador.getNome(),
                jogador.getDiscord(),
                jogador.getFinais(),
                jogador.getTitulos(),
                jogador.getGolsMarcados(),
                jogador.getGolsSofridos(),
                jogador.getPartidasJogadas(),
                jogador.getVitorias(),
                jogador.getEmpates(),
                jogador.getDerrotas(),
                jogador.getCriacaoConta(),
                jogador.getModificacaoConta(),
                jogador.getStatusJogador(),
                jogador.isContaReivindicada(),
                jogador.getCargo(),
                jogador.getImagem(),
                jogador.getDescricao(),
                jogador.getSuspensoAte(),
                jogador.getCartoesAmarelos(),
                jogador.getCartoesVermelhos(),
                jogador.getSaldoVirtual(),
                jogador.getInsignias(),
                jogador.getPontosCoeficiente()
        );
    }
}