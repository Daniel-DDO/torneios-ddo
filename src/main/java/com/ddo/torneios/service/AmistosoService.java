package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.Amistoso;
import com.ddo.torneios.repository.AmistosoRepository;
import com.ddo.torneios.repository.ClubeRepository;
import com.ddo.torneios.repository.JogadorRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class AmistosoService {

    private static final BigDecimal VALOR_PARTICIPACAO = new BigDecimal("5000.00");
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    @Autowired
    private AmistosoRepository amistosoRepository;
    @Autowired
    private JogadorRepository jogadorRepository;
    @Autowired
    private ClubeRepository clubeRepository;
    @Autowired
    private JogadorService jogadorService;

    // ---- Criação (PROPRIETARIO / DIRETOR) ----

    @Transactional
    public AmistosoDTO criar(CriarAmistosoRequest req) {
        if (req.jogadorMandanteId().equals(req.jogadorVisitanteId())) {
            throw new IllegalStateException("Um jogador não pode enfrentar a si mesmo");
        }
        if (!jogadorRepository.existsById(req.jogadorMandanteId())
                || !jogadorRepository.existsById(req.jogadorVisitanteId())) {
            throw new EntityNotFoundException("Jogador não encontrado");
        }
        if (!clubeRepository.existsById(req.clubeMandanteId())
                || !clubeRepository.existsById(req.clubeVisitanteId())) {
            throw new EntityNotFoundException("Clube não encontrado");
        }

        Amistoso a = new Amistoso();
        a.setJogadorMandante(jogadorRepository.getReferenceById(req.jogadorMandanteId()));
        a.setClubeMandante(clubeRepository.getReferenceById(req.clubeMandanteId()));
        a.setJogadorVisitante(jogadorRepository.getReferenceById(req.jogadorVisitanteId()));
        a.setClubeVisitante(clubeRepository.getReferenceById(req.clubeVisitanteId()));
        a.setDataHora(req.dataHora());
        a.setCriadoEm(LocalDateTime.now(BRASILIA));
        amistosoRepository.save(a);

        return buscarPorId(a.getId());
    }

    // ---- Registro / correção de resultado (PROPRIETARIO / DIRETOR / ADMINISTRADOR) ----

    @Transactional
    public AmistosoDTO registrarResultado(String id, RegistrarResultadoAmistosoRequest req) {
        if (req.golsMandante() == null || req.golsVisitante() == null) {
            throw new IllegalArgumentException("Informe o placar");
        }
        if (req.wo() && req.golsMandante().equals(req.golsVisitante())) {
            throw new IllegalArgumentException("W.O. exige um vencedor: o placar não pode ser empate");
        }

        Amistoso a = amistosoRepository.buscarParaAtualizar(id)
                .orElseThrow(() -> new EntityNotFoundException("Amistoso não encontrado"));

        a.setGolsMandante(req.golsMandante());
        a.setGolsVisitante(req.golsVisitante());
        a.setWo(req.wo());
        a.setCartoesAmarelosMandante(zeroSeNulo(req.cartoesAmarelosMandante()));
        a.setCartoesVermelhosMandante(zeroSeNulo(req.cartoesVermelhosMandante()));
        a.setCartoesAmarelosVisitante(zeroSeNulo(req.cartoesAmarelosVisitante()));
        a.setCartoesVermelhosVisitante(zeroSeNulo(req.cartoesVermelhosVisitante()));
        if (req.linkPartida() != null && !req.linkPartida().isBlank()) {
            a.setLinkPartida(req.linkPartida());
        }
        if (!a.isRealizada()) {
            a.setRealizada(true);
            a.setRegistradoEm(LocalDateTime.now(BRASILIA));
        }

        boolean mandanteRecebe = true;
        boolean visitanteRecebe = true;
        if (a.isWo()) { // W.O.: só o vencedor recebe
            mandanteRecebe = a.getGolsMandante() > a.getGolsVisitante();
            visitanteRecebe = a.getGolsVisitante() > a.getGolsMandante();
        }

        BigDecimal devidoMandante = mandanteRecebe ? VALOR_PARTICIPACAO : BigDecimal.ZERO;
        BigDecimal devidoVisitante = visitanteRecebe ? VALOR_PARTICIPACAO : BigDecimal.ZERO;

        // Credita/estorna apenas a diferença em relação ao que já foi pago: corrigir não duplica pagamento
        ajustarSaldo(a.getJogadorMandante().getId(), devidoMandante.subtract(a.getReceitaMandante()));
        ajustarSaldo(a.getJogadorVisitante().getId(), devidoVisitante.subtract(a.getReceitaVisitante()));
        a.setReceitaMandante(devidoMandante);
        a.setReceitaVisitante(devidoVisitante);

        return buscarPorId(id);
    }

    private void ajustarSaldo(String jogadorId, BigDecimal diferenca) {
        int sinal = diferenca.signum();
        if (sinal == 0) return;

        MovimentacaoSaldoDTO mov = sinal > 0
                ? new MovimentacaoSaldoDTO(diferenca, "Participação em amistoso",
                MovimentacaoSaldoDTO.TipoOperacao.ADICIONAR, false)
                : new MovimentacaoSaldoDTO(diferenca.abs(), "Ajuste de amistoso (resultado corrigido)",
                MovimentacaoSaldoDTO.TipoOperacao.REMOVER, true);

        // Sem try/catch de propósito: se o saldo falhar, o registro inteiro é revertido.
        jogadorService.atualizarSaldo(jogadorId, mov, "SISTEMA");
    }

    // ---- Consultas ----

    @Transactional(readOnly = true)
    public AmistosoDTO buscarPorId(String id) {
        return amistosoRepository.buscarDetalhe(id)
                .orElseThrow(() -> new EntityNotFoundException("Amistoso não encontrado"));
    }

    @Transactional(readOnly = true)
    public PaginacaoDTO<AmistosoDTO> listar(Boolean realizada, int pagina, int tamanho) {
        PageRequest pr = PageRequest.of(pagina, limitar(tamanho));
        Page<AmistosoDTO> page = realizada == null
                ? amistosoRepository.listarTodos(pr)
                : amistosoRepository.listarPorStatus(realizada, pr);
        return montar(page);
    }

    @Transactional(readOnly = true)
    public PaginacaoDTO<AmistosoDTO> listarPorJogador(String jogadorId, Boolean realizada, int pagina, int tamanho) {
        PageRequest pr = PageRequest.of(pagina, limitar(tamanho));
        Page<AmistosoDTO> page;
        if (realizada == null) {
            page = amistosoRepository.listarPorJogador(jogadorId, pr);
        } else if (realizada) {
            page = amistosoRepository.listarRealizadasPorJogador(jogadorId, pr);
        } else {
            page = amistosoRepository.listarPendentesPorJogador(jogadorId, pr);
        }
        return montar(page);
    }

    @Transactional(readOnly = true)
    public AmistosoEstatisticaDTO estatisticasDoJogador(String jogadorId) {
        return amistosoRepository.buscarEstatisticasDoJogador(jogadorId);
    }

    // ---- util ----

    private PaginacaoDTO<AmistosoDTO> montar(Page<AmistosoDTO> p) {
        return new PaginacaoDTO<>(p.getContent(), p.getNumber(), p.getTotalPages(),
                p.getTotalElements(), p.getSize(), p.isLast());
    }

    private int limitar(int tamanho) {
        return Math.min(Math.max(tamanho, 1), 50);
    }

    private int zeroSeNulo(Integer v) {
        return v == null ? 0 : v;
    }
}