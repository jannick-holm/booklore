package org.booklore.service.opds;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.booklore.model.dto.OpdsUserV2;
import org.booklore.util.FileService;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;

@Slf4j
@Service
public class EpubOptimizationService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final Set<String> TEXT_EXTENSIONS = Set.of("xhtml", "html", "htm", "opf", "ncx");

    public File optimizeEpub(File epubFile, File tempDir, OpdsUserV2 settings) throws IOException {
        File outputFile = new File(tempDir, epubFile.getName());

        // Step 1: scan entries to build rename map (non-JPEG images → .jpg)
        Map<String, String> renameMap = new LinkedHashMap<>();
        try (ZipFile zipFile = ZipFile.builder().setFile(epubFile).setUseUnicodeExtraFields(true).get()) {
            for (ZipArchiveEntry entry : Collections.list(zipFile.getEntries())) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                String ext = extension(name).toLowerCase();
                if (IMAGE_EXTENSIONS.contains(ext) && !isJpeg(ext)) {
                    renameMap.put(name, name.substring(0, name.lastIndexOf('.')) + ".jpg");
                }
            }
        }

        // Step 2: write optimized EPUB
        try (ZipFile zipFile = ZipFile.builder().setFile(epubFile).setUseUnicodeExtraFields(true).get();
             ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(outputFile)) {

            zipOut.setEncoding(StandardCharsets.UTF_8.name());

            // mimetype must be first and STORED (EPUB spec requirement)
            ZipArchiveEntry mimetypeEntry = zipFile.getEntry("mimetype");
            if (mimetypeEntry != null) {
                writeMimetype(zipFile, mimetypeEntry, zipOut);
            }

            for (ZipArchiveEntry entry : Collections.list(zipFile.getEntries())) {
                if (entry.isDirectory() || "mimetype".equals(entry.getName())) continue;

                String name = entry.getName();
                String ext = extension(name).toLowerCase();

                if (IMAGE_EXTENSIONS.contains(ext)) {
                    String outName = renameMap.getOrDefault(name, name);
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        byte[] raw = is.readAllBytes();
                        byte[] processed = processImageBytes(raw, settings);
                        ZipArchiveEntry outEntry = new ZipArchiveEntry(outName);
                        zipOut.putArchiveEntry(outEntry);
                        zipOut.write(processed);
                        zipOut.closeArchiveEntry();
                    }
                } else if (TEXT_EXTENSIONS.contains(ext)) {
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        if (!renameMap.isEmpty()) {
                            content = applyRenameMap(content, renameMap);
                        }
                        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                        ZipArchiveEntry outEntry = new ZipArchiveEntry(name);
                        zipOut.putArchiveEntry(outEntry);
                        zipOut.write(bytes);
                        zipOut.closeArchiveEntry();
                    }
                } else {
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        ZipArchiveEntry outEntry = new ZipArchiveEntry(name);
                        zipOut.putArchiveEntry(outEntry);
                        is.transferTo(zipOut);
                        zipOut.closeArchiveEntry();
                    }
                }
            }
        }

        log.info("Optimized EPUB {} → {} bytes", epubFile.getName(), outputFile.length());
        return outputFile;
    }

    private void writeMimetype(ZipFile zipFile, ZipArchiveEntry mimetypeEntry, ZipArchiveOutputStream zipOut) throws IOException {
        try (InputStream is = zipFile.getInputStream(mimetypeEntry)) {
            byte[] bytes = is.readAllBytes();
            CRC32 crc = new CRC32();
            crc.update(bytes);
            ZipArchiveEntry outEntry = new ZipArchiveEntry("mimetype");
            outEntry.setMethod(ZipArchiveEntry.STORED);
            outEntry.setSize(bytes.length);
            outEntry.setCrc(crc.getValue());
            zipOut.putArchiveEntry(outEntry);
            zipOut.write(bytes);
            zipOut.closeArchiveEntry();
        }
    }

    private byte[] processImageBytes(byte[] rawBytes, OpdsUserV2 settings) {
        BufferedImage image;
        try {
            image = FileService.readImage(rawBytes);
        } catch (Exception e) {
            log.debug("Could not decode image during EPUB optimization, keeping original: {}", e.getMessage());
            return rawBytes;
        }

        if (image == null) {
            return rawBytes;
        }

        if (settings.isEpubResizeImages()) {
            image = resizeImageFit(image, settings.getEpubMaxImageWidth(), settings.getEpubMaxImageHeight());
        }

        if (settings.isEpubEnableGrayscale()) {
            image = toGrayscaleRgb(image);
        }

        try {
            return encodeAsJpeg(image, settings.getEpubJpegQuality() / 100f);
        } catch (IOException e) {
            log.warn("Failed to encode image as JPEG, keeping original bytes: {}", e.getMessage());
            return rawBytes;
        }
    }

    private BufferedImage resizeImageFit(BufferedImage img, int maxWidth, int maxHeight) {
        int w = img.getWidth(), h = img.getHeight();
        if (w <= maxWidth && h <= maxHeight) return img;
        double scale = Math.min((double) maxWidth / w, (double) maxHeight / h);
        int newW = (int) Math.round(w * scale);
        int newH = (int) Math.round(h * scale);
        BufferedImage result = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, newW, newH, null);
        g.dispose();
        return result;
    }

    private BufferedImage toGrayscaleRgb(BufferedImage img) {
        BufferedImage rgb;
        if (img.getType() != BufferedImage.TYPE_INT_RGB) {
            rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
        } else {
            rgb = img;
        }
        int w = rgb.getWidth(), h = rgb.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = rgb.getRGB(x, y);
                int r = (c >> 16) & 0xFF, gv = (c >> 8) & 0xFF, b = c & 0xFF;
                int luma = (int) (0.299 * r + 0.587 * gv + 0.114 * b);
                result.setRGB(x, y, (luma << 16) | (luma << 8) | luma);
            }
        }
        return result;
    }

    private byte[] encodeAsJpeg(BufferedImage image, float quality) throws IOException {
        BufferedImage rgbImage = image;
        if (image.getType() != BufferedImage.TYPE_INT_RGB) {
            rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgbImage.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) throw new IOException("No JPEG writer available");
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgbImage, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    private String applyRenameMap(String content, Map<String, String> renameMap) {
        for (Map.Entry<String, String> entry : renameMap.entrySet()) {
            String oldBase = basename(entry.getKey());
            String newBase = basename(entry.getValue());
            if (!oldBase.equals(newBase)) {
                content = content.replace(oldBase, newBase);
            }
        }
        return content;
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : "";
    }

    private String basename(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private boolean isJpeg(String ext) {
        return "jpg".equals(ext) || "jpeg".equals(ext);
    }
}
