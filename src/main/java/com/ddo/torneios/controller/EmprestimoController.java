package com.ddo.torneios.controller;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.Cargo;
import com.ddo.torneios.repository.JogadorRepository;
import com.ddo.torneios.exception.RegraNegocioException;
import com.ddo.torneios.request.EmprestimoRequest;
import com.ddo.torneios.service.EmprestimoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/emprestimos")
@RequiredArgsConstructor
public class EmprestimoController {

    private final EmprestimoService emprestimoService;
    private final JogadorRepository jogadorRepository;

    @GetMapping("/{jogadorId}/elegibilidade")
    public ResponseEntity<ElegibilidadeEmprestimoDTO> verificarElegibilidade(@PathVariable String jogadorId) {
        return ResponseEntity.ok(emprestimoService.verificarElegibilidade(jogadorId));
    }

    @GetMapping("/simular")
    public ResponseEntity<SimulacaoEmprestimoDTO> simular(
            @RequestParam BigDecimal valor,
            @RequestParam Integer parcelas) {
        return ResponseEntity.ok(emprestimoService.simular(valor, parcelas));
    }

    @PostMapping
    public ResponseEntity<EmprestimoDTO> solicitar(@RequestBody EmprestimoRequest request) {
        return ResponseEntity.ok(emprestimoService.solicitarEmprestimo(request));
    }

    @GetMapping("/{jogadorId}/ativo")
    public ResponseEntity<EmprestimoDTO> buscarAtivo(@PathVariable String jogadorId) {
        return ResponseEntity.ok(emprestimoService.buscarEmprestimoAtivo(jogadorId));
    }

    @GetMapping("/{jogadorId}/historico")
    public ResponseEntity<List<EmprestimoDTO>> listarHistorico(@PathVariable String jogadorId) {
        return ResponseEntity.ok(emprestimoService.listarHistorico(jogadorId));
    }

    @PostMapping("/parcelas/{parcelaId}/pagar-antecipado")
    public ResponseEntity<ParcelaDTO> pagarAntecipado(@PathVariable String parcelaId) {
        return ResponseEntity.ok(emprestimoService.pagarParcelaAntecipadamente(parcelaId));
    }

    /**
     * Fallback manual do PROPRIETARIO: cobra na força as parcelas que vencem hoje,
     * caso o job agendado (EmprestimoScheduler) não tenha rodado.
     * Só pega parcelas com vencimento no dia de hoje — não antecipa nem atrasa nada.
     */
    @PostMapping("/admin/forcar-recebimento")
    @PreAuthorize("hasRole('PROPRIETARIO')")
    public ResponseEntity<String> forcarRecebimento(@RequestParam String idAdmin) {
        var admin = jogadorRepository.findById(idAdmin)
                .orElseThrow(() -> new EntityNotFoundException("Admin não encontrado"));

        if (admin.getCargo() != Cargo.PROPRIETARIO) {
            throw new RegraNegocioException("Apenas o PROPRIETARIO pode forçar o recebimento do banco.");
        }

        int processadas = emprestimoService.forcarRecebimentoDoDia();
        return ResponseEntity.ok(processadas + " parcela(s) com vencimento hoje foram cobradas.");
    }
}