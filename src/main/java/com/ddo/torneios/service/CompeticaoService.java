package com.ddo.torneios.service;

import com.ddo.torneios.dto.PaginacaoDTO;
import com.ddo.torneios.exception.CompeticaoExisteException;
import com.ddo.torneios.model.Clube;
import com.ddo.torneios.model.Competicao;
import com.ddo.torneios.repository.CompeticaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompeticaoService {

    @Autowired
    private CompeticaoRepository competicaoRepository;

    public void criarCompeticao(Competicao competicao) {
        if (competicaoRepository.existsByNome(competicao.getNome())) {
            throw new CompeticaoExisteException(competicao.getNome());
        }

        competicaoRepository.save(competicao);
    }

    public PaginacaoDTO<Competicao> listarCompeticoes(
            String nomeFiltro,
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Competicao> paginaEntidades;

        if (nomeFiltro != null && !nomeFiltro.isBlank()) {
            paginaEntidades = competicaoRepository.findByNomeContainingIgnoreCase(nomeFiltro, pageable);
        } else {
            paginaEntidades = competicaoRepository.findAll(pageable);
        }

        return new PaginacaoDTO<>(
                paginaEntidades.getContent(),
                paginaEntidades.getNumber(),
                paginaEntidades.getTotalPages(),
                paginaEntidades.getTotalElements(),
                paginaEntidades.getSize(),
                paginaEntidades.isLast()
        );
    }

    public List<Competicao> listarTodasSemPaginacao() {
        return competicaoRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
    }

    public List<Competicao> buscarAutocomplete(String termo) {
        if (termo == null || termo.trim().isEmpty()) {
            return List.of();
        }

        Pageable limit = PageRequest.of(0, 10, Sort.by("nome").ascending());

        return competicaoRepository.findByNomeContainingIgnoreCase(termo.trim(), limit)
                .getContent();
    }
}
