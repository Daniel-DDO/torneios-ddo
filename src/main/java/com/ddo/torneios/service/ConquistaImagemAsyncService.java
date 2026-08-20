package com.ddo.torneios.service;

import com.ddo.torneios.repository.ConquistaRepository;
import com.ddo.torneios.util.ByteArrayMultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ConquistaImagemAsyncService {

    @Autowired
    private PostGeradorService postGeradorService;

    @Autowired
    private ImgBBService imgBBService;

    @Autowired
    private ConquistaRepository conquistaRepository;

    @Async
    public void processarImagemComRetentativas(String conquistaId, String imagemGerarPost,
                                               String urlLogoParaPost, String jogadorId,
                                               String jogadorNome, String prefixoArquivo) {

        //Intervalos entre tentativas: 0s (1ª), 30s (2ª), 120s (3ª)
        long[] intervalosMs = {0, 30_000, 120_000};

        for (int tentativa = 1; tentativa <= 3; tentativa++) {
            try {
                long espera = intervalosMs[tentativa - 1];
                if (espera > 0) {
                    Thread.sleep(espera);
                }

                log.info("Tentativa {}/3 de gerar imagem para {}", tentativa, jogadorNome);

                byte[] imagemBytes = postGeradorService.gerarImagemTitulo(
                        imagemGerarPost, urlLogoParaPost, jogadorNome
                );

                if (imagemBytes != null) {
                    String nomeArquivo = prefixoArquivo + jogadorId + "_" + System.currentTimeMillis() + ".png";

                    MultipartFile multipartFile = new ByteArrayMultipartFile(
                            imagemBytes, "image", nomeArquivo, "image/png"
                    );

                    String urlImgBB = imgBBService.uploadImagem(multipartFile);

                    conquistaRepository.atualizarImagem(conquistaId, urlImgBB);
                    log.info("Imagem salva com sucesso no ImgBB na tentativa {}: {}", tentativa, urlImgBB);
                    return; // Sucesso, encerra o loop
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread interrompida durante espera de retentativa", e);
                return;
            } catch (Exception e) {
                log.warn("Falha na tentativa {}/3 para {}: {}", tentativa, jogadorNome, e.getMessage());
            }
        }

        log.error("Todas as 3 tentativas de geração de imagem falharam para {}", jogadorNome);
    }
}