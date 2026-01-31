package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeilaoService {

    @Autowired private LanceRepository lanceRepository;
    @Autowired private JogadorRepository jogadorRepository;
    @Autowired private ClubeRepository clubeRepository;
    @Autowired private LeilaoRepository leilaoRepository;
    @Autowired private TemporadaRepository temporadaRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private TransferenciaRepository transferenciaRepository;

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
        Leilao leilao = leilaoRepository.findById(dto.leilaoId())
                .orElseThrow(() -> new RuntimeException("Leilão não encontrado"));

        if (!leilao.isAtivo()) throw new RuntimeException("O leilão não está ativo.");
        if (LocalDateTime.now().isAfter(leilao.getDataFim())) throw new RuntimeException("O tempo acabou.");

        Jogador jogador = jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado"));

        BigDecimal maiorValorOfertado = dto.preferencias().stream()
                .map(ItemLanceDTO::valor)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        if (jogador.getSaldoVirtual().compareTo(maiorValorOfertado) < 0) {
            throw new RuntimeException("Saldo insuficiente. Seu saldo é " + jogador.getSaldoVirtual() +
                    " mas sua maior oferta é " + maiorValorOfertado);
        }

        Set<Integer> prioridadesRecebidas = dto.preferencias().stream()
                .map(ItemLanceDTO::prioridade)
                .collect(Collectors.toSet());

        List<Lance> lancesNoBanco = lanceRepository.findByLeilaoAndJogador(leilao, jogador);

        for (Lance lanceAntigo : lancesNoBanco) {
            if (!prioridadesRecebidas.contains(lanceAntigo.getPrioridade())) {
                lanceRepository.delete(lanceAntigo);
            }
        }

        for (ItemLanceDTO item : dto.preferencias()) {
            Clube clubeAlvo = clubeRepository.findById(item.clubeId())
                    .orElseThrow(() -> new RuntimeException("Clube inválido: " + item.clubeId()));

            if (item.valor().compareTo(clubeAlvo.getLanceMinimo()) < 0) {
                throw new RuntimeException("O lance para " + clubeAlvo.getNome() +
                        " deve ser no mínimo " + clubeAlvo.getLanceMinimo());
            }

            Optional<Lance> meuLanceNessaPrioridade = lancesNoBanco.stream()
                    .filter(l -> l.getPrioridade().equals(item.prioridade()))
                    .findFirst();

            if (meuLanceNessaPrioridade.isPresent()) {
                Lance lanceExistente = meuLanceNessaPrioridade.get();
                boolean mesmoClube = lanceExistente.getClube().getId().equals(clubeAlvo.getId());

                if (mesmoClube) {
                    if (item.valor().compareTo(lanceExistente.getValor()) < 0) {
                        throw new RuntimeException("Na prioridade " + item.prioridade() +
                                " (" + clubeAlvo.getNome() + "), você não pode diminuir o valor ofertado.");
                    }
                } else {
                    validarSeSuperaLider(leilao, clubeAlvo, jogadorId, item.valor());
                }

                lanceExistente.setClube(clubeAlvo);
                lanceExistente.setValor(item.valor());
                lanceExistente.setDataHoraLance(LocalDateTime.now());
                lanceRepository.save(lanceExistente);

            } else {
                validarSeSuperaLider(leilao, clubeAlvo, jogadorId, item.valor());

                Lance novoLance = new Lance(leilao, jogador, clubeAlvo, item.valor(), item.prioridade());
                lanceRepository.save(novoLance);
            }
        }

        messagingTemplate.convertAndSend("/topic/leilao/" + leilao.getId() + "/feed",
                new NotificacaoLanceDTO(jogador.getNome(), LocalDateTime.now()));
    }

    private void validarSeSuperaLider(Leilao leilao, Clube clube, String meuJogadorId, BigDecimal meuValor) {
        Optional<Lance> liderAtualOpt = lanceRepository.findTopByLeilaoAndClubeOrderByValorDesc(leilao, clube);

        if (liderAtualOpt.isPresent()) {
            Lance liderAtual = liderAtualOpt.get();

            if (!liderAtual.getJogador().getId().equals(meuJogadorId)) {
                BigDecimal valorMinimoNecessario = liderAtual.getValor().add(INCREMENTO_MINIMO);

                if (meuValor.compareTo(valorMinimoNecessario) < 0) {
                    throw new RuntimeException("Para entrar na disputa pelo " + clube.getNome() +
                            ", seu lance deve superar o líder atual. Mínimo: " + valorMinimoNecessario);
                }
            }
        }
    }

    @Transactional
    public void finalizarLeilao(String leilaoId) {
        Leilao leilao = leilaoRepository.findById(leilaoId)
                .orElseThrow(() -> new RuntimeException("Leilão não encontrado"));

        if (!leilao.isAtivo()) return;

        Map<String, Lance> vencedores = executarAlgoritmoGaleShapley(leilaoId);

        List<String> logsResultado = new ArrayList<>();

        for (Lance lanceVencedor : vencedores.values()) {
            Jogador jogador = lanceVencedor.getJogador();
            Clube clube = lanceVencedor.getClube();
            BigDecimal valor = lanceVencedor.getValor();

            jogador.setSaldoVirtual(jogador.getSaldoVirtual().subtract(valor));

            Transferencia transferencia = new Transferencia(leilao, jogador, clube, valor);
            transferenciaRepository.save(transferencia);

            jogadorRepository.save(jogador);
            clubeRepository.save(clube);

            logsResultado.add(jogador.getNome() + " levou " + clube.getNome() + " por " + valor);
        }

        leilao.setAtivo(false);
        leilaoRepository.save(leilao);

        messagingTemplate.convertAndSend("/topic/leilao/" + leilao.getId() + "/resultado", logsResultado);
    }

    public List<ResultadoLeilaoDTO> obterResultadoFinal(String leilaoId) {
        List<Transferencia> transferencias = transferenciaRepository.findByLeilaoIdOrderByValorPagoDesc(leilaoId);

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
     * Algoritmo Central: Deferred Acceptance (Gale-Shapley)
     * Retorna um Mapa onde a Chave é o ID do Clube e o Valor é o Lance Vencedor.
     */
    private Map<String, Lance> executarAlgoritmoGaleShapley(String leilaoId) {
        List<Lance> todosLances = lanceRepository.findAllByLeilaoId(leilaoId);

        Map<String, Lance> donosProvisorios = new HashMap<>();
        Map<String, Integer> tentativaAtual = new HashMap<>();
        Queue<String> fila = new LinkedList<>();

        todosLances.stream().map(l -> l.getJogador().getId()).distinct().forEach(id -> {
            fila.add(id);
            tentativaAtual.put(id, 1);
        });

        while (!fila.isEmpty()) {
            String jogadorId = fila.poll();
            Integer prioridade = tentativaAtual.get(jogadorId);

            if (prioridade > 5) continue;

            Optional<Lance> lanceOpt = todosLances.stream()
                    .filter(l -> l.getJogador().getId().equals(jogadorId) && l.getPrioridade().equals(prioridade))
                    .findFirst();

            if (lanceOpt.isEmpty()) {
                tentativaAtual.put(jogadorId, prioridade + 1);
                fila.add(jogadorId);
                continue;
            }

            Lance meuLance = lanceOpt.get();
            String clubeId = meuLance.getClube().getId();
            Lance donoAtual = donosProvisorios.get(clubeId);

            if (donoAtual == null) {
                donosProvisorios.put(clubeId, meuLance);
            } else {
                int comparacao = meuLance.getValor().compareTo(donoAtual.getValor());
                boolean vitoriaDoDesafiante = false;

                if (comparacao > 0) {
                    vitoriaDoDesafiante = true;
                } else if (comparacao == 0) {
                    if (meuLance.getDataHoraLance().isBefore(donoAtual.getDataHoraLance())) {
                        vitoriaDoDesafiante = true;
                    }
                }

                if (vitoriaDoDesafiante) {
                    String idAntigo = donoAtual.getJogador().getId();
                    tentativaAtual.put(idAntigo, tentativaAtual.get(idAntigo) + 1);
                    fila.add(idAntigo);
                    donosProvisorios.put(clubeId, meuLance);
                } else {
                    tentativaAtual.put(jogadorId, prioridade + 1);
                    fila.add(jogadorId);
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
        Clube clube = clubeRepository.findById(clubeId)
                .orElseThrow(() -> new RuntimeException("Clube não encontrado"));

        List<Lance> lancesDoClube = lanceRepository.findByLeilaoIdAndClubeIdOrderByValorDesc(leilaoId, clubeId);

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
}