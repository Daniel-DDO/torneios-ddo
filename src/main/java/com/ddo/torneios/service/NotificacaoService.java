package com.ddo.torneios.service;

import com.ddo.torneios.dto.NotificacaoDTO;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.model.Notificacao;
import com.ddo.torneios.model.TipoNotificacao;
import com.ddo.torneios.repository.NotificacaoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificacaoService {

    @Autowired
    private NotificacaoRepository repository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void enviarParaJogador(Jogador jogador, String titulo, String msg, String link, TipoNotificacao tipo) {
        Notificacao notif = new Notificacao();
        notif.setJogador(jogador);
        notif.setTitulo(titulo);
        notif.setMensagem(msg);
        notif.setLink(link);
        notif.setTipo(tipo);

        notif = repository.save(notif);

        messagingTemplate.convertAndSendToUser(
                jogador.getId(),
                "/queue/notificacoes",
                NotificacaoDTO.fromEntity(notif)
        );
    }

    public void enviarParaTodos(String titulo, String msg, String link) {
        NotificacaoDTO dto = new NotificacaoDTO(titulo, msg, link);

        messagingTemplate.convertAndSend("/topic/notificacoes-gerais", dto);
    }

    public void marcarComoLida(String notificacaoId) {
        repository.findById(notificacaoId).ifPresent(n -> {
            n.marcarComoLida();
            repository.save(n);
        });
    }

    public List<NotificacaoDTO> listarPorJogador(String jogadorId) {
        return repository.findByJogadorIdOrderByDataCriacaoDesc(jogadorId)
                .stream()
                .map(NotificacaoDTO::fromEntity)
                .toList();
    }

    public Page<NotificacaoDTO> listarTodas(Pageable pageable) {
        return repository.findAll(pageable).map(NotificacaoDTO::fromEntity);
    }

    @Transactional
    public int apagarTodasLidas() {
        return repository.deleteByLidaTrue();
    }

    @Transactional
    public int apagarMaisAntigasQue(int dias) {
        LocalDateTime dataLimite = LocalDateTime.now().minusDays(dias);
        return repository.deleteByDataCriacaoBefore(dataLimite);
    }

    @Transactional
    public void apagarTudo() {
        repository.deleteAll();
    }
}