package com.ddo.torneios.service;

import com.ddo.torneios.dto.TituloResumoDTO;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.*;
import com.ddo.torneios.request.ConcederTituloColetivoRequest;
import com.ddo.torneios.request.TituloRequest;
import com.ddo.torneios.util.ByteArrayMultipartFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        JogadorClube jogadorClube = jogadorClubeRepository.findById(jogadorClubeId)
                .orElseThrow(() -> new RuntimeException("Vínculo Jogador-Clube não encontrado"));

        Jogador jogador = jogadorClube.getJogador();
        Clube clube = jogadorClube.getClube();

        boolean jaPossui = conquistaRepository.existsByTituloIdAndNomeEdicaoAndJogadorId(
                idTitulo,
                nomeEdicao,
                jogador.getId()
        );

        if (jaPossui) {
            throw new RuntimeException("O jogador " + jogador.getNome() + " já possui o título desta edição (" + nomeEdicao + ").");
        }

        Titulo titulo = tituloRepository.findById(idTitulo)
                .orElseThrow(() -> new RuntimeException("Título não encontrado: " + idTitulo));

        Conquista novaConquista = new Conquista(titulo, nomeEdicao, clube, jogador);
        novaConquista.setDataConquista(LocalDateTime.now());

        try {
            if (titulo.getImagemGerarPost() != null && !titulo.getImagemGerarPost().isEmpty()) {

                String urlLogoParaPost = clube.getImagem();
                if (urlLogoParaPost == null || urlLogoParaPost.isEmpty()) {
                    urlLogoParaPost = jogador.getImagem();
                }

                log.info("Gerando post do título para {}", jogador.getNome());

                byte[] imagemBytes = postGeradorService.gerarImagemTitulo(
                        titulo.getImagemGerarPost(),
                        urlLogoParaPost,
                        jogador.getNome()
                );

                if (imagemBytes != null) {
                    String nomeArquivo = "titulo_" + jogador.getId() + "_" + System.currentTimeMillis() + ".png";

                    MultipartFile multipartFile = new ByteArrayMultipartFile(
                            imagemBytes,
                            "image",
                            nomeArquivo,
                            "image/png"
                    );

                    String urlImgBB = imgBBService.uploadImagem(multipartFile);

                    novaConquista.setImagem(urlImgBB);
                    log.info("Imagem salva no ImgBB: {}", urlImgBB);
                }
            }
        } catch (Exception e) {
            log.error("Erro ao gerar imagem (o título será concedido sem imagem): ", e);
        }

        conquistaRepository.save(novaConquista);

        jogador.getConquistas().add(novaConquista);
        clube.getConquistas().add(novaConquista);

        if (jogador.getTitulos() == null) jogador.setTitulos(0);
        jogador.setTitulos(jogador.getTitulos() + 1);

        if (clube.getTitulos() == null) clube.setTitulos(0);
        clube.setTitulos(clube.getTitulos() + 1);

        jogadorRepository.save(jogador);
        clubeRepository.save(clube);

        return novaConquista;
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

        Jogador jogador = jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado: " + jogadorId));

        Clube clube = clubeRepository.findById(clubeId)
                .orElseThrow(() -> new RuntimeException("Clube não encontrado: " + clubeId));

        boolean jaPossui = conquistaRepository.existsByTituloIdAndNomeEdicaoAndJogadorId(
                idTitulo,
                nomeEdicao,
                jogador.getId()
        );

        if (jaPossui) {
            throw new RuntimeException("O jogador " + jogador.getNome() + " já possui o título desta edição (" + nomeEdicao + ").");
        }

        Titulo titulo = tituloRepository.findById(idTitulo)
                .orElseThrow(() -> new RuntimeException("Título não encontrado: " + idTitulo));

        Conquista novaConquista = new Conquista(titulo, nomeEdicao, clube, jogador);
        novaConquista.setDataConquista(data);

        try {
            if (titulo.getImagemGerarPost() != null && !titulo.getImagemGerarPost().isEmpty()) {

                String urlLogoParaPost = clube.getImagem();
                if (urlLogoParaPost == null || urlLogoParaPost.isEmpty()) {
                    urlLogoParaPost = jogador.getImagem();
                }

                log.info("Gerando post do título legado para {}", jogador.getNome());

                byte[] imagemBytes = postGeradorService.gerarImagemTitulo(
                        titulo.getImagemGerarPost(),
                        urlLogoParaPost,
                        jogador.getNome()
                );

                if (imagemBytes != null) {
                    String nomeArquivo = "titulo_legado_" + jogador.getId() + "_" + System.currentTimeMillis() + ".png";

                    MultipartFile multipartFile = new ByteArrayMultipartFile(
                            imagemBytes,
                            "image",
                            nomeArquivo,
                            "image/png"
                    );

                    String urlImgBB = imgBBService.uploadImagem(multipartFile);
                    novaConquista.setImagem(urlImgBB);
                }
            }
        } catch (Exception e) {
            log.error("Erro ao gerar imagem legado: ", e);
        }

        conquistaRepository.save(novaConquista);

        if (jogador.getConquistas() != null) jogador.getConquistas().add(novaConquista);
        if (clube.getConquistas() != null) clube.getConquistas().add(novaConquista);

        jogador.setTitulos(jogador.getTitulos() == null ? 1 : jogador.getTitulos() + 1);
        clube.setTitulos(clube.getTitulos() == null ? 1 : clube.getTitulos() + 1);

        jogadorRepository.save(jogador);
        clubeRepository.save(clube);

        return novaConquista;
    }

    @Transactional
    public List<Conquista> concederTituloColetivo(ConcederTituloColetivoRequest request) {
        Clube clube = clubeRepository.findById(request.getClubeId())
                .orElseThrow(() -> new RuntimeException("Clube não encontrado: " + request.getClubeId()));

        Titulo titulo = tituloRepository.findById(request.getIdTitulo())
                .orElseThrow(() -> new RuntimeException("Título não encontrado: " + request.getIdTitulo()));

        List<Conquista> conquistasGeradas = new ArrayList<>();
        List<Jogador> jogadoresParaSalvar = new ArrayList<>();

        for (String jogadorId : request.getJogadoresIds()) {
            Jogador jogador = jogadorRepository.findById(jogadorId)
                    .orElseThrow(() -> new RuntimeException("Jogador não encontrado: " + jogadorId));

            boolean jaPossui = conquistaRepository.existsByTituloIdAndNomeEdicaoAndJogadorId(
                    titulo.getId(),
                    request.getEdicao(),
                    jogador.getId()
            );

            if (jaPossui) {
                log.warn("O jogador {} já possui o título {}", jogador.getNome(), request.getEdicao());
                continue;
            }

            Conquista novaConquista = new Conquista(titulo, request.getEdicao(), clube, jogador);
            novaConquista.setDataConquista(request.getData());

            try {
                if (titulo.getImagemGerarPost() != null && !titulo.getImagemGerarPost().isEmpty()) {

                    String urlLogoParaPost = clube.getImagem();

                    log.info("Gerando post para {}", jogador.getNome());

                    byte[] imagemBytes = postGeradorService.gerarImagemTitulo(
                            titulo.getImagemGerarPost(),
                            urlLogoParaPost,
                            jogador.getNome()
                    );

                    if (imagemBytes != null) {
                        String nomeArquivo = "titulo_coletivo_" + jogador.getId() + "_" + System.currentTimeMillis() + ".png";

                        MultipartFile multipartFile = new ByteArrayMultipartFile(
                                imagemBytes, "image", nomeArquivo, "image/png"
                        );

                        String urlImgBB = imgBBService.uploadImagem(multipartFile);
                        novaConquista.setImagem(urlImgBB);
                    }
                }
            } catch (Exception e) {
                log.error("Erro ao gerar imagem para jogador {}: ", jogador.getNome(), e);
            }

            jogador.setTitulos(jogador.getTitulos() == null ? 1 : jogador.getTitulos() + 1);
            jogadoresParaSalvar.add(jogador);

            conquistasGeradas.add(novaConquista);
        }

        conquistaRepository.saveAll(conquistasGeradas);
        jogadorRepository.saveAll(jogadoresParaSalvar);

        clube.setTitulos(clube.getTitulos() == null ? 1 : clube.getTitulos() + 1);
        clubeRepository.save(clube);

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