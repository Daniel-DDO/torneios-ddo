package com.ddo.torneios.service;

import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.JogadorClubeRepository;
import com.ddo.torneios.repository.JogadorRepository;
import com.ddo.torneios.repository.TituloRepository;
import com.ddo.torneios.request.TituloRequest;
import com.ddo.torneios.util.ByteArrayMultipartFile; // Import da nossa classe utilitária
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

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
    private PostGeradorService postGeradorService;
    @Autowired
    private ImgBBService imgBBService;

    @Transactional
    public Conquista concederTituloAoJogador(String jogadorId, String nomeTitulo, String nomeEdicao) {
        Jogador jogador = jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado"));

        Titulo titulo = tituloRepository.findByNome(nomeTitulo)
                .orElseThrow(() -> new RuntimeException("Título não encontrado: " + nomeTitulo));

        Conquista novaConquista = new Conquista(titulo, nomeEdicao);

        try {
            if (titulo.getImagemGerarPost() != null && !titulo.getImagemGerarPost().isEmpty()) {

                String urlLogoClube = obterUrlImagemClube(jogadorId);
                if (urlLogoClube == null) urlLogoClube = jogador.getImagem();

                log.info("Gerando post do título para {}", jogador.getNome());

                byte[] imagemBytes = postGeradorService.gerarImagemTitulo(
                        titulo.getImagemGerarPost(),
                        urlLogoClube,
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

        jogador.getConquistas().add(novaConquista);
        if (jogador.getTitulos() == null) jogador.setTitulos(0);
        jogador.setTitulos(jogador.getTitulos() + 1);

        jogadorRepository.save(jogador);

        return novaConquista;
    }

    private String obterUrlImagemClube(String jogadorClubeId) {
        return jogadorClubeRepository.findById(jogadorClubeId)
                .map(jc -> jc.getClube().getImagem())
                .orElse(null);
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

    private Titulo converterDto(TituloRequest req) {
        Titulo t = new Titulo();
        t.setNome(req.nome());
        t.setValor(req.valor());
        t.setDescricao(req.descricao());
        t.setImagem(req.imagem());
        t.setImagemGerarPost(req.imagemGerarPost());
        return t;
    }
}