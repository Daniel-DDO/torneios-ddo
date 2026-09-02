package com.ddo.torneios.service;

import com.ddo.torneios.dto.ChatRequestDTO;
import com.ddo.torneios.dto.MensagemChatDTO;
import com.ddo.torneios.dto.SuporteDTO;
import com.ddo.torneios.model.RegulamentoComponent;
import com.ddo.torneios.service.ia.ProvedorChatIA;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SuporteVirtualService {

    @Autowired
    private RegulamentoComponent regulamentoComponent;

    @Autowired
    @Qualifier("geminiProvedor")
    private ProvedorChatIA provedorPrincipal;

    @Autowired
    @Qualifier("groqProvedor")
    private ProvedorChatIA provedorReserva;

    public SuporteDTO responderComContexto(ChatRequestDTO requestDTO) {
        String historicoTexto = montarHistorico(requestDTO.historico());

        String promptCompleto = montarPrompt(regulamentoComponent.getTextoRegulamento(), historicoTexto, requestDTO.novaPergunta());

        try {
            String resposta = provedorPrincipal.gerarResposta(promptCompleto);
            log.info("Suporte respondido via {}", provedorPrincipal.nome());
            return new SuporteDTO(requestDTO.novaPergunta(), resposta);
        } catch (Exception e) {
            log.warn("Provedor {} falhou após todas as tentativas: {}", provedorPrincipal.nome(), e.getMessage());
        }

        String promptReduzido = montarPrompt(regulamentoComponent.getTextoRegulamentoParaFallback(), historicoTexto, requestDTO.novaPergunta());

        try {
            String resposta = provedorReserva.gerarResposta(promptReduzido);
            log.info("Suporte respondido via {} (fallback com regulamento reduzido)", provedorReserva.nome());
            return new SuporteDTO(requestDTO.novaPergunta(), resposta);
        } catch (Exception e) {
            log.error("Todos os provedores de IA falharam. Última falha ({}): {}", provedorReserva.nome(), e.getMessage(), e);
            throw new RuntimeException("O suporte está indisponível no momento. Todos os provedores de IA falharam.");
        }
    }

    private String montarHistorico(List<MensagemChatDTO> historico) {
        if (historico == null || historico.isEmpty()) {
            return "Nenhuma mensagem anterior.";
        }
        return historico.stream()
                .map(msg -> (msg.role().equalsIgnoreCase("user") ? "JOGADOR: " : "VOCÊ (SUPORTE): ") + msg.texto())
                .collect(Collectors.joining("\n"));
    }

    private String montarPrompt(String regulamento, String historicoTexto, String novaPergunta) {
        return String.format("""
            Você é o SUPORTE VIRTUAL INTELIGENTE DOS TORNEIOS DDO.
            Sua função é tirar dúvidas e conversar com os jogadores de forma educada, clara e objetiva.

            === BASE DE CONHECIMENTO (REGULAMENTO OFICIAL) ===
            %s
            ==================================================

            === HISTÓRICO DA CONVERSA ATUAL ===
            %s
            ===================================

            PERGUNTA ATUAL DO JOGADOR:
            "%s"

            DIRETRIZES DE RESPOSTA:
            1. Use o HISTÓRICO para entender o contexto (ex: se ele disser "e sobre aquilo?", refira-se ao que foi dito antes).
            2. Responda APENAS com base no REGULAMENTO acima.
            3. Se a pergunta for social (ex: "oi", "obrigado"), seja educado, mas lembre que você é o suporte da Liga.
            4. Se a informação não estiver no regulamento, diga que não sabe e oriente procurar um administrador.
            """,
                regulamento, historicoTexto, novaPergunta
        );
    }
}