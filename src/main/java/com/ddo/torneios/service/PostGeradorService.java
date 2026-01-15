package com.ddo.torneios.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Service
public class PostGeradorService {

    private static final int CANVAS_SIZE = 1080;

    private static final int LOGO_AREA_Y = 144;
    private static final int LOGO_AREA_HEIGHT = 590;
    private static final int LOGO_MAX_WIDTH = 600;

    private static final int TEXT_Y_BASELINE = 920;
    private static final float FONT_SIZE = 40f;

    public byte[] gerarImagemTitulo(String urlBackground, String urlLogoClube, String nomeJogador) {
        try {
            BufferedImage canvas = new BufferedImage(CANVAS_SIZE, CANVAS_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = canvas.createGraphics();

            configurarQualidadeGrafica(g2d);
            BufferedImage background = carregarImagemOriginal(urlBackground);

            if (background != null) {
                g2d.drawImage(background, 0, 0, CANVAS_SIZE, CANVAS_SIZE, null);
            } else {
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
            }

            if (urlLogoClube != null && !urlLogoClube.isEmpty()) {
                BufferedImage logo = carregarImagemOriginal(urlLogoClube);
                if (logo != null) {
                    desenharLogoCentralizada(g2d, logo);
                }
            }

            if (nomeJogador != null) {
                escreverNome(g2d, nomeJogador);
            }

            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(canvas, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void desenharLogoCentralizada(Graphics2D g2d, BufferedImage logo) {
        int imgW = logo.getWidth();
        int imgH = logo.getHeight();

        double scaleH = (double) LOGO_AREA_HEIGHT / imgH;
        double scaleW = (double) LOGO_MAX_WIDTH / imgW;
        double scale = Math.min(scaleH, scaleW);

        if (scale > 1.0) scale = 1.0;

        int newWidth = (int) (imgW * scale);
        int newHeight = (int) (imgH * scale);

        int x = (CANVAS_SIZE - newWidth) / 2;
        int y = LOGO_AREA_Y + ((LOGO_AREA_HEIGHT - newHeight) / 2);

        g2d.drawImage(logo, x, y, newWidth, newHeight, null);
    }

    private void escreverNome(Graphics2D g2d, String nome) {
        String texto = nome.toUpperCase();
        try {
            InputStream is = new ClassPathResource("fonts/FontsFree-Net-Integral-CF-Regular.ttf").getInputStream();
            Font font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(FONT_SIZE);
            Map<TextAttribute, Object> attributes = new HashMap<>();
            attributes.put(TextAttribute.TRACKING, 0.05);
            g2d.setFont(font.deriveFont(attributes));
        } catch (Exception e) {
            System.err.println("Fonte não encontrada, usando Arial.");
            g2d.setFont(new Font("Arial", Font.BOLD, (int) FONT_SIZE));
        }

        FontMetrics metrics = g2d.getFontMetrics();
        int x = (CANVAS_SIZE - metrics.stringWidth(texto)) / 2;

        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.drawString(texto, x + 4, TEXT_Y_BASELINE + 4);

        g2d.setColor(Color.WHITE);
        g2d.drawString(texto, x, TEXT_Y_BASELINE);
    }

    private BufferedImage carregarImagemOriginal(String urlString) {
        try {
            if (urlString == null || urlString.isEmpty()) return null;

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(15000);
            connection.setConnectTimeout(15000);

            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
            connection.setRequestProperty("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Sec-Fetch-Dest", "image");
            connection.setRequestProperty("Sec-Fetch-Mode", "no-cors");
            connection.setRequestProperty("Sec-Fetch-Site", "cross-site");

            connection.setRequestProperty("Referer", "https://www.google.com/");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (InputStream is = connection.getInputStream()) {
                    BufferedImage img = ImageIO.read(is);
                    if (img != null) {
                        System.out.println("Imagem baixada com sucesso: " + urlString + " | Resolução: " + img.getWidth() + "x" + img.getHeight());
                    }
                    return img;
                }
            } else {
                System.err.println("Falha ao baixar (HTTP " + responseCode + "): " + urlString);
                return null;
            }

        } catch (Exception e) {
            System.err.println("Erro ao baixar imagem: " + urlString + " -> " + e.getMessage());
            return null;
        }
    }

    private void configurarQualidadeGrafica(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }
}