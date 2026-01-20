package com.ddo.torneios.service;

import com.ddo.torneios.dto.PreviaClassificadosDTO;
import com.ddo.torneios.model.*;
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
    public void inicializarFaseMataMata(String novaFaseId) {
        FaseTorneio faseNova = faseRepository.findById(novaFaseId)
                .orElseThrow(() -> new IllegalArgumentException("Fase não encontrada com ID: " + novaFaseId));

        if (faseNova.getAlgoritmoMataMata() == null) {
            throw new IllegalStateException("A fase " + faseNova.getNome() + " não possui algoritmo configurado.");
        }

        boolean ehFaseInicial = (faseNova.getOrdem() == 1) || Boolean.TRUE.equals(faseNova.getFaseInicialMataMata());

        if (ehFaseInicial) {
            gerarMataMataSemOrigem(faseNova);
        } else {
            transicionarDaFaseAnterior(faseNova);
        }
    }

    private void gerarMataMataSemOrigem(FaseTorneio fase) {
        List<ParticipacaoFase> participantesAtuais = participacaoRepository.findByFase(fase);

        if (participantesAtuais.isEmpty()) {
            throw new IllegalArgumentException("A fase é inicial/independente, mas não há times cadastrados nela. Adicione os times manualmente antes de gerar o mata-mata.");
        }

        int qtdEsperada = determinarQuantidadeClassificados(fase);
        if (participantesAtuais.size() != qtdEsperada) {
            throw new IllegalArgumentException("Quantidade incorreta de times. Esperado: " + qtdEsperada + ", Encontrado: " + participantesAtuais.size());
        }

        if (fase.getAlgoritmoMataMata() == AlgoritmoGeracaoMataMata.POTES_MANUAIS) {
            return;
        }

        gerarPartidasInterno(fase, participantesAtuais, fase.getAlgoritmoMataMata());
    }

    private void transicionarDaFaseAnterior(FaseTorneio faseNova) {
        int ordemAnterior = faseNova.getOrdem() - 1;
        FaseTorneio faseAnterior = faseRepository
                .findByTorneioIdAndOrdem(faseNova.getTorneio().getId(), ordemAnterior)
                .orElseThrow(() -> new IllegalArgumentException("Fase anterior (Ordem " + ordemAnterior + ") não encontrada."));

        int quantidadeClassificados = determinarQuantidadeClassificados(faseNova);
        Pageable limit = PageRequest.of(0, quantidadeClassificados);

        List<ParticipacaoFase> classificadosLiga = participacaoRepository
                .findByFaseIdOrderByPontosDescVitoriasDescSaldoGolsDescGolsProDesc(faseAnterior.getId(), limit);

        if (classificadosLiga.size() < quantidadeClassificados) {
            throw new IllegalArgumentException(
                    String.format("Fase anterior tem apenas %d participantes, mas a fase %s exige %d classificados.",
                            classificadosLiga.size(), faseNova.getNome(), quantidadeClassificados)
            );
        }

        List<ParticipacaoFase> novasParticipacoes = criarParticipacoesParaNovaFase(classificadosLiga, faseNova, faseNova.getAlgoritmoMataMata());
        participacaoRepository.saveAll(novasParticipacoes);

        if (faseNova.getAlgoritmoMataMata() != AlgoritmoGeracaoMataMata.POTES_MANUAIS) {
            gerarPartidasInterno(faseNova, novasParticipacoes, faseNova.getAlgoritmoMataMata());
        }
    }

    @Transactional
    public void confirmarMataMataManual(String faseId) {
        FaseTorneio fase = faseRepository.findById(faseId)
                .orElseThrow(() -> new IllegalArgumentException("Fase não encontrada."));

        if (fase.getAlgoritmoMataMata() != AlgoritmoGeracaoMataMata.POTES_MANUAIS) {
            throw new IllegalArgumentException("Esta ação é exclusiva para fases configuradas como POTES_MANUAIS.");
        }

        List<ParticipacaoFase> participantesAtuais = participacaoRepository.findByFase(fase);

        if (participantesAtuais.isEmpty()) {
            throw new IllegalStateException("Nenhum participante encontrado nesta fase.");
        }

        gerarPartidasInterno(fase, participantesAtuais, AlgoritmoGeracaoMataMata.POTES_MANUAIS);
    }

    private void gerarPartidasInterno(FaseTorneio fase, List<ParticipacaoFase> participantes, AlgoritmoGeracaoMataMata algoritmo) {
        if (partidaRepository.countByFaseId(fase.getId()) > 0) {
            throw new IllegalStateException("Já existem partidas geradas para esta fase. Exclua as partidas antes de gerar novamente.");
        }

        if (partidaRepository.existsByFaseId(fase.getId())) {
           throw new IllegalStateException("Já existem partidas geradas...");
        }


        GeradorPartidasStrategy<Partida> estrategia = geradorFactory.obterEstrategia(algoritmo);
        List<Partida> partidasGeradas = estrategia.gerar(fase, participantes);

        if (!partidasGeradas.isEmpty()) {
            partidaRepository.saveAll(partidasGeradas);
        }
    }

    private List<ParticipacaoFase> criarParticipacoesParaNovaFase(List<ParticipacaoFase> origem,
                                                                  FaseTorneio destino,
                                                                  AlgoritmoGeracaoMataMata algoritmo) {
        List<ParticipacaoFase> listaNova = new ArrayList<>();
        int totalClassificados = origem.size();

        for (int i = 0; i < totalClassificados; i++) {
            ParticipacaoFase pAntiga = origem.get(i);
            ParticipacaoFase pNova = new ParticipacaoFase();

            pNova.setFase(destino);
            pNova.setJogadorClube(pAntiga.getJogadorClube());
            pNova.setPosicaoClassificacao(i + 1);

            pNova.setPontos(0);
            pNova.setVitorias(0);
            pNova.setEmpates(0);
            pNova.setDerrotas(0);
            pNova.setGolsPro(0);
            pNova.setGolsContra(0);
            pNova.setSaldoGols(0);
            pNova.setPartidasJogadas(0);

            if (algoritmo == AlgoritmoGeracaoMataMata.POTES_MANUAIS ||
                    algoritmo == AlgoritmoGeracaoMataMata.SORTEIO_DIRIGIDO) {
                if (i < totalClassificados / 2) {
                    pNova.setGrupo("Pote A");
                } else {
                    pNova.setGrupo("Pote B");
                }
            } else {
                pNova.setGrupo(null);
            }

            listaNova.add(pNova);
        }
        return listaNova;
    }

    private int determinarQuantidadeClassificados(FaseTorneio fase) {
        if (fase.getNome() == null) return 16;
        String nomeNormalizado = fase.getNome().toLowerCase();

        if (nomeNormalizado.contains("32 avos") || nomeNormalizado.contains("trinta")) return 64;
        if (nomeNormalizado.contains("16 avos") || nomeNormalizado.contains("dezesseis")) return 32;
        if (nomeNormalizado.contains("oitavas")) return 16;
        if (nomeNormalizado.contains("quartas")) return 8;
        if (nomeNormalizado.contains("semi")) return 4;
        if (nomeNormalizado.contains("final")) return 2;

        return 16;
    }

    public PreviaClassificadosDTO obterPreviaClassificados(String novaFaseId) {
        FaseTorneio faseNova = faseRepository.findById(novaFaseId)
                .orElseThrow(() -> new IllegalArgumentException("Fase não encontrada."));

        boolean ehFaseInicial = (faseNova.getOrdem() == 1) || Boolean.TRUE.equals(faseNova.getFaseInicialMataMata());

        if (ehFaseInicial) {
            List<ParticipacaoFase> atuais = participacaoRepository.findByFase(faseNova);
            List<PreviaClassificadosDTO.ResumoClassificado> lista = atuais.stream()
                    .map(p -> new PreviaClassificadosDTO.ResumoClassificado(
                            p.getPosicaoClassificacao(),
                            p.getJogadorClube().getId(),
                            p.getJogadorClube().getJogador().getNome(),
                            p.getJogadorClube().getClube().getNome(),
                            p.getJogadorClube().getClube().getImagem()
                    )).toList();
            return new PreviaClassificadosDTO("ID atual","Fase Atual (Manual)", lista.size(), lista);
        }

        int ordemAnterior = faseNova.getOrdem() - 1;
        FaseTorneio faseAnterior = faseRepository
                .findByTorneioIdAndOrdem(faseNova.getTorneio().getId(), ordemAnterior)
                .orElseThrow(() -> new IllegalArgumentException("Fase anterior não encontrada."));

        int qtd = determinarQuantidadeClassificados(faseNova);
        Pageable limit = PageRequest.of(0, qtd);

        List<ParticipacaoFase> classificados = participacaoRepository
                .findByFaseIdOrderByPontosDescVitoriasDescSaldoGolsDescGolsProDesc(faseAnterior.getId(), limit);

        List<PreviaClassificadosDTO.ResumoClassificado> listaResumo = new ArrayList<>();

        for (int i = 0; i < classificados.size(); i++) {
            ParticipacaoFase p = classificados.get(i);
            listaResumo.add(new PreviaClassificadosDTO.ResumoClassificado(
                    i + 1,
                    p.getJogadorClube().getId(),
                    p.getJogadorClube().getJogador().getNome(),
                    p.getJogadorClube().getClube().getNome(),
                    p.getJogadorClube().getClube().getImagem()
            ));
        }

        return new PreviaClassificadosDTO(faseAnterior.getId(), faseAnterior.getNome(), qtd, listaResumo);
    }
}