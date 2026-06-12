package com.xiaoan.bookstore.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoan.bookstore.entity.InkStroke;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.InkStrokeMapper;
import com.xiaoan.bookstore.service.reader.PdfReaderAdapter;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InkExportService {

    private static final Logger log = LoggerFactory.getLogger(InkExportService.class);
    private static final float DEFAULT_DPI = 150f;

    private final InkStrokeMapper inkStrokeMapper;
    private final PdfReaderAdapter pdfReaderAdapter;
    private final ObjectMapper objectMapper;

    public byte[] exportPageWithInk(Long userId, Long bookId, Integer pageNum, String filePath) {
        try {
            List<InkStroke> strokes = inkStrokeMapper.selectByPage(userId, bookId, pageNum);
            BufferedImage pageImage = renderPageWithInk(filePath, pageNum, strokes, DEFAULT_DPI);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(pageImage, "png", baos);
            log.info("导出带墨迹的页面图片: bookId={}, page={}, strokeCount={}", bookId, pageNum, strokes.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("导出页面墨迹失败", e);
            throw new BusinessException("导出页面墨迹失败: " + e.getMessage());
        }
    }

    public byte[] exportPagesWithInk(Long userId, Long bookId, List<Integer> pageNums, String filePath) {
        try {
            List<Integer> pagesToExport = (pageNums == null || pageNums.isEmpty())
                    ? getAllPageNumbers(userId, bookId)
                    : pageNums;

            Collections.sort(pagesToExport);
            Map<Integer, List<InkStroke>> strokesByPage = getStrokesGroupedByPage(userId, bookId, pagesToExport);

            List<BufferedImage> images = new ArrayList<>();
            for (Integer pageNum : pagesToExport) {
                List<InkStroke> strokes = strokesByPage.getOrDefault(pageNum, Collections.emptyList());
                BufferedImage image = renderPageWithInk(filePath, pageNum, strokes, DEFAULT_DPI);
                images.add(image);
            }

            return imagesToPdf(images);
        } catch (Exception e) {
            log.error("导出多页墨迹失败", e);
            throw new BusinessException("导出多页墨迹失败: " + e.getMessage());
        }
    }

    public byte[] exportBookWithInk(Long userId, Long bookId, String filePath) {
        return exportPagesWithInk(userId, bookId, null, filePath);
    }

    public byte[] exportInkOnlyAsImage(Long userId, Long bookId, Integer pageNum, int width, int height) {
        try {
            List<InkStroke> strokes = inkStrokeMapper.selectByPage(userId, bookId, pageNum);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            for (InkStroke stroke : strokes) {
                drawInkStroke(g2d, stroke, 1.0);
            }

            g2d.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("导出墨迹图片失败", e);
            throw new BusinessException("导出墨迹图片失败: " + e.getMessage());
        }
    }

    private BufferedImage renderPageWithInk(String filePath, int pageNum, List<InkStroke> strokes, float dpi) throws Exception {
        byte[] pageBytes = pdfReaderAdapter.getUnitImageWithDpi(filePath, pageNum, dpi);
        if (pageBytes == null || pageBytes.length == 0) {
            throw new BusinessException("无法获取PDF页面图片");
        }

        BufferedImage pageImage = ImageIO.read(new java.io.ByteArrayInputStream(pageBytes));
        if (pageImage == null) {
            throw new BusinessException("无法解析PDF页面图片");
        }

        int imageWidth = pageImage.getWidth();
        int imageHeight = pageImage.getHeight();

        try (PDDocument doc = Loader.loadPDF(Paths.get(filePath).toFile())) {
            PDPage pdfPage = doc.getPage(pageNum - 1);
            float pdfWidth = pdfPage.getMediaBox().getWidth();
            float pdfHeight = pdfPage.getMediaBox().getHeight();
            double scaleX = imageWidth / pdfWidth;
            double scaleY = imageHeight / pdfHeight;
            double scale = Math.min(scaleX, scaleY);

            Graphics2D g2d = pageImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            for (InkStroke stroke : strokes) {
                drawInkStroke(g2d, stroke, scale);
            }

            g2d.dispose();
        }

        return pageImage;
    }

    private void drawInkStroke(Graphics2D g2d, InkStroke stroke, double scale) {
        try {
            List<double[]> points = objectMapper.readValue(stroke.getPoints(), new TypeReference<List<double[]>>() {});
            if (points == null || points.isEmpty()) {
                return;
            }

            String strokeType = stroke.getStrokeType();
            if ("eraser".equals(strokeType)) {
                return;
            }

            Color color = Color.decode(stroke.getColor());
            float lineWidth = stroke.getLineWidth().floatValue();
            float opacity = stroke.getOpacity() != null ? stroke.getOpacity().floatValue() : 1.0f;

            if ("highlighter".equals(strokeType)) {
                opacity *= 0.5f;
            }

            Color finalColor = new Color(
                    color.getRed() / 255f,
                    color.getGreen() / 255f,
                    color.getBlue() / 255f,
                    opacity
            );

            g2d.setColor(finalColor);
            g2d.setStroke(new BasicStroke(
                    (float) (lineWidth * scale),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            ));

            if (points.size() == 1) {
                double[] p = points.get(0);
                int x = (int) (p[0] * scale);
                int y = (int) (p[1] * scale);
                int r = (int) (lineWidth * scale / 2);
                g2d.fillOval(x - r, y - r, r * 2, r * 2);
                return;
            }

            Path2D path = new Path2D.Double();
            double[] first = points.get(0);
            path.moveTo(first[0] * scale, first[1] * scale);

            for (int i = 1; i < points.size() - 1; i++) {
                double[] curr = points.get(i);
                double[] next = points.get(i + 1);
                double xc = (curr[0] + next[0]) / 2.0 * scale;
                double yc = (curr[1] + next[1]) / 2.0 * scale;
                path.quadTo(curr[0] * scale, curr[1] * scale, xc, yc);
            }

            if (points.size() >= 2) {
                double[] last = points.get(points.size() - 1);
                path.lineTo(last[0] * scale, last[1] * scale);
            }

            g2d.draw(path);
        } catch (Exception e) {
            log.warn("绘制墨迹失败: strokeId={}, error={}", stroke.getStrokeId(), e.getMessage());
        }
    }

    private byte[] imagesToPdf(List<BufferedImage> images) throws Exception {
        if (images.isEmpty()) {
            throw new BusinessException("没有可导出的页面");
        }

        try (PDDocument doc = new PDDocument()) {
            for (BufferedImage image : images) {
                PDPage page = new PDPage(new org.apache.pdfbox.pdmodel.common.PDRectangle(image.getWidth(), image.getHeight()));
                doc.addPage(page);

                PDImageXObject pdImage = LosslessFactory.createFromImage(doc, image);
                try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                    contentStream.drawImage(pdImage, 0, 0, image.getWidth(), image.getHeight());
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private List<Integer> getAllPageNumbers(Long userId, Long bookId) {
        List<Map<String, Object>> stats = inkStrokeMapper.selectPageStats(userId, bookId);
        List<Integer> pageNums = new ArrayList<>();
        for (Map<String, Object> stat : stats) {
            Object pageNum = stat.get("page_num");
            if (pageNum != null) {
                pageNums.add(((Number) pageNum).intValue());
            }
        }
        return pageNums;
    }

    private Map<Integer, List<InkStroke>> getStrokesGroupedByPage(Long userId, Long bookId, List<Integer> pageNums) {
        Map<Integer, List<InkStroke>> grouped = new HashMap<>();
        for (Integer pageNum : pageNums) {
            List<InkStroke> strokes = inkStrokeMapper.selectByPage(userId, bookId, pageNum);
            grouped.put(pageNum, strokes);
        }
        return grouped;
    }

    public byte[] exportInkToPdfOverlay(Long userId, Long bookId, String filePath, List<Integer> pageNums) {
        try {
            Path pdfPath = Paths.get(filePath);
            if (!pdfPath.toFile().exists()) {
                throw new BusinessException("PDF文件不存在");
            }

            List<Integer> pagesToExport = (pageNums == null || pageNums.isEmpty())
                    ? getAllPageNumbers(userId, bookId)
                    : pageNums;

            Collections.sort(pagesToExport);

            try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
                int totalPages = doc.getNumberOfPages();

                for (Integer pageNum : pagesToExport) {
                    if (pageNum < 1 || pageNum > totalPages) {
                        continue;
                    }

                    List<InkStroke> strokes = inkStrokeMapper.selectByPage(userId, bookId, pageNum);
                    if (strokes.isEmpty()) {
                        continue;
                    }

                    PDPage pdfPage = doc.getPage(pageNum - 1);
                    float pdfWidth = pdfPage.getMediaBox().getWidth();
                    float pdfHeight = pdfPage.getMediaBox().getHeight();

                    try (PDPageContentStream contentStream = new PDPageContentStream(
                            doc, pdfPage, PDPageContentStream.AppendMode.APPEND, true, true)) {

                        for (InkStroke stroke : strokes) {
                            drawInkStrokeToPdf(contentStream, stroke, pdfWidth, pdfHeight);
                        }
                    }
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                doc.save(baos);
                log.info("导出带墨迹的PDF: bookId={}, pageCount={}", bookId, pagesToExport.size());
                return baos.toByteArray();
            }
        } catch (Exception e) {
            log.error("导出PDF墨迹失败", e);
            throw new BusinessException("导出PDF墨迹失败: " + e.getMessage());
        }
    }

    private void drawInkStrokeToPdf(PDPageContentStream contentStream, InkStroke stroke, float pdfWidth, float pdfHeight) throws Exception {
        String strokeType = stroke.getStrokeType();
        if ("eraser".equals(strokeType)) {
            return;
        }

        List<double[]> points = objectMapper.readValue(stroke.getPoints(), new TypeReference<List<double[]>>() {});
        if (points == null || points.isEmpty()) {
            return;
        }

        Color color = Color.decode(stroke.getColor());
        float lineWidth = stroke.getLineWidth().floatValue();
        float opacity = stroke.getOpacity() != null ? stroke.getOpacity().floatValue() : 1.0f;

        if ("highlighter".equals(strokeType)) {
            opacity *= 0.5f;
        }

        contentStream.setStrokingColor(color.getRed(), color.getGreen(), color.getBlue());
        contentStream.setLineWidth(lineWidth);
        contentStream.setLineCapStyle(1);
        contentStream.setLineJoinStyle(1);
        contentStream.setNonStrokingColor(color.getRed(), color.getGreen(), color.getBlue());

        if (points.size() == 1) {
            double[] p = points.get(0);
            float x = (float) p[0];
            float y = pdfHeight - (float) p[1];
            float r = lineWidth / 2;
            contentStream.addCircle(x, y, r);
            contentStream.fill();
            return;
        }

        double[] first = points.get(0);
        contentStream.moveTo((float) first[0], pdfHeight - (float) first[1]);

        for (int i = 1; i < points.size() - 1; i++) {
            double[] curr = points.get(i);
            double[] next = points.get(i + 1);
            float xc = (float) ((curr[0] + next[0]) / 2.0);
            float yc = (float) (pdfHeight - (curr[1] + next[1]) / 2.0);
            contentStream.curveTo1(
                    (float) curr[0], pdfHeight - (float) curr[1],
                    xc, yc
            );
        }

        if (points.size() >= 2) {
            double[] last = points.get(points.size() - 1);
            contentStream.lineTo((float) last[0], pdfHeight - (float) last[1]);
        }

        contentStream.stroke();
    }
}
