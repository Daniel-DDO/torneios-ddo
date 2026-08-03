package com.ddo.torneios.service;

import com.ddo.torneios.dto.ClubeResumoConcessaoView;
import com.ddo.torneios.dto.JogadorClubeConcessaoView;
import com.ddo.torneios.dto.JogadorResumoConcessaoView;
import com.ddo.torneios.dto.TituloResumoDTO;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.*;
import com.ddo.torneios.request.ConcederTituloColetivoRequest;
import com.ddo.torneios.request.TituloRequest;
import com.ddo.torneios.util.ByteArrayMultipartFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TituloService {

    @Autowired
    private TituloRepository tituloRepository;
    @Autowired
    private JogadorRepository jogadorRepository;
    @Autowired
    private JogadorClubeRepository jogadorClubeRepository;
    @Autowired
    private ClubeRepository clubeRepository;
    @Autowired
    private PostGeradorService postGeradorService;
    @Autowired
    private ImgBBService imgBBService;
    @Autowired
    private ConquistaRepository conquistaRepository;

    @Transactional
    public Conquista concederTituloAoJogador(String jogadorClubeId, String idTitulo, String nomeEdicao) {

        if (nomeEdicao == null || nomeEdicao.trim().isEmpty()) {
            throw new RuntimeException("O nome da edição é obrigatório.");
        }
        nomeEdicao = nomeEdicao.trim();

        JogadorClubeConcessaoView view = jogadorClubeRepository.buscarParaConcessao(jogadorClubeId)
                .orElseThrow(() -> new RuntimeException("Vínculo Jogador-Clube não encontrado"));

        boolean jaPossui = conquistaRepository.existsByTituloIdAndNomeEdicaoAndJogadorId(
                idTitulo, nomeEdicao, view.getJogadorId()
        );
        if (jaPossui) {
            throw new RuntimeException("O jogador " + view.getJogadorNome() + " já possui o título desta edição (" + nomeEdicao + ").");
        }

        Titulo titulo = tituloRepository.findById(idTitulo)
                .orElseThrow(() -> new RuntimeException("Título não encontrado: " + idTitulo));

        Jogador jogadorRef = jogadorRepository.getReferenceById(view.getJogadorId());
        Clube clubeRef = clubeRepository.getReferenceById(view.getClubeId());

        Conquista novaConquista = new Conquista(titulo, nomeEdicao, clubeRef, jogadorRef);
        novaConquista.setDataConquista(LocalDateTime.now());

        try {
            conquistaRepository.saveAndFlush(novaConquista);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("O jogador " + view.getJogadorNome() + " já possui o título desta edição (" + nomeEdicao + ").");
        }

        jogadorRepository.incrementarTitulos(view.getJogadorId());
        clubeRepository.incrementarTitulos(view.getClubeId());

        gerarImagemEAtualizarLeve(novaConquista, titulo, view.getClubeImagem(), view.getJogadorId(), view.getJogadorNome(), view.getJogadorImagem(), "titulo_");

        return novaConquista;
    }

    private void gerarImagemEAtualizarLeve(Conquista conquista, Titulo titulo,
                                           String clubeImagem, String jogadorId,
                                           String jogadorNome, String jogadorImagem,
                                           String prefixoArquivo) {
        try {
            if (titulo.getImagemGerarPost() != null && !titulo.getImagemGerarPost().isEmpty()) {

                String urlLogoParaPost = clubeImagem;
                if (urlLogoParaPost == null || urlLogoParaPost.isEmpty()) {
                    urlLogoParaPost = jogadorImagem;
                }

                log.info("Gerando post do título para {}", jogadorNome);

                byte[] imagemBytes = postGeradorService.gerarImagemTitulo(
                        titulo.getImagemGerarPost(), urlLogoParaPost, jogadorNome
                );

                if (imagemBytes != null) {
                    String nomeArquivo = prefixoArquivo + jogadorId + "_" + System.currentTimeMillis() + ".png";

                    MultipartFile multipartFile = new ByteArrayMultipartFile(
                            imagemBytes, "image", nomeArquivo, "image/png"
                    );

                    String urlImgBB = imgBBService.uploadImagem(multipartFile);

                    conquista.setImagem(urlImgBB);
                    conquistaRepository.save(conquista);
                    log.info("Imagem salva no ImgBB: {}", urlImgBB);
                }
            }
        } catch (Exception e) {
            log.error("Erro ao gerar imagem (o título já foi concedido sem imagem): ", e);
        }
    }

    private void gerarImagemEAtualizar(Conquista conquista, Titulo titulo, Clube clube, Jogador jogador, String prefixoArquivo) {
        try {
            if (titulo.getImagemGerarPost() != null && !titulo.getImagemGerarPost().isEmpty()) {

                String urlLogoParaPost = clube.getImagem();
                if (urlLogoParaPost == null || urlLogoParaPost.isEmpty()) {
                    urlLogoParaPost = jogador.getImagem();
                }

                log.info("Gerando post do título para {}", jogador.getNome());

                byte[] imagemBytes = postGeradorService.gerarImagemTitulo(
                        titulo.getImagemGerarPost(), urlLogoParaPost, jogador.getNome()
                );

                if (imagemBytes != null) {
                    String nomeArquivo = prefixoArquivo + jogador.getId() + "_" + System.currentTimeMillis() + ".png";

                    MultipartFile multipartFile = new ByteArrayMultipartFile(
                            imagemBytes, "image", nomeArquivo, "image/png"
                    );

                    String urlImgBB = imgBBService.uploadImagem(multipartFile);

                    conquista.setImagem(urlImgBB);
                    conquistaRepository.save(conquista);
                    log.info("Imagem salva no ImgBB: {}", urlImgBB);
                }
            }
        } catch (Exception e) {
            log.error("Erro ao gerar imagem (o título já foi concedido sem imagem): ", e);
        }
    }

    @Transactional
    public Titulo criarTitulo(TituloRequest request) {
        if (tituloRepository.findByNome(request.nome()).isPresent()) {
            throw new RuntimeException("Já existe um título catalogado com o nome: " + request.nome());
        }
        return tituloRepository.save(converterDto(request));
    }

    @Transactional
    public List<Titulo> criarTitulosEmLote(List<TituloRequest> requests) {
        List<Titulo> novosTitulos = requests.stream()
                .filter(req -> tituloRepository.findByNome(req.nome()).isEmpty())
                .map(this::converterDto)
                .toList();

        if (novosTitulos.isEmpty()) {
            log.info("Nenhum título novo para salvar.");
            return List.of();
        }

        log.info("Salvando {} novos títulos no catálogo.", novosTitulos.size());
        return tituloRepository.saveAll(novosTitulos);
    }

    @Transactional(readOnly = true)
    public List<Titulo> listarTodos() {
        return tituloRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Titulo> listarAtivos() {
        return tituloRepository.findByAtivoTrue();
    }

    @Transactional(readOnly = true)
    public List<Titulo> listarInativos() {
        return tituloRepository.findByAtivoFalse();
    }

    private Titulo converterDto(TituloRequest req) {
        Titulo t = new Titulo();
        t.setNome(req.nome());
        t.setValor(req.valor());
        t.setDescricao(req.descricao());
        t.setImagem(req.imagem());
        t.setImagemGerarPost(req.imagemGerarPost());
        t.setAtivo(req.ativo() != null ? req.ativo() : true);

        return t;
    }

    @Transactional
    public Conquista concederTituloLegado(String jogadorId, String clubeId, String idTitulo, String nomeEdicao, LocalDateTime data) {

        if (nomeEdicao == null || nomeEdicao.trim().isEmpty()) {
            throw new RuntimeException("O nome da edição é obrigatório.");
        }
        nomeEdicao = nomeEdicao.trim();

        JogadorResumoConcessaoView jogadorView = jogadorRepository.buscarResumoParaConcessao(jogadorId)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado: " + jogadorId));

        ClubeResumoConcessaoView clubeView = clubeRepository.buscarResumoParaConcessao(clubeId)
                .orElseThrow(() -> new RuntimeException("Clube não encontrado: " + clubeId));

        boolean jaPossui = conquistaRepository.existsByTituloIdAndNomeEdicaoAndJogadorId(
                idTitulo, nomeEdicao, jogadorId
        );
        if (jaPossui) {
            throw new RuntimeException("O jogador " + jogadorView.getNome() + " já possui o título desta edição (" + nomeEdicao + ").");
        }

        Titulo titulo = tituloRepository.findById(idTitulo)
                .orElseThrow(() -> new RuntimeException("Título não encontrado: " + idTitulo));

        Jogador jogadorRef = jogadorRepository.getReferenceById(jogadorId);
        Clube clubeRef = clubeRepository.getReferenceById(clubeId);

        Conquista novaConquista = new Conquista(titulo, nomeEdicao, clubeRef, jogadorRef);
        novaConquista.setDataConquista(data);

        try {
            conquistaRepository.saveAndFlush(novaConquista);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("O jogador " + jogadorView.getNome() + " já possui o título desta edição (" + nomeEdicao + ").");
        }

        jogadorRepository.incrementarTitulosEmLote(List.of(jogadorId));
        clubeRepository.incrementarTitulos(clubeId, 1);

        gerarImagemEAtualizarLeve(
                novaConquista, titulo,
                clubeView.getImagem(), jogadorId, jogadorView.getNome(), jogadorView.getImagem(),
                "titulo_legado_"
        );

        return novaConquista;
    }

    @Transactional
    public List<Conquista> concederTituloColetivo(ConcederTituloColetivoRequest request) {

        ClubeResumoConcessaoView clubeView = clubeRepository.buscarResumoParaConcessao(request.getClubeId())
                .orElseThrow(() -> new RuntimeException("Clube não encontrado: " + request.getClubeId()));

        Titulo titulo = tituloRepository.findById(request.getIdTitulo())
                .orElseThrow(() -> new RuntimeException("Título não encontrado: " + request.getIdTitulo()));

        List<JogadorResumoConcessaoView> jogadoresViews =
                jogadorRepository.buscarResumosParaConcessao(request.getJogadoresIds());

        Map<String, JogadorResumoConcessaoView> jogadoresPorId = jogadoresViews.stream()
                .collect(Collectors.toMap(JogadorResumoConcessaoView::getId, v -> v));

        for (String jogadorId : request.getJogadoresIds()) {
            if (!jogadoresPorId.containsKey(jogadorId)) {
                throw new RuntimeException("Jogador não encontrado: " + jogadorId);
            }
        }

        Clube clubeRef = clubeRepository.getReferenceById(request.getClubeId());

        List<Conquista> conquistasGeradas = new ArrayList<>();
        List<String> jogadoresPremiadosIds = new ArrayList<>();

        for (String jogadorId : request.getJogadoresIds()) {
            JogadorResumoConcessaoView jv = jogadoresPorId.get(jogadorId);

            boolean jaPossui = conquistaRepository.existsByTituloIdAndNomeEdicaoAndJogadorId(
                    titulo.getId(), request.getEdicao(), jogadorId
            );
            if (jaPossui) {
                log.warn("O jogador {} já possui o título {}", jv.getNome(), request.getEdicao());
                continue;
            }

            Jogador jogadorRef = jogadorRepository.getReferenceById(jogadorId);
            Conquista novaConquista = new Conquista(titulo, request.getEdicao(), clubeRef, jogadorRef);
            novaConquista.setDataConquista(request.getData());

            try {
                conquistaRepository.saveAndFlush(novaConquista);
            } catch (DataIntegrityViolationException e) {
                log.warn("Título duplicado detectado na constraint para o jogador {} — pulando.", jv.getNome());
                continue;
            }

            gerarImagemEAtualizarLeve(
                    novaConquista, titulo,
                    clubeView.getImagem(), jogadorId, jv.getNome(), jv.getImagem(),
                    "titulo_coletivo_"
            );

            conquistasGeradas.add(novaConquista);
            jogadoresPremiadosIds.add(jogadorId);
        }

        if (!jogadoresPremiadosIds.isEmpty()) {
            jogadorRepository.incrementarTitulosEmLote(jogadoresPremiadosIds);
            clubeRepository.incrementarTitulos(request.getClubeId(), jogadoresPremiadosIds.size());
        }

        return conquistasGeradas;
    }

    public List<TituloResumoDTO> buscarAutocomplete(String termo) {
        if (termo == null || termo.trim().length() < 3) {
            return List.of();
        }
        return tituloRepository.buscarAutocomplete(termo.trim(), PageRequest.of(0, 10));
    }

    @Transactional(readOnly = true)
    public Titulo buscarPorId(String id) {
        return tituloRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Título não encontrado: " + id));
    }
}