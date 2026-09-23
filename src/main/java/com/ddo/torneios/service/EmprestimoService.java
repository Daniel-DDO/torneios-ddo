package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.request.*;
import com.ddo.torneios.exception.EmprestimoNaoElegivelException;
import com.ddo.torneios.exception.RegraNegocioException;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.EmprestimoRepository;
import com.ddo.torneios.repository.JogadorRepository;
import com.ddo.torneios.repository.ParcelaEmprestimoRepository;
import com.ddo.torneios.repository.TransacaoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
public class EmprestimoService {

    @Autowired private EmprestimoRepository emprestimoRepository;
    @Autowired private ParcelaEmprestimoRepository parcelaEmprestimoRepository;
    @Autowired private JogadorRepository jogadorRepository;
    @Autowired private TransacaoRepository transacaoRepository;
    @Autowired private NotificacaoService notificacaoService;
    @Autowired private CartaoGlobalCache cartaoGlobalCache;

    // ---------------------------------------------------------------------
    // TABELAS DE REGRAS DE NEGÓCIO
    // ---------------------------------------------------------------------

    /** juros (%) por quantidade de parcelas: 1x = 4%, 2x = 6%, ... 12x = 26% (passo de 2 em 2) */
    private BigDecimal jurosPorParcelas(int parcelas) {
        int percentual = 2 + (parcelas * 2); // 1->4, 2->6 ... 12->26
        return BigDecimal.valueOf(percentual);
    }

    /** limite de empréstimo (sem considerar histórico de negativação) de acordo com partidas jogadas */
    private static final TreeMap<Integer, BigDecimal> LIMITE_POR_PARTIDAS = new TreeMap<>(Map.of(
            10, new BigDecimal("100000"),
            25, new BigDecimal("250000"),
            41, new BigDecimal("400000"),
            71, new BigDecimal("700000"),
            101, new BigDecimal("1000000"),
            151, new BigDecimal("1400000"),
            201, new BigDecimal("2000000")
    ));

    private static final int MINIMO_PARTIDAS_PARA_EMPRESTIMO = 10;
    private static final BigDecimal SALDO_MINIMO_NUNCA_NEGATIVADO = new BigDecimal("50000");
    private static final BigDecimal SALDO_MINIMO_JA_NEGATIVADO = new BigDecimal("100000");
    private static final BigDecimal TETO_EMPRESTIMO = new BigDecimal("2000000");
    private static final int DIAS_CRITERIO_RIGOROSO_APOS_NEGATIVACAO = 30;

    /** Redução máxima no limite de empréstimo pra quem tem cartão muito acima da média (10%) */
    private static final BigDecimal REDUCAO_MAXIMA_CARTOES = new BigDecimal("0.10");

    // ---------------------------------------------------------------------
    // ELEGIBILIDADE
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ElegibilidadeEmprestimoDTO verificarElegibilidade(String jogadorId) {
        JogadorEmprestimoProjecaoDTO jogador = jogadorRepository.buscarProjecaoEmprestimoPorId(jogadorId)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado com ID: " + jogadorId));

        return calcularElegibilidade(jogador);
    }

