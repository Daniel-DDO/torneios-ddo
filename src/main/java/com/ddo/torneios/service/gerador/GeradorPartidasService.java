package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.FaseTorneioRepository;
import com.ddo.torneios.repository.PartidaRepository;
import com.ddo.torneios.repository.RodadaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GeradorPartidasService {

    private final FaseTorneioRepository faseRepository;
    private final RodadaRepository rodadaRepository;
    private final PartidaRepository partidaRepository;
    private final GeradorStrategyFactory strategyFactory;

    @Transactional
    public void gerarEstruturaFase(String faseId, AlgoritmoGeracaoMataMata novoAlgMataMata, AlgoritmoGeracaoLiga novoAlgLiga) {
        FaseTorneio fase = faseRepository.findById(faseId)
                .orElseThrow(() -> new IllegalArgumentException("Fase não encontrada"));

        if (novoAlgMataMata != null) fase.setAlgoritmoMataMata(novoAlgMataMata);
        if (novoAlgLiga != null) fase.setAlgoritmoLiga(novoAlgLiga);

        faseRepository.save(fase);

        if (checarSeJaExistemResultados(fase)) {
            throw new IllegalStateException("Esta fase já possui resultados. Impossível gerar novamente.");
        }
        limparGeracoesAnteriores(fase);

        GeradorPartidasStrategy<?> strategy = strategyFactory.getStrategy(fase);
        List<ParticipacaoFase> participantes = fase.getParticipacoes();

        Object resultado = strategy.gerar(fase, participantes);

        processarESalvar(resultado);
    }

    private boolean checarSeJaExistemResultados(FaseTorneio fase) {
        return partidaRepository.existsByFaseAndRealizadaTrue(fase);
    }

    @Transactional
    public void limparGeracoesAnteriores(FaseTorneio fase) {
        partidaRepository.deleteByFaseAndRodadaIsNull(fase);
        if (fase.getRodadas() != null && !fase.getRodadas().isEmpty()) {
            rodadaRepository.deleteAll(fase.getRodadas());
            fase.getRodadas().clear();
        }
    }

    @SuppressWarnings("unchecked")
    private void processarESalvar(Object resultado) {
        if (!(resultado instanceof List<?> lista) || lista.isEmpty()) return;

        Object primeiro = lista.get(0);

        if (primeiro instanceof Rodada) {
            rodadaRepository.saveAll((List<Rodada>) lista);
        }
        else if (primeiro instanceof Partida) {
            partidaRepository.saveAll((List<Partida>) lista);
            partidaRepository.flush();
        }
    }

    public Optional<FaseTorneio> buscarPorId(String faseId) {
        return faseRepository.findById(faseId);
    }
}