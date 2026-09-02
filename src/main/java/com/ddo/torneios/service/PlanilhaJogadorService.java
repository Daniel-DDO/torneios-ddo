package com.ddo.torneios.service;

import com.ddo.torneios.dto.JogadorHistoriaDTO;
import com.ddo.torneios.dto.RivalidadeDTO;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.repository.JogadorRepository;
import jakarta.persistence.EntityNotFoundException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PlanilhaJogadorService {

    @Autowired
    private JogadorRepository jogadorRepository;

    @Autowired
    private JogadorService jogadorService;

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] gerarPlanilha(String jogadorId) {
        Jogador jogador = jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado com ID: " + jogadorId));

        JogadorHistoriaDTO historia = jogadorService.obterResumoHistoria(jogadorId);
        List<RivalidadeDTO> carrascos = jogadorService.buscarTop3Carrascos(jogadorId);
        List<RivalidadeDTO> patos = jogadorService.buscarTop3Patos(jogadorId);
        List<String> momento = jogadorService.obterMomentoAtual(jogadorId);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle estiloTitulo = criarEstiloTitulo(workbook);
            CellStyle estiloCabecalho = criarEstiloCabecalho(workbook);
            CellStyle estiloLabel = criarEstiloLabel(workbook);

            criarAbaResumo(workbook, jogador, historia, momento, estiloTitulo, estiloCabecalho, estiloLabel);
            criarAbaRivalidades(workbook, "Carrascos (quem mais venceu você)", carrascos, estiloTitulo, estiloCabecalho);
            criarAbaRivalidades(workbook, "Patos (quem você mais venceu)", patos, estiloTitulo, estiloCabecalho);

            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            workbook.write(saida);
            return saida.toByteArray();

        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao gerar planilha do jogador", e);
        }
    }

    private void criarAbaResumo(
            XSSFWorkbook workbook,
            Jogador jogador,
            JogadorHistoriaDTO historia,
            List<String> momento,
            CellStyle estiloTitulo,
            CellStyle estiloCabecalho,
            CellStyle estiloLabel) {

        Sheet aba = workbook.createSheet("Resumo");
        int linhaAtual = 0;

        Row linhaTitulo = aba.createRow(linhaAtual++);
        Cell celulaTitulo = linhaTitulo.createCell(0);
        celulaTitulo.setCellValue("Estatísticas de " + jogador.getNome());
        celulaTitulo.setCellStyle(estiloTitulo);
        aba.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 1));

        linhaAtual++;

        linhaAtual = escreverLinha(aba, linhaAtual, "Discord", jogador.getDiscord(), estiloLabel);
        linhaAtual = escreverLinha(aba, linhaAtual, "Cargo", jogador.getCargo().name(), estiloLabel);
        linhaAtual = escreverLinha(aba, linhaAtual, "Conta desde", jogador.getCriacaoConta().format(FORMATO_DATA), estiloLabel);

        linhaAtual++;
        linhaAtual = escreverCabecalhoSecao(aba, linhaAtual, "Desempenho geral", estiloCabecalho);

        linhaAtual = escreverLinha(aba, linhaAtual, "Partidas jogadas", (historia.vitorias()+historia.derrotas()+historia.empates()), estiloLabel);
        linhaAtual = escreverLinha(aba, linhaAtual, "Vitórias", historia.vitorias(), estiloLabel);
        linhaAtual = escreverLinha(aba, linhaAtual, "Empates", historia.empates(), estiloLabel);
        linhaAtual = escreverLinha(aba, linhaAtual, "Derrotas", historia.derrotas(), estiloLabel);
        linhaAtual = escreverLinha(aba, linhaAtual, "Aproveitamento", historia.aproveitamento(), estiloLabel);
        linhaAtual = escreverLinha(aba, linhaAtual, "Gols marcados", historia.golsMarcados(), estiloLabel);
        linhaAtual = escreverLinha(aba, linhaAtual, "Gols sofridos", historia.golsSofridos(), estiloLabel);
        linhaAtual = escreverLinha(aba, linhaAtual, "Saldo de gols", historia.saldoGols(), estiloLabel);
        linhaAtual = escreverLinha(aba, linhaAtual, "Média de gols por jogo", historia.golsMarcados()/(historia.vitorias()+historia.derrotas()+historia.empates()), estiloLabel);

        linhaAtual++;
        linhaAtual = escreverCabecalhoSecao(aba, linhaAtual, "Conquistas", estiloCabecalho);

        linhaAtual = escreverLinha(aba, linhaAtual, "Títulos", historia.titulos(), estiloLabel);
        linhaAtual = escreverLinha(aba, linhaAtual, "Finais disputadas", historia.finais(), estiloLabel);
        linhaAtual = escreverLinha(aba, linhaAtual, "Pontos de coeficiente", historia.pontosCoeficiente(), estiloLabel);

        linhaAtual++;
        linhaAtual = escreverCabecalhoSecao(aba, linhaAtual, "Últimos resultados", estiloCabecalho);

        Row linhaMomento = aba.createRow(linhaAtual++);
        linhaMomento.createCell(0).setCellValue(String.join(" | ", momento));

        aba.autoSizeColumn(0);
        aba.autoSizeColumn(1);
    }

    private void criarAbaRivalidades(
            XSSFWorkbook workbook,
            String nomeAba,
            List<RivalidadeDTO> rivalidades,
            CellStyle estiloTitulo,
            CellStyle estiloCabecalho) {

        Sheet aba = workbook.createSheet(nomeAba.length() > 31 ? nomeAba.substring(0, 31) : nomeAba);
        int linhaAtual = 0;

        Row linhaTitulo = aba.createRow(linhaAtual++);
        Cell celulaTitulo = linhaTitulo.createCell(0);
        celulaTitulo.setCellValue(nomeAba);
        celulaTitulo.setCellStyle(estiloTitulo);

        linhaAtual++;

        String[] colunas = {"Adversário", "Discord", "Jogos", "Vitórias", "Empates", "Derrotas", "Gols feitos", "Gols sofridos", "Saldo", "Aproveitamento"};
        Row linhaCabecalho = aba.createRow(linhaAtual++);
        for (int i = 0; i < colunas.length; i++) {
            Cell celula = linhaCabecalho.createCell(i);
            celula.setCellValue(colunas[i]);
            celula.setCellStyle(estiloCabecalho);
        }

        for (RivalidadeDTO r : rivalidades) {
            Row linha = aba.createRow(linhaAtual++);
            linha.createCell(0).setCellValue(r.getAdversarioNome());
            linha.createCell(1).setCellValue(r.getAdversarioDiscord());
            linha.createCell(2).setCellValue(r.getPartidasJogadas());
            linha.createCell(3).setCellValue(r.getMinhasVitorias());
            linha.createCell(4).setCellValue(r.getMeusEmpates());
            linha.createCell(5).setCellValue(r.getMinhasDerrotas());
            linha.createCell(6).setCellValue(r.getGolsFeitos());
            linha.createCell(7).setCellValue(r.getGolsSofridos());
            linha.createCell(8).setCellValue(r.getSaldoGols());
            linha.createCell(9).setCellValue(r.getAproveitamento());
        }

        for (int i = 0; i < colunas.length; i++) {
            aba.autoSizeColumn(i);
        }
    }

    private int escreverLinha(Sheet aba, int linhaAtual, String label, Object valor, CellStyle estiloLabel) {
        Row linha = aba.createRow(linhaAtual);
        Cell celulaLabel = linha.createCell(0);
        celulaLabel.setCellValue(label);
        celulaLabel.setCellStyle(estiloLabel);

        Cell celulaValor = linha.createCell(1);
        if (valor instanceof Number numero) {
            celulaValor.setCellValue(numero.doubleValue());
        } else {
            celulaValor.setCellValue(String.valueOf(valor));
        }

        return linhaAtual + 1;
    }

    private int escreverCabecalhoSecao(Sheet aba, int linhaAtual, String texto, CellStyle estiloCabecalho) {
        Row linha = aba.createRow(linhaAtual);
        Cell celula = linha.createCell(0);
        celula.setCellValue(texto);
        celula.setCellStyle(estiloCabecalho);
        return linhaAtual + 1;
    }

    private CellStyle criarEstiloTitulo(Workbook workbook) {
        Font fonte = workbook.createFont();
        fonte.setBold(true);
        fonte.setFontHeightInPoints((short) 14);

        CellStyle estilo = workbook.createCellStyle();
        estilo.setFont(fonte);
        return estilo;
    }

    private CellStyle criarEstiloCabecalho(Workbook workbook) {
        Font fonte = workbook.createFont();
        fonte.setBold(true);
        fonte.setColor(IndexedColors.WHITE.getIndex());

        CellStyle estilo = workbook.createCellStyle();
        estilo.setFont(fonte);
        estilo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private CellStyle criarEstiloLabel(Workbook workbook) {
        Font fonte = workbook.createFont();
        fonte.setBold(true);

        CellStyle estilo = workbook.createCellStyle();
        estilo.setFont(fonte);
        return estilo;
    }
}