    private ElegibilidadeEmprestimoDTO calcularElegibilidade(JogadorEmprestimoProjecaoDTO jogador) {
        int partidas = jogador.partidasJogadas() != null ? jogador.partidasJogadas() : 0;
        BigDecimal saldo = jogador.saldoVirtual() != null ? jogador.saldoVirtual() : BigDecimal.ZERO;

        // 1) já tem empréstimo em andamento?
        if (emprestimoRepository.existsByJogador_IdAndStatus(jogador.id(), StatusEmprestimo.EM_ANDAMENTO)) {
            return negado(jogador, "Você já possui um empréstimo em andamento. Quite-o antes de pedir outro.");
        }

        // 2) está negativado agora?
        if (jogador.negativado() || saldo.compareTo(BigDecimal.ZERO) < 0) {
            return negado(jogador, "Seu nome está negativado. Regularize seu saldo antes de pedir um novo empréstimo.");
        }

        // 3) mínimo de partidas
        if (partidas < MINIMO_PARTIDAS_PARA_EMPRESTIMO) {
            return negado(jogador, "É necessário ter jogado ao menos " + MINIMO_PARTIDAS_PARA_EMPRESTIMO + " partidas para pedir empréstimo.");
        }

        // 4) histórico de negativação -> saldo mínimo exigido é maior
        boolean jaFoiNegativado = jogador.jaFoiNegativadoAlgumaVez();
        BigDecimal saldoMinimoExigido = jaFoiNegativado ? SALDO_MINIMO_JA_NEGATIVADO : SALDO_MINIMO_NUNCA_NEGATIVADO;

        if (saldo.compareTo(saldoMinimoExigido) < 0) {
            return negado(jogador, "Saldo mínimo exigido para pedir empréstimo é de " + formatarMoeda(saldoMinimoExigido) +
                    (jaFoiNegativado ? " (exigência maior por já ter ficado negativado antes)." : "."));
        }

        // 5) limite base pelas partidas jogadas
        BigDecimal limite = limitePorPartidas(partidas);

        // 6) critério mais rigoroso se saiu da negativação há até 30 dias (mesmo já limpo)
        if (jaFoiNegativado && jogador.dataQuitacaoNegativacao() != null) {
            long diasDesdeQuitacao = ChronoUnit.DAYS.between(jogador.dataQuitacaoNegativacao(), LocalDateTime.now());
            if (diasDesdeQuitacao <= DIAS_CRITERIO_RIGOROSO_APOS_NEGATIVACAO) {
                limite = limite.multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.DOWN);
            }
        }

        // 7) penalização por cartões (amarelos + vermelhos) muito acima da média: até 10% a menos
        limite = aplicarPenalizacaoCartoes(limite, jogador, partidas);

        limite = limite.min(TETO_EMPRESTIMO);

