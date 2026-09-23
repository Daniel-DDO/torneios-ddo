package com.ddo.torneios.service;

import com.ddo.torneios.dto.InflacaoMercadoDTO;
import com.ddo.torneios.exception.RegraNegocioException;
import com.ddo.torneios.model.EstadoMercado;
import com.ddo.torneios.repository.ClubeRepository;
import com.ddo.torneios.repository.EstadoMercadoRepository;
import com.ddo.torneios.repository.JogadorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class InflacaoMercadoService {

    @Autowired private JogadorRepository jogadorRepository;
    @Autowired private ClubeRepository clubeRepository;
    @Autowired private EstadoMercadoRepository estadoMercadoRepository;

    /** Piso mínimo de valorAvaliado de um clube, mesmo depois de qualquer ajuste (mesmo valor já usado no multiplicar manual) */
    private static final BigDecimal VALOR_PISO_CLUBE = new BigDecimal("40000");

    /**
     * Quanto do crescimento do saldo dos jogadores é "repassado" pro preço dos
     * clubes. 0.5 = metade do crescimento vira inflação. Se todo o dinheiro
     * dobrasse (crescimento de 100%), os clubes subiriam só 50%.
     */
    private static final BigDecimal FATOR_REPASSE = new BigDecimal("0.5");

    /** Teto de inflação aplicada de uma vez só, pra nenhuma execução dar um salto absurdo */
    private static final BigDecimal INFLACAO_MAXIMA_POR_EXECUCAO = new BigDecimal("0.15");

    // ---------------------------------------------------------------------
    // SIMULAÇÃO (não persiste nada, não mexe nos clubes nem no baseline)
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public InflacaoMercadoDTO simular() {
        Indicadores indicadores = calcularIndicadores();

        EstadoMercado estadoAtual = estadoMercadoRepository.findById("GLOBAL").orElse(null);

        if (estadoAtual == null || estadoAtual.getUltimoIndicadorSaldo() == null
                || estadoAtual.getUltimoIndicadorSaldo().compareTo(BigDecimal.ZERO) <= 0) {
            return new InflacaoMercadoDTO(
                    false, "Ainda não existe uma medição anterior salva — a primeira execução real só define o ponto de partida, sem inflacionar nada.",
                    indicadores.media(), indicadores.mediana(), indicadores.indicador(), null, null, null, null, LocalDateTime.now()
            );
        }

        BigDecimal indicadorAnterior = estadoAtual.getUltimoIndicadorSaldo();
        BigDecimal crescimento = calcularCrescimentoPercentual(indicadores.indicador(), indicadorAnterior);

        if (crescimento.compareTo(BigDecimal.ZERO) <= 0) {
            return new InflacaoMercadoDTO(
                    false, "O indicador de saldo não cresceu desde a última medição, então nenhuma inflação seria aplicada.",
                    indicadores.media(), indicadores.mediana(), indicadores.indicador(), indicadorAnterior, crescimento, BigDecimal.ONE, 0, LocalDateTime.now()
            );
        }

        BigDecimal multiplicador = calcularMultiplicador(crescimento);

        return new InflacaoMercadoDTO(
                false, "Simulação: se aplicada agora, os valores dos clubes subiriam " +
                pct(multiplicador.subtract(BigDecimal.ONE)) + ".",
                indicadores.media(), indicadores.mediana(), indicadores.indicador(), indicadorAnterior, crescimento, multiplicador, null, LocalDateTime.now()
        );
    }

    // ---------------------------------------------------------------------
    // APLICAÇÃO REAL
    // ---------------------------------------------------------------------

    @Transactional
    public InflacaoMercadoDTO aplicar() {
        Indicadores indicadores = calcularIndicadores();

        EstadoMercado estadoAtual = estadoMercadoRepository.findById("GLOBAL").orElse(null);

        // Primeira execução: só define o baseline, sem mexer nos clubes.
        if (estadoAtual == null || estadoAtual.getUltimoIndicadorSaldo() == null
                || estadoAtual.getUltimoIndicadorSaldo().compareTo(BigDecimal.ZERO) <= 0) {

            estadoMercadoRepository.save(EstadoMercado.novoEstadoInicial(indicadores.indicador()));

            return new InflacaoMercadoDTO(
                    false, "Ponto de partida do mercado definido. Nenhuma inflação foi aplicada nessa primeira execução — a próxima chamada já compara com esse valor.",
                    indicadores.media(), indicadores.mediana(), indicadores.indicador(), null, null, null, 0, LocalDateTime.now()
            );
        }

        BigDecimal indicadorAnterior = estadoAtual.getUltimoIndicadorSaldo();
        BigDecimal crescimento = calcularCrescimentoPercentual(indicadores.indicador(), indicadorAnterior);

        // O baseline sempre é atualizado pro indicador atual, suba ou desça,
        // pra próxima comparação refletir o nível real de dinheiro em jogo.
        estadoAtual.setUltimoIndicadorSaldo(indicadores.indicador());
        estadoAtual.setDataUltimaAplicacao(LocalDateTime.now());

        if (crescimento.compareTo(BigDecimal.ZERO) <= 0) {
            estadoAtual.setUltimoMultiplicadorAplicado(BigDecimal.ONE);
            estadoMercadoRepository.save(estadoAtual);

            return new InflacaoMercadoDTO(
                    false, "O indicador de saldo não cresceu desde a última medição — nenhuma inflação foi aplicada.",
                    indicadores.media(), indicadores.mediana(), indicadores.indicador(), indicadorAnterior, crescimento, BigDecimal.ONE, 0, LocalDateTime.now()
            );
        }

        BigDecimal multiplicador = calcularMultiplicador(crescimento);

        int clubesAtualizados = clubeRepository.aplicarFatorInflacao(multiplicador, VALOR_PISO_CLUBE);

        estadoAtual.setUltimoMultiplicadorAplicado(multiplicador);
        estadoMercadoRepository.save(estadoAtual);

        log.info("[InflacaoMercadoService] Inflação aplicada: crescimento={}, multiplicador={}, clubes atualizados={}",
                crescimento, multiplicador, clubesAtualizados);

        return new InflacaoMercadoDTO(
                true, "Inflação aplicada: os valores dos clubes subiram " + pct(multiplicador.subtract(BigDecimal.ONE)) + ".",
                indicadores.media(), indicadores.mediana(), indicadores.indicador(), indicadorAnterior, crescimento, multiplicador, clubesAtualizados, LocalDateTime.now()
        );
    }

    // ---------------------------------------------------------------------
    // CÁLCULO
    // ---------------------------------------------------------------------

    private record Indicadores(BigDecimal media, BigDecimal mediana, BigDecimal indicador) {}

    private Indicadores calcularIndicadores() {
        List<BigDecimal> saldos = jogadorRepository.buscarSaldosDeContasReivindicadas();

        if (saldos == null || saldos.isEmpty()) {
            throw new RegraNegocioException("Não há jogadores com conta reivindicada e saldo pra calcular a inflação do mercado.");
        }

        BigDecimal media = calcularMedia(saldos);
        BigDecimal mediana = calcularMediana(saldos);

        // Combina média (sensível a outliers, "baleias") e mediana (mais realista pro jogador comum),
        // pra um jogador rico sozinho não inflacionar o mercado inteiro sozinho.
        BigDecimal indicador = media.add(mediana).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_EVEN);

        return new Indicadores(media, mediana, indicador);
    }

    private BigDecimal calcularMedia(List<BigDecimal> valores) {
        BigDecimal soma = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return soma.divide(BigDecimal.valueOf(valores.size()), 2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularMediana(List<BigDecimal> valores) {
        List<BigDecimal> ordenado = valores.stream().sorted().toList();
        int n = ordenado.size();

        if (n % 2 == 1) {
            return ordenado.get(n / 2);
        }

        BigDecimal a = ordenado.get(n / 2 - 1);
        BigDecimal b = ordenado.get(n / 2);
        return a.add(b).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularCrescimentoPercentual(BigDecimal indicadorAtual, BigDecimal indicadorAnterior) {
        if (indicadorAnterior.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return indicadorAtual.subtract(indicadorAnterior)
                .divide(indicadorAnterior, 6, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularMultiplicador(BigDecimal crescimentoPercentual) {
        BigDecimal inflacaoRepassada = crescimentoPercentual.multiply(FATOR_REPASSE);
        BigDecimal inflacaoLimitada = inflacaoRepassada.min(INFLACAO_MAXIMA_POR_EXECUCAO);
        return BigDecimal.ONE.add(inflacaoLimitada).setScale(4, RoundingMode.HALF_EVEN);
    }

    private String pct(BigDecimal fracao) {
        return fracao.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_EVEN) + "%";
    }
}