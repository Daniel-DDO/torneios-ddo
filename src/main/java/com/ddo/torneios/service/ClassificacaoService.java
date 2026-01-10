package com.ddo.torneios.service;

import com.ddo.torneios.dto.LinhaClassificacaoDTO;
import com.ddo.torneios.dto.PartidaDTO;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class ClassificacaoService {

    @Autowired
    private ParticipacaoFaseRepository participacaoRepository;
    @Autowired
    private FaseTorneioRepository faseRepository;
    @Autowired
    private PartidaRepository partidaRepository;
    @Autowired
    private JogadorClubeRepository jogadorClubeRepository;
    @Autowired
    private JogadorRepository jogadorRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private EconomiaService economiaService;
    @Autowired
    private InsigniaService insigniaService;
    @Autowired
    private BracketService bracketService;

    @Transactional
    public void registrarResultado(PartidaDTO dto) {
        if (!dto.realizada()) return;

        Partida partida = partidaRepository.findById(dto.id())
                .orElseThrow(() -> new RuntimeException("Partida não encontrada"));

        if (partida.isRealizada()) {
            log.warn("Tentativa de registrar resultado em partida já realizada: {}", dto.id());
            return;
        }

        FaseTorneio fase = partida.getFase();

        Integer valorCompeticao = fase.getTorneio().getCompeticao().getValor();
        if (valorCompeticao == null) valorCompeticao = 100;

        ParticipacaoFase pMandante = encontrarParticipacao(fase.getId(), partida.getMandante().getId());
        ParticipacaoFase pVisitante = encontrarParticipacao(fase.getId(), partida.getVisitante().getId());

        BigDecimal coefM = calcularCoeficiente(
                dto.golsMandante(), dto.golsVisitante(),
                dto.golsMandante() > dto.golsVisitante(),
                dto.golsMandante().equals(dto.golsVisitante()),
                dto.golsMandante() < dto.golsVisitante(),
                dto.cartoesAmarelosMandante(), dto.cartoesVermelhosMandante(),
                partida.getMandante().getClube().getEstrelas(),
                valorCompeticao
        );

        BigDecimal coefV = calcularCoeficiente(
                dto.golsVisitante(), dto.golsMandante(),
                dto.golsVisitante() > dto.golsMandante(),
                dto.golsVisitante().equals(dto.golsMandante()),
                dto.golsVisitante() < dto.golsMandante(),
                dto.cartoesAmarelosVisitante(), dto.cartoesVermelhosVisitante(),
                partida.getVisitante().getClube().getEstrelas(),
                valorCompeticao
        );

        partida.setDataHora(LocalDateTime.now());
        partida.setCoeficienteMandante(coefM);
        partida.setCoeficienteVisitante(coefV);
        partida.setGolsMandante(dto.golsMandante());
        partida.setGolsVisitante(dto.golsVisitante());
        partida.setRealizada(true);
        partida.setWo(dto.wo());
        partida.setCartoesAmarelosMandante(dto.cartoesAmarelosMandante());
        partida.setCartoesVermelhosMandante(dto.cartoesVermelhosMandante());
        partida.setCartoesAmarelosVisitante(dto.cartoesAmarelosVisitante());
        partida.setCartoesVermelhosVisitante(dto.cartoesVermelhosVisitante());

        atribuirHistoricoJogadores(partida, pMandante, pVisitante);

        JogadorClube jcMandante = partida.getMandante();
        JogadorClube jcVisitante = partida.getVisitante();
        Jogador jGlobalMandante = jcMandante.getJogador();
        Jogador jGlobalVisitante = jcVisitante.getJogador();

        jcMandante.setPontosCoeficiente(safeAdd(jcMandante.getPontosCoeficiente(), coefM));
        jcVisitante.setPontosCoeficiente(safeAdd(jcVisitante.getPontosCoeficiente(), coefV));

        jGlobalMandante.setPontosCoeficiente(safeAdd(jGlobalMandante.getPontosCoeficiente(), coefM));
        jGlobalVisitante.setPontosCoeficiente(safeAdd(jGlobalVisitante.getPontosCoeficiente(), coefV));

        if (fase.getTipoTorneio() == TipoTorneio.MATA_MATA) {
            processarMataMata(dto, pMandante, pVisitante);
            bracketService.processarAvancoVencedor(partida);
        } else {
            processarLiga(dto, pMandante, pVisitante);
        }

        partidaRepository.save(partida);
        jogadorClubeRepository.saveAll(List.of(jcMandante, jcVisitante));
        jogadorRepository.saveAll(List.of(jGlobalMandante, jGlobalVisitante));
        participacaoRepository.saveAll(List.of(pMandante, pVisitante));

        economiaService.processarEconomiaPartida(partida);

        List<LinhaClassificacaoDTO> novaClassificacao = calcularClassificacao(fase);
        atualizarPosicoesNoBanco(novaClassificacao, fase);

        insigniaService.processarPosPartida(jGlobalMandante,dto.golsMandante());
        insigniaService.processarPosPartida(jGlobalVisitante, dto.golsVisitante());

        try {

            String topico = "/topic/classificacao/" + fase.getId();
            messagingTemplate.convertAndSend(topico, novaClassificacao);

            log.info("Classificação atualizada e enviada via WebSocket para a fase: {}", fase.getId());
        } catch (Exception e) {
            log.error("Erro ao enviar atualização de classificação via WebSocket", e);
        }
    }

    private void atualizarPosicoesNoBanco(List<LinhaClassificacaoDTO> classificacao, FaseTorneio fase) {
        List<ParticipacaoFase> participacoes = fase.getParticipacoes();
        boolean houveAlteracao = false;

        for (LinhaClassificacaoDTO linha : classificacao) {
            Optional<ParticipacaoFase> match = participacoes.stream()
                    .filter(p -> p.getJogadorClube().getId().equals(linha.jogadorClubeId()))
                    .findFirst();

            if (match.isPresent()) {
                ParticipacaoFase p = match.get();
                if (!Objects.equals(p.getPosicaoClassificacao(), linha.posicao())) {
                    p.setPosicaoClassificacao(linha.posicao());
                    houveAlteracao = true;
                }
            }
        }

        if (houveAlteracao) {
            participacaoRepository.saveAll(participacoes);
        }
    }

    /**
     * Atualiza estatísticas gerais (Gols, Jogos, Cartões, Vitórias) em todas as camadas.
     */
    private void atribuirHistoricoJogadores(Partida partida, ParticipacaoFase pMandante, ParticipacaoFase pVisitante) {
        JogadorClube jcMandante = pMandante.getJogadorClube();
        JogadorClube jcVisitante = pVisitante.getJogadorClube();
        Jogador jMandante = jcMandante.getJogador();
        Jogador jVisitante = jcVisitante.getJogador();

        int gm = safeInt(partida.getGolsMandante());
        int gv = safeInt(partida.getGolsVisitante());
        int cam = safeInt(partida.getCartoesAmarelosMandante());
        int cvm = safeInt(partida.getCartoesVermelhosMandante());
        int cav = safeInt(partida.getCartoesAmarelosVisitante());
        int cvv = safeInt(partida.getCartoesVermelhosVisitante());

        atualizarStatsEntidades(pMandante, jcMandante, jMandante, gm, gv, cam, cvm);
        atualizarStatsEntidades(pVisitante, jcVisitante, jVisitante, gv, gm, cav, cvv);

        if (gm > gv) {
            incrementarResultado(pMandante, jcMandante, jMandante, 1, 0, 0); //vitoria mandante
            incrementarResultado(pVisitante, jcVisitante, jVisitante, 0, 0, 1); //derrota visitante
        } else if (gv > gm) {
            incrementarResultado(pMandante, jcMandante, jMandante, 0, 0, 1); //derrota mandante
            incrementarResultado(pVisitante, jcVisitante, jVisitante, 1, 0, 0); //vitoria visitante
        } else {
            incrementarResultado(pMandante, jcMandante, jMandante, 0, 1, 0); //empate
            incrementarResultado(pVisitante, jcVisitante, jVisitante, 0, 1, 0); //empate
        }
    }

    private void atualizarStatsEntidades(ParticipacaoFase pf, JogadorClube jc, Jogador j, int golsPro, int golsContra, int ca, int cv) {
        pf.setPartidasJogadas(safeInt(pf.getPartidasJogadas()) + 1);
        jc.setPartidasJogadas(safeInt(jc.getPartidasJogadas()) + 1);
        j.setPartidasJogadas(safeInt(j.getPartidasJogadas()) + 1);

        pf.setGolsPro(safeInt(pf.getGolsPro()) + golsPro);
        jc.setTotalGolsMarcados(safeInt(jc.getTotalGolsMarcados()) + golsPro);
        j.setGolsMarcados(safeInt(j.getGolsMarcados()) + golsPro);

        pf.setGolsContra(safeInt(pf.getGolsContra()) + golsContra);
        jc.setTotalGolsSofridos(safeInt(jc.getTotalGolsSofridos()) + golsContra);
        j.setGolsSofridos(safeInt(j.getGolsSofridos()) + golsContra);

        pf.setSaldoGols(pf.getGolsPro() - pf.getGolsContra());

        jc.setTotalCartoesAmarelos(safeInt(jc.getTotalCartoesAmarelos()) + ca);
        jc.setTotalCartoesVermelhos(safeInt(jc.getTotalCartoesVermelhos()) + cv);

        j.setCartoesAmarelos(safeLong(j.getCartoesAmarelos()) + ca);
        j.setCartoesVermelhos(safeLong(j.getCartoesVermelhos()) + cv);
    }

    private void incrementarResultado(ParticipacaoFase pf, JogadorClube jc, Jogador j, int v, int e, int d) {
        pf.setVitorias(safeInt(pf.getVitorias()) + v);
        pf.setEmpates(safeInt(pf.getEmpates()) + e);
        pf.setDerrotas(safeInt(pf.getDerrotas()) + d);

        jc.setVitorias(safeInt(jc.getVitorias()) + v);
        jc.setEmpates(safeInt(jc.getEmpates()) + e);
        jc.setDerrotas(safeInt(jc.getDerrotas()) + d);

        j.setVitorias(safeInt(j.getVitorias()) + v);
        j.setEmpates(safeInt(j.getEmpates())+  e);
        j.setDerrotas(safeInt(j.getDerrotas()) + d);
    }

    private void processarLiga(PartidaDTO dto, ParticipacaoFase m, ParticipacaoFase v) {
        //Os gols e saldos já foram atualizados em 'atribuirHistoricoJogadores'

        int gm = safeInt(dto.golsMandante());
        int gv = safeInt(dto.golsVisitante());

        if (gm > gv) {
            m.setPontos(safeInt(m.getPontos()) + 3);
        } else if (gm < gv) {
            v.setPontos(safeInt(v.getPontos()) + 3);
        } else {
            m.setPontos(safeInt(m.getPontos()) + 1);
            v.setPontos(safeInt(v.getPontos()) + 1);
        }
    }

    private void processarMataMata(PartidaDTO dto, ParticipacaoFase pMandante, ParticipacaoFase pVisitante) {
        Partida partida = partidaRepository.findById(dto.id())
                .orElseThrow(() -> new RuntimeException("Partida não encontrada"));

        FaseTorneio fase = partida.getFase();
        FaseMataMata etapa = partida.getEtapaMataMata();
        Integer chave = partida.getChaveIndex();

        //Se ainda tem jogo pendente (ex: acabou de jogar a IDA), não muda status de classificação de ninguém ainda.
        if (partidaRepository.existeJogoPendente(fase, etapa, chave)) {
            return;
        }

        List<Partida> jogosDoConfronto = partidaRepository.findByFaseAndEtapaMataMataAndChaveIndex(fase, etapa, chave);

        JogadorClube vencedorJc = calcularVencedorConfronto(jogosDoConfronto);

        ParticipacaoFase venceu;
        ParticipacaoFase perdeu;

        if (pMandante.getJogadorClube().equals(vencedorJc)) {
            venceu = pMandante;
            perdeu = pVisitante;
        } else {
            venceu = pVisitante;
            perdeu = pMandante;
        }

        perdeu.setStatusClassificacao(StatusClassificacao.ELIMINADO);

        if (dto.etapaMataMata() != null) {
            try {
                FaseMataMata atual = FaseMataMata.valueOf(dto.etapaMataMata());
                venceu.setStatusClassificacao(definirProximoStatus(atual));
            } catch (Exception ignored) {
            }
        }
    }

    private JogadorClube calcularVencedorConfronto(List<Partida> jogos) {
        if (jogos.isEmpty()) return null;
        if (jogos.size() == 1) return jogos.get(0).getVencedor();

        //Lógica para IDA e VOLTA
        Partida ida = jogos.stream().filter(p -> isIda(p.getTipoPartida())).findFirst().orElse(null);
        Partida volta = jogos.stream().filter(p -> isVolta(p.getTipoPartida())).findFirst().orElse(null);

        if (ida == null || volta == null) return null; // Inconsistência

        JogadorClube timeA = ida.getMandante();
        JogadorClube timeB = ida.getVisitante();

        int golsTimeA = (ida.getGolsMandante() != null ? ida.getGolsMandante() : 0);
        int golsTimeB = (ida.getGolsVisitante() != null ? ida.getGolsVisitante() : 0);

        if (volta.getMandante().equals(timeA)) {
            golsTimeA += (volta.getGolsMandante() != null ? volta.getGolsMandante() : 0);
            golsTimeB += (volta.getGolsVisitante() != null ? volta.getGolsVisitante() : 0);
        } else {
            golsTimeA += (volta.getGolsVisitante() != null ? volta.getGolsVisitante() : 0);
            golsTimeB += (volta.getGolsMandante() != null ? volta.getGolsMandante() : 0);
        }

        if (golsTimeA > golsTimeB) return timeA;
        if (golsTimeB > golsTimeA) return timeB;

        return volta.getVencedor();
    }

    private boolean isIda(TipoPartida tp) {
        return tp == TipoPartida.MATA_MATA_IDA || tp == TipoPartida.FINAL_IDA;
    }

    private boolean isVolta(TipoPartida tp) {
        return tp == TipoPartida.MATA_MATA_VOLTA || tp == TipoPartida.FINAL_VOLTA;
    }

    private BigDecimal calcularCoeficiente(
            Integer golsM, Integer golsS, boolean vit, boolean emp, boolean der,
            Integer ca, Integer cv, BigDecimal estrelas, Integer valorTorneio
    ) {
        double gm = safeInt(golsM);
        double gs = safeInt(golsS);
        double amt = safeInt(ca);
        double vrm = safeInt(cv);
        double nivelTime = estrelas != null ? estrelas.doubleValue() : 1.0;
        double pesoTorneio = valorTorneio != null ? valorTorneio / 100.0 : 1.0;

        double pontosGols = Math.min(gm, 6.0);
        double pontosResultadoPos = vit ? 4.0 : (emp ? 2.0 : 0.0);
        double pontosGoleada = (gm - gs > 3) ? 2.0 : 0.0;
        double pontosCleanSheet = (gs == 0) ? 2.0 : 0.0;

        double positivos = pontosGols + pontosResultadoPos + pontosGoleada + pontosCleanSheet;

        double pontosResultadoNeg = der ? -1.0 : 0.0;
        double penalidadeAmarelos = Math.max(0, amt - 2) * -0.5;
        double penalidadeVermelhos = vrm * -2.0;
        double penalidadeGolsSofridos = gs * -0.5;

        double negativos = pontosResultadoNeg + penalidadeAmarelos + penalidadeVermelhos + penalidadeGolsSofridos;

        double multiplicadorNegativos = 1.0 + (nivelTime - 1.0) / 4.0;
        double negativosAjustados = negativos * multiplicadorNegativos;

        double pontosTotais = (positivos + negativosAjustados) * pesoTorneio;

        return BigDecimal.valueOf(Math.max(pontosTotais, -8.0)).setScale(2, RoundingMode.HALF_UP);
    }

    private StatusClassificacao definirProximoStatus(FaseMataMata etapaAtual) {
        return switch (etapaAtual) {
            case OITAVAS -> StatusClassificacao.QUARTAS;
            case QUARTAS -> StatusClassificacao.SEMIFINALISTA;
            case SEMIFINAL -> StatusClassificacao.FINALISTA;
            case FINAL -> StatusClassificacao.CAMPEAO;
            default -> StatusClassificacao.ATIVO;
        };
    }

    private ParticipacaoFase encontrarParticipacao(String fId, String jcId) {
        return participacaoRepository.findByFaseIdAndJogadorClubeId(fId, jcId)
                .orElseThrow(() -> new RuntimeException("Participação não encontrada"));
    }

    private BigDecimal safeAdd(BigDecimal base, BigDecimal toAdd) {
        if (base == null) base = BigDecimal.ZERO;
        if (toAdd == null) toAdd = BigDecimal.ZERO;
        return base.add(toAdd);
    }

    private Integer safeInt(Integer v) { return v == null ? 0 : v; }
    private Long safeLong(Long v) { return v == null ? 0L : v; }

    public List<LinhaClassificacaoDTO> calcularClassificacao(FaseTorneio fase) {
        List<Partida> partidas = partidaRepository.findByFaseAndRealizadaTrue(fase);
        Map<String, AcumuladorStatus> mapa = new HashMap<>();

        fase.getParticipacoes().forEach(p -> {
            String id = p.getJogadorClube().getId();
            AcumuladorStatus acc = new AcumuladorStatus();
            acc.setJogadorClubeId(id);
            acc.setNomeJogador(p.getJogadorClube().getJogador().getNome());
            acc.setNomeClube(p.getJogadorClube().getClube().getNome());
            acc.setImagemClube(p.getJogadorClube().getClube().getImagem());
            mapa.put(id, acc);
        });

        for (Partida p : partidas) {
            acumularPartida(mapa, p);
        }

        List<AcumuladorStatus> ordenados = mapa.values().stream()
                .sorted((a, b) -> {
                    //Pontos
                    if (b.getPontos() != a.getPontos()) return b.getPontos() - a.getPontos();
                    //Saldo de Gols
                    if (b.getSaldo() != a.getSaldo()) return b.getSaldo() - a.getSaldo();
                    //Vitórias
                    if (b.getVitorias() != a.getVitorias()) return b.getVitorias() - a.getVitorias();
                    //Gols Pró
                    if (b.getGolsPro() != a.getGolsPro()) return b.getGolsPro() - a.getGolsPro();
                    //Gols Contra (Menos é melhor)
                    if (a.getGolsContra() != b.getGolsContra()) return a.getGolsContra() - b.getGolsContra();
                    //Cartões Amarelos (Menos é melhor)
                    if (a.getAmarelos() != b.getAmarelos()) return a.getAmarelos() - b.getAmarelos();
                    //Cartões Vermelhos (Menos é melhor)
                    if (a.getVermelhos() != b.getVermelhos()) return a.getVermelhos() - b.getVermelhos();

                    //confronto Direto (se tudo acima empatar)
                    return compararConfrontoDireto(a, b, partidas);
                })
                .toList();

        return atribuirZonasEPosicao(ordenados, fase);
    }

    private void acumularPartida(Map<String, AcumuladorStatus> mapa, Partida p) {
        AcumuladorStatus m = mapa.get(p.getMandante().getId());
        AcumuladorStatus v = mapa.get(p.getVisitante().getId());

        int gM = p.getGolsMandante() != null ? p.getGolsMandante() : 0;
        int gV = p.getGolsVisitante() != null ? p.getGolsVisitante() : 0;

        //gols e jogos
        m.jogos++; v.jogos++;
        m.golsPro += gM; m.golsContra += gV;
        v.golsPro += gV; v.golsContra += gM;

        //cartões (Null-safe)
        m.amarelos += (p.getCartoesAmarelosMandante() != null ? p.getCartoesAmarelosMandante() : 0);
        m.vermelhos += (p.getCartoesVermelhosMandante() != null ? p.getCartoesVermelhosMandante() : 0);
        v.amarelos += (p.getCartoesAmarelosVisitante() != null ? p.getCartoesAmarelosVisitante() : 0);
        v.vermelhos += (p.getCartoesVermelhosVisitante() != null ? p.getCartoesVermelhosVisitante() : 0);

        //pontuação
        if (gM > gV) {
            m.pontos += 3; m.vitorias++; v.derrotas++;
        } else if (gV > gM) {
            v.pontos += 3; v.vitorias++; m.derrotas++;
        } else {
            m.pontos += 1; v.pontos += 1; m.empates++; v.empates++;
        }
    }

    private int compararConfrontoDireto(AcumuladorStatus a, AcumuladorStatus b, List<Partida> partidas) {
        int pontosA = 0;
        int pontosB = 0;

        for (Partida p : partidas) {
            String mId = p.getMandante().getId();
            String vId = p.getVisitante().getId();

            if ((mId.equals(a.getJogadorClubeId()) && vId.equals(b.getJogadorClubeId())) ||
                    (mId.equals(b.getJogadorClubeId()) && vId.equals(a.getJogadorClubeId()))) {

                int gM = p.getGolsMandante();
                int gV = p.getGolsVisitante();

                if (gM > gV) {
                    if (mId.equals(a.getJogadorClubeId())) pontosA += 3; else pontosB += 3;
                } else if (gV > gM) {
                    if (vId.equals(a.getJogadorClubeId())) pontosA += 3; else pontosB += 3;
                } else {
                    pontosA += 1; pontosB += 1;
                }
            }
        }
        return pontosB - pontosA;
    }

    private List<LinhaClassificacaoDTO> atribuirZonasEPosicao(List<AcumuladorStatus> lista, FaseTorneio fase) {
        List<LinhaClassificacaoDTO> resultado = new ArrayList<>();

        for (int i = 0; i < lista.size(); i++) {
            int pos = i + 1;
            AcumuladorStatus acc = lista.get(i);

            ZonaFase zona = (fase.getZonas() == null) ? null : fase.getZonas().stream()
                    .filter(z -> pos >= z.getPosicaoDe() && pos <= z.getPosicaoAte())
                    .findFirst().orElse(null);

            resultado.add(new LinhaClassificacaoDTO(
                    pos, acc.jogadorClubeId, acc.nomeJogador, acc.nomeClube, acc.imagemClube,
                    acc.pontos, acc.jogos, acc.vitorias, acc.empates, acc.derrotas,
                    acc.golsPro, acc.golsContra, acc.getSaldo(),
                    zona != null ? zona.getNome() : "",
                    zona != null ? zona.getCorHex() : "#FFFFFF"
            ));
        }
        return resultado;
    }

    public void recalcularETransmitir(FaseTorneio fase) {
        List<LinhaClassificacaoDTO> dtos = calcularClassificacao(fase);

        String topico = "/topic/classificacao/" + fase.getId();
        messagingTemplate.convertAndSend(topico, dtos);
    }

    @Getter
    @Setter
    class AcumuladorStatus {
        int pontos, jogos, vitorias, empates, derrotas, golsPro, golsContra, amarelos, vermelhos;
        String jogadorClubeId, nomeJogador, nomeClube, imagemClube;

        int getSaldo() { return golsPro - golsContra; }
    }

    @Transactional
    public void desfazerResultado(String partidaId) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new RuntimeException("Partida não encontrada"));

        if (!partida.isRealizada()) return;

        FaseTorneio fase = partida.getFase();
        ParticipacaoFase pMandante = encontrarParticipacao(fase.getId(), partida.getMandante().getId());
        ParticipacaoFase pVisitante = encontrarParticipacao(fase.getId(), partida.getVisitante().getId());

        removerHistoricoJogadores(partida, pMandante, pVisitante);

        if (fase.getTipoTorneio() == TipoTorneio.MATA_MATA) {
            reverterMataMata(pMandante, pVisitante);
        } else {
            reverterPontosLiga(partida, pMandante, pVisitante);
        }

        reverterCoeficientes(partida);
        economiaService.estornarEconomiaPartida(partida);

        limparDadosPartida(partida);

        partidaRepository.save(partida);
        participacaoRepository.saveAll(List.of(pMandante, pVisitante));

        jogadorClubeRepository.saveAll(List.of(pMandante.getJogadorClube(), pVisitante.getJogadorClube()));
        jogadorRepository.saveAll(List.of(pMandante.getJogadorClube().getJogador(), pVisitante.getJogadorClube().getJogador()));

        List<LinhaClassificacaoDTO> novaClassificacao = calcularClassificacao(fase);
        atualizarPosicoesNoBanco(novaClassificacao, fase);

        try {
            String topico = "/topic/classificacao/" + fase.getId();
            messagingTemplate.convertAndSend(topico, novaClassificacao);
            log.info("Desfazer partida: Classificação atualizada para a fase: {}", fase.getId());
        } catch (Exception e) {
            log.error("Erro ao enviar atualização após desfazer partida", e);
        }
    }

    private void removerHistoricoJogadores(Partida partida, ParticipacaoFase pMandante, ParticipacaoFase pVisitante) {
        JogadorClube jcMandante = pMandante.getJogadorClube();
        JogadorClube jcVisitante = pVisitante.getJogadorClube();
        Jogador jMandante = jcMandante.getJogador();
        Jogador jVisitante = jcVisitante.getJogador();

        int gm = safeInt(partida.getGolsMandante());
        int gv = safeInt(partida.getGolsVisitante());
        int cam = safeInt(partida.getCartoesAmarelosMandante());
        int cvm = safeInt(partida.getCartoesVermelhosMandante());
        int cav = safeInt(partida.getCartoesAmarelosVisitante());
        int cvv = safeInt(partida.getCartoesVermelhosVisitante());

        removerStatsEntidades(pMandante, jcMandante, jMandante, gm, gv, cam, cvm);
        removerStatsEntidades(pVisitante, jcVisitante, jVisitante, gv, gm, cav, cvv);

        if (gm > gv) {
            decrementarResultado(pMandante, jcMandante, jMandante, 1, 0, 0); // Remove vitoria mandante
            decrementarResultado(pVisitante, jcVisitante, jVisitante, 0, 0, 1); // Remove derrota visitante
        } else if (gv > gm) {
            decrementarResultado(pMandante, jcMandante, jMandante, 0, 0, 1); // Remove derrota mandante
            decrementarResultado(pVisitante, jcVisitante, jVisitante, 1, 0, 0); // Remove vitoria visitante
        } else {
            decrementarResultado(pMandante, jcMandante, jMandante, 0, 1, 0); // Remove empate
            decrementarResultado(pVisitante, jcVisitante, jVisitante, 0, 1, 0); // Remove empate
        }
    }

    private void removerStatsEntidades(ParticipacaoFase pf, JogadorClube jc, Jogador j, int golsPro, int golsContra, int ca, int cv) {
        //Subtrai Jogos
        pf.setPartidasJogadas(Math.max(0, safeInt(pf.getPartidasJogadas()) - 1));
        jc.setPartidasJogadas(Math.max(0, safeInt(jc.getPartidasJogadas()) - 1));
        j.setPartidasJogadas(Math.max(0, safeInt(j.getPartidasJogadas()) - 1));

        //Subtrai Gols
        pf.setGolsPro(Math.max(0, safeInt(pf.getGolsPro()) - golsPro));
        jc.setTotalGolsMarcados(Math.max(0, safeInt(jc.getTotalGolsMarcados()) - golsPro));
        j.setGolsMarcados(Math.max(0, safeInt(j.getGolsMarcados()) - golsPro));

        pf.setGolsContra(Math.max(0, safeInt(pf.getGolsContra()) - golsContra));
        jc.setTotalGolsSofridos(Math.max(0, safeInt(jc.getTotalGolsSofridos()) - golsContra));
        j.setGolsSofridos(Math.max(0, safeInt(j.getGolsSofridos()) - golsContra));

        pf.setSaldoGols(pf.getGolsPro() - pf.getGolsContra());

        jc.setTotalCartoesAmarelos(Math.max(0, safeInt(jc.getTotalCartoesAmarelos()) - ca));
        jc.setTotalCartoesVermelhos(Math.max(0, safeInt(jc.getTotalCartoesVermelhos()) - cv));
        j.setCartoesAmarelos(Math.max(0, safeLong(j.getCartoesAmarelos()) - ca));
        j.setCartoesVermelhos(Math.max(0, safeLong(j.getCartoesVermelhos()) - cv));
    }

    private void decrementarResultado(ParticipacaoFase pf, JogadorClube jc, Jogador j, int v, int e, int d) {
        pf.setVitorias(Math.max(0, safeInt(pf.getVitorias()) - v));
        pf.setEmpates(Math.max(0, safeInt(pf.getEmpates()) - e));
        pf.setDerrotas(Math.max(0, safeInt(pf.getDerrotas()) - d));

        jc.setVitorias(Math.max(0, safeInt(jc.getVitorias()) - v));
        jc.setEmpates(Math.max(0, safeInt(jc.getEmpates()) - e));
        jc.setDerrotas(Math.max(0, safeInt(jc.getDerrotas()) - d));

        j.setVitorias(Math.max(0, safeInt(j.getVitorias()) - v));
        j.setEmpates(Math.max(0, safeInt(j.getEmpates()) - e));
        j.setDerrotas(Math.max(0, safeInt(j.getDerrotas()) - d));
    }

    private void reverterPontosLiga(Partida partida, ParticipacaoFase m, ParticipacaoFase v) {
        int gm = safeInt(partida.getGolsMandante());
        int gv = safeInt(partida.getGolsVisitante());

        if (gm > gv) {
            m.setPontos(Math.max(0, safeInt(m.getPontos()) - 3));
        } else if (gm < gv) {
            v.setPontos(Math.max(0, safeInt(v.getPontos()) - 3));
        } else {
            m.setPontos(Math.max(0, safeInt(m.getPontos()) - 1));
            v.setPontos(Math.max(0, safeInt(v.getPontos()) - 1));
        }
    }

    private void reverterMataMata(ParticipacaoFase m, ParticipacaoFase v) {
        m.setStatusClassificacao(StatusClassificacao.ATIVO);
        v.setStatusClassificacao(StatusClassificacao.ATIVO);
    }

    private void reverterCoeficientes(Partida partida) {
        BigDecimal coefM = partida.getCoeficienteMandante();
        BigDecimal coefV = partida.getCoeficienteVisitante();

        if (coefM == null) coefM = BigDecimal.ZERO;
        if (coefV == null) coefV = BigDecimal.ZERO;

        JogadorClube jcMandante = partida.getMandante();
        JogadorClube jcVisitante = partida.getVisitante();
        Jogador jGlobalMandante = jcMandante.getJogador();
        Jogador jGlobalVisitante = jcVisitante.getJogador();

        jcMandante.setPontosCoeficiente(safeAdd(jcMandante.getPontosCoeficiente(), coefM.negate()));
        jcVisitante.setPontosCoeficiente(safeAdd(jcVisitante.getPontosCoeficiente(), coefV.negate()));

        jGlobalMandante.setPontosCoeficiente(safeAdd(jGlobalMandante.getPontosCoeficiente(), coefM.negate()));
        jGlobalVisitante.setPontosCoeficiente(safeAdd(jGlobalVisitante.getPontosCoeficiente(), coefV.negate()));
    }

    private void limparDadosPartida(Partida partida) {
        partida.setRealizada(false);
        partida.setDataHora(null);
        partida.setGolsMandante(null);
        partida.setGolsVisitante(null);
        partida.setCoeficienteMandante(null);
        partida.setCoeficienteVisitante(null);
        partida.setCartoesAmarelosMandante(null);
        partida.setCartoesVermelhosMandante(null);
        partida.setCartoesAmarelosVisitante(null);
        partida.setCartoesVermelhosVisitante(null);
        partida.setWo(false);
    }
}