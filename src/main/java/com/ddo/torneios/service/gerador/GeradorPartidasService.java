package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.ClubeRepository;
import com.ddo.torneios.repository.FaseTorneioRepository;
import com.ddo.torneios.repository.PartidaRepository;
import com.ddo.torneios.repository.RodadaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GeradorPartidasService {

    private final FaseTorneioRepository faseRepository;
    private final RodadaRepository rodadaRepository;
    private final PartidaRepository partidaRepository;
    private final GeradorStrategyFactory strategyFactory;
    private final ClubeRepository clubeRepository;

    @Transactional
    public void gerarEstruturaFase(String faseId, AlgoritmoGeracaoMataMata novoAlgMataMata, AlgoritmoGeracaoLiga novoAlgLiga) {
        FaseTorneio fase = faseRepository.findById(faseId)
                .orElseThrow(() -> new IllegalArgumentException("Fase não encontrada"));

        if (novoAlgMataMata != null) fase.setAlgoritmoMataMata(novoAlgMataMata);
        if (novoAlgLiga != null) fase.setAlgoritmoLiga(novoAlgLiga);

        faseRepository.save(fase);

        if (checarSeJaExistemResultados(fase)) {
            throw new IllegalStateException("Esta fase já possui partidas realizadas (com placar). Impossível gerar novamente sem resetar os jogos antes.");
        }

        if (fase.getEstadioFinal() == null) {
            fase.setEstadioFinal(sortearEstadioFinal());
            faseRepository.save(fase);
        }

        limparGeracoesAnteriores(fase);

        GeradorPartidasStrategy<?> strategy = strategyFactory.getStrategy(fase);
        List<ParticipacaoFase> participantes = fase.getParticipacoes();

        Object resultado = strategy.gerar(fase, participantes);

        processarESalvar(resultado, fase);
    }

    private boolean checarSeJaExistemResultados(FaseTorneio fase) {
        return partidaRepository.existsByFaseAndRealizadaTrue(fase);
    }

    @Transactional
    public void limparGeracoesAnteriores(FaseTorneio fase) {
        partidaRepository.deleteByFaseAndRodadaIsNull(fase);
        partidaRepository.flush();

        if (fase.getRodadas() != null && !fase.getRodadas().isEmpty()) {
            List<Rodada> rodadasParaRemover = new ArrayList<>(fase.getRodadas());
            fase.getRodadas().clear();

            rodadaRepository.deleteAll(rodadasParaRemover);
            rodadaRepository.flush();
            faseRepository.saveAndFlush(fase);
        }
    }

    @SuppressWarnings("unchecked")
    private void processarESalvar(Object resultado, FaseTorneio fase) {
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

            partidas.forEach(p -> {
                if (p.getTipoPartida() == TipoPartida.FINAL_UNICA) {
                    vincularEstadioFinal(p, fase.getEstadioFinal());
                } else {
                    vincularEstadioDoMandante(p);
                }
            });

            partidaRepository.saveAll(partidas);
            partidaRepository.flush();
        }
    }

    public Optional<FaseTorneio> buscarPorId(String faseId) {
        return faseRepository.findById(faseId);
    }

    private void vincularEstadioFinal(Partida partida, String estadioSorteado) {
        if (estadioSorteado != null) {
            partida.setEstadio(estadioSorteado);
        } else {
            vincularEstadioDoMandante(partida);
        }
    }

    private void vincularEstadioDoMandante(Partida partida) {
        if (partida.getMandante() != null &&
                partida.getMandante().getClube() != null &&
                partida.getMandante().getClube().getEstadio() != null) {

            partida.setEstadio(partida.getMandante().getClube().getEstadio());
        }
    }

    private String sortearEstadioFinal() {
        List<String> estadiosBanco = clubeRepository.findEstadiosDeClubesTop();

        List<String> lendarios = List.of(
                "Wembley Stadium", "Santiago Bernabéu", "San Siro", "Stade de France",
                "Allianz Arena", "Olympiastadion", "Estádio da Luz", "Estádio do Dragão",
                "Atatürk Olympic Stadium", "Hampden Park", "Ernst-Happel-Stadion", "Camp Nou",
                "Puskás Aréna", "Stadio Olimpico", "Stade Vélodrome", "King Baudouin Stadium",
                "Aviva Stadium", "Luzhniki Stadium", "NSC Olimpiyskiy Stadium", "De Kuip", "Praterstadion"
        );

        Set<String> poolEstadios = new HashSet<>(lendarios);

        if (estadiosBanco != null && !estadiosBanco.isEmpty()) {
            estadiosBanco.stream()
                    .filter(e -> e != null && !e.isBlank())
                    .forEach(poolEstadios::add);
        }

        List<String> listaFinal = new ArrayList<>(poolEstadios);

        if (listaFinal.isEmpty()) return "Cívitas Metropolitano";

        return listaFinal.get(new Random().nextInt(listaFinal.size()));
    }

    @Transactional
    public void atualizarEstadioFinalManualmente(String faseId, String novoEstadio) {
        FaseTorneio fase = faseRepository.findById(faseId)
                .orElseThrow(() -> new IllegalArgumentException("Fase não encontrada para o ID: " + faseId));

        fase.setEstadioFinal(novoEstadio);
        faseRepository.save(fase);

        List<Partida> partidasDaFase = partidaRepository.findByFase(fase);

        Optional<Partida> partidaFinal = partidasDaFase.stream()
                .filter(p -> p.getTipoPartida() == TipoPartida.FINAL_UNICA)
                .findFirst();

        if (partidaFinal.isPresent()) {
            Partida p = partidaFinal.get();
            p.setEstadio(novoEstadio);
            partidaRepository.save(p);
        }
    }
}