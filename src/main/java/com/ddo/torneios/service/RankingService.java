package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.JogadorRepository;
import com.ddo.torneios.repository.PartidaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class RankingService {

    private static final int MIN_PARTIDAS_COLOCACAO = 5;
    private static final int STRIKES_PARA_REBAIXAR = 2;
    private static final int LARGURA_REENTRADA_DECAIMENTO = 100;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private JogadorRepository jogadorRepository;
    @Autowired private PartidaRepository partidaRepository;

    public enum Lado { MANDANTE, VISITANTE }

    private record Estado(int pontos, RankJogador rank, int partidas, int strikes) {}
    private record Transicao(Estado antes, Estado depois) {}

    @Autowired private InsigniaService insigniaService;

    private static final Map<RankJogador, String> INSIGNIA_POR_RANK = Map.of(
            RankJogador.BRONZE, InsigniaService.RANK_BRONZE,
            RankJogador.PRATA, InsigniaService.RANK_PRATA,
            RankJogador.OURO, InsigniaService.RANK_OURO,
            RankJogador.DIAMANTE, InsigniaService.RANK_DIAMANTE,
            RankJogador.CHAMPION, InsigniaService.RANK_CHAMPION
    );

    private void concederInsigniaDeRankSeAplicavel(String jogadorId, Estado antes, Estado depois) {
        if (depois.rank().ordinal() <= antes.rank().ordinal()) return;
        String nomeInsignia = INSIGNIA_POR_RANK.get(depois.rank());
        if (nomeInsignia != null) {
            insigniaService.concederInsigniaManual(jogadorId, nomeInsignia);
        }
    }

    private Transicao calcularProximoEstado(Estado atual, ResultadoPartida resultado) {
        int delta = atual.rank().getPontosPorResultado(resultado);
        int pontosDepois = Math.max(0, atual.pontos() + delta);
        int partidasDepois = atual.partidas() + 1;
        int strikesDepois = atual.strikes();

        RankJogador rankCalculado = RankJogador.porPontos(pontosDepois);
        RankJogador rankDepois = atual.rank();
        boolean emColocacao = partidasDepois < MIN_PARTIDAS_COLOCACAO;

        if (emColocacao) {
            rankDepois = RankJogador.SEM_RANK;
        } else if (partidasDepois == MIN_PARTIDAS_COLOCACAO && atual.rank() == RankJogador.SEM_RANK) {
            rankDepois = rankCalculado;
            strikesDepois = 0;
        } else if (rankCalculado.ordinal() > atual.rank().ordinal()) {
            rankDepois = rankCalculado;
            strikesDepois = 0;
        } else if (pontosDepois < atual.rank().getPontosMinimos()) {
            strikesDepois = atual.strikes() + 1;
            if (strikesDepois >= STRIKES_PARA_REBAIXAR) {
                rankDepois = rankCalculado;
                strikesDepois = 0;
            }
        } else {
            strikesDepois = 0;
        }

        return new Transicao(atual, new Estado(pontosDepois, rankDepois, partidasDepois, strikesDepois));
    }

    @Transactional
    public void aplicarResultado(String jogadorId, ResultadoPartida resultado, String partidaId, Lado lado) {
        RankingEstadoDTO estadoDto = jogadorRepository.buscarEstadoRanking(jogadorId)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado para ranking: " + jogadorId));

        Estado atual = paraEstado(estadoDto);
        Transicao transicao = calcularProximoEstado(atual, resultado);
        Estado depois = transicao.depois();


        jogadorRepository.atualizarEstadoRanking(jogadorId, depois.pontos(), depois.rank(), depois.partidas(), depois.strikes());
        salvarSnapshot(partidaId, lado, transicao);
        concederInsigniaDeRankSeAplicavel(jogadorId, transicao.antes(), depois);
    }

    @Transactional
    public void reverterResultado(String partidaId, String jogadorId, Lado lado) {
        String json = lado == Lado.MANDANTE
                ? partidaRepository.buscarSnapshotMandante(partidaId)
                : partidaRepository.buscarSnapshotVisitante(partidaId);
        if (json == null || json.isBlank()) return;

        RankingSnapshot snapshot = desserializar(json);
        jogadorRepository.atualizarEstadoRanking(jogadorId, snapshot.pontosAntes(), snapshot.rankAntes(),
                snapshot.partidasAntes(), snapshot.strikesAntes());

        if (lado == Lado.MANDANTE) partidaRepository.atualizarSnapshotMandante(partidaId, null);
        else partidaRepository.atualizarSnapshotVisitante(partidaId, null);
    }

    @Transactional
    public void zerarRanking(String jogadorId) {
        jogadorRepository.zerarRankingPorId(jogadorId);
        partidaRepository.limparSnapshotsMandanteDoJogador(jogadorId);
        partidaRepository.limparSnapshotsVisitanteDoJogador(jogadorId);
    }

    @Transactional
    public void zerarRankingTodos() {
        jogadorRepository.zerarRankingTodos();
        partidaRepository.limparTodosSnapshots();
    }

    @Transactional
    public void recalcularApartirHistorico(String jogadorId) {
        zerarRanking(jogadorId);

        List<PartidaResultadoDTO> partidas = partidaRepository.buscarResultadosDoJogador(jogadorId);

        Estado estado = new Estado(0, RankJogador.SEM_RANK, 0, 0);

        for (PartidaResultadoDTO partida : partidas) {
            if (partida.wo()) continue;

            boolean mandante = jogadorId.equals(partida.mandanteJogadorId());
            ResultadoPartida resultado = obterResultado(partida, mandante);
            if (resultado == null) continue;

            Transicao transicao = calcularProximoEstado(estado, resultado);
            concederInsigniaDeRankSeAplicavel(jogadorId, transicao.antes(), transicao.depois());
            estado = transicao.depois();

            salvarSnapshot(partida.partidaId(), mandante ? Lado.MANDANTE : Lado.VISITANTE, transicao);
        }

        jogadorRepository.atualizarEstadoRanking(jogadorId, estado.pontos(), estado.rank(), estado.partidas(), estado.strikes());
    }

    @Transactional
    public void recalcularTodos() {
        for (String jogadorId : jogadorRepository.buscarTodosIds()) {
            recalcularApartirHistorico(jogadorId);
        }
    }

    private ResultadoPartida obterResultado(PartidaResultadoDTO partida, boolean jogadorEhMandante) {
        int gf = valor(jogadorEhMandante ? partida.golsMandante() : partida.golsVisitante());
        int gc = valor(jogadorEhMandante ? partida.golsVisitante() : partida.golsMandante());

        if (gf > gc) return ResultadoPartida.VITORIA;
        if (gc > gf) return ResultadoPartida.DERROTA;

        Integer penFavor = jogadorEhMandante ? partida.penaltisMandante() : partida.penaltisVisitante();
        Integer penContra = jogadorEhMandante ? partida.penaltisVisitante() : partida.penaltisMandante();
        if (penFavor != null && penContra != null) {
            if (penFavor > penContra) return ResultadoPartida.VITORIA;
            if (penContra > penFavor) return ResultadoPartida.DERROTA;
        }
        return ResultadoPartida.EMPATE;
    }

    @Transactional
    public void aplicarDecaimentoTemporada(int quantidadeRanksParaCair) {
        RankJogador[] ranks = RankJogador.values();

        for (int i = RankJogador.BRONZE.ordinal(); i <= RankJogador.CHAMPION.ordinal(); i++) {
            RankJogador rankAntes = ranks[i];
            int novoOrdinal = Math.max(RankJogador.BRONZE.ordinal(), i - quantidadeRanksParaCair);
            RankJogador rankDepois = ranks[novoOrdinal];
            if (rankDepois == rankAntes) continue;

            int teto = rankDepois == RankJogador.CHAMPION ? Integer.MAX_VALUE : rankDepois.getPontosMaximos();
            int pontosDepois = Math.min(rankDepois.getPontosMinimos() + LARGURA_REENTRADA_DECAIMENTO, teto);

            jogadorRepository.aplicarDecaimentoPorRank(rankAntes, rankDepois, pontosDepois);
        }
    }

    public RankingDetalheDTO obterDetalhe(String jogadorId) {
        RankingEstadoDTO dto = jogadorRepository.buscarEstadoRanking(jogadorId)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado para ranking: " + jogadorId));
        return montarDetalhe(dto);
    }

    public List<RankingDetalheDTO> obterTabela() {
        return jogadorRepository.buscarTabelaRanking().stream().map(this::montarDetalhe).toList();
    }

    private Estado paraEstado(RankingEstadoDTO dto) {
        return new Estado(
                valor(dto.rankPoints()),
                dto.rank() != null ? dto.rank() : RankJogador.SEM_RANK,
                valor(dto.partidasRankeadas()),
                valor(dto.strikesRebaixamento())
        );
    }

    private RankingDetalheDTO montarDetalhe(RankingEstadoDTO dto) {
        RankJogador rank = dto.rank() != null ? dto.rank() : RankJogador.SEM_RANK;
        int pontos = valor(dto.rankPoints());
        int partidasRankeadas = valor(dto.partidasRankeadas());
        int strikes = valor(dto.strikesRebaixamento());
        boolean emColocacao = partidasRankeadas < MIN_PARTIDAS_COLOCACAO;

        return new RankingDetalheDTO(
                dto.jogadorId(), dto.nome(), rank.getNomeExibicao(), pontos,
                emColocacao, emColocacao ? MIN_PARTIDAS_COLOCACAO - partidasRankeadas : 0,
                rank.getPontosParaProximoRank(pontos), pontos - rank.getPontosMinimos(),
                strikes, STRIKES_PARA_REBAIXAR
        );
    }

    private void salvarSnapshot(String partidaId, Lado lado, Transicao t) {
        String json = serializar(new RankingSnapshot(
                t.antes().pontos(), t.antes().rank(), t.antes().partidas(), t.antes().strikes(),
                t.depois().pontos(), t.depois().rank(), t.depois().partidas(), t.depois().strikes()
        ));
        if (lado == Lado.MANDANTE) partidaRepository.atualizarSnapshotMandante(partidaId, json);
        else partidaRepository.atualizarSnapshotVisitante(partidaId, json);
    }

    private String serializar(RankingSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao serializar snapshot de ranking", e);
        }
    }

    private RankingSnapshot desserializar(String json) {
        try {
            return objectMapper.readValue(json, RankingSnapshot.class);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao desserializar snapshot de ranking", e);
        }
    }

    private int valor(Integer v) { return v == null ? 0 : v; }
}