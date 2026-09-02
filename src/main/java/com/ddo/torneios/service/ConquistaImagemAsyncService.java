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

    private static final long[] INTERVALOS_MS = {0, 30_000, 120_000};
    private static final int MAX_TENTATIVAS = 3;

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

        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                long espera = INTERVALOS_MS[tentativa - 1];
                if (espera > 0) {
                    Thread.sleep(espera);
                }

                log.info("Tentativa {}/{} de gerar imagem para {}", tentativa, MAX_TENTATIVAS, jogadorNome);

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
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread interrompida durante espera de retentativa", e);
                return;
            } catch (Exception e) {
                log.warn("Falha na tentativa {}/{} para {}: {}", tentativa, MAX_TENTATIVAS, jogadorNome, e.getMessage());
            }
        }

        log.error("Todas as {} tentativas de geração de imagem falharam para {}", MAX_TENTATIVAS, jogadorNome);
    }
}