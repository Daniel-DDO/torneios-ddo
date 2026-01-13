package com.ddo.torneios.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Service
public class PostGeradorService {

    // 1080x1080
    private static final int CANVAS_SIZE = 1080;

    private static final int LOGO_BOX_SIZE = 590;
    private static final int LOGO_START_Y = 144;

    private static final int TEXT_Y = 892;
    private static final float FONT_SIZE = 85f;

    public byte[] gerarImagemTitulo(String urlBackground, String urlLogoClube, String nomeJogador) {
        try {
            BufferedImage background = ImageIO.read(new URL(urlBackground));

            Graphics2D g2d = background.createGraphics();

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (urlLogoClube != null && !urlLogoClube.isEmpty()) {
                BufferedImage logo = ImageIO.read(new URL(urlLogoClube));
                desenharLogo(g2d, logo);
            }

            if (nomeJogador != null) {
                escreverNome(g2d, nomeJogador);
            }

            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(background, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void desenharLogo(Graphics2D g2d, BufferedImage logo) {
        double scale = Math.min((double) LOGO_BOX_SIZE / logo.getWidth(), (double) LOGO_BOX_SIZE / logo.getHeight());
        int newWidth = (int) (logo.getWidth() * scale);
        int newHeight = (int) (logo.getHeight() * scale);

        int x = (CANVAS_SIZE - newWidth) / 2;
        int y = LOGO_START_Y + ((LOGO_BOX_SIZE - newHeight) / 2);

        g2d.drawImage(logo, x, y, newWidth, newHeight, null);
    }

    private void escreverNome(Graphics2D g2d, String nome) {
        try {
            InputStream is = new ClassPathResource("fonts/FontsFree-Net-Integral-CF-Regular.ttf").getInputStream();
            Font font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(FONT_SIZE);

            Map<TextAttribute, Object> attributes = new HashMap<>();
            attributes.put(TextAttribute.TRACKING, 0.05);
            g2d.setFont(font.deriveFont(attributes));

            g2d.setColor(Color.WHITE);

            FontMetrics metrics = g2d.getFontMetrics();
            int x = (CANVAS_SIZE - metrics.stringWidth(nome.toUpperCase())) / 2;

            g2d.drawString(nome.toUpperCase(), x, TEXT_Y);

        } catch (Exception e) {
            System.err.println("Erro na fonte customizada: " + e.getMessage());
            g2d.setFont(new Font("Arial", Font.BOLD, (int) FONT_SIZE));
            g2d.setColor(Color.WHITE);
            int x = (CANVAS_SIZE - g2d.getFontMetrics().stringWidth(nome.toUpperCase())) / 2;
            g2d.drawString(nome.toUpperCase(), x, TEXT_Y);
        }
    }
}