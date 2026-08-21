package com.ddo.torneios.service;

import com.ddo.torneios.dto.CompeticaoDTO;
import com.ddo.torneios.dto.PaginacaoDTO;
import com.ddo.torneios.exception.CompeticaoExisteException;
import com.ddo.torneios.model.Clube;
import com.ddo.torneios.model.Competicao;
import com.ddo.torneios.model.Titulo;
import com.ddo.torneios.repository.CompeticaoRepository;
import com.ddo.torneios.repository.TituloRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
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

    @Autowired
    private TituloRepository tituloRepository;

    public void criarCompeticao(Competicao competicao) {
        if (competicaoRepository.existsByNome(competicao.getNome())) {
            throw new CompeticaoExisteException(competicao.getNome());
        }

        competicao.setAtivo(true);
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
            paginaEntidades = competicaoRepository.findByAtivoTrueAndNomeContainingIgnoreCase(nomeFiltro, pageable);
        } else {
            paginaEntidades = competicaoRepository.findByAtivoTrue(pageable);
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
        return competicaoRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"))
                .stream()
                .filter(Competicao::getAtivo)
                .toList();
    }

    public List<Competicao> buscarAutocomplete(String termo) {
        if (termo == null || termo.trim().isEmpty()) {
            return List.of();
        }

        Pageable limit = PageRequest.of(0, 10, Sort.by("nome").ascending());

        return competicaoRepository.findByAtivoTrueAndNomeContainingIgnoreCase(termo.trim(), limit)
                .getContent();
    }

    public CompeticaoDTO buscarDetalhesPorId(String id) {
        return competicaoRepository.buscarDetalhesPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Competição não encontrada com ID: " + id));
    }

    @Transactional
    public void vincularTitulo(String competicaoId, String tituloId) {
        Competicao competicao = competicaoRepository.findById(competicaoId)
                .orElseThrow(() -> new EntityNotFoundException("Competição não encontrada com ID: " + competicaoId));

        if (tituloId == null || tituloId.isBlank()) {
            competicao.setTitulo(null);
        } else {
            Titulo titulo = tituloRepository.findById(tituloId)
                    .orElseThrow(() -> new EntityNotFoundException("Título não encontrado com ID: " + tituloId));
            competicao.setTitulo(titulo);
        }

        competicaoRepository.save(competicao);
    }

    @Transactional
    public void alternarStatus(String competicaoId, boolean ativo) {
        Competicao competicao = competicaoRepository.findById(competicaoId)
                .orElseThrow(() -> new EntityNotFoundException("Competição não encontrada com ID: " + competicaoId));

        competicao.setAtivo(ativo);
        competicaoRepository.save(competicao);
    }
}