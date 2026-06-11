package com.xiaoan.bookstore.service.reader;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Component
public class PdfReaderAdapter implements ReaderAdapter {

    private static final Logger log = LoggerFactory.getLogger(PdfReaderAdapter.class);
    private static final float DEFAULT_DPI = 150f;
    private static final float THUMBNAIL_MAX_WIDTH = 200f;

    @Override
    public String getFormat() {
        return "pdf";
    }

    @Override
    public String getStreamType() {
        return STREAM_TYPE_IMAGE;
    }

    @Override
    public List<Map<String, Object>> getToc(String filePath) {
        List<Map<String, Object>> toc = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(Paths.get(filePath).toFile())) {
            PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();
            if (outline != null) {
                for (PDOutlineItem item : outline.children()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("title", item.getTitle());
                    int pageIndex = getPageNumber(doc, item);
                    entry.put("page", pageIndex + 1);
                    List<Map<String, Object>> children = new ArrayList<>();
                    for (PDOutlineItem child : item.children()) {
                        Map<String, Object> childEntry = new HashMap<>();
                        childEntry.put("title", child.getTitle());
                        childEntry.put("page", getPageNumber(doc, child) + 1);
                        children.add(childEntry);
                    }
                    if (!children.isEmpty()) {
                        entry.put("children", children);
                    }
                    toc.add(entry);
                }
            }
        } catch (Exception e) {
            log.error("获取PDF目录失败", e);
        }
        return toc;
    }

    @Override
    public String getUnitContent(String filePath, int pageNum) {
        try (PDDocument doc = Loader.loadPDF(Paths.get(filePath).toFile())) {
            if (pageNum < 1 || pageNum > doc.getNumberOfPages()) {
                return "";
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(pageNum);
            stripper.setEndPage(pageNum);
            return stripper.getText(doc);
        } catch (Exception e) {
            log.error("获取PDF文本失败", e);
            return "";
        }
    }

    @Override
    public byte[] getUnitImage(String filePath, int pageNum) {
        return getUnitImageWithDpi(filePath, pageNum, DEFAULT_DPI);
    }

    public byte[] getUnitImageWithDpi(String filePath, int pageNum, float dpi) {
        try (PDDocument doc = Loader.loadPDF(Paths.get(filePath).toFile())) {
            if (pageNum < 1 || pageNum > doc.getNumberOfPages()) {
                return new byte[0];
            }
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage image = renderer.renderImageWithDPI(pageNum - 1, dpi, ImageType.RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("获取PDF页面图片失败, page={}, dpi={}", pageNum, dpi, e);
            return new byte[0];
        }
    }

    public byte[] generateThumbnail(String filePath, int pageNum, float dpi) {
        try (PDDocument doc = Loader.loadPDF(Paths.get(filePath).toFile())) {
            if (pageNum < 1 || pageNum > doc.getNumberOfPages()) {
                return new byte[0];
            }
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage image = renderer.renderImageWithDPI(pageNum - 1, dpi, ImageType.RGB);

            BufferedImage scaledImage = scaleToThumbnail(image, THUMBNAIL_MAX_WIDTH);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(scaledImage, "jpeg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("生成PDF缩略图失败, page={}, dpi={}", pageNum, dpi, e);
            return new byte[0];
        }
    }

    private BufferedImage scaleToThumbnail(BufferedImage original, float maxWidth) {
        if (original == null) return null;
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        if (originalWidth <= maxWidth) {
            return original;
        }

        float scale = maxWidth / originalWidth;
        int newWidth = Math.round(maxWidth);
        int newHeight = Math.round(originalHeight * scale);

        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        return scaled;
    }

    public String saveThumbnailToFile(String filePath, int pageNum, float dpi, String outputDir, String fileName) {
        try {
            byte[] thumbnailBytes = generateThumbnail(filePath, pageNum, dpi);
            if (thumbnailBytes == null || thumbnailBytes.length == 0) {
                return null;
            }

            Path dirPath = Paths.get(outputDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            Path outputPath = dirPath.resolve(fileName + ".jpg");
            Files.write(outputPath, thumbnailBytes);
            return outputPath.toString();
        } catch (Exception e) {
            log.error("保存缩略图到文件失败", e);
            return null;
        }
    }

    public boolean preRenderPages(String filePath, int startPage, int endPage, float dpi, String outputDir) {
        try (PDDocument doc = Loader.loadPDF(Paths.get(filePath).toFile())) {
            int totalPages = doc.getNumberOfPages();
            int actualEnd = Math.min(endPage, totalPages);

            Path dirPath = Paths.get(outputDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            PDFRenderer renderer = new PDFRenderer(doc);
            for (int i = startPage; i <= actualEnd; i++) {
                try {
                    BufferedImage image = renderer.renderImageWithDPI(i - 1, dpi, ImageType.RGB);
                    Path outputPath = dirPath.resolve("page_" + i + ".png");
                    File outputFile = outputPath.toFile();
                    ImageIO.write(image, "png", outputFile);
                    log.debug("预渲染PDF页面完成: page={}, path={}", i, outputPath);
                } catch (Exception e) {
                    log.error("预渲染PDF页面失败: page={}", i, e);
                }
            }
            log.info("PDF预渲染完成: start={}, end={}, total={}", startPage, actualEnd, totalPages);
            return true;
        } catch (Exception e) {
            log.error("PDF预渲染失败", e);
            return false;
        }
    }

    @Override
    public int getTotalUnits(String filePath) {
        try (PDDocument doc = Loader.loadPDF(Paths.get(filePath).toFile())) {
            return doc.getNumberOfPages();
        } catch (Exception e) {
            log.error("获取PDF页数失败", e);
            return 0;
        }
    }

    private int getPageNumber(PDDocument doc, PDOutlineItem item) {
        try {
            PDPage page = item.findDestinationPage(doc);
            if (page != null) {
                return doc.getPages().indexOf(page);
            }
        } catch (Exception e) {
            log.debug("获取目录页码失败: {}", item.getTitle());
        }
        return 0;
    }

    @Override
    public Map<String, String> extractMetadata(String filePath) {
        Map<String, String> meta = new HashMap<>();
        try (PDDocument doc = Loader.loadPDF(Paths.get(filePath).toFile())) {
            var info = doc.getDocumentInformation();
            if (info != null) {
                String title = info.getTitle();
                String author = info.getAuthor();
                if (title != null && !title.isBlank()) {
                    meta.put("title", title.trim());
                }
                if (author != null && !author.isBlank()) {
                    meta.put("author", author.trim());
                }
                String subject = info.getSubject();
                if (subject != null && !subject.isBlank()) {
                    meta.put("subject", subject.trim());
                }
                String keywords = info.getKeywords();
                if (keywords != null && !keywords.isBlank()) {
                    meta.put("keywords", keywords.trim());
                }
            }
        } catch (Exception e) {
            log.warn("提取PDF元数据失败: {}", e.getMessage());
        }
        return meta;
    }
}
