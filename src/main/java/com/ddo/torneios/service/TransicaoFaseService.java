package com.ddo.torneios.service;

import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.model.ParticipacaoFase;
import com.ddo.torneios.model.Partida;
import com.ddo.torneios.model.TipoGeracao;
import com.ddo.torneios.repository.FaseTorneioRepository;
import com.ddo.torneios.repository.ParticipacaoFaseRepository;
import com.ddo.torneios.repository.PartidaRepository;
import com.ddo.torneios.service.gerador.GeradorMataMataFactory;
import com.ddo.torneios.service.gerador.GeradorPartidasStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TransicaoFaseService {

    @Autowired
    private ParticipacaoFaseRepository participacaoRepository;

    @Autowired
    private PartidaRepository partidaRepository;

    @Autowired
    private GeradorMataMataFactory geradorFactory;

    @Autowired
    private FaseTorneioRepository faseRepository;

    @Transactional
    public void inicializarFaseMataMata(String novaFaseId, TipoGeracao tipoGeracao) {
        FaseTorneio faseNova = faseRepository.findById(novaFaseId)
                .orElseThrow(() -> new IllegalArgumentException("Fase não encontrada."));

        int ordemAnterior = faseNova.getOrdem() - 1;

        FaseTorneio faseAnterior = faseRepository
                .findByTorneioIdAndOrdem(faseNova.getTorneio().getId(), ordemAnterior)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Não foi encontrada uma fase anterior (Ordem " + ordemAnterior + ") para este torneio."));

        gerarFaseMataMata(faseAnterior, faseNova, tipoGeracao);
    }

    @Transactional
    public void gerarFaseMataMata(FaseTorneio faseLigaAnterior, FaseTorneio faseMataMataNova, TipoGeracao tipoGeracao) {

        int quantidadeClassificados = determinarQuantidadeClassificados(faseMataMataNova);

        Pageable limit = PageRequest.of(0, quantidadeClassificados);

        List<ParticipacaoFase> classificadosLiga = participacaoRepository
                .findByFaseIdOrderByPontosDescVitoriasDescSaldoGolsDescGolsProDesc(faseLigaAnterior.getId(), limit);

        if (classificadosLiga.size() < quantidadeClassificados) {
            throw new IllegalArgumentException(
                    String.format("Fase anterior tem apenas %d participantes, mas a fase %s exige %d classificados.",
                            classificadosLiga.size(), faseMataMataNova.getNome(), quantidadeClassificados)
            );
        }

        List<ParticipacaoFase> novasParticipacoes = new ArrayList<>();

        for (int i = 0; i < classificadosLiga.size(); i++) {
            ParticipacaoFase pLiga = classificadosLiga.get(i);

            ParticipacaoFase pMataMata = new ParticipacaoFase();
            pMataMata.setFase(faseMataMataNova);
            pMataMata.setJogadorClube(pLiga.getJogadorClube());

            pMataMata.setPosicaoClassificacao(i + 1);

            pMataMata.setPontos(0);
            pMataMata.setVitorias(0);
            pMataMata.setSaldoGols(0);
            pMataMata.setGolsPro(0);
            pMataMata.setGolsContra(0);
            pMataMata.setPartidasJogadas(0);
            pMataMata.setEmpates(0);
            pMataMata.setDerrotas(0);

            if (tipoGeracao == TipoGeracao.POTES_MANUAIS) {
                if (i < quantidadeClassificados / 2) {
                    pMataMata.setGrupo("Pote 1");
                } else {
                    pMataMata.setGrupo("Pote 2");
                }
            }

            novasParticipacoes.add(pMataMata);
        }

        participacaoRepository.saveAll(novasParticipacoes);

        if (tipoGeracao == TipoGeracao.POTES_MANUAIS) {
            return;
        }

        gerarPartidasInterno(faseMataMataNova, novasParticipacoes, tipoGeracao);
    }

    @Transactional
    public void gerarPartidasAposEdicaoManual(FaseTorneio faseMataMata, TipoGeracao tipoGeracao) {
        List<ParticipacaoFase> participantes = participacaoRepository.findByFase(faseMataMata);

        if (participantes.isEmpty()) {
            throw new IllegalArgumentException("Não há participantes nesta fase para gerar partidas.");
        }

        gerarPartidasInterno(faseMataMata, participantes, tipoGeracao);
    }

    private void gerarPartidasInterno(FaseTorneio fase, List<ParticipacaoFase> participantes, TipoGeracao tipo) {
        GeradorPartidasStrategy<Partida> estrategia = geradorFactory.obterEstrategia(tipo);
        List<Partida> partidasGeradas = estrategia.gerar(fase, participantes);

        if (!partidasGeradas.isEmpty()) {
            partidaRepository.saveAll(partidasGeradas);
        }
    }

    private int determinarQuantidadeClassificados(FaseTorneio fase) {
        if (fase.getNome() == null) return 16;
        String nome = fase.getNome().toLowerCase();

        if (nome.contains("16 avos")) return 32;
        if (nome.contains("oitavas")) return 16;
        if (nome.contains("quartas")) return 8;
        if (nome.contains("semi")) return 4;
        if (nome.contains("final")) return 2;

        return 16;
    }
}