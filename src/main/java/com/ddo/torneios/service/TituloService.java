package com.ddo.torneios.service;

import com.ddo.torneios.model.Conquista;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.model.Titulo;
import com.ddo.torneios.repository.JogadorRepository;
import com.ddo.torneios.repository.TituloRepository;
import com.ddo.torneios.request.TituloRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class TituloService {

    @Autowired
    private TituloRepository tituloRepository;

    @Autowired
    private JogadorRepository jogadorRepository;

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

    @Transactional
    public void concederTituloAoJogador(String jogadorId, String nomeTitulo, String nomeEdicao) {
        Jogador jogador = jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado"));

        Titulo titulo = tituloRepository.findByNome(nomeTitulo)
                .orElseThrow(() -> new RuntimeException("Título não encontrado no catálogo: " + nomeTitulo));

        Conquista novaConquista = new Conquista(titulo, nomeEdicao);

        jogador.getConquistas().add(novaConquista);

        if (jogador.getTitulos() == null) jogador.setTitulos(0);
        jogador.setTitulos(jogador.getTitulos() + 1);

        jogadorRepository.save(jogador);

        log.info("Título '{}' concedido ao jogador {} na edição '{}'.", nomeTitulo, jogador.getNome(), nomeEdicao);
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