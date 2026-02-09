package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.*;
import com.ddo.torneios.request.JogadorClubeRequest;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LeilaoService {

    @Autowired private LanceRepository lanceRepository;
    @Autowired private JogadorRepository jogadorRepository;
    @Autowired private ClubeRepository clubeRepository;
    @Autowired private LeilaoRepository leilaoRepository;
    @Autowired private TemporadaRepository temporadaRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private TransferenciaRepository transferenciaRepository;
    @Autowired private JogadorClubeService jogadorClubeService;
    @Autowired private NotificacaoService notificacaoService;

    private static final BigDecimal INCREMENTO_MINIMO = new BigDecimal("1000");

    @Transactional
    public Leilao iniciarLeilao(String temporadaId, LocalDateTime dataFim, boolean isSelecao) {
        Temporada temporada = temporadaRepository.findById(temporadaId)
                .orElseThrow(() -> new RuntimeException("Temporada não encontrada"));

        if (leilaoRepository.existsByTemporadaAndAtivoTrue(temporada)) {
            throw new RuntimeException("Já existe um leilão ativo nesta temporada.");
        }

        Leilao leilao = new Leilao();
        leilao.setTemporada(temporada);
        leilao.setDataInicio(LocalDateTime.now());
        leilao.setDataFim(dataFim);
        leilao.setAtivo(true);
        leilao.setDescricao("Janela de Transferências - " + temporada.getNome());
        leilao.setSelecao(isSelecao);

        Leilao leilaoSalvo = leilaoRepository.save(leilao);
        messagingTemplate.convertAndSend("/topic/leilao/status", "ABERTO");
        return leilaoSalvo;
    }

    public List<HistoricoLancesClubeDTO> obterHistoricoLances(String leilaoId, String clubeId) {
        return lanceRepository.buscarHistoricoLancesDoClube(leilaoId, clubeId);
    }

    @Transactional
    public void registrarLances(String jogadorId, RealizarLanceDTO dto) {
        Leilao leilao = validarLeilao(dto.leilaoId());
        Jogador jogador = validarJogador(jogadorId);
        validarSaldoGlobal(jogador, dto.preferencias());

        List<Lance> lancesBanco = lanceRepository.findByLeilaoAndJogador(leilao, jogador);

        Map<String, Lance> vencedoresAntes = executarAlgoritmoGaleShapley(dto.leilaoId());

        Map<String, ItemLanceDTO> payloadMap = dto.preferencias().stream()
                .collect(Collectors.toMap(ItemLanceDTO::clubeId, Function.identity()));

        List<Lance> lancesParaDeletar = new ArrayList<>();

        for (Lance lance : lancesBanco) {
            String clubeId = lance.getClube().getId();

            if (!payloadMap.containsKey(clubeId)) {
                lancesParaDeletar.add(lance);
            } else {
                ItemLanceDTO itemNovo = payloadMap.get(clubeId);
                boolean mudou = !lance.getPrioridade().equals(itemNovo.prioridade())
                        || lance.getValor().compareTo(itemNovo.valor()) != 0;

                if (mudou) {
                    lancesParaDeletar.add(lance);
                } else {
                    payloadMap.remove(clubeId);
                }
            }
        }

        if (!lancesParaDeletar.isEmpty()) {
            lanceRepository.deleteAll(lancesParaDeletar);
            lanceRepository.flush();
        }

        List<Lance> novosLances = new ArrayList<>();

        for (ItemLanceDTO item : payloadMap.values()) {
            Clube clube = clubeRepository.findById(item.clubeId())
                    .orElseThrow(() -> new RuntimeException("Clube inválido"));

            if (item.valor().compareTo(clube.getLanceMinimo()) < 0) {
                throw new RuntimeException("Valor abaixo do mínimo para " + clube.getNome());
            }

            validarSeSuperaLider(leilao, clube, jogadorId, item.valor(), item.prioridade());

            Lance lance = new Lance();
            lance.setLeilao(leilao);
            lance.setJogador(jogador);
            lance.setClube(clube);
            lance.setValor(item.valor());
            lance.setPrioridade(item.prioridade());
            lance.setDataHoraLance(LocalDateTime.now());

            novosLances.add(lance);
        }

        notificarFeed(leilao, jogador);

        if (!novosLances.isEmpty()) {
            lanceRepository.saveAll(novosLances);
            lanceRepository.flush();
        }

        Map<String, Lance> vencedoresDepois = executarAlgoritmoGaleShapley(dto.leilaoId());

        try {
            verificarEEnviarNotificacoesDePerda(dto.leilaoId(), vencedoresAntes, vencedoresDepois, jogadorId);
        } catch (Exception e) {
            log.error("Erro ao calcular notificações de perda", e);
        }
    }

    private Leilao validarLeilao(String leilaoId) {
        Leilao leilao = leilaoRepository.findById(leilaoId)
                .orElseThrow(() -> new RuntimeException("Leilão não encontrado"));

        if (!leilao.isAtivo()) throw new RuntimeException("Leilão encerrado.");
        if (leilao.getDataFim() != null && LocalDateTime.now().isAfter(leilao.getDataFim())) {
            throw new RuntimeException("Tempo esgotado.");
        }
        return leilao;
    }

    private Jogador validarJogador(String jogadorId) {
        return jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new RuntimeException("Jogador inválido."));
    }

    private void validarSaldoGlobal(Jogador jogador, List<ItemLanceDTO> itens) {
        BigDecimal maiorOferta = itens.stream()
                .map(ItemLanceDTO::valor)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        if (jogador.getSaldoVirtual().compareTo(maiorOferta) < 0) {
            throw new RuntimeException("Saldo insuficiente (D$ " + jogador.getSaldoVirtual() +
                    ") para cobrir sua maior oferta de D$ " + maiorOferta);
        }

        Set<String> checkDuplicados = new HashSet<>();
        for(ItemLanceDTO i : itens) {
            if(!checkDuplicados.add(i.clubeId())) {
                throw new RuntimeException("Existem clubes duplicados na sua lista de lances.");
            }
        }
    }

    private void validarSeSuperaLider(Leilao leilao, Clube clube, String meuJogadorId, BigDecimal meuValor, Integer minhaPrioridade) {
        Optional<Lance> liderDessaPrioridadeOpt = lanceRepository
                .findTopByLeilaoAndClubeAndPrioridadeOrderByValorDesc(leilao, clube, minhaPrioridade);

        if (liderDessaPrioridadeOpt.isPresent()) {
            Lance lider = liderDessaPrioridadeOpt.get();

            if (lider.getJogador().getId().equals(meuJogadorId)) {
                return;
            }

            BigDecimal valorLider = lider.getValor();
            BigDecimal minimoNecessario = valorLider.add(BigDecimal.valueOf(1000));

            if (meuValor.compareTo(minimoNecessario) < 0) {

                String nomeLider = lider.getJogador().getNome();

                throw new RuntimeException(String.format(
                        "Poxa! O %s já ofertou D$ %s escolhendo o %s como %ª opção. " +
                                "Para assumir o lugar dele nessa fila, você precisa ofertar pelo menos D$ %s.",
                        nomeLider,
                        valorLider,
                        clube.getNome(),
                        minhaPrioridade,
                        minimoNecessario
                ));
            }
        }
    }

    private void notificarFeed(Leilao leilao, Jogador jogador) {
        try {
            messagingTemplate.convertAndSend("/topic/leilao/" + leilao.getId() + "/feed",
                    new NotificacaoLanceDTO(jogador.getNome(), LocalDateTime.now()));
        } catch (Exception e) {
        }
    }

    @Transactional
    public void resetarLancesDoJogador(String leilaoId, String jogadorId) {
        Leilao leilao = leilaoRepository.findById(leilaoId)
                .orElseThrow(() -> new RuntimeException("Leilão não encontrado"));

        if (!leilao.isAtivo()) {
            throw new RuntimeException("Não é possível resetar lances de um leilão encerrado.");
        }

        Jogador jogador = jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado"));

        lanceRepository.deleteByLeilaoIdAndJogadorId(leilaoId, jogadorId);
        lanceRepository.flush();

        try {
            messagingTemplate.convertAndSend("/topic/leilao/" + leilao.getId() + "/feed",
                    new NotificacaoLanceDTO(jogador.getNome(), LocalDateTime.now()));
        } catch (Exception e) {
            System.err.println("Erro socket: " + e.getMessage());
        }
    }

    @Transactional
    public void registrarLancesBlindado(String jogadorId, RealizarLanceDTO dto) {
        Leilao leilao = leilaoRepository.findById(dto.leilaoId())
                .orElseThrow(() -> new RuntimeException("Leilão não encontrado"));

        if (!leilao.isAtivo()) throw new RuntimeException("O leilão não está ativo.");
        if (leilao.getDataFim() != null && LocalDateTime.now().isAfter(leilao.getDataFim())) {
            throw new RuntimeException("O tempo acabou.");
        }

        Jogador jogador = jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado"));

        Set<String> clubesNoPayload = new HashSet<>();
        for (ItemLanceDTO item : dto.preferencias()) {
            if (!clubesNoPayload.add(item.clubeId())) {
                throw new RuntimeException("Erro: Você enviou lances duplicados para o mesmo clube na lista.");
            }
        }

        BigDecimal maiorValorOfertado = dto.preferencias().stream()
                .map(ItemLanceDTO::valor)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        if (jogador.getSaldoVirtual().compareTo(maiorValorOfertado) < 0) {
            throw new RuntimeException("Saldo insuficiente. Seu saldo é D$ " + jogador.getSaldoVirtual() +
                    ", mas sua maior oferta é de D$ " + maiorValorOfertado);
        }

        List<Lance> lancesAntigos = lanceRepository.findByLeilaoAndJogador(leilao, jogador);

        if (!lancesAntigos.isEmpty()) {
            lanceRepository.deleteAll(lancesAntigos);
            lanceRepository.flush();
        }

        List<Lance> novosLances = new ArrayList<>();

        for (ItemLanceDTO item : dto.preferencias()) {
            Clube clubeAlvo = clubeRepository.findById(item.clubeId())
                    .orElseThrow(() -> new RuntimeException("Clube inválido: " + item.clubeId()));

            if (item.valor().compareTo(clubeAlvo.getLanceMinimo()) < 0) {
                throw new RuntimeException("O lance para " + clubeAlvo.getNome() +
                        " deve ser no mínimo " + clubeAlvo.getLanceMinimo());
            }

            validarSeSuperaLider(leilao, clubeAlvo, jogadorId, item.valor(), item.prioridade());

            Lance novoLance = new Lance();
            novoLance.setLeilao(leilao);
            novoLance.setJogador(jogador);
            novoLance.setClube(clubeAlvo);
            novoLance.setValor(item.valor());
            novoLance.setPrioridade(item.prioridade());
            novoLance.setDataHoraLance(LocalDateTime.now());

            novosLances.add(novoLance);
        }

        lanceRepository.saveAll(novosLances);

        try {
            messagingTemplate.convertAndSend("/topic/leilao/" + leilao.getId() + "/feed",
                    new NotificacaoLanceDTO(jogador.getNome(), LocalDateTime.now()));
        } catch (Exception e) {
            System.err.println("Erro socket: " + e.getMessage());
        }
    }

    @Value("${app.frontend.url}")
    private String linkFront;

    @Transactional
    public void finalizarLeilao(String leilaoId) {
        Leilao leilao = leilaoRepository.findById(leilaoId)
                .orElseThrow(() -> new RuntimeException("Leilão não encontrado"));

        if (!leilao.isAtivo()) throw new RuntimeException("Leilão já finalizado.");

        Map<String, Lance> vencedores = executarAlgoritmoGaleShapley(leilaoId);
        List<String> logs = new ArrayList<>();

        java.text.NumberFormat nf = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("pt", "BR"));

        for (Lance lance : vencedores.values()) {
            Jogador jogador = lance.getJogador();
            Clube clube = lance.getClube();
            BigDecimal valorFinal = lance.getValor();

            jogador.setSaldoVirtual(jogador.getSaldoVirtual().subtract(valorFinal));

            Transferencia transferencia = new Transferencia();
            transferencia.setLeilao(leilao);
            transferencia.setJogador(jogador);
            transferencia.setClube(clube);
            transferencia.setValorPago(valorFinal);
            transferencia.setDataCompra(LocalDateTime.now());

            transferenciaRepository.save(transferencia);
            jogadorRepository.save(jogador);

            logs.add("O jogador " + jogador.getNome() + " assumiu o " + clube.getNome() + " por D$ " + valorFinal);

            //inscrevendo jogador
            try {
                JogadorClubeRequest request = new JogadorClubeRequest();
                request.setJogadorId(jogador.getId());
                request.setClubeId(clube.getId());
                request.setTemporadaId(leilao.getTemporada().getId());

                jogadorClubeService.inscreverJogador(request);

            } catch (Exception e) {
                logs.add("ATENÇÃO: Transferência realizada, mas falha ao associar jogador com clube na temporada. " + e.getMessage());
            }

            try {
                String valorFormatado = nf.format(valorFinal);
                String nomeTemporada = leilao.getTemporada().getNome();

                String titulo = "Clube Conquistado!";
                String mensagem = String.format(
                        "Parabéns! Você venceu o leilão e assumiu o %s por %s na %s.",
                        clube.getNome(),
                        valorFormatado,
                        nomeTemporada
                );

                String link = linkFront+leilao.getTemporada().getId()+"/torneios/jogadores";

                notificacaoService.enviarParaJogador(
                        jogador,
                        titulo,
                        mensagem,
                        link,
                        TipoNotificacao.LEILAO
                );

            } catch (Exception e) {
                log.error("Erro ao notificar vencedor do leilão: {}", jogador.getNome(), e);
            }
        }

        leilao.setAtivo(false);
        leilaoRepository.save(leilao);

        messagingTemplate.convertAndSend("/topic/leilao/" + leilaoId + "/resultado", logs);
    }

    public List<ResultadoLeilaoDTO> obterResultadoFinal(String leilaoId) {
        List<Transferencia> transferencias = transferenciaRepository.findByLeilaoIdOrderByValorPagoDesc(leilaoId);

        if (transferencias.isEmpty()) {
            throw new RuntimeException("O leilão ainda não foi finalizado ou não houve lances.");
        }

        return transferencias.stream()
                .map(t -> new ResultadoLeilaoDTO(
                        t.getClube().getNome(),
                        t.getClube().getImagem(),
                        t.getJogador().getNome(),
                        t.getValorPago()
                ))
                .toList();
    }

    /**
     * Executa a lógica de distribuição dos times respeitando:
     * 1. Cada jogador só leva 1 time.
     * 2. Prioridade vence Dinheiro (P1 de 100k ganha de P2 de 1 milhão).
     * 3. Dinheiro desempata Prioridades iguais.
     */
    private Map<String, Lance> executarAlgoritmoGaleShapley(String leilaoId) {
        List<Lance> todosLances = lanceRepository.findAllByLeilaoId(leilaoId);

        Map<String, Lance> donosProvisorios = new HashMap<>();

        Map<String, Integer> tentativaAtualDoJogador = new HashMap<>();

        Queue<String> filaJogadoresLivres = new LinkedList<>();

        todosLances.stream()
                .map(l -> l.getJogador().getId())
                .distinct()
                .forEach(id -> {
                    filaJogadoresLivres.add(id);
                    tentativaAtualDoJogador.put(id, 1);
                });

        while (!filaJogadoresLivres.isEmpty()) {
            String jogadorId = filaJogadoresLivres.poll();
            Integer prioridadeTentada = tentativaAtualDoJogador.get(jogadorId);

            if (prioridadeTentada > 20) continue;

            Optional<Lance> lanceOpt = todosLances.stream()
                    .filter(l -> l.getJogador().getId().equals(jogadorId) && l.getPrioridade().equals(prioridadeTentada))
                    .findFirst();

            if (lanceOpt.isEmpty()) {
                tentativaAtualDoJogador.put(jogadorId, prioridadeTentada + 1);
                filaJogadoresLivres.add(jogadorId);
                continue;
            }

            Lance meuLance = lanceOpt.get();
            String clubeId = meuLance.getClube().getId();
            Lance donoAtual = donosProvisorios.get(clubeId);

            if (donoAtual == null) {
                donosProvisorios.put(clubeId, meuLance);
            } else {
                boolean vitoriaDoDesafiante = false;

                if (meuLance.getPrioridade() < donoAtual.getPrioridade()) {
                    vitoriaDoDesafiante = true;
                }
                else if (meuLance.getPrioridade().equals(donoAtual.getPrioridade())) {
                    if (meuLance.getValor().compareTo(donoAtual.getValor()) > 0) {
                        vitoriaDoDesafiante = true;
                    } else if (meuLance.getValor().compareTo(donoAtual.getValor()) == 0) {
                        if (meuLance.getDataHoraLance().isBefore(donoAtual.getDataHoraLance())) {
                            vitoriaDoDesafiante = true;
                        }
                    }
                }
                if (vitoriaDoDesafiante) {
                    String idDonoAntigo = donoAtual.getJogador().getId();
                    tentativaAtualDoJogador.put(idDonoAntigo, tentativaAtualDoJogador.get(idDonoAntigo) + 1);
                    filaJogadoresLivres.add(idDonoAntigo);

                    donosProvisorios.put(clubeId, meuLance);
                } else {
                    tentativaAtualDoJogador.put(jogadorId, prioridadeTentada + 1);
                    filaJogadoresLivres.add(jogadorId);
                }
            }
        }

        return donosProvisorios;
    }

    /**
     * Simula o resultado do leilão para dizer ao jogador como ele está em cada prioridade.
     */
    @Transactional
    public List<StatusLanceJogadorDTO> obterStatusDoJogador(String leilaoId, String jogadorId) {
        Map<String, Lance> resultadoSimulado = executarAlgoritmoGaleShapley(leilaoId);

        List<Lance> meusLances = lanceRepository.findAllByLeilaoId(leilaoId).stream()
                .filter(l -> l.getJogador().getId().equals(jogadorId))
                .sorted(Comparator.comparing(Lance::getPrioridade))
                .toList();

        List<StatusLanceJogadorDTO> statusList = new ArrayList<>();

        Optional<Lance> lanceVencedorOpt = resultadoSimulado.values().stream()
                .filter(l -> l.getJogador().getId().equals(jogadorId))
                .findFirst();

        Integer prioridadeVencedora = lanceVencedorOpt.map(Lance::getPrioridade).orElse(999);

        for (Lance lance : meusLances) {
            String status;

            if (lance.getPrioridade().equals(prioridadeVencedora)) {
                status = "GANHANDO";
            } else if (lance.getPrioridade() < prioridadeVencedora) {
                status = "PERDENDO";
            } else {
                status = "ANULADO";
            }

            statusList.add(new StatusLanceJogadorDTO(
                    lance.getPrioridade(),
                    lance.getClube().getNome(),
                    lance.getValor(),
                    status
            ));
        }

        return statusList;
    }

    public DisputaClubeDTO obterDetalhesDisputa(String leilaoId, String clubeId) {
        Leilao leilao = leilaoRepository.findById(leilaoId)
                .orElseThrow(() -> new RuntimeException("Leilão não encontrado"));

        Clube clube = clubeRepository.findById(clubeId)
                .orElseThrow(() -> new RuntimeException("Clube não encontrado"));

        List<Lance> lancesDoClube = lanceRepository.findByLeilaoAndClubeOrderByPrioridadeAscValorDesc(leilao, clube);

        List<ItemDisputaDTO> ranking = lancesDoClube.stream()
                .map(lance -> new ItemDisputaDTO(
                        lance.getJogador().getNome(),
                        lance.getValor(),
                        lance.getPrioridade(),
                        lance.getDataHoraLance()
                ))
                .toList();

        return new DisputaClubeDTO(
                clube.getId(),
                clube.getNome(),
                clube.getImagem(),
                lancesDoClube.size(),
                ranking
        );
    }

    public List<Leilao> listarPorTemporada(String temporadaId) {
        return leilaoRepository.findByTemporadaIdOrderByDataInicioDesc(temporadaId);
    }

    public boolean existeLeilaoParaTemporada(String temporadaId) {
        return leilaoRepository.existsByTemporadaId(temporadaId);
    }

    public List<LanceResumoDTO> obterLancesAtuais(String leilaoId) {
        return lanceRepository.encontrarMaioresLancesPorLeilao(leilaoId);
    }

    public List<LanceDetalheDTO> buscarLancesDoJogador(String leilaoId, String jogadorId) {
        List<Lance> lances = lanceRepository.findByLeilaoIdAndJogadorIdOrderByPrioridadeAsc(leilaoId, jogadorId);

        return lances.stream()
                .map(lance -> new LanceDetalheDTO(
                        lance.getId(),
                        lance.getClube().getId(),
                        lance.getClube().getNome(),
                        lance.getClube().getImagem(),
                        lance.getClube().getLanceMinimo(),
                        lance.getValor(),
                        lance.getPrioridade()
                ))
                .toList();
    }

    public List<FeedItemDTO> obterFeedInicial(String leilaoId) {
        return lanceRepository.buscarUltimosLances(leilaoId);
    }

    public List<ClubeDisputadoDTO> obterTermometro(String leilaoId) {
        return lanceRepository.buscarClubesMaisDisputados(leilaoId, PageRequest.of(0, 15));
    }

    public List<ResultadoParcialDTO> calcularResultadosParciais(String leilaoId) {
        Map<String, Lance> mapaVencedores = executarAlgoritmoGaleShapley(leilaoId);

        return mapaVencedores.values().stream()
                .sorted(Comparator.comparing(l -> l.getClube().getNome()))
                .map(lance -> new ResultadoParcialDTO(
                        lance.getClube().getNome(),
                        lance.getClube().getImagem(),
                        lance.getJogador().getNome(),
                        lance.getValor(),
                        lance.getPrioridade()
                ))
                .collect(Collectors.toList());
    }

    private void verificarEEnviarNotificacoesDePerda(
            String leilaoId,
            Map<String, Lance> antes,
            Map<String, Lance> depois,
            String jogadorQueDeuLanceAgoraId) {

        Optional<Leilao> leilao = leilaoRepository.findById(leilaoId);

        for (Map.Entry<String, Lance> entryAntes : antes.entrySet()) {
            String clubeId = entryAntes.getKey();
            Lance lanceGanhadorAntigo = entryAntes.getValue();
            String jogadorAntigoId = lanceGanhadorAntigo.getJogador().getId();

            if (jogadorAntigoId.equals(jogadorQueDeuLanceAgoraId)) {
                continue;
            }

            Lance lanceGanhadorNovo = depois.get(clubeId);

            boolean perdeuPosicao = false;

            if (lanceGanhadorNovo == null) {
                perdeuPosicao = true;
            } else if (!lanceGanhadorNovo.getJogador().getId().equals(jogadorAntigoId)) {
                perdeuPosicao = true;
            }

            if (perdeuPosicao) {
                String nomeClube = lanceGanhadorAntigo.getClube().getNome();
                String titulo = "Atenção: Você perdeu " + nomeClube;
                String msg = "A configuração do leilão mudou e você não é mais o vencedor provisório do " + nomeClube + ".";
                String link = linkFront+"/"+leilao.get().getTemporada().getId()+"/torneios/leilao";

                notificacaoService.enviarParaJogador(
                        lanceGanhadorAntigo.getJogador(),
                        titulo,
                        msg,
                        link,
                        TipoNotificacao.ALERTA
                );

                log.info("Notificação de perda enviada para {} sobre o clube {}",
                        lanceGanhadorAntigo.getJogador().getNome(), nomeClube);
            }
        }
    }
}