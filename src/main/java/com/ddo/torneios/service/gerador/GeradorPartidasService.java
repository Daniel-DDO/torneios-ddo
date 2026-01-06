package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.FaseTorneioRepository;
import com.ddo.torneios.repository.PartidaRepository;
import com.ddo.torneios.repository.RodadaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

        //se já existirem partidas realizadas (com placar), nn permite gerar.
        if (checarSeJaExistemResultados(fase)) {
            throw new IllegalStateException("Esta fase já possui partidas realizadas (com placar). Impossível gerar novamente sem resetar os jogos antes.");
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
        partidaRepository.flush(); // Garante que saiu do banco

        if (fase.getRodadas() != null && !fase.getRodadas().isEmpty()) {
            List<Rodada> rodadasParaRemover = new ArrayList<>(fase.getRodadas());
            fase.getRodadas().clear();

            rodadaRepository.deleteAll(rodadasParaRemover);
            rodadaRepository.flush();
            faseRepository.saveAndFlush(fase);
        }
    }

    @SuppressWarnings("unchecked")
    private void processarESalvar(Object resultado) {
        if (!(resultado instanceof List<?> lista) || lista.isEmpty()) return;

        Object primeiro = lista.get(0);

        if (primeiro instanceof Rodada) {
            List<Rodada> rodadas = (List<Rodada>) lista;
            rodadas.forEach(rodada -> {
                if (rodada.getPartidas() != null) {
                    rodada.getPartidas().forEach(this::vincularEstadioDoMandante);
                }
            });
            rodadaRepository.saveAll(rodadas);
        }
        else if (primeiro instanceof Partida) {
            List<Partida> partidas = (List<Partida>) lista;
            partidas.forEach(this::vincularEstadioDoMandante);
            partidaRepository.saveAll(partidas);
            partidaRepository.flush();
        }
    }

    public Optional<FaseTorneio> buscarPorId(String faseId) {
        return faseRepository.findById(faseId);
    }

    private void vincularEstadioDoMandante(Partida partida) {
        if (partida.getMandante() != null &&
                partida.getMandante().getClube() != null &&
                partida.getMandante().getClube().getEstadio() != null) {

            partida.setEstadio(partida.getMandante().getClube().getEstadio());
        }
    }
}