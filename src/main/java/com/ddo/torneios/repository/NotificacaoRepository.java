package com.ddo.torneios.repository;

import com.ddo.torneios.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, String> {
    void deleteByLidaTrueAndDataLeituraBefore(LocalDateTime dataLimiteLeitura);
    List<Notificacao> findByJogadorIdOrderByDataCriacaoDesc(String jogadorId);
    int deleteByLidaTrue();
    int deleteByDataCriacaoBefore(LocalDateTime dataLimite);
}