package com.ddo.torneios.service;

import com.ddo.torneios.dto.PartidaDTO;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

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

        JogadorClube jcMandante = partida.getMandante();
        JogadorClube jcVisitante = partida.getVisitante();

        jcMandante.setPontosCoeficiente(safeAdd(jcMandante.getPontosCoeficiente(), coefM));
        jcVisitante.setPontosCoeficiente(safeAdd(jcVisitante.getPontosCoeficiente(), coefV));

        Jogador jGlobalMandante = jcMandante.getJogador();
        Jogador jGlobalVisitante = jcVisitante.getJogador();

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

    private BigDecimal calcularCoeficiente(
            Integer golsM, Integer golsS, boolean vit, boolean emp, boolean der,
            Integer ca, Integer cv, BigDecimal estrelas, Integer valorTorneio
    ) {
        double gm = golsM != null ? golsM : 0;
        double gs = golsS != null ? golsS : 0;
        double amt = ca != null ? ca : 0;
        double vrm = cv != null ? cv : 0;
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

    private BigDecimal safeAdd(BigDecimal base, BigDecimal toAdd) {
        if (base == null) base = BigDecimal.ZERO;
        if (toAdd == null) toAdd = BigDecimal.ZERO;
        return base.add(toAdd);
    }

    private void processarLiga(PartidaDTO dto, ParticipacaoFase m, ParticipacaoFase v) {
        m.setPartidasJogadas(m.getPartidasJogadas() + 1);
        v.setPartidasJogadas(v.getPartidasJogadas() + 1);

        int gm = dto.golsMandante() != null ? dto.golsMandante() : 0;
        int gv = dto.golsVisitante() != null ? dto.golsVisitante() : 0;

        m.setGolsPro(m.getGolsPro() + gm);
        m.setGolsContra(m.getGolsContra() + gv);
        v.setGolsPro(v.getGolsPro() + gv);
        v.setGolsContra(v.getGolsContra() + gm);
        m.setSaldoGols(m.getGolsPro() - m.getGolsContra());
        v.setSaldoGols(v.getGolsPro() - v.getGolsContra());

        if (gm > gv) {
            m.setPontos(m.getPontos() + 3);
            m.setVitorias(m.getVitorias() + 1);
            v.setDerrotas(v.getDerrotas() + 1);
        } else if (gm < gv) {
            v.setPontos(v.getPontos() + 3);
            v.setVitorias(v.getVitorias() + 1);
            m.setDerrotas(m.getDerrotas() + 1);
        } else {
            m.setPontos(m.getPontos() + 1);
            v.setPontos(v.getPontos() + 1);
            m.setEmpates(m.getEmpates() + 1);
            v.setEmpates(v.getEmpates() + 1);
        }
    }

    private void processarMataMata(PartidaDTO dto, ParticipacaoFase m, ParticipacaoFase v) {
        int totalM = (dto.golsMandante() != null ? dto.golsMandante() : 0) + (dto.penaltisMandante() != null ? dto.penaltisMandante() : 0);
        int totalV = (dto.golsVisitante() != null ? dto.golsVisitante() : 0) + (dto.penaltisVisitante() != null ? dto.penaltisVisitante() : 0);

        ParticipacaoFase venceu = totalM > totalV ? m : v;
        ParticipacaoFase perdeu = venceu == m ? v : m;

        perdeu.setStatusClassificacao(StatusClassificacao.ELIMINADO);

        if (dto.etapaMataMata() != null) {
            try {
                FaseMataMata atual = FaseMataMata.valueOf(dto.etapaMataMata());
                venceu.setStatusClassificacao(definirProximoStatus(atual));
            } catch (Exception e) {
            }
        }
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

    public List<ParticipacaoFase> ordernarRanking(List<ParticipacaoFase> lista) {
        return lista.stream()
                .sorted(Comparator.comparing(ParticipacaoFase::getPontos).reversed()
                        .thenComparing(Comparator.comparing(ParticipacaoFase::getVitorias).reversed())
                        .thenComparing(Comparator.comparing(ParticipacaoFase::getSaldoGols).reversed())
                        .thenComparing(Comparator.comparing(ParticipacaoFase::getGolsPro).reversed()))
                .collect(Collectors.toList());
    }
}