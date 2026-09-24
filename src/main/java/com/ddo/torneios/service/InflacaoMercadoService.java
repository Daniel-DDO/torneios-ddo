package com.ddo.torneios.service;

import com.ddo.torneios.dto.InflacaoMercadoDTO;
import com.ddo.torneios.exception.RegraNegocioException;
import com.ddo.torneios.model.Clube;
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
import java.util.List;

@Slf4j
@Service
public class InflacaoMercadoService {

    @Autowired private JogadorRepository jogadorRepository;
    @Autowired private ClubeRepository clubeRepository;
    @Autowired private EstadoMercadoRepository estadoMercadoRepository;

    private static final BigDecimal VALOR_PISO_CLUBE = new BigDecimal("40000");

    /**
     * Quanto do crescimento do saldo dos jogadores é "repassado" pro preço BASE
     * dos clubes. 0.5 = metade do crescimento vira inflação. Esse é o número
     * médio do mercado — cada clube depois varia em cima disso (ver
     * calcularFatorIndividual).
     */
    private static final BigDecimal FATOR_REPASSE = new BigDecimal("0.5");

    /** Teto de inflação BASE aplicada de uma vez só, antes da variação por clube */
    private static final BigDecimal INFLACAO_MAXIMA_POR_EXECUCAO = new BigDecimal("0.15");

    /**
     * Quanto o desvio de estrelas de um clube em relação à média amplia ou
     * reduz a inflação DELE especificamente. Ex: 0.6 = um clube com 50% mais
     * estrelas que a média tem sua inflação amplificada em até 30%
     * (0.5 * 0.6), sempre dentro do clamp abaixo.
     */
    private static final BigDecimal SENSIBILIDADE_ESTRELAS = new BigDecimal("0.6");

    /** Nenhum clube recebe menos que 40% da inflação base... */
    private static final BigDecimal FATOR_INDIVIDUAL_MINIMO = new BigDecimal("0.4");
    /** ...nem mais que 160% dela. Garante variação sem exagero. */
    private static final BigDecimal FATOR_INDIVIDUAL_MAXIMO = new BigDecimal("1.6");

    // ---------------------------------------------------------------------
    // SIMULAÇÃO (não persiste nada, não mexe nos clubes nem no baseline)
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public InflacaoMercadoDTO simular() {
        Indicadores indicadores = calcularIndicadores();
        BigDecimal mediaEstrelas = obterMediaEstrelas();

        EstadoMercado estadoAtual = estadoMercadoRepository.findById("GLOBAL").orElse(null);

        if (estadoAtual == null || estadoAtual.getUltimoIndicadorSaldo() == null
                || estadoAtual.getUltimoIndicadorSaldo().compareTo(BigDecimal.ZERO) <= 0) {
            return new InflacaoMercadoDTO(
                    false, "Ainda não existe uma medição anterior salva — a primeira execução real só define o ponto de partida, sem inflacionar nada.",
                    indicadores.media(), indicadores.mediana(), indicadores.indicador(), null, null, null, mediaEstrelas, null, LocalDateTime.now()
            );
        }

        BigDecimal indicadorAnterior = estadoAtual.getUltimoIndicadorSaldo();
        BigDecimal crescimento = calcularCrescimentoPercentual(indicadores.indicador(), indicadorAnterior);

        if (crescimento.compareTo(BigDecimal.ZERO) <= 0) {
            return new InflacaoMercadoDTO(
                    false, "O indicador de saldo não cresceu desde a última medição, então nenhuma inflação seria aplicada.",
                    indicadores.media(), indicadores.mediana(), indicadores.indicador(), indicadorAnterior, crescimento, BigDecimal.ONE, mediaEstrelas, 0, LocalDateTime.now()
            );
        }

        BigDecimal multiplicadorBase = calcularMultiplicadorBase(crescimento);

        return new InflacaoMercadoDTO(
                false, "Simulação: se aplicada agora, os clubes subiriam em média " +
                pct(multiplicadorBase.subtract(BigDecimal.ONE)) + " (variando por clube conforme as estrelas — veja o detalhe por clube).",
                indicadores.media(), indicadores.mediana(), indicadores.indicador(), indicadorAnterior, crescimento, multiplicadorBase, mediaEstrelas, null, LocalDateTime.now()
        );
    }

    // ---------------------------------------------------------------------
    // APLICAÇÃO REAL
    // ---------------------------------------------------------------------

