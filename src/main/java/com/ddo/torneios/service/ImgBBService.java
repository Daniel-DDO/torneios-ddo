package com.ddo.torneios.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ImgBBService {

    @Value("${api.imgbb.key}")
    private String apiKey;

    @Value("${api.imgbb.url}")
    private String apiUrl;

    public String uploadImagem(MultipartFile file) throws IOException {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("key", apiKey);

        body.add("image", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename() != null ? file.getOriginalFilename() : "imagem.jpg";
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            String response = restTemplate.postForObject(apiUrl, requestEntity, String.class);
            return extrairUrlDoJson(response);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao comunicar com ImgBB: " + e.getMessage());
        }
    }

    private String extrairUrlDoJson(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);

            JsonNode dataNode = root.path("data");
            if (dataNode.isMissingNode()) {
                throw new RuntimeException("Erro no upload: " + jsonResponse);
            }

            return dataNode.path("url").asText();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar resposta do ImgBB", e);
        }
    }
}