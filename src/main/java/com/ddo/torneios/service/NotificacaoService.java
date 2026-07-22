package com.ddo.torneios.service;

import com.ddo.torneios.dto.NotificacaoDTO;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.model.Notificacao;
import com.ddo.torneios.model.TipoNotificacao;
import com.ddo.torneios.repository.JogadorRepository;
import com.ddo.torneios.repository.NotificacaoRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificacaoService {

    @Autowired
    private NotificacaoRepository repository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private JogadorRepository jogadorRepository;

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

    @Transactional
    public void enviarParaTodos(String titulo, String msg, String link) {
        NotificacaoDTO dto = new NotificacaoDTO(titulo, msg, link);
        messagingTemplate.convertAndSend("/topic/notificacoes-gerais", dto);

        List<Jogador> todosJogadores = jogadorRepository.findAll();

        if (todosJogadores.isEmpty()) return;

        List<Notificacao> listaNotificacoes = todosJogadores.stream()
                .map(jogador -> {
                    Notificacao n = new Notificacao();
                    n.setJogador(jogador);
                    n.setTitulo(titulo);
                    n.setMensagem(msg);
                    n.setLink(link);
                    n.setTipo(TipoNotificacao.INFORMACAO);
                    n.setLida(false);
                    n.setDataCriacao(LocalDateTime.now());
                    return n;
                })
                .collect(Collectors.toList());

        repository.saveAll(listaNotificacoes);

        log.info("Notificação salva para {} jogadores.", listaNotificacoes.size());
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

    //anúncio
    public record AnuncioCriadoEvent(String titulo, String mensagem, String link) {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void lidarComAnuncioCriado(AnuncioCriadoEvent event) {
        log.info("Recebido evento de anúncio: {}", event.titulo());
        try {
            this.enviarParaTodos(
                    event.titulo(),
                    event.mensagem(),
                    event.link()
            );
        } catch (Exception e) {
            log.error("Erro ao processar notificação de anúncio", e);
        }
    }
}