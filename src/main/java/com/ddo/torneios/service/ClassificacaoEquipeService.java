package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ClassificacaoEquipeService {

    private final ParticipacaoFaseRepository participacaoRepo;
    private final PartidaRepository partidaRepo;
    private final JogadorClubeRepository jogadorClubeRepo;

    // pontos > saldo > gols pró > vitórias > menos cartões > melhor posição individual na liga
    private static final Comparator<ClassificacaoEquipeDTO> CRITERIOS =
            Comparator.comparingLong(ClassificacaoEquipeDTO::pontos).reversed()
                    .thenComparing(Comparator.comparingLong(ClassificacaoEquipeDTO::saldoGols).reversed())
                    .thenComparing(Comparator.comparingLong(ClassificacaoEquipeDTO::golsPro).reversed())
                    .thenComparing(Comparator.comparingLong(ClassificacaoEquipeDTO::vitorias).reversed())
                    .thenComparingLong(ClassificacaoEquipeDTO::cartoes)
                    .thenComparing(ClassificacaoEquipeDTO::melhorPosicao,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    @Transactional(readOnly = true)
    public List<ClassificacaoEquipeDTO> classificar(FaseTorneio fase) {
        Map<String, Long> cartoes = cartoesPorEquipe(fase);

        List<ClassificacaoEquipeDTO> ordenada = participacaoRepo.classificacaoEquipesBase(fase.getId()).stream()
                .map(b -> new ClassificacaoEquipeDTO(0, b.equipeId(), b.nome(),
                        b.pontos(), b.vitorias(), b.golsPro(), b.saldoGols(),
                        cartoes.getOrDefault(b.equipeId(), 0L), b.melhorPosicao()))
                .sorted(CRITERIOS)
                .toList();

        return IntStream.range(0, ordenada.size())
                .mapToObj(i -> {
                    ClassificacaoEquipeDTO d = ordenada.get(i);
                    return new ClassificacaoEquipeDTO(i + 1, d.equipeId(), d.nome(), d.pontos(),
                            d.vitorias(), d.golsPro(), d.saldoGols(), d.cartoes(), d.melhorPosicao());
                })
                .toList();
    }

    private Map<String, Long> cartoesPorEquipe(FaseTorneio fase) {
        String faseId = fase.getId();

        Map<String, String> equipePorJc = new HashMap<>();
        for (JogadorClubeEquipeDTO d : participacaoRepo.equipePorJogadorClube(faseId)) {
            equipePorJc.put(d.jogadorClubeId(), d.equipeId());
        }

        Map<String, String> participacaoParaJc = participacaoRepo.buscarIdsJogadorClubePorFase(faseId).stream()
                .collect(Collectors.toMap(
                        ParticipacaoFaseRepository.ParticipacaoJogadorClubeIdProjection::getId,
                        ParticipacaoFaseRepository.ParticipacaoJogadorClubeIdProjection::getJogadorClubeId));

        for (ParticipacaoFaseRepository.HistoricoAliasProjection h : participacaoRepo.buscarHistoricoPorFase(faseId)) {
            String jcAtual = participacaoParaJc.get(h.getParticipacaoFaseId());
            String equipe = jcAtual != null ? equipePorJc.get(jcAtual) : null;
            if (equipe != null) equipePorJc.putIfAbsent(h.getJogadorClubeIdAntigo(), equipe);
        }

        Map<String, Long> total = new HashMap<>();
        for (PartidaClassificacaoProjection p : partidaRepo.buscarDadosClassificacao(fase)) {
            somar(total, resolverEquipe(equipePorJc, p.mandanteId()),
                    n(p.cartoesAmarelosMandante()) + n(p.cartoesVermelhosMandante()));
            somar(total, resolverEquipe(equipePorJc, p.visitanteId()),
                    n(p.cartoesAmarelosVisitante()) + n(p.cartoesVermelhosVisitante()));
        }
        return total;
    }

    private String resolverEquipe(Map<String, String> equipePorJc, String jogadorClubeId) {
        if (jogadorClubeId == null) return null;
        String direta = equipePorJc.get(jogadorClubeId);
        if (direta != null) return direta;
        return jogadorClubeRepo.buscarIdSubstituto(jogadorClubeId).map(equipePorJc::get).orElse(null);
    }

    private void somar(Map<String, Long> mapa, String equipeId, long valor) {
        if (equipeId != null) mapa.merge(equipeId, valor, Long::sum);
    }

    private long n(Integer v) { return v == null ? 0L : v; }
}