package com.ddo.torneios.service;

import com.ddo.torneios.dto.ParametrosEconomicosDTO;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.JogadorClubeRepository;
import com.ddo.torneios.repository.JogadorRepository;
import com.ddo.torneios.repository.PartidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class EconomiaService {

    private final JogadorRepository jogadorRepository;
    private final JogadorClubeRepository jogadorClubeRepository;
    private final PartidaRepository partidaRepository;

    private static final BigDecimal COTA_TV_FIXA = new BigDecimal("5000");
    private static final BigDecimal VALOR_POR_ESTRELA_BILHETERIA = new BigDecimal("2000");
    private static final BigDecimal PREMIO_VITORIA = new BigDecimal("10000");
    private static final BigDecimal PREMIO_EMPATE = new BigDecimal("4000");
    private static final BigDecimal CUSTO_BASE_ESTRELA = new BigDecimal("500");
    private static final BigDecimal BONUS_ZEBRA_POR_ESTRELA = new BigDecimal("3000");
    private static final BigDecimal FATOR_PUNICAO_GOLEADA = new BigDecimal("0.40");

    //piso mínimo
    private static final int VALOR_MINIMO_COMPETICAO = 15;

    @Transactional
    public void processarEconomiaPartida(Partida partida) {
        if (!partida.isRealizada()) {
            throw new IllegalArgumentException("A partida precisa estar realizada para processar economia.");
        }

        Competicao competicao = partida.getFase().getTorneio().getCompeticao();

        BigDecimal fatorCompeticao = BigDecimal.ONE;

        if (competicao != null && competicao.getValor() != null) {
            int valorEfetivo = competicao.getValor();

            if (valorEfetivo < VALOR_MINIMO_COMPETICAO) {
                valorEfetivo = VALOR_MINIMO_COMPETICAO;
            }

            fatorCompeticao = new BigDecimal(valorEfetivo)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
        }

        //mandante
        BigDecimal lucroMandante = calcularEAtualizar(partida.getMandante(), partida.getVisitante(),
                partida.getGolsMandante(), partida.getGolsVisitante(), fatorCompeticao);

        //visitante
        BigDecimal lucroVisitante = calcularEAtualizar(partida.getVisitante(), partida.getMandante(),
                partida.getGolsVisitante(), partida.getGolsMandante(), fatorCompeticao);

        //atualiza a receita na partida e salva
        partida.setReceitaMandante(lucroMandante);
        partida.setReceitaVisitante(lucroVisitante);
        partidaRepository.save(partida);
    }

    private BigDecimal calcularEAtualizar(JogadorClube meuTime, JogadorClube adversario,
                                          Integer golsPro, Integer golsContra,
                                          BigDecimal fatorCompeticao) {

        BigDecimal minhasEstrelas = meuTime.getClube().getEstrelas();
        BigDecimal estrelasAdversario = adversario.getClube().getEstrelas();

        if (minhasEstrelas == null) minhasEstrelas = BigDecimal.ONE;
        if (estrelasAdversario == null) estrelasAdversario = BigDecimal.ONE;

        TipoResultado resultado = getResultado(golsPro, golsContra);

        BigDecimal custoOperacional = minhasEstrelas.multiply(minhasEstrelas)
                .multiply(CUSTO_BASE_ESTRELA);

        BigDecimal somaEstrelas = minhasEstrelas.add(estrelasAdversario);
        BigDecimal receitaBilheteria = somaEstrelas.multiply(VALOR_POR_ESTRELA_BILHETERIA);

        int diferencaGols = golsContra - golsPro;
        if (resultado == TipoResultado.DERROTA && diferencaGols >= 4) {
            BigDecimal valorPerdidoGoleada = receitaBilheteria.multiply(FATOR_PUNICAO_GOLEADA);
            receitaBilheteria = receitaBilheteria.subtract(valorPerdidoGoleada);
        }

        BigDecimal receitaPremiacao = BigDecimal.ZERO;
        if (resultado == TipoResultado.VITORIA) receitaPremiacao = PREMIO_VITORIA;
        else if (resultado == TipoResultado.EMPATE) receitaPremiacao = PREMIO_EMPATE;

        BigDecimal bonusZebra = BigDecimal.ZERO;
        if (resultado != TipoResultado.DERROTA && estrelasAdversario.compareTo(minhasEstrelas) > 0) {
            BigDecimal diferencaEstrelas = estrelasAdversario.subtract(minhasEstrelas);
            bonusZebra = diferencaEstrelas.multiply(BONUS_ZEBRA_POR_ESTRELA);
        }

        BigDecimal receitaTotal = COTA_TV_FIXA
                .add(receitaBilheteria)
                .add(receitaPremiacao)
                .add(bonusZebra);

        BigDecimal lucroLiquido = receitaTotal.subtract(custoOperacional);

        //aplica o fator da competição (mínimo de 0.15)
        lucroLiquido = lucroLiquido.multiply(fatorCompeticao)
                .setScale(2, RoundingMode.HALF_EVEN);

        //atualizar JogadorClube
        BigDecimal balancoAtual = meuTime.getBalancoFinanceiro() != null ?
                meuTime.getBalancoFinanceiro() : BigDecimal.ZERO;
        meuTime.setBalancoFinanceiro(balancoAtual.add(lucroLiquido));
        jogadorClubeRepository.save(meuTime);

        //atualizar Jogador
        Jogador jogador = meuTime.getJogador();
        BigDecimal saldoAtual = jogador.getSaldoVirtual() != null ?
                jogador.getSaldoVirtual() : BigDecimal.ZERO;

        jogador.setSaldoVirtual(saldoAtual.add(lucroLiquido));
        jogadorRepository.save(jogador);

        return lucroLiquido;
    }

    private TipoResultado getResultado(int golsPro, int golsContra) {
        if (golsPro > golsContra) return TipoResultado.VITORIA;
        if (golsPro == golsContra) return TipoResultado.EMPATE;
        return TipoResultado.DERROTA;
    }

    private enum TipoResultado {
        VITORIA, EMPATE, DERROTA
    }

    @Transactional
    public void estornarEconomiaPartida(Partida partida) {
        BigDecimal receitaMandante = partida.getReceitaMandante() != null ?
                partida.getReceitaMandante() : BigDecimal.ZERO;

        BigDecimal receitaVisitante = partida.getReceitaVisitante() != null ?
                partida.getReceitaVisitante() : BigDecimal.ZERO;

        debitarSaldos(partida.getMandante(), receitaMandante);
        debitarSaldos(partida.getVisitante(), receitaVisitante);

        partida.setReceitaMandante(null);
        partida.setReceitaVisitante(null);

        partidaRepository.save(partida);
    }

    private void debitarSaldos(JogadorClube jc, BigDecimal valorParaRemover) {
        if (valorParaRemover.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal balancoAtual = jc.getBalancoFinanceiro() != null ?
                jc.getBalancoFinanceiro() : BigDecimal.ZERO;

        jc.setBalancoFinanceiro(balancoAtual.subtract(valorParaRemover));
        jogadorClubeRepository.save(jc);

        Jogador jogador = jc.getJogador();
        BigDecimal saldoAtual = jogador.getSaldoVirtual() != null ?
                jogador.getSaldoVirtual() : BigDecimal.ZERO;

        jogador.setSaldoVirtual(saldoAtual.subtract(valorParaRemover));
        jogadorRepository.save(jogador);
    }

    /**
     * Portal da Transparência: Retorna todas as variáveis econômicas
     * usadas no cálculo das partidas.
     */
    public ParametrosEconomicosDTO getParametrosTransparencia() {
        return ParametrosEconomicosDTO.builder()
                .cotaTvFixa(COTA_TV_FIXA)
                .valorPorEstrelaBilheteria(VALOR_POR_ESTRELA_BILHETERIA)
                .premioVitoria(PREMIO_VITORIA)
                .premioEmpate(PREMIO_EMPATE)
                .custoBaseEstrela(CUSTO_BASE_ESTRELA)
                .bonusZebraPorEstrela(BONUS_ZEBRA_POR_ESTRELA)
                .fatorPunicaoGoleada(FATOR_PUNICAO_GOLEADA)
                .percentualMinimoCompeticao(VALOR_MINIMO_COMPETICAO)
                .explicacaoFatorCompeticao(
                        "O valor final é multiplicado pelo peso da competição (0-100%). " +
                                "Se o peso for menor que " + VALOR_MINIMO_COMPETICAO + "%, será considerado " + VALOR_MINIMO_COMPETICAO + "%."
                )
                .build();
    }
}