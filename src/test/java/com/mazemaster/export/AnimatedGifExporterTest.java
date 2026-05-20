package com.mazemaster.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnimatedGifExporterTest {

    @Test
    void shouldExportAnimatedGif(@TempDir Path tempDir) throws IOException {
        AnimatedGifExporter exporter = new AnimatedGifExporter();
        Path output = tempDir.resolve("maze.gif");

        exporter.export(List.of(createFrame(Color.BLACK), createFrame(Color.WHITE)), output, 80);

        assertThat(Files.exists(output)).isTrue();
        assertThat(Files.size(output)).isGreaterThan(0);
        assertThat(ImageIO.read(output.toFile())).isNotNull();
    }

    @Test
    void shouldRejectEmptyFrameList(@TempDir Path tempDir) {
        AnimatedGifExporter exporter = new AnimatedGifExporter();
        Path output = tempDir.resolve("empty.gif");

        assertThatThrownBy(() -> exporter.export(List.of(), output, 80))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("At least one frame");
    }

    private BufferedImage createFrame(Color color) {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        return image;
    }
}
