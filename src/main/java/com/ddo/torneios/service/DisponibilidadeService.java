package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.exception.RegraNegocioException;
import com.ddo.torneios.model.DisponibilidadeJogador;
import com.ddo.torneios.model.ExigenciaDisponibilidade;
import com.ddo.torneios.model.ObservacaoDisponibilidade;
import com.ddo.torneios.repository.DisponibilidadeJogadorRepository;
import com.ddo.torneios.repository.ObservacaoDisponibilidadeRepository;
import com.ddo.torneios.util.DisponibilidadeMascara;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class DisponibilidadeService {

    @Autowired private DisponibilidadeJogadorRepository disponibilidadeRepository;
    @Autowired private ObservacaoDisponibilidadeRepository observacaoRepository;

    /** Tudo que o servidor grava de data/hora é horário de Brasília. */
    private static final ZoneId ZONA_BRASIL = ZoneId.of("America/Sao_Paulo");

    /** Tempo mínimo entre duas alterações da GRADE (contado da última alteração). Observações não têm limite. */
    private static final Duration JANELA_ALTERACAO_GRADE = Duration.ofHours(24);

    private static final int TAMANHO_MAXIMO_PAGINA = 50;
    private static final int TAMANHO_MAXIMO_OBSERVACAO = 500;
    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    // ---------------------------------------------------------------------
    // GRADE
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DisponibilidadeDTO buscarAtual(String jogadorId) {
        return disponibilidadeRepository.buscarAtual(jogadorId)
                .map(p -> DisponibilidadeDTO.de(p, JANELA_ALTERACAO_GRADE, agora()))
                .orElseGet(() -> DisponibilidadeDTO.vazia(jogadorId));
    }

    @Transactional
    public DisponibilidadeDTO atualizarGrade(String jogadorId, DisponibilidadeRequestDTO request) {
        int novaMascara = DisponibilidadeMascara.deGrade(request.grade());

        // Piso universal: qualquer campeonato exige pelo menos o mínimo do curto.
        // O mínimo específico de cada torneio é validado na inscrição (validarParaInscricao).
        validarMinimo(novaMascara, ExigenciaDisponibilidade.CAMPEONATO_CURTO.getMinDias());

        // Serializa alterações simultâneas do mesmo jogador até o fim da transação.
        disponibilidadeRepository.travarAlteracaoDoJogador(jogadorId);

        LocalDateTime agora = agora();
        Optional<DisponibilidadeAtualProjecaoDTO> atual = disponibilidadeRepository.buscarAtual(jogadorId);

        if (atual.isPresent()) {
            DisponibilidadeAtualProjecaoDTO anterior = atual.get();

            if (anterior.mascara() == novaMascara) {
                throw new RegraNegocioException("A grade enviada é igual à atual — nada foi alterado.");
            }

            LocalDateTime liberaEm = anterior.criadoEm().plus(JANELA_ALTERACAO_GRADE);
            if (agora.isBefore(liberaEm)) {
                throw new RegraNegocioException("Você só pode alterar sua grade de disponibilidade novamente a partir de "
                        + liberaEm.format(FORMATO_DATA_HORA) + " (horário de Brasília).");
            }
        }

        disponibilidadeRepository.save(new DisponibilidadeJogador(jogadorId, novaMascara, agora));

        log.info("[DisponibilidadeService] Grade atualizada: jogadorId={}, mascara={}", jogadorId, novaMascara);

        return DisponibilidadeDTO.de(new DisponibilidadeAtualProjecaoDTO(jogadorId, novaMascara, agora), JANELA_ALTERACAO_GRADE, agora);
    }

    /**
     * Chamar no fluxo de inscrição em campeonato. Barra a inscrição se a grade atual
     * não cumpre o mínimo do tipo de campeonato.
     */
    @Transactional(readOnly = true)
    public void validarParaInscricao(String jogadorId, ExigenciaDisponibilidade exigencia) {
        int mascara = disponibilidadeRepository.buscarAtual(jogadorId)
                .map(DisponibilidadeAtualProjecaoDTO::mascara)
                .orElse(0);

        validarMinimo(mascara, exigencia.getMinDias());
    }

    @Transactional(readOnly = true)
    public Page<DisponibilidadeHistoricoDTO> listarHistorico(String jogadorId, int pagina, int tamanho) {
        return disponibilidadeRepository.listarHistorico(jogadorId, paginar(pagina, tamanho));
    }

    // ---------------------------------------------------------------------
    // OBSERVAÇÕES (registros de imprevisto — sem limite de 24h, nunca reescritos)
    // ---------------------------------------------------------------------

    @Transactional
    public ObservacaoDisponibilidadeDTO registrarObservacao(String jogadorId, String texto) {
        String limpo = texto == null ? "" : texto.trim();

        if (limpo.isEmpty()) {
            throw new RegraNegocioException("A observação não pode ficar em branco.");
        }
        if (limpo.length() > TAMANHO_MAXIMO_OBSERVACAO) {
            throw new RegraNegocioException("A observação pode ter no máximo " + TAMANHO_MAXIMO_OBSERVACAO + " caracteres.");
        }

        ObservacaoDisponibilidade salva = observacaoRepository.save(
                new ObservacaoDisponibilidade(jogadorId, limpo, agora())
        );

        return new ObservacaoDisponibilidadeDTO(salva.getId(), salva.getTexto(), salva.getCriadoEm());
    }

    @Transactional(readOnly = true)
    public Page<ObservacaoDisponibilidadeDTO> listarObservacoes(String jogadorId, int pagina, int tamanho) {
        return observacaoRepository.listarPorJogador(jogadorId, paginar(pagina, tamanho));
    }

    // ---------------------------------------------------------------------
    // MESCLAR CONTAS
    // ---------------------------------------------------------------------

    /**
     * Chamar dentro do mesclarContas, ANTES de remover as contas secundárias.
     *
     * - Principal já tem grade: fica como está.
     * - Principal não tem: herda a versão mais recente entre as secundárias.
     * - Ninguém tem: não faz nada.
     * - As demais linhas de grade das secundárias são descartadas (se ficassem, uma
     *   versão mais nova de conta secundária poderia virar a "atual" da principal).
     * - Observações das secundárias passam pra principal (são registros avulsos, sem conflito).
     */
    @Transactional
    public void mesclarDisponibilidade(String principalId, List<String> secundariasIds) {
        if (secundariasIds == null || secundariasIds.isEmpty()) {
            return;
        }

        if (!disponibilidadeRepository.jogadorTemGrade(principalId)) {
            disponibilidadeRepository.buscarIdsMaisRecentes(secundariasIds, PageRequest.of(0, 1))
                    .stream()
                    .findFirst()
                    .ifPresent(id -> disponibilidadeRepository.transferirParaJogador(id, principalId));
        }

        disponibilidadeRepository.excluirPorJogadores(secundariasIds);
        observacaoRepository.transferirParaJogador(secundariasIds, principalId);

        log.info("[DisponibilidadeService] Disponibilidade mesclada: principal={}, secundarias={}", principalId, secundariasIds.size());
    }

    // ---------------------------------------------------------------------
    // INTERNOS
    // ---------------------------------------------------------------------

    private void validarMinimo(int mascara, int minDias) {
        int dias = DisponibilidadeMascara.contarDiasValidos(mascara);

        if (dias < minDias) {
            throw new RegraNegocioException("Marque pelo menos " + minDias + " dias da semana com algum horário de manhã, tarde ou noite "
                    + "(a madrugada sozinha não conta). Você marcou " + dias + ".");
        }
    }

    private Pageable paginar(int pagina, int tamanho) {
        return PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO_PAGINA));
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(ZONA_BRASIL);
    }
}
