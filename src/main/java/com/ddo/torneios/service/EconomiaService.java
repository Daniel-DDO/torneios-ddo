package com.ddo.torneios.service;

import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.JogadorClubeRepository;
import com.ddo.torneios.repository.JogadorRepository;
import com.ddo.torneios.repository.PartidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
    private static final BigDecimal FATOR_PUNICAO_GOLEADA = new BigDecimal("0.40"); //40%

    @Transactional
    public void processarEconomiaPartida(Partida partida) {
        if (!partida.isRealizada()) {
            throw new IllegalArgumentException("A partida precisa estar realizada para processar economia.");
        }

        //mandante
        BigDecimal lucroMandante = calcularEAtualizar(partida.getMandante(), partida.getVisitante(),
                partida.getGolsMandante(), partida.getGolsVisitante());

        //visitante
        BigDecimal lucroVisitante = calcularEAtualizar(partida.getVisitante(), partida.getMandante(),
                partida.getGolsVisitante(), partida.getGolsMandante());

        //atualiza a receita na partida e salva
        partida.setReceitaMandante(lucroMandante);
        partida.setReceitaVisitante(lucroVisitante);
        partidaRepository.save(partida);
    }

    private BigDecimal calcularEAtualizar(JogadorClube meuTime, JogadorClube adversario,
                                          Integer golsPro, Integer golsContra) {

        BigDecimal minhasEstrelas = meuTime.getClube().getEstrelas();
        BigDecimal estrelasAdversario = adversario.getClube().getEstrelas();

        if (minhasEstrelas == null) minhasEstrelas = BigDecimal.ONE;
        if (estrelasAdversario == null) estrelasAdversario = BigDecimal.ONE;

        //determinar resultado
        TipoResultado resultado = getResultado(golsPro, golsContra);

        //custo operacional: (Estrelas * Estrelas) * 500
        BigDecimal custoOperacional = minhasEstrelas.multiply(minhasEstrelas)
                .multiply(CUSTO_BASE_ESTRELA);

        //bilheteria base: (minhas + adversário) * 2000
        BigDecimal somaEstrelas = minhasEstrelas.add(estrelasAdversario);
        BigDecimal receitaBilheteria = somaEstrelas.multiply(VALOR_POR_ESTRELA_BILHETERIA);

        //verifica goleada (derrota por 4 ou mais gols)
        int diferencaGols = golsContra - golsPro;
        BigDecimal valorPerdidoGoleada = BigDecimal.ZERO;

        if (resultado == TipoResultado.DERROTA && diferencaGols >= 4) {
            valorPerdidoGoleada = receitaBilheteria.multiply(FATOR_PUNICAO_GOLEADA);
            receitaBilheteria = receitaBilheteria.subtract(valorPerdidoGoleada);
        }

        //premiação
        BigDecimal receitaPremiacao = BigDecimal.ZERO;
        if (resultado == TipoResultado.VITORIA) receitaPremiacao = PREMIO_VITORIA;
        else if (resultado == TipoResultado.EMPATE) receitaPremiacao = PREMIO_EMPATE;

        //bônus zebra (não perdeu e adversário é mais forte)
        BigDecimal bonusZebra = BigDecimal.ZERO;
        if (resultado != TipoResultado.DERROTA && estrelasAdversario.compareTo(minhasEstrelas) > 0) {
            BigDecimal diferencaEstrelas = estrelasAdversario.subtract(minhasEstrelas);
            bonusZebra = diferencaEstrelas.multiply(BONUS_ZEBRA_POR_ESTRELA);
        }

        //cálculo final
        BigDecimal receitaTotal = COTA_TV_FIXA
                .add(receitaBilheteria)
                .add(receitaPremiacao)
                .add(bonusZebra);

        BigDecimal lucroLiquido = receitaTotal.subtract(custoOperacional);

        //atualizar JogadorClube (balanço da temporada)
        BigDecimal balancoAtual = meuTime.getBalancoFinanceiro() != null ?
                meuTime.getBalancoFinanceiro() : BigDecimal.ZERO;
        meuTime.setBalancoFinanceiro(balancoAtual.add(lucroLiquido));
        jogadorClubeRepository.save(meuTime);

        //atualizar Jogador (carteira global)
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
}