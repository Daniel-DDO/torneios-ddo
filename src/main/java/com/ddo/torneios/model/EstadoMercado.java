package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Linha única (id fixo "GLOBAL") que guarda o último indicador de "dinheiro
 * em circulação" usado pra calcular a inflação dos clubes. Comparando o
 * indicador atual com esse valor salvo, dá pra saber se o saldo dos
 * jogadores cresceu (e quanto) desde a última vez que o mercado foi ajustado.
 */
@Getter
@Setter
@Entity
@Table(name = "estado_mercado")
public class EstadoMercado {

    @Id
    private String id = "GLOBAL";

    /** Combinação de média + mediana do saldoVirtual dos jogadores na última execução */
    private BigDecimal ultimoIndicadorSaldo;

    private LocalDateTime dataUltimaAplicacao;

    /** Último multiplicador de inflação efetivamente aplicado aos clubes (histórico/log) */
    private BigDecimal ultimoMultiplicadorAplicado;

    public EstadoMercado() {}

    public static EstadoMercado novoEstadoInicial(BigDecimal indicadorAtual) {
        EstadoMercado estado = new EstadoMercado();
        estado.setId("GLOBAL");
        estado.setUltimoIndicadorSaldo(indicadorAtual);
        estado.setDataUltimaAplicacao(LocalDateTime.now());
        estado.setUltimoMultiplicadorAplicado(BigDecimal.ONE);
        return estado;
    }
}