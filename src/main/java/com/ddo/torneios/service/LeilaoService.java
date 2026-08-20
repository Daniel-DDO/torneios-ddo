package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.exception.RegraNegocioException;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.*;
import com.ddo.torneios.request.JogadorClubeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    @Autowired private JogadorService jogadorService;

    @Value("${app.frontend.url}")
    private String linkFront;

    @Transactional
    public LeilaoResumoDTO iniciarLeilao(String temporadaId, LocalDateTime dataFim, boolean isSelecao) {
        Temporada temporada = temporadaRepository.findById(temporadaId)
                .orElseThrow(() -> new RegraNegocioException("Temporada não encontrada"));

        if (leilaoRepository.existsByTemporadaAndAtivoTrue(temporada)) {
            throw new RegraNegocioException("Já existe um leilão ativo nesta temporada.");
        }

        Leilao leilao = new Leilao();
        leilao.setTemporada(temporada);
        leilao.setDataInicio(LocalDateTime.now());
        leilao.setDataFim(dataFim);
        leilao.setAtivo(true);
        leilao.setDescricao("Janela de Transferências - " + temporada.getNome());
        leilao.setSelecao(isSelecao);

        Leilao salvo = leilaoRepository.save(leilao);

        messagingTemplate.convertAndSend("/topic/leilao/status", "ABERTO");

        return new LeilaoResumoDTO(salvo.getId(), salvo.getDescricao(), salvo.getDataInicio(),
                salvo.getDataFim(), salvo.isAtivo(), salvo.isSelecao(), temporada.getId());
    }

    @Transactional(readOnly = true)
    public List<LeilaoResumoDTO> listarPorTemporada(String temporadaId) {
        return leilaoRepository.listarResumoPorTemporada(temporadaId);
    }

    @Transactional(readOnly = true)
    public boolean existeLeilaoParaTemporada(String temporadaId) {
        return leilaoRepository.existsByTemporadaId(temporadaId);
    }

    @Transactional
    public void registrarLances(String jogadorId, RealizarLanceDTO dto) {
        processarLances(jogadorId, dto, true);
    }

    @Transactional
    public void registrarLancesBlindado(String jogadorId, RealizarLanceDTO dto) {
        processarLances(jogadorId, dto, false);
    }

    /**
     * Lógica única compartilhada pelos endpoints /lance e /lance/v2.
     * O parâmetro `comNotificacaoDePerda` mantém a diferença de comportamento
     * que já existia entre as duas rotas, sem duplicar 80 linhas de código.
     *
     * IMPORTANTE: aqui só trabalhamos com projeções (JogadorLeilaoDTO / ClubeLeilaoDTO)
     * e com getReferenceById para gravar FKs. Isso evita que o Hibernate materialize
     * Jogador/Clube inteiros (com insignias, conquistas etc. em EAGER) a cada lance.
     */
    private void processarLances(String jogadorId, RealizarLanceDTO dto, boolean comNotificacaoDePerda) {
        Leilao leilao = validarLeilao(dto.leilaoId());

        JogadorLeilaoDTO jogadorInfo = jogadorRepository.buscarParaLeilao(jogadorId)
                .orElseThrow(() -> new RegraNegocioException("Jogador inválido."));
        validarSaldoGlobal(jogadorInfo, dto.preferencias());

        Jogador jogadorRef = jogadorRepository.getReferenceById(jogadorId);

        List<Lance> lancesBanco = lanceRepository.findByLeilaoIdAndJogadorId(leilao.getId(), jogadorId);

        Map<String, LanceAlgoritmoDTO> vencedoresAntes = comNotificacaoDePerda
                ? executarAlgoritmoGaleShapley(dto.leilaoId())
                : Collections.emptyMap();

        Map<String, ItemLanceDTO> payloadMap = dto.preferencias().stream()
                .collect(Collectors.toMap(ItemLanceDTO::clubeId, java.util.function.Function.identity()));

        Set<String> checkDuplicados = new HashSet<>();
        for (ItemLanceDTO i : dto.preferencias()) {
            if (!checkDuplicados.add(i.clubeId())) {
                throw new RegraNegocioException("Existem clubes duplicados na sua lista de lances.");
            }
        }

        Set<String> clubesAfetados = new HashSet<>();
        List<Lance> lancesParaDeletar = new ArrayList<>();

        for (Lance lance : lancesBanco) {
            String clubeId = lance.getClube().getId();
            clubesAfetados.add(clubeId);

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
            clubesAfetados.add(item.clubeId());

            ClubeLeilaoDTO clubeInfo = clubeRepository.buscarParaLeilao(item.clubeId())
                    .orElseThrow(() -> new RegraNegocioException("Clube inválido"));

            if (item.valor().compareTo(clubeInfo.lanceMinimo()) < 0) {
                throw new RegraNegocioException("Valor abaixo do mínimo para " + clubeInfo.nome());
            }

            validarSeSuperaLider(leilao, clubeInfo, jogadorId, item.valor(), item.prioridade());

            Lance lance = new Lance();
            lance.setLeilao(leilao);
            lance.setJogador(jogadorRef);
            lance.setClube(clubeRepository.getReferenceById(item.clubeId()));
            lance.setValor(item.valor());
            lance.setPrioridade(item.prioridade());
            lance.setDataHoraLance(LocalDateTime.now());
            novosLances.add(lance);

            notificarFeed(leilao, jogadorInfo, item, clubeInfo);
        }

        if (!novosLances.isEmpty()) {
            lanceRepository.saveAll(novosLances);
            lanceRepository.flush();
        }

        publicarAtualizacaoDeClubes(dto.leilaoId(), clubesAfetados);

        if (comNotificacaoDePerda) {
            Map<String, LanceAlgoritmoDTO> vencedoresDepois = executarAlgoritmoGaleShapley(dto.leilaoId());
            try {
                verificarEEnviarNotificacoesDePerda(dto.leilaoId(), vencedoresAntes, vencedoresDepois, jogadorId);
            } catch (Exception e) {
                log.error("Erro ao calcular notificações de perda", e);
            }
        }
    }

    /**
     * Em vez de recalcular o leilão inteiro (agregação sobre TODOS os clubes)
     * a cada lance, busca só os clubes que esse lance tocou. Isso é o que
     * garante resposta em ms mesmo com o WS acoplado no fluxo síncrono.
     */
    private void publicarAtualizacaoDeClubes(String leilaoId, Set<String> clubeIds) {
        if (clubeIds.isEmpty()) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    List<LanceResumoDTO> atualizacoes = lanceRepository.encontrarMaioresLancesPorClubes(leilaoId, clubeIds);
                    messagingTemplate.convertAndSend("/topic/leilao/" + leilaoId + "/atualizacoes-lances", atualizacoes);
                } catch (Exception e) {
                    log.warn("Falha ao publicar atualização de lances via WS", e);
                }
            }
        });
    }

    private Leilao validarLeilao(String leilaoId) {
        Leilao leilao = leilaoRepository.findById(leilaoId)
                .orElseThrow(() -> new RegraNegocioException("Leilão não encontrado"));
        if (!leilao.isAtivo()) throw new RegraNegocioException("Leilão encerrado.");
        if (leilao.getDataFim() != null && LocalDateTime.now().isAfter(leilao.getDataFim())) {
            throw new RegraNegocioException("Tempo esgotado.");
        }
        return leilao;
    }

    private void validarSaldoGlobal(JogadorLeilaoDTO jogador, List<ItemLanceDTO> itens) {
        BigDecimal maiorOferta = itens.stream()
                .map(ItemLanceDTO::valor)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        if (jogador.saldoVirtual().compareTo(maiorOferta) < 0) {
            throw new RegraNegocioException("Saldo insuficiente (D$ " + jogador.saldoVirtual() +
                    ") para cobrir sua maior oferta de D$ " + maiorOferta);
        }
    }

    private void validarSeSuperaLider(Leilao leilao, ClubeLeilaoDTO clubeInfo, String meuJogadorId, BigDecimal meuValor, Integer minhaPrioridade) {
        Clube clubeRef = clubeRepository.getReferenceById(clubeInfo.id());

        List<LiderLanceDTO> lideres = lanceRepository.buscarLiderProjetado(
                leilao, clubeRef, minhaPrioridade, PageRequest.of(0, 1));

        if (!lideres.isEmpty()) {
            LiderLanceDTO lider = lideres.get(0);
            if (lider.jogadorId().equals(meuJogadorId)) return;

            BigDecimal minimoNecessario = lider.valor().add(BigDecimal.valueOf(1000));

            NumberFormat formatoMoeda = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
            formatoMoeda.setMinimumFractionDigits(2);
            formatoMoeda.setMaximumFractionDigits(2);

            if (meuValor.compareTo(minimoNecessario) < 0) {

                throw new RegraNegocioException(String.format(
                        "Poxa! O %s já ofertou D$ %s escolhendo o %s como %dª opção. " +
                                "Para assumir o lugar dele nessa fila, você precisa ofertar pelo menos D$ %s.",
                        lider.jogadorNome(),
                        formatoMoeda.format(lider.valor()),
                        clubeInfo.nome(),
                        minhaPrioridade,
                        formatoMoeda.format(minimoNecessario)
                ));
            }
        }
    }

    private void notificarFeed(Leilao leilao, JogadorLeilaoDTO jogador, ItemLanceDTO item, ClubeLeilaoDTO clubeInfo) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    FeedItemDTO evento = new FeedItemDTO(
                            jogador.id(), jogador.nome(), clubeInfo.id(), clubeInfo.nome(),
                            clubeInfo.imagem(), item.valor(), LocalDateTime.now()
                    );
                    messagingTemplate.convertAndSend("/topic/leilao/" + leilao.getId() + "/feed", evento);
                } catch (Exception e) {
                    log.warn("Falha ao publicar feed via WS", e);
                }
            }
        });
    }

    @Transactional
    public void resetarLancesDoJogador(String leilaoId, String jogadorId) {
        Leilao leilao = leilaoRepository.findById(leilaoId)
                .orElseThrow(() -> new RegraNegocioException("Leilão não encontrado"));
        if (!leilao.isAtivo()) throw new RegraNegocioException("Não é possível resetar lances de um leilão encerrado.");

        JogadorLeilaoDTO jogadorInfo = jogadorRepository.buscarParaLeilao(jogadorId)
                .orElseThrow(() -> new RegraNegocioException("Jogador não encontrado"));

        Set<String> clubesAfetados = lanceRepository.findByLeilaoIdAndJogadorId(leilaoId, jogadorId).stream()
                .map(l -> l.getClube().getId())
                .collect(Collectors.toSet());

        lanceRepository.deleteByLeilaoIdAndJogadorId(leilaoId, jogadorId);
        lanceRepository.flush();

        publicarAtualizacaoDeClubes(leilaoId, clubesAfetados);
    }

    @Transactional(readOnly = true)
    protected Map<String, LanceAlgoritmoDTO> executarAlgoritmoGaleShapley(String leilaoId) {
        return executarAlgoritmoGaleShapley(lanceRepository.buscarParaAlgoritmo(leilaoId));
    }

    private Map<String, LanceAlgoritmoDTO> executarAlgoritmoGaleShapley(List<LanceAlgoritmoDTO> todosLances) {
        if (todosLances.isEmpty()) return Collections.emptyMap();

        Map<String, Map<Integer, LanceAlgoritmoDTO>> porJogador = new HashMap<>();
        for (LanceAlgoritmoDTO l : todosLances) {
            porJogador.computeIfAbsent(l.jogadorId(), k -> new HashMap<>()).put(l.prioridade(), l);
        }

        Map<String, LanceAlgoritmoDTO> donosProvisorios = new HashMap<>();
        Map<String, Integer> tentativaAtual = new HashMap<>();
        Queue<String> filaLivres = new LinkedList<>();

        for (String jogadorId : porJogador.keySet()) {
            filaLivres.add(jogadorId);
            tentativaAtual.put(jogadorId, 1);
        }

        while (!filaLivres.isEmpty()) {
            String jogadorId = filaLivres.poll();
            int prioridadeTentada = tentativaAtual.get(jogadorId);
            if (prioridadeTentada > 20) continue;

            LanceAlgoritmoDTO meuLance = porJogador.get(jogadorId).get(prioridadeTentada);
            if (meuLance == null) {
                tentativaAtual.put(jogadorId, prioridadeTentada + 1);
                filaLivres.add(jogadorId);
                continue;
            }

            String clubeId = meuLance.clubeId();
            LanceAlgoritmoDTO donoAtual = donosProvisorios.get(clubeId);

            if (donoAtual == null) {
                donosProvisorios.put(clubeId, meuLance);
                continue;
            }

            boolean vitoriaDoDesafiante = meuLance.prioridade() < donoAtual.prioridade()
                    || (meuLance.prioridade().equals(donoAtual.prioridade()) && (
                    meuLance.valor().compareTo(donoAtual.valor()) > 0
                            || (meuLance.valor().compareTo(donoAtual.valor()) == 0
                            && meuLance.dataHoraLance().isBefore(donoAtual.dataHoraLance()))
            ));

            if (vitoriaDoDesafiante) {
                String idDonoAntigo = donoAtual.jogadorId();
                tentativaAtual.put(idDonoAntigo, tentativaAtual.get(idDonoAntigo) + 1);
                filaLivres.add(idDonoAntigo);
                donosProvisorios.put(clubeId, meuLance);
            } else {
                tentativaAtual.put(jogadorId, prioridadeTentada + 1);
                filaLivres.add(jogadorId);
            }
        }

        return donosProvisorios;
    }

    @Transactional(readOnly = true)
    public List<StatusLanceJogadorDTO> obterStatusDoJogador(String leilaoId, String jogadorId) {
        List<LanceAlgoritmoDTO> todosLances = lanceRepository.buscarParaAlgoritmo(leilaoId);
        Map<String, LanceAlgoritmoDTO> resultadoSimulado = executarAlgoritmoGaleShapley(todosLances);

        List<LanceAlgoritmoDTO> meusLances = todosLances.stream()
                .filter(l -> l.jogadorId().equals(jogadorId))
                .sorted(Comparator.comparing(LanceAlgoritmoDTO::prioridade))
                .toList();

        Integer prioridadeVencedora = resultadoSimulado.values().stream()
                .filter(l -> l.jogadorId().equals(jogadorId))
                .map(LanceAlgoritmoDTO::prioridade)
                .findFirst().orElse(999);

        return meusLances.stream()
                .map(l -> new StatusLanceJogadorDTO(
                        l.prioridade(),
                        l.clubeNome(),
                        l.valor(),
                        l.prioridade().equals(prioridadeVencedora) ? "GANHANDO"
                                : l.prioridade() < prioridadeVencedora ? "PERDENDO" : "ANULADO"
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResultadoParcialDTO> calcularResultadosParciais(String leilaoId) {
        Map<String, LanceAlgoritmoDTO> mapaVencedores = executarAlgoritmoGaleShapley(leilaoId);
        return mapaVencedores.values().stream()
                .sorted(Comparator.comparing(LanceAlgoritmoDTO::clubeNome))
                .map(l -> new ResultadoParcialDTO(l.clubeNome(), l.clubeImagem(), l.jogadorNome(), l.valor(), l.prioridade()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DisputaClubeDTO obterDetalhesDisputa(String leilaoId, String clubeId) {
        ClubeBasicoDTO clube = clubeRepository.buscarBasico(clubeId)
                .orElseThrow(() -> new RegraNegocioException("Clube não encontrado"));

        List<ItemDisputaDTO> ranking = lanceRepository.buscarDisputaProjetada(leilaoId, clubeId);

        return new DisputaClubeDTO(clube.id(), clube.nome(), clube.imagem(), ranking.size(), ranking);
    }

    @Transactional(readOnly = true)
    public List<LanceResumoDTO> obterLancesAtuais(String leilaoId) {
        return lanceRepository.encontrarMaioresLancesPorLeilao(leilaoId);
    }

    @Transactional(readOnly = true)
    public List<HistoricoLancesClubeDTO> obterHistoricoLances(String leilaoId, String clubeId, int page, int size) {
        return lanceRepository.buscarHistoricoLancesDoClube(leilaoId, clubeId, PageRequest.of(page, size)).getContent();
    }

    @Transactional(readOnly = true)
    public List<ResultadoLeilaoDTO> obterResultadoFinal(String leilaoId) {
        List<ResultadoLeilaoDTO> resultado = transferenciaRepository.buscarResultadoProjetado(leilaoId);
        if (resultado.isEmpty()) {
            throw new RegraNegocioException("O leilão ainda não foi finalizado ou não houve lances.");
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<LanceDetalheDTO> buscarLancesDoJogador(String leilaoId, String jogadorId) {
        return lanceRepository.findByLeilaoIdAndJogadorIdOrderByPrioridadeAsc(leilaoId, jogadorId).stream()
                .map(lance -> new LanceDetalheDTO(
                        lance.getId(), lance.getClube().getId(), lance.getClube().getNome(),
                        lance.getClube().getImagem(), lance.getClube().getLanceMinimo(),
                        lance.getValor(), lance.getPrioridade()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedItemDTO> obterFeedInicial(String leilaoId) {
        return lanceRepository.buscarUltimosLances(leilaoId);
    }

    @Transactional(readOnly = true)
    public List<ClubeDisputadoDTO> obterTermometro(String leilaoId) {
        return lanceRepository.buscarClubesMaisDisputados(leilaoId, PageRequest.of(0, 15));
    }

    @Transactional
    public void finalizarLeilao(String leilaoId) {
        Leilao leilao = leilaoRepository.findById(leilaoId)
                .orElseThrow(() -> new RegraNegocioException("Leilão não encontrado"));
        if (!leilao.isAtivo()) throw new RegraNegocioException("Leilão já finalizado.");

        Map<String, LanceAlgoritmoDTO> vencedores = executarAlgoritmoGaleShapley(leilaoId);
        List<String> logs = new ArrayList<>();

        if (vencedores.isEmpty()) {
            leilao.setAtivo(false);
            leilaoRepository.save(leilao);
            messagingTemplate.convertAndSend("/topic/leilao/" + leilaoId + "/resultado",
                    List.of("Leilão encerrado sem lances registrados."));
            return;
        }

        Set<String> jogadorIds = vencedores.values().stream().map(LanceAlgoritmoDTO::jogadorId).collect(Collectors.toSet());
        Set<String> clubeIds = vencedores.values().stream().map(LanceAlgoritmoDTO::clubeId).collect(Collectors.toSet());

        // Aqui o finalizarLeilao PRECISA das entidades completas mesmo (grava
        // Transferencia, chama jogadorService.atualizarSaldo, notificações...),
        // roda uma única vez por leilão (não a cada lance), então o custo de
        // carregar Jogador/Clube completos aqui é aceitável e intencional.
        Map<String, Jogador> jogadoresMap = jogadorRepository.findAllById(jogadorIds).stream()
                .collect(Collectors.toMap(Jogador::getId, j -> j));
        Map<String, Clube> clubesMap = clubeRepository.findAllById(clubeIds).stream()
                .collect(Collectors.toMap(Clube::getId, c -> c));

        java.text.NumberFormat nf = java.text.NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        for (LanceAlgoritmoDTO vencedor : vencedores.values()) {
            Jogador jogador = jogadoresMap.get(vencedor.jogadorId());
            Clube clube = clubesMap.get(vencedor.clubeId());
            BigDecimal valorFinal = vencedor.valor();

            try {
                MovimentacaoSaldoDTO debitoDTO = new MovimentacaoSaldoDTO(
                        valorFinal, "Compra do time " + clube.getNome() + " (Leilão)",
                        MovimentacaoSaldoDTO.TipoOperacao.REMOVER, true);
                jogadorService.atualizarSaldo(jogador.getId(), debitoDTO, "SISTEMA_LEILAO");
            } catch (Exception e) {
                logs.add("ERRO CRÍTICO: Falha ao debitar saldo do jogador " + jogador.getNome() + ": " + e.getMessage());
            }

            Transferencia transferencia = new Transferencia();
            transferencia.setLeilao(leilao);
            transferencia.setJogador(jogador);
            transferencia.setClube(clube);
            transferencia.setValorPago(valorFinal);
            transferencia.setDataCompra(LocalDateTime.now());
            transferenciaRepository.save(transferencia);

            logs.add("O jogador " + jogador.getNome() + " assumiu o " + clube.getNome() + " por D$ " + valorFinal);

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
                String link = linkFront + "/" + leilao.getTemporada().getId() + "/torneios/jogadores";
                notificacaoService.enviarParaJogador(jogador, "Clube Conquistado!",
                        String.format("Parabéns! Você venceu o leilão e assumiu o %s por %s na %s.",
                                clube.getNome(), valorFormatado, leilao.getTemporada().getNome()),
                        link, TipoNotificacao.LEILAO);
            } catch (Exception e) {
                log.error("Erro ao notificar vencedor do leilão: {}", jogador.getNome(), e);
            }
        }

        leilao.setAtivo(false);
        leilaoRepository.save(leilao);

        messagingTemplate.convertAndSend("/topic/leilao/" + leilaoId + "/resultado", logs);
        messagingTemplate.convertAndSend("/topic/leilao/status", "FECHADO");
    }

    private void verificarEEnviarNotificacoesDePerda(
            String leilaoId,
            Map<String, LanceAlgoritmoDTO> antes,
            Map<String, LanceAlgoritmoDTO> depois,
            String jogadorQueDeuLanceAgoraId) {

        Leilao leilao = leilaoRepository.findById(leilaoId).orElse(null);
        if (leilao == null) return;

        String temporadaId = leilao.getTemporada().getId();

        for (Map.Entry<String, LanceAlgoritmoDTO> entryAntes : antes.entrySet()) {
            LanceAlgoritmoDTO ganhadorAntigo = entryAntes.getValue();
            String jogadorAntigoId = ganhadorAntigo.jogadorId();

            if (jogadorAntigoId.equals(jogadorQueDeuLanceAgoraId)) continue;

            LanceAlgoritmoDTO ganhadorNovo = depois.get(entryAntes.getKey());
            boolean perdeuPosicao = ganhadorNovo == null || !ganhadorNovo.jogadorId().equals(jogadorAntigoId);

            if (perdeuPosicao) {
                jogadorRepository.buscarParaLeilao(jogadorAntigoId).ifPresent(jogadorAntigo -> {
                    String nomeClube = ganhadorAntigo.clubeNome();
                    String link = linkFront + "/" + temporadaId + "/torneios/leilao";
                    notificacaoService.enviarParaJogador(
                            jogadorRepository.getReferenceById(jogadorAntigo.id()),
                            "Atenção: Você perdeu " + nomeClube,
                            "A configuração do leilão mudou e você não é mais o vencedor provisório do " + nomeClube + ".",
                            link,
                            TipoNotificacao.ALERTA);
                });
            }
        }
    }
}