        return new ElegibilidadeEmprestimoDTO(true, "Elegível para empréstimo.", limite, saldoMinimoExigido, saldo, partidas);
    }

    private ElegibilidadeEmprestimoDTO negado(JogadorEmprestimoProjecaoDTO jogador, String motivo) {
        int partidas = jogador.partidasJogadas() != null ? jogador.partidasJogadas() : 0;
        BigDecimal saldo = jogador.saldoVirtual() != null ? jogador.saldoVirtual() : BigDecimal.ZERO;
        return new ElegibilidadeEmprestimoDTO(false, motivo, BigDecimal.ZERO, null, saldo, partidas);
    }

    private BigDecimal limitePorPartidas(int partidas) {
        Map.Entry<Integer, BigDecimal> entry = LIMITE_POR_PARTIDAS.floorEntry(partidas);
        return entry != null ? entry.getValue() : BigDecimal.ZERO;
    }

    /**
     * Quem tem (cartões amarelos + vermelhos) por partida acima da média global
     * perde até 10% do limite calculado. Quanto maior o excesso em relação à
     * média, mais perto do teto de 10%, mas nunca passa disso.
     */
    private BigDecimal aplicarPenalizacaoCartoes(BigDecimal limite, JogadorEmprestimoProjecaoDTO jogador, int partidas) {
        double mediaGlobal = cartaoGlobalCache.obterMediaCartoesPorPartida();
        if (mediaGlobal <= 0 || partidas <= 0) {
            return limite;
        }

        long amarelos = jogador.cartoesAmarelos() != null ? jogador.cartoesAmarelos() : 0L;
        long vermelhos = jogador.cartoesVermelhos() != null ? jogador.cartoesVermelhos() : 0L;
        double cartoesPorPartidaDoJogador = (amarelos + vermelhos) / (double) partidas;

        if (cartoesPorPartidaDoJogador <= mediaGlobal) {
            return limite;
        }

        double excessoPercentual = (cartoesPorPartidaDoJogador - mediaGlobal) / mediaGlobal;
        BigDecimal reducao = BigDecimal.valueOf(excessoPercentual).min(REDUCAO_MAXIMA_CARTOES);

        return limite.multiply(BigDecimal.ONE.subtract(reducao)).setScale(2, RoundingMode.DOWN);
    }

    // ---------------------------------------------------------------------
    // SIMULAÇÃO (preview sem persistir nada)
    // ---------------------------------------------------------------------

    public SimulacaoEmprestimoDTO simular(BigDecimal valorSolicitado, int quantidadeParcelas) {
        validarParcelas(quantidadeParcelas);
        BigDecimal juros = jurosPorParcelas(quantidadeParcelas);
        BigDecimal valorTotal = calcularValorComJuros(valorSolicitado, juros);
        BigDecimal valorParcela = valorTotal.divide(BigDecimal.valueOf(quantidadeParcelas), 2, RoundingMode.HALF_EVEN);
        BigDecimal totalJuros = valorTotal.subtract(valorSolicitado);

        return new SimulacaoEmprestimoDTO(valorSolicitado, quantidadeParcelas, juros, valorTotal, valorParcela, totalJuros);
    }

    private BigDecimal calcularValorComJuros(BigDecimal valor, BigDecimal percentualJuros) {
        BigDecimal fator = BigDecimal.ONE.add(percentualJuros.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_EVEN));
        return valor.multiply(fator).setScale(2, RoundingMode.HALF_EVEN);
    }

    private void validarParcelas(int quantidadeParcelas) {
        if (quantidadeParcelas < 1 || quantidadeParcelas > 12) {
            throw new RegraNegocioException("A quantidade de parcelas deve ser entre 1 e 12.");
        }
    }

    // ---------------------------------------------------------------------
    // SOLICITAÇÃO / CONTRATAÇÃO
    // ---------------------------------------------------------------------

    @Transactional
    public EmprestimoDTO solicitarEmprestimo(EmprestimoRequest request) {
        validarParcelas(request.quantidadeParcelas());

        if (request.valorSolicitado() == null || request.valorSolicitado().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("O valor solicitado deve ser maior que zero.");
        }

        JogadorEmprestimoProjecaoDTO projecao = jogadorRepository.buscarProjecaoEmprestimoPorId(request.jogadorId())
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado com ID: " + request.jogadorId()));

        ElegibilidadeEmprestimoDTO elegibilidade = calcularElegibilidade(projecao);
        if (!elegibilidade.elegivel()) {
            throw new EmprestimoNaoElegivelException(elegibilidade.motivo());
        }

        if (request.valorSolicitado().compareTo(elegibilidade.limiteMaximo()) > 0) {
            throw new EmprestimoNaoElegivelException("Valor solicitado (" + formatarMoeda(request.valorSolicitado()) +
                    ") excede o limite disponível de " + formatarMoeda(elegibilidade.limiteMaximo()) + " para o seu perfil.");
        }

        Jogador jogador = jogadorRepository.findById(request.jogadorId())
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado"));

        BigDecimal juros = jurosPorParcelas(request.quantidadeParcelas());
        BigDecimal valorTotal = calcularValorComJuros(request.valorSolicitado(), juros);
        BigDecimal valorParcela = valorTotal.divide(BigDecimal.valueOf(request.quantidadeParcelas()), 2, RoundingMode.HALF_EVEN);

        Emprestimo emprestimo = new Emprestimo();
        emprestimo.setJogador(jogador);
        emprestimo.setValorSolicitado(request.valorSolicitado());
        emprestimo.setPercentualJuros(juros);
        emprestimo.setValorTotalComJuros(valorTotal);
        emprestimo.setValorParcela(valorParcela);
        emprestimo.setQuantidadeParcelas(request.quantidadeParcelas());
        emprestimo.setParcelasPagas(0);
        emprestimo.setStatus(StatusEmprestimo.EM_ANDAMENTO);
        emprestimo.setDataContratacao(LocalDateTime.now());

        for (int i = 1; i <= request.quantidadeParcelas(); i++) {
            ParcelaEmprestimo parcela = new ParcelaEmprestimo();
            parcela.setEmprestimo(emprestimo);
            parcela.setNumeroParcela(i);
            parcela.setValor(valorParcela);
            parcela.setDataVencimento(LocalDateTime.now().plusWeeks(i));
            parcela.setPaga(false);
            emprestimo.getParcelas().add(parcela);
        }

        emprestimo = emprestimoRepository.save(emprestimo);

        // credita o valor solicitado na conta do jogador
        BigDecimal saldoAnterior = jogador.getSaldoVirtual() != null ? jogador.getSaldoVirtual() : BigDecimal.ZERO;
        BigDecimal saldoPosterior = saldoAnterior.add(request.valorSolicitado());
        jogador.setSaldoVirtual(saldoPosterior);
        jogadorRepository.save(jogador);

        transacaoRepository.save(new Transacao(
                jogador, TipoTransacao.CREDITO, request.valorSolicitado(), saldoAnterior, saldoPosterior,
                "Empréstimo liberado (" + request.quantidadeParcelas() + "x, juros de " + juros + "%)",
                "SISTEMA_BANCO"
        ));

        notificarSeguro(jogador, "Empréstimo aprovado!",
                "Você recebeu " + formatarMoeda(request.valorSolicitado()) + " em " + request.quantidadeParcelas() +
                        " parcela(s) de " + formatarMoeda(valorParcela) + ".");

        return EmprestimoDTO.de(emprestimo);
    }

    // ---------------------------------------------------------------------
    // CONSULTA
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public EmprestimoDTO buscarEmprestimoAtivo(String jogadorId) {
        Emprestimo emprestimo = emprestimoRepository.findByJogador_IdAndStatus(jogadorId, StatusEmprestimo.EM_ANDAMENTO)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não possui empréstimo em andamento."));
        return EmprestimoDTO.de(emprestimoRepository.buscarComParcelas(emprestimo.getId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<EmprestimoDTO> listarHistorico(String jogadorId) {
        return emprestimoRepository.buscarHistoricoComParcelas(jogadorId)
                .stream().map(EmprestimoDTO::de).toList();
    }

    // ---------------------------------------------------------------------
    // CONSULTAS PÚBLICAS (visível pra todo mundo, não só o dono da conta)
    // ---------------------------------------------------------------------

    /** Lista pública paginada: quem pegou empréstimo, de quanto, quanto já pagou. */
    @Transactional(readOnly = true)
    public Page<EmprestimoPublicoDTO> listarPublico(Pageable pageable) {
        return emprestimoRepository.buscarTodosParaListaPublica(pageable).map(EmprestimoPublicoDTO::de);
    }

    /** Todo mundo com o nome sujo (negativado) agora. */
    @Transactional(readOnly = true)
    public List<StatusNomeDTO> listarNomesSujos() {
        return jogadorRepository.buscarNomesSujos();
    }

    /** Situação de nome (limpo/sujo) de um jogador específico. */
    @Transactional(readOnly = true)
    public StatusNomeDTO buscarStatusNome(String jogadorId) {
        return jogadorRepository.buscarStatusNomePorId(jogadorId)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado com ID: " + jogadorId));
    }

    /** Visão completa: nome limpo/sujo + elegibilidade atual + empréstimo em andamento (se tiver). */
    @Transactional(readOnly = true)
    public SituacaoJogadorEmprestimoDTO obterSituacaoCompleta(String jogadorId) {
        JogadorEmprestimoProjecaoDTO jogador = jogadorRepository.buscarProjecaoEmprestimoPorId(jogadorId)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado com ID: " + jogadorId));

        ElegibilidadeEmprestimoDTO elegibilidade = calcularElegibilidade(jogador);

        EmprestimoDTO emprestimoAtivo = emprestimoRepository.findByJogador_IdAndStatus(jogadorId, StatusEmprestimo.EM_ANDAMENTO)
                .flatMap(e -> emprestimoRepository.buscarComParcelas(e.getId()))
                .map(EmprestimoDTO::de)
                .orElse(null);

        return new SituacaoJogadorEmprestimoDTO(
                jogador.id(), jogador.nome(), jogador.discord(), jogador.imagem(),
                jogador.negativado(), jogador.jaFoiNegativadoAlgumaVez(),
                jogador.dataNegativacao(), jogador.dataQuitacaoNegativacao(),
                elegibilidade, emprestimoAtivo
        );
    }

    // ---------------------------------------------------------------------
    // PAGAMENTO ANTECIPADO (jogador escolhe pagar antes do vencimento)
    // ---------------------------------------------------------------------

    @Transactional
    public ParcelaDTO pagarParcelaAntecipadamente(String parcelaId) {
        ParcelaEmprestimo parcela = parcelaEmprestimoRepository.findByIdAndPagaFalse(parcelaId)
                .orElseThrow(() -> new EntityNotFoundException("Parcela não encontrada ou já paga."));

        processarPagamentoDeParcela(parcela);
        return ParcelaDTO.de(parcela);
    }

    // ---------------------------------------------------------------------
    // COBRANÇA AUTOMÁTICA (scheduler) / FORÇADA (admin)
    // ---------------------------------------------------------------------

    /** Chamado pelo job agendado diariamente: cobra tudo que já venceu e ainda não foi pago. */
    @Transactional
    public int processarParcelasVencidas() {
        List<ParcelaEmprestimo> pendentes = parcelaEmprestimoRepository.buscarParcelasVencidasPendentes(LocalDateTime.now());
        for (ParcelaEmprestimo parcela : pendentes) {
            processarPagamentoDeParcela(parcela);
        }
        return pendentes.size();
    }

    /** Chamado pelo endpoint do PROPRIETARIO: cobra só o que vence hoje (fallback manual). */
    @Transactional
    public int forcarRecebimentoDoDia() {
        LocalDateTime inicioDia = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime fimDia = inicioDia.plusDays(1);

        List<ParcelaEmprestimo> parcelasDoDia = parcelaEmprestimoRepository.buscarParcelasQueVencemHoje(inicioDia, fimDia);
        for (ParcelaEmprestimo parcela : parcelasDoDia) {
            processarPagamentoDeParcela(parcela);
        }
        return parcelasDoDia.size();
    }

    /** Núcleo comum do débito de parcela, usado pelo pagamento antecipado, pelo job e pela cobrança forçada. */
    private void processarPagamentoDeParcela(ParcelaEmprestimo parcela) {
        Emprestimo emprestimo = parcela.getEmprestimo();
        Jogador jogador = emprestimo.getJogador();

        BigDecimal saldoAnterior = jogador.getSaldoVirtual() != null ? jogador.getSaldoVirtual() : BigDecimal.ZERO;
        BigDecimal saldoPosterior = saldoAnterior.subtract(parcela.getValor());

        jogador.setSaldoVirtual(saldoPosterior);

        boolean ficouNegativado = saldoPosterior.compareTo(BigDecimal.ZERO) < 0;

        if (ficouNegativado) {
            if (!jogador.isNegativado()) {
                jogador.setNegativado(true);
                jogador.setDataNegativacao(LocalDateTime.now());
                jogador.setJaFoiNegativadoAlgumaVez(true);
            }
            parcela.setPagaComSaldoNegativo(true);
        } else if (jogador.isNegativado()) {
            // saldo voltou a ficar positivo/zero: sai da inadimplência agora
            jogador.setNegativado(false);
            jogador.setDataQuitacaoNegativacao(LocalDateTime.now());
        }

        parcela.setPaga(true);
        parcela.setDataPagamento(LocalDateTime.now());

        emprestimo.setParcelasPagas(emprestimo.getParcelasPagas() + 1);
        if (emprestimo.getParcelasPagas().intValue() >= emprestimo.getQuantidadeParcelas().intValue()) {
            emprestimo.setStatus(StatusEmprestimo.QUITADO);
            emprestimo.setDataQuitacao(LocalDateTime.now());
        }

        jogadorRepository.save(jogador);
        emprestimoRepository.save(emprestimo);
        parcelaEmprestimoRepository.save(parcela);

        transacaoRepository.save(new Transacao(
                jogador, TipoTransacao.DEBITO, parcela.getValor(), saldoAnterior, saldoPosterior,
                "Pagamento parcela " + parcela.getNumeroParcela() + "/" + emprestimo.getQuantidadeParcelas() +
                        " do empréstimo" + (ficouNegativado ? " (saldo insuficiente, conta negativada)" : ""),
                "SISTEMA_BANCO"
        ));

        String titulo = ficouNegativado ? "Parcela debitada — saldo negativo" : "Parcela do empréstimo paga";
        String mensagem = ficouNegativado
                ? "Foi debitado " + formatarMoeda(parcela.getValor()) + " da sua conta e seu saldo ficou negativo. Você está negativado."
                : "Foi debitado " + formatarMoeda(parcela.getValor()) + " referente à parcela " + parcela.getNumeroParcela() +
                "/" + emprestimo.getQuantidadeParcelas() + " do seu empréstimo.";
        notificarSeguro(jogador, titulo, mensagem);

        log.info("Parcela {} do empréstimo {} processada. Jogador {} - negativado: {}",
                parcela.getNumeroParcela(), emprestimo.getId(), jogador.getId(), ficouNegativado);
    }

    private void notificarSeguro(Jogador jogador, String titulo, String mensagem) {
        try {
            notificacaoService.enviarParaJogador(jogador, titulo, mensagem, "/minha-conta/emprestimos", TipoNotificacao.INFORMACAO);
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de empréstimo para o jogador {}", jogador.getId(), e);
        }
    }

    private String formatarMoeda(BigDecimal valor) {
        return java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("pt", "BR")).format(valor);
    }
}