    @Transactional
    public InflacaoMercadoDTO aplicar() {
        Indicadores indicadores = calcularIndicadores();
        BigDecimal mediaEstrelas = obterMediaEstrelas();

        EstadoMercado estadoAtual = estadoMercadoRepository.findById("GLOBAL").orElse(null);

        // Primeira execução: só define o baseline, sem mexer nos clubes.
        if (estadoAtual == null || estadoAtual.getUltimoIndicadorSaldo() == null
                || estadoAtual.getUltimoIndicadorSaldo().compareTo(BigDecimal.ZERO) <= 0) {

            estadoMercadoRepository.save(EstadoMercado.novoEstadoInicial(indicadores.indicador()));

            return new InflacaoMercadoDTO(
                    false, "Ponto de partida do mercado definido. Nenhuma inflação foi aplicada nessa primeira execução — a próxima chamada já compara com esse valor.",
                    indicadores.media(), indicadores.mediana(), indicadores.indicador(), null, null, null, mediaEstrelas, 0, LocalDateTime.now()
            );
        }

        BigDecimal indicadorAnterior = estadoAtual.getUltimoIndicadorSaldo();
        BigDecimal crescimento = calcularCrescimentoPercentual(indicadores.indicador(), indicadorAnterior);

        // O baseline sempre é atualizado pro indicador atual, suba ou desça.
        estadoAtual.setUltimoIndicadorSaldo(indicadores.indicador());
        estadoAtual.setDataUltimaAplicacao(LocalDateTime.now());

        if (crescimento.compareTo(BigDecimal.ZERO) <= 0) {
            estadoAtual.setUltimoMultiplicadorAplicado(BigDecimal.ONE);
            estadoMercadoRepository.save(estadoAtual);

            return new InflacaoMercadoDTO(
                    false, "O indicador de saldo não cresceu desde a última medição — nenhuma inflação foi aplicada.",
                    indicadores.media(), indicadores.mediana(), indicadores.indicador(), indicadorAnterior, crescimento, BigDecimal.ONE, mediaEstrelas, 0, LocalDateTime.now()
            );
        }

        BigDecimal multiplicadorBase = calcularMultiplicadorBase(crescimento);
        BigDecimal inflacaoBase = multiplicadorBase.subtract(BigDecimal.ONE);

        // Aqui não dá pra usar um UPDATE em massa de uma linha só, porque cada
        // clube tem um multiplicador diferente (depende das estrelas dele).
        // Como Clube não tem coleções pesadas carregadas por padrão (conquistas
        // é LAZY), buscar tudo e recalcular em memória é leve o suficiente.
        List<Clube> clubes = clubeRepository.findAll().stream()
                .filter(c -> c.getValorAvaliado() != null)
                .toList();

        for (Clube clube : clubes) {
            BigDecimal fatorIndividual = calcularFatorIndividual(clube.getEstrelas(), mediaEstrelas);
            BigDecimal multiplicadorClube = BigDecimal.ONE.add(inflacaoBase.multiply(fatorIndividual));

            BigDecimal novoValorAvaliado = clube.getValorAvaliado()
                    .multiply(multiplicadorClube)
                    .max(VALOR_PISO_CLUBE)
                    .setScale(2, RoundingMode.HALF_EVEN);

            clube.setValorAvaliado(novoValorAvaliado);
            clube.atualizarLanceMinimo();
        }

        clubeRepository.saveAll(clubes);

        estadoAtual.setUltimoMultiplicadorAplicado(multiplicadorBase);
        estadoMercadoRepository.save(estadoAtual);

        log.info("[InflacaoMercadoService] Inflação aplicada: crescimento={}, multiplicadorBase={}, clubes atualizados={}",
                crescimento, multiplicadorBase, clubes.size());

        return new InflacaoMercadoDTO(
                true, "Inflação aplicada: os clubes subiram em média " + pct(inflacaoBase) + " (variando por clube conforme as estrelas).",
                indicadores.media(), indicadores.mediana(), indicadores.indicador(), indicadorAnterior, crescimento, multiplicadorBase, mediaEstrelas, clubes.size(), LocalDateTime.now()
        );
    }

    // ---------------------------------------------------------------------
    // CÁLCULO — indicador de dinheiro em circulação
    // ---------------------------------------------------------------------

    private record Indicadores(BigDecimal media, BigDecimal mediana, BigDecimal indicador) {}

    private Indicadores calcularIndicadores() {
        List<BigDecimal> saldos = jogadorRepository.buscarSaldosDeContasReivindicadas();

        if (saldos == null || saldos.isEmpty()) {
            throw new RegraNegocioException("Não há jogadores com conta reivindicada e saldo pra calcular a inflação do mercado.");
        }

        BigDecimal media = calcularMedia(saldos);
        BigDecimal mediana = calcularMediana(saldos);

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

    private BigDecimal calcularMultiplicadorBase(BigDecimal crescimentoPercentual) {
        BigDecimal inflacaoRepassada = crescimentoPercentual.multiply(FATOR_REPASSE);
        BigDecimal inflacaoLimitada = inflacaoRepassada.min(INFLACAO_MAXIMA_POR_EXECUCAO);
        return BigDecimal.ONE.add(inflacaoLimitada).setScale(4, RoundingMode.HALF_EVEN);
    }

    // ---------------------------------------------------------------------
    // CÁLCULO — variação por clube (critério: estrelas em relação à média)
    // ---------------------------------------------------------------------

    private BigDecimal obterMediaEstrelas() {
        BigDecimal media = clubeRepository.buscarMediaEstrelas();
        return media != null ? media.setScale(2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO;
    }

    /**
     * Clubes com estrelas acima da média (mais cobiçados) sentem MAIS a
     * inflação; clubes abaixo da média sentem MENOS. Nunca inverte o sinal
     * da inflação base — só varia a intensidade, sempre dentro do clamp
     * [FATOR_INDIVIDUAL_MINIMO, FATOR_INDIVIDUAL_MAXIMO].
     */
    private BigDecimal calcularFatorIndividual(BigDecimal estrelasClube, BigDecimal mediaEstrelas) {
        if (estrelasClube == null || mediaEstrelas == null || mediaEstrelas.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }

        BigDecimal desvio = estrelasClube.subtract(mediaEstrelas)
                .divide(mediaEstrelas, 6, RoundingMode.HALF_EVEN);

        BigDecimal fator = BigDecimal.ONE.add(desvio.multiply(SENSIBILIDADE_ESTRELAS));

        if (fator.compareTo(FATOR_INDIVIDUAL_MINIMO) < 0) return FATOR_INDIVIDUAL_MINIMO;
        if (fator.compareTo(FATOR_INDIVIDUAL_MAXIMO) > 0) return FATOR_INDIVIDUAL_MAXIMO;
        return fator;
    }

    private String pct(BigDecimal fracao) {
        return fracao.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_EVEN) + "%";
    }
}