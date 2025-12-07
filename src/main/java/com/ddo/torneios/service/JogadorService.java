package com.ddo.torneios.service;

import com.ddo.torneios.dto.JogadorDTO;
import com.ddo.torneios.dto.PaginacaoDTO;
import com.ddo.torneios.exception.JogadorExisteException;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.repository.JogadorRepository;
import com.ddo.torneios.request.JogadorRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class JogadorService {

    @Autowired
    private JogadorRepository jogadorRepository;

    public void cadastrarJogador(JogadorRequest request) {
        if (jogadorRepository.existsJogadorByDiscord(request.getDiscord())) {
            throw new JogadorExisteException(request.getDiscord());
        }

        Jogador jogador = new Jogador(request.getNome(), request.getDiscord());
        jogadorRepository.save(jogador);
    }

    public PaginacaoDTO<JogadorDTO> listarJogadores(
            String nomeFiltro,
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Jogador> paginaEntidades;

        if (nomeFiltro != null && !nomeFiltro.isBlank()) {
            paginaEntidades = jogadorRepository.findByNomeContainingIgnoreCase(nomeFiltro, pageable);
        } else {
            paginaEntidades = jogadorRepository.findAll(pageable);
        }

        Page<JogadorDTO> paginaDTO = paginaEntidades.map(JogadorDTO::new);

        return new PaginacaoDTO<>(
                paginaDTO.getContent(),
                paginaDTO.getNumber(),
                paginaDTO.getTotalPages(),
                paginaDTO.getTotalElements(),
                paginaDTO.getSize(),
                paginaDTO.isLast()
        );
    }

    public ResponseEntity<JogadorDTO> retornarJogador(String id) {
        return jogadorRepository.findById(id)
                .map(jogador -> ResponseEntity.ok(new JogadorDTO(jogador)))
                .orElse(ResponseEntity.notFound().build());
    }
}
