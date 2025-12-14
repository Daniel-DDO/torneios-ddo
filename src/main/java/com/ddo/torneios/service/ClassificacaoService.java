package com.ddo.torneios.service;

import com.ddo.torneios.dto.LinhaClassificacaoDTO;
import com.ddo.torneios.dto.PartidaDTO;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Transactional
    public void registrarResultado(PartidaDTO dto) {
        if (!dto.realizada()) return;

        Partida partida = partidaRepository.findById(dto.id())
                .orElseThrow(() -> new RuntimeException("Partida não encontrada"));

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
        } else {
            processarLiga(dto, pMandante, pVisitante);
        }

        partidaRepository.save(partida);
        jogadorClubeRepository.saveAll(List.of(jcMandante, jcVisitante));
        jogadorRepository.saveAll(List.of(jGlobalMandante, jGlobalVisitante));
        participacaoRepository.saveAll(List.of(pMandante, pVisitante));
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

    private void processarMataMata(PartidaDTO dto, ParticipacaoFase m, ParticipacaoFase v) {
        int totalM = safeInt(dto.golsMandante()) + safeInt(dto.penaltisMandante());
        int totalV = safeInt(dto.golsVisitante()) + safeInt(dto.penaltisVisitante());

        ParticipacaoFase venceu = totalM > totalV ? m : v;
        ParticipacaoFase perdeu = venceu == m ? v : m;

        perdeu.setStatusClassificacao(StatusClassificacao.ELIMINADO);

        if (dto.etapaMataMata() != null) {
            try {
                FaseMataMata atual = FaseMataMata.valueOf(dto.etapaMataMata());
                venceu.setStatusClassificacao(definirProximoStatus(atual));
            } catch (Exception ignored) {
            }
        }
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

    @Getter
    @Setter
    class AcumuladorStatus {
        int pontos, jogos, vitorias, empates, derrotas, golsPro, golsContra, amarelos, vermelhos;
        String jogadorClubeId, nomeJogador, nomeClube, imagemClube;

        int getSaldo() { return golsPro - golsContra; }
    }
}