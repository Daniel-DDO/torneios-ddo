package com.ddo.torneios.service;

import com.ddo.torneios.dto.EstatisticaTemporadaDTO;
import com.ddo.torneios.dto.PremioTemporadaDTO;
import com.ddo.torneios.model.CategoriaPremio;
import com.ddo.torneios.model.PremioTemporada;
import com.ddo.torneios.model.Temporada;
import com.ddo.torneios.repository.JogadorClubeRepository;
import com.ddo.torneios.repository.JogadorRepository;
import com.ddo.torneios.repository.PremioTemporadaRepository;
import com.ddo.torneios.repository.TemporadaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PremioTemporadaService {

    private static final double PESO_CARTAO_VERMELHO = 3.0;

    @Autowired private JogadorClubeRepository jogadorClubeRepository;
    @Autowired private PremioTemporadaRepository premioTemporadaRepository;
    @Autowired private JogadorRepository jogadorRepository;
    @Autowired private TemporadaRepository temporadaRepository;

    private record CandidatoIndice(EstatisticaTemporadaDTO stat, double indice) {}

    // Apuração oficial (persiste)

    @Transactional
    public List<PremioTemporadaDTO> apurarPremiosDaTemporada(String temporadaId) {
        List<PremioTemporadaDTO> calculados = apurarSemPersistir(temporadaId);
        List<PremioTemporadaDTO> resultado = new ArrayList<>();
        for (PremioTemporadaDTO dto : calculados) {
            resultado.add(salvarOuAtualizar(temporadaId, dto.categoria(), dto.jogadorId(),
                    dto.jogadorNome(), dto.valorEstatistica()));
        }
        return resultado;
    }

    public List<PremioTemporadaDTO> obterPremiosDaTemporada(String temporadaId) {
        return premioTemporadaRepository.buscarPremiosDaTemporada(temporadaId);
    }

    public List<PremioTemporadaDTO> obterPremiosDoJogador(String jogadorId) {
        return premioTemporadaRepository.buscarPremiosDoJogador(jogadorId);
    }

    // ---------- Preview em tempo real (não persiste) ----------

    /**
     * Mostra quem seriam os campeões AGORA, sem salvar nada.
     * Só calcula se a temporada estiver em andamento (data atual entre início e fim).
     * Se a temporada já terminou, não perde tempo calculando: devolve lista vazia.
     */
    public List<PremioTemporadaDTO> previewCampeoesDaTemporada(String temporadaId) {
        Temporada temporada = temporadaRepository.findById(temporadaId)
                .orElseThrow(() -> new IllegalArgumentException("Temporada não encontrada: " + temporadaId));

        if (!isTemporadaAtual(temporada)) {
            return List.of();
        }

        return apurarSemPersistir(temporadaId);
    }

    private boolean isTemporadaAtual(Temporada temporada) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = temporada.getDataInicio();
        LocalDate fim = temporada.getDataFim();

        if (inicio != null && hoje.isBefore(inicio)) return false;
        if (fim != null && hoje.isAfter(fim)) return false;
        return true;
    }

    // Cálculo puro, sem tocar no banco de PremioTemporada

    private List<PremioTemporadaDTO> apurarSemPersistir(String temporadaId) {
        List<EstatisticaTemporadaDTO> stats = jogadorClubeRepository.buscarEstatisticasTemporada(temporadaId);
        if (stats.isEmpty()) return List.of();

        List<PremioTemporadaDTO> resultado = new ArrayList<>();

        stats.stream()
                .filter(s -> valor(s.partidasJogadas()) > 0)
                .max(Comparator.comparingInt(s -> valor(s.golsMarcados())))
                .ifPresent(s -> resultado.add(dto(CategoriaPremio.ARTILHEIRO, s.jogadorId(), s.jogadorNome(),
                        BigDecimal.valueOf(valor(s.golsMarcados())))));

        calcularFairPlay(stats)
                .ifPresent(c -> resultado.add(dto(CategoriaPremio.FAIR_PLAY, c.stat().jogadorId(),
                        c.stat().jogadorNome(), arredondar(c.indice()))));

        calcularMelhorDefesa(stats)
                .ifPresent(c -> resultado.add(dto(CategoriaPremio.MELHOR_DEFESA, c.stat().jogadorId(),
                        c.stat().jogadorNome(), arredondar(c.indice()))));

        stats.stream()
                .filter(s -> s.pontosCoeficiente() != null)
                .max(Comparator.comparing(EstatisticaTemporadaDTO::pontosCoeficiente))
                .ifPresent(s -> resultado.add(dto(CategoriaPremio.MELHOR_JOGADOR, s.jogadorId(),
                        s.jogadorNome(), s.pontosCoeficiente())));

        stats.stream()
                .filter(s -> s.rankPoints() != null)
                .max(Comparator.comparingInt(EstatisticaTemporadaDTO::rankPoints))
                .ifPresent(s -> resultado.add(dto(CategoriaPremio.MELHOR_RANKING, s.jogadorId(),
                        s.jogadorNome(), BigDecimal.valueOf(valor(s.rankPoints())))));

        return resultado;
    }

    private PremioTemporadaDTO dto(CategoriaPremio categoria, String jogadorId, String jogadorNome, BigDecimal valor) {
        // id e dataApuracao nulos = indica que é preview, ainda não foi salvo
        return new PremioTemporadaDTO(null, categoria, jogadorId, jogadorNome, valor, null);
    }

    // Fair Play

    private Optional<CandidatoIndice> calcularFairPlay(List<EstatisticaTemporadaDTO> stats) {
        return stats.stream()
                .filter(s -> valor(s.partidasJogadas()) > 0)
                .map(s -> {
                    double indice = (valor(s.cartoesAmarelos()) + valor(s.cartoesVermelhos()) * PESO_CARTAO_VERMELHO)
                            / (double) valor(s.partidasJogadas());
                    return new CandidatoIndice(s, indice);
                })
                .min(Comparator.comparingDouble(CandidatoIndice::indice));
    }

    // Melhor defesa (média ponderada, tipo "rating bayesiano")

    private Optional<CandidatoIndice> calcularMelhorDefesa(List<EstatisticaTemporadaDTO> stats) {
        List<EstatisticaTemporadaDTO> comPartidas = stats.stream()
                .filter(s -> valor(s.partidasJogadas()) > 0)
                .toList();
        if (comPartidas.isEmpty()) return Optional.empty();

        int somaPartidas = comPartidas.stream().mapToInt(s -> valor(s.partidasJogadas())).sum();
        int somaGolsSofridos = comPartidas.stream().mapToInt(s -> valor(s.golsSofridos())).sum();

        double mediaGolsPorPartidaGeral = somaGolsSofridos / (double) somaPartidas;
        double mediaPartidasPorJogador = somaPartidas / (double) comPartidas.size();

        return comPartidas.stream()
                .map(s -> {
                    double partidas = valor(s.partidasJogadas());
                    double mediaJogador = valor(s.golsSofridos()) / partidas;

                    double indice = (partidas / (partidas + mediaPartidasPorJogador)) * mediaJogador
                            + (mediaPartidasPorJogador / (partidas + mediaPartidasPorJogador)) * mediaGolsPorPartidaGeral;

                    return new CandidatoIndice(s, indice);
                })
                .min(Comparator.comparingDouble(CandidatoIndice::indice));
    }

    // upsert leve, sem carregar entidades

    private PremioTemporadaDTO salvarOuAtualizar(String temporadaId, CategoriaPremio categoria,
                                                 String jogadorId, String jogadorNome, BigDecimal valor) {
        PremioTemporada premio = premioTemporadaRepository.findByTemporadaIdAndCategoria(temporadaId, categoria)
                .orElseGet(PremioTemporada::new);

        if (premio.getId() == null) {
            premio.setTemporada(temporadaRepository.getReferenceById(temporadaId));
            premio.setCategoria(categoria);
        }
        premio.setJogador(jogadorRepository.getReferenceById(jogadorId));
        premio.setJogadorNomeSnapshot(jogadorNome);
        premio.setValorEstatistica(valor);

        PremioTemporada salvo = premioTemporadaRepository.save(premio);
        return new PremioTemporadaDTO(salvo.getId(), categoria, jogadorId, jogadorNome, valor, salvo.getDataApuracao());
    }

    @Transactional
    public void removerApuracao(String temporadaId) {
        premioTemporadaRepository.deleteByTemporadaId(temporadaId);
    }

    private BigDecimal arredondar(double v) {
        return BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP);
    }

    private int valor(Integer v) { return v == null ? 0 : v; }
}