package com.befapress.service;

import com.befapress.entity.News;
import com.befapress.entity.Opinion;
import com.befapress.exception.ResourceNotFoundException;
import com.befapress.repository.NewsRepository;
import com.befapress.repository.OpinionRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

/**
 * Service for exporting news and opinions to PDF format
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportService {

        private final NewsRepository newsRepository;
        private final OpinionRepository opinionRepository;

        @Value("${app.name:BEFA Press}")
        private String appName;

        // Ethiopian-inspired colors
        private static final DeviceRgb GREEN = new DeviceRgb(22, 163, 74);
        private static final DeviceRgb GOLD = new DeviceRgb(245, 158, 11);
        private static final DeviceRgb RED = new DeviceRgb(239, 68, 68);
        private static final DeviceRgb BLUE_DARK = new DeviceRgb(30, 58, 138);

        public byte[] exportNewsToPdf(Long newsId) {
                News news = newsRepository.findById(newsId)
                                .orElseThrow(() -> new ResourceNotFoundException("News", "id", newsId));

                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                        PdfWriter writer = new PdfWriter(outputStream);
                        PdfDocument pdfDoc = new PdfDocument(writer);
                        Document document = new Document(pdfDoc, PageSize.A4);
                        document.setMargins(40, 40, 40, 40);

                        configureFontProvider(document);
                        addHeader(document);

                        // Category
                        if (news.getCategory() != null) {
                                document.add(new Paragraph(news.getCategory().getName().toUpperCase())
                                                .setFontSize(10).setFontColor(GREEN).setBold().setMarginTop(10));
                        }

                        // Breaking Badge - Fixed usage of isBreaking()
                        if (news.isBreaking()) {
                                document.add(new Paragraph("BREAKING NEWS")
                                                .setFontSize(10).setFontColor(ColorConstants.WHITE)
                                                .setBackgroundColor(RED)
                                                .setPaddingLeft(5).setPaddingRight(5)
                                                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));
                        }

                        // Title
                        document.add(new Paragraph(news.getTitle())
                                        .setBold().setFontSize(22).setFontColor(ColorConstants.BLACK)
                                        .setMarginTop(5).setMarginBottom(10));

                        // Author and date
                        String authorInfo = "By "
                                        + (news.getAuthor() != null ? news.getAuthor().getFullName() : "Unknown");
                        if (news.getPublishedAt() != null) {
                                authorInfo += " | " + news.getPublishedAt()
                                                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
                        }
                        document.add(new Paragraph(authorInfo)
                                        .setItalic().setFontSize(10).setFontColor(ColorConstants.GRAY)
                                        .setMarginBottom(10));

                        // Content
                        document.add(new Paragraph(stripHtml(news.getContent()))
                                        .setFontSize(11).setTextAlignment(TextAlignment.JUSTIFIED)
                                        .setMultipliedLeading(1.4f));

                        addFooter(document);
                        document.close();
                        return outputStream.toByteArray();
                } catch (Exception e) {
                        log.error("PDF Export failed", e);
                        throw new RuntimeException("PDF Gen Failed", e);
                }
        }

        public byte[] exportOpinionToPdf(Long opinionId) {
                Opinion opinion = opinionRepository.findById(opinionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "id", opinionId));

                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                        PdfWriter writer = new PdfWriter(outputStream);
                        PdfDocument pdfDoc = new PdfDocument(writer);
                        Document document = new Document(pdfDoc, PageSize.A4);
                        document.setMargins(40, 40, 40, 40);

                        configureFontProvider(document);
                        addHeader(document);

                        document.add(new Paragraph("OPINION").setFontSize(10).setFontColor(GOLD).setBold()
                                        .setMarginTop(10));
                        document.add(new Paragraph(opinion.getTitle())
                                        .setBold().setFontSize(22).setFontColor(ColorConstants.BLACK).setMarginTop(5)
                                        .setMarginBottom(10));

                        String authorInfo = "By "
                                        + (opinion.getAuthor() != null ? opinion.getAuthor().getFullName() : "Unknown");
                        if (opinion.getPublishedAt() != null) {
                                authorInfo += "\n" + opinion.getPublishedAt()
                                                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
                        }
                        document.add(new Paragraph(authorInfo)
                                        .setItalic().setFontSize(10).setFontColor(ColorConstants.GRAY)
                                        .setMarginBottom(10));

                        document.add(new Paragraph(stripHtml(opinion.getContent()))
                                        .setFontSize(11).setTextAlignment(TextAlignment.JUSTIFIED)
                                        .setMultipliedLeading(1.4f));

                        addFooter(document);
                        document.close();
                        return outputStream.toByteArray();
                } catch (Exception e) {
                        log.error("PDF Export failed", e);
                        throw new RuntimeException("PDF Gen Failed", e);
                }
        }

        private void configureFontProvider(Document document) {
                FontProvider fontProvider = new FontProvider();
                fontProvider.addStandardPdfFonts();
                try {
                        loadFont(fontProvider, "/fonts/NotoSans-Regular.ttf");
                        loadFont(fontProvider, "/fonts/NotoSans-Bold.ttf");
                        loadFont(fontProvider, "/fonts/NotoSansEthiopic-Regular.ttf");
                        loadFont(fontProvider, "/fonts/NotoSansEthiopic-Bold.ttf");
                } catch (Exception e) {
                        log.warn("Could not load custom Amharic fonts: " + e.getMessage());
                }
                document.setFontProvider(fontProvider);
                document.setProperty(com.itextpdf.layout.properties.Property.FONT,
                                new String[] { "Noto Sans", "Noto Sans Ethiopic", "Helvetica" });
        }

        private void loadFont(FontProvider fp, String path) throws IOException {
                try (InputStream is = getClass().getResourceAsStream(path)) {
                        if (is != null) {
                                fp.addFont(is.readAllBytes());
                        } else {
                                log.warn("Font file not found in resources: {}", path);
                        }
                }
        }

        private void addHeader(Document document) {
                // Full width colored bars
                document.add(new Div().setBackgroundColor(GREEN).setHeight(6)
                                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(0));
                document.add(new Div().setBackgroundColor(GOLD).setHeight(6).setWidth(UnitValue.createPercentValue(100))
                                .setMarginBottom(0));
                document.add(new Div().setBackgroundColor(RED).setHeight(6).setWidth(UnitValue.createPercentValue(100))
                                .setMarginBottom(0));

                // Logo / Title
                Paragraph logoPara = new Paragraph()
                                .setTextAlignment(TextAlignment.CENTER)
                                .setMarginTop(15)
                                .setMarginBottom(0);

                logoPara.add(new Text("BEFA ").setFontSize(24).setBold().setFontColor(BLUE_DARK));
                logoPara.add(new Text("PRESS").setFontSize(24).setBold().setFontColor(GOLD));
                document.add(logoPara);

                // Subtitle
                document.add(new Paragraph("Breaking Ethiopian Facts & Articles")
                                .setFontSize(10).setTextAlignment(TextAlignment.CENTER)
                                .setFontColor(ColorConstants.GRAY).setMarginBottom(10));

                // Separator
                document.add(new Div().setHeight(1).setBackgroundColor(ColorConstants.LIGHT_GRAY).setMarginBottom(20));
        }

        private void addFooter(Document document) {
                Paragraph footer = new Paragraph()
                                .setMarginTop(30)
                                .setFontSize(9)
                                .setFontColor(ColorConstants.GRAY)
                                .setTextAlignment(TextAlignment.CENTER);

                footer.add(new Text("━".repeat(60) + "\n"));
                footer.add(new Text("© " + java.time.Year.now().getValue() + " BEFA Press\n"));
                footer.add(new Text("www.befapress.com"));

                document.add(footer);
        }

        private String stripHtml(String html) {
                if (html == null)
                        return "";
                return html.replaceAll("<[^>]*>", "")
                                .replaceAll("&nbsp;", " ")
                                .replaceAll("&amp;", "&")
                                .replaceAll("&lt;", "<")
                                .replaceAll("&gt;", ">")
                                .replaceAll("&quot;", "\"")
                                .replaceAll("&#39;", "'")
                                .trim();
        }
}
