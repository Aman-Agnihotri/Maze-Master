package com.mazemaster.export;

import org.w3c.dom.Node;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Writes a sequence of rendered maze frames as an animated GIF.
 */
public class AnimatedGifExporter {
    private static final String GIF_FORMAT = "gif";
    private static final String NETSCAPE_EXTENSION = "NETSCAPE";
    private static final String NETSCAPE_AUTH_CODE = "2.0";

    public void export(List<BufferedImage> frames, Path path, int frameDelayMillis) throws IOException {
        Objects.requireNonNull(frames, "frames");
        Objects.requireNonNull(path, "path");
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("At least one frame is required");
        }

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        ImageWriter writer = getGifWriter();
        try (OutputStream outputStream = Files.newOutputStream(path);
             ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            if (imageOutputStream == null) {
                throw new IOException("Unable to create GIF image output stream");
            }

            writer.setOutput(imageOutputStream);
            writer.prepareWriteSequence(null);
            for (int index = 0; index < frames.size(); index++) {
                BufferedImage frame = normalizeFrame(frames.get(index));
                IIOMetadata metadata = createMetadata(writer, frame, frameDelayMillis, index == 0);
                writer.writeToSequence(new IIOImage(frame, null, metadata), null);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    private ImageWriter getGifWriter() throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix(GIF_FORMAT);
        if (!writers.hasNext()) {
            throw new IOException("No GIF image writer is available");
        }
        return writers.next();
    }

    private BufferedImage normalizeFrame(BufferedImage frame) {
        if (frame.getType() != BufferedImage.TYPE_CUSTOM) {
            return frame;
        }

        BufferedImage normalized = new BufferedImage(
            frame.getWidth(),
            frame.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D graphics = normalized.createGraphics();
        graphics.drawImage(frame, 0, 0, null);
        graphics.dispose();
        return normalized;
    }

    private IIOMetadata createMetadata(ImageWriter writer, BufferedImage frame,
                                       int frameDelayMillis, boolean firstFrame) throws IOException {
        ImageTypeSpecifier imageType = ImageTypeSpecifier.createFromRenderedImage(frame);
        IIOMetadata metadata = writer.getDefaultImageMetadata(imageType, null);
        String metadataFormat = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadataFormat);

        IIOMetadataNode graphicControlExtension = getOrCreateNode(root, "GraphicControlExtension");
        graphicControlExtension.setAttribute("disposalMethod", "none");
        graphicControlExtension.setAttribute("userInputFlag", "FALSE");
        graphicControlExtension.setAttribute("transparentColorFlag", "FALSE");
        graphicControlExtension.setAttribute("delayTime", String.valueOf(toCentiseconds(frameDelayMillis)));
        graphicControlExtension.setAttribute("transparentColorIndex", "0");

        if (firstFrame) {
            IIOMetadataNode applicationExtensions = getOrCreateNode(root, "ApplicationExtensions");
            IIOMetadataNode applicationExtension = new IIOMetadataNode("ApplicationExtension");
            applicationExtension.setAttribute("applicationID", NETSCAPE_EXTENSION);
            applicationExtension.setAttribute("authenticationCode", NETSCAPE_AUTH_CODE);
            applicationExtension.setUserObject(new byte[]{0x1, 0x0, 0x0});
            applicationExtensions.appendChild(applicationExtension);
        }

        metadata.setFromTree(metadataFormat, root);
        return metadata;
    }

    private int toCentiseconds(int frameDelayMillis) {
        return Math.max(1, Math.round(frameDelayMillis / 10.0f));
    }

    private IIOMetadataNode getOrCreateNode(IIOMetadataNode root, String nodeName) {
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (nodeName.equals(child.getNodeName())) {
                return (IIOMetadataNode) child;
            }
        }

        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        root.appendChild(node);
        return node;
    }
}
