package com.xiaoan.bookstore.service.reader;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Paths;
import java.util.*;

@Component
public class PdfReaderAdapter implements ReaderAdapter {

    private static final Logger log = LoggerFactory.getLogger(PdfReaderAdapter.class);

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
        try (PDDocument doc = Loader.loadPDF(Paths.get(filePath).toFile())) {
            if (pageNum < 1 || pageNum > doc.getNumberOfPages()) {
                return new byte[0];
            }
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage image = renderer.renderImageWithDPI(pageNum - 1, 150);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("获取PDF页面图片失败", e);
            return new byte[0];
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
