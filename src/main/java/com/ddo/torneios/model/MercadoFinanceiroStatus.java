package com.ddo.torneios.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "mercado_financeiro_status")
public class MercadoFinanceiroStatus {

    @Id
    private String id = "SINGLETON";

    private LocalDateTime ultimaExecucao;
    private LocalDateTime ultimaExecucaoComSucesso;
    private BigDecimal ultimaVariacaoUsdAplicada;
    private BigDecimal ultimaCotacaoUsd;
    private Integer clubesAtualizadosUltimaExecucao;
    private boolean ultimaExecucaoComErro;
    private String ultimoErro;

    private LocalDateTime ultimaExecucaoIpca;
    private BigDecimal ultimaVariacaoIpcaAplicada;
}