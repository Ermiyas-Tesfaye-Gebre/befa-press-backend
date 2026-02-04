package com.befapress.service;

import com.befapress.entity.News;
import com.befapress.entity.Opinion;
import com.befapress.exception.ResourceNotFoundException;
import com.befapress.repository.NewsRepository;
import com.befapress.repository.OpinionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Service for exporting news and opinions to Word (DOCX) format
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WordExportService {

    private final NewsRepository newsRepository;
    private final OpinionRepository opinionRepository;

    @Value("${app.name:BEFA Press}")
    private String appName;

    // Ethiopian colors in hex
    private static final String GREEN = "16A34A";
    private static final String GOLD = "F59E0B";
    private static final String RED = "EF4444";

    /**
     * Export news article to Word document
     */
    public byte[] exportNewsToWord(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("News", "id", newsId));

        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Header with Ethiopian colors
            addColoredHeader(document);

            // App name header
            XWPFParagraph headerPara = document.createParagraph();
            headerPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun headerRun = headerPara.createRun();
            headerRun.setText(appName);
            headerRun.setBold(true);
            headerRun.setFontSize(16);
            headerRun.setColor("333333");

            // Tagline
            XWPFParagraph taglinePara = document.createParagraph();
            taglinePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun taglineRun = taglinePara.createRun();
            taglineRun.setText("Breaking Ethiopian Facts & Articles");
            taglineRun.setFontSize(10);
            taglineRun.setColor("666666");
            taglineRun.setItalic(true);

            // Spacing
            document.createParagraph();

            // Category
            XWPFParagraph categoryPara = document.createParagraph();
            XWPFRun categoryRun = categoryPara.createRun();
            categoryRun.setText(news.getCategory().getName().toUpperCase());
            categoryRun.setBold(true);
            categoryRun.setFontSize(10);
            categoryRun.setColor(GREEN);

            // Title
            XWPFParagraph titlePara = document.createParagraph();
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(news.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(22);
            titleRun.setColor("1a1a1a");

            // Amharic title if available
            if (news.getTitleAmharic() != null && !news.getTitleAmharic().isEmpty()) {
                XWPFParagraph amharicPara = document.createParagraph();
                XWPFRun amharicRun = amharicPara.createRun();
                amharicRun.setText(news.getTitleAmharic());
                amharicRun.setFontSize(14);
                amharicRun.setColor("666666");
                amharicRun.setItalic(true);
            }

            // Author and date
            XWPFParagraph authorPara = document.createParagraph();
            XWPFRun authorRun = authorPara.createRun();
            StringBuilder authorInfo = new StringBuilder("By " + news.getAuthor().getFullName());
            if (news.getPublishedAt() != null) {
                authorInfo.append(" | ")
                        .append(news.getPublishedAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            }
            authorRun.setText(authorInfo.toString());
            authorRun.setItalic(true);
            authorRun.setFontSize(11);
            authorRun.setColor("666666");

            // Divider
            XWPFParagraph dividerPara = document.createParagraph();
            dividerPara.setBorderBottom(Borders.SINGLE);

            // Excerpt if available
            if (news.getExcerpt() != null && !news.getExcerpt().isEmpty()) {
                XWPFParagraph excerptPara = document.createParagraph();
                XWPFRun excerptRun = excerptPara.createRun();
                excerptRun.setText(news.getExcerpt());
                excerptRun.setBold(true);
                excerptRun.setFontSize(12);
                excerptRun.setColor("444444");
                document.createParagraph();
            }

            // Content
            String contentText = stripHtml(news.getContent());
            String[] paragraphs = contentText.split("\n\n");
            for (String para : paragraphs) {
                if (!para.trim().isEmpty()) {
                    XWPFParagraph contentPara = document.createParagraph();
                    contentPara.setAlignment(ParagraphAlignment.BOTH);
                    XWPFRun contentRun = contentPara.createRun();
                    contentRun.setText(para.trim());
                    contentRun.setFontSize(12);
                }
            }

            // Footer
            addFooter(document, news.getSlug());

            document.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Error generating Word document for news: {}", newsId, e);
            throw new RuntimeException("Failed to generate Word document", e);
        }
    }

    /**
     * Export opinion article to Word document
     */
    public byte[] exportOpinionToWord(Long opinionId) {
        Opinion opinion = opinionRepository.findById(opinionId)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "id", opinionId));

        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Header with Ethiopian colors
            addColoredHeader(document);

            // App name header
            XWPFParagraph headerPara = document.createParagraph();
            headerPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun headerRun = headerPara.createRun();
            headerRun.setText(appName);
            headerRun.setBold(true);
            headerRun.setFontSize(16);
            headerRun.setColor("333333");

            // Tagline
            XWPFParagraph taglinePara = document.createParagraph();
            taglinePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun taglineRun = taglinePara.createRun();
            taglineRun.setText("Breaking Ethiopian Facts & Articles");
            taglineRun.setFontSize(10);
            taglineRun.setColor("666666");
            taglineRun.setItalic(true);

            // Spacing
            document.createParagraph();

            // Opinion label
            XWPFParagraph labelPara = document.createParagraph();
            XWPFRun labelRun = labelPara.createRun();
            labelRun.setText("OPINION");
            labelRun.setBold(true);
            labelRun.setFontSize(10);
            labelRun.setColor(GOLD);

            // Title
            XWPFParagraph titlePara = document.createParagraph();
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(opinion.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(22);
            titleRun.setColor("1a1a1a");

            // Author info with credentials
            XWPFParagraph authorPara = document.createParagraph();
            XWPFRun authorRun = authorPara.createRun();
            StringBuilder authorInfo = new StringBuilder("By " + opinion.getAuthor().getFullName());
            if (opinion.getAuthor().getAffiliation() != null) {
                authorInfo.append("\n").append(opinion.getAuthor().getAffiliation());
            }
            if (opinion.getAuthor().getExpertiseField() != null) {
                authorInfo.append(" | ").append(opinion.getAuthor().getExpertiseField());
            }
            authorRun.setText(authorInfo.toString());
            authorRun.setItalic(true);
            authorRun.setFontSize(11);
            authorRun.setColor("666666");

            // Date
            if (opinion.getPublishedAt() != null) {
                XWPFParagraph datePara = document.createParagraph();
                XWPFRun dateRun = datePara.createRun();
                dateRun.setText(opinion.getPublishedAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
                dateRun.setFontSize(10);
                dateRun.setColor("888888");
            }

            // Divider
            XWPFParagraph dividerPara = document.createParagraph();
            dividerPara.setBorderBottom(Borders.SINGLE);

            // Excerpt if available
            if (opinion.getExcerpt() != null && !opinion.getExcerpt().isEmpty()) {
                XWPFParagraph excerptPara = document.createParagraph();
                XWPFRun excerptRun = excerptPara.createRun();
                excerptRun.setText(opinion.getExcerpt());
                excerptRun.setBold(true);
                excerptRun.setFontSize(12);
                excerptRun.setColor("444444");
                document.createParagraph();
            }

            // Content
            String contentText = stripHtml(opinion.getContent());
            String[] paragraphs = contentText.split("\n\n");
            for (String para : paragraphs) {
                if (!para.trim().isEmpty()) {
                    XWPFParagraph contentPara = document.createParagraph();
                    contentPara.setAlignment(ParagraphAlignment.BOTH);
                    XWPFRun contentRun = contentPara.createRun();
                    contentRun.setText(para.trim());
                    contentRun.setFontSize(12);
                }
            }

            // Footer
            addFooter(document, opinion.getSlug());

            document.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Error generating Word document for opinion: {}", opinionId, e);
            throw new RuntimeException("Failed to generate Word document", e);
        }
    }

    private void addColoredHeader(XWPFDocument document) {
        // Create a table for the Ethiopian flag colors header
        XWPFTable table = document.createTable(1, 3);
        table.setWidth("100%");

        XWPFTableRow row = table.getRow(0);

        // Green cell
        XWPFTableCell greenCell = row.getCell(0);
        greenCell.setColor(GREEN);
        greenCell.setText(" ");

        // Gold cell
        XWPFTableCell goldCell = row.getCell(1);
        goldCell.setColor(GOLD);
        goldCell.setText(" ");

        // Red cell
        XWPFTableCell redCell = row.getCell(2);
        redCell.setColor(RED);
        redCell.setText(" ");

        document.createParagraph();
    }

    private void addFooter(XWPFDocument document, String slug) {
        // Spacing
        document.createParagraph();
        document.createParagraph();

        // Divider
        XWPFParagraph dividerPara = document.createParagraph();
        dividerPara.setBorderTop(Borders.SINGLE);

        // Footer text
        XWPFParagraph footerPara = document.createParagraph();
        footerPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun footerRun = footerPara.createRun();
        footerRun.setText("© " + java.time.Year.now().getValue() + " " + appName);
        footerRun.setFontSize(9);
        footerRun.setColor("888888");

        XWPFParagraph urlPara = document.createParagraph();
        urlPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun urlRun = urlPara.createRun();
        urlRun.setText("www.befapress.com/" + slug);
        urlRun.setFontSize(9);
        urlRun.setColor("16A34A");
    }

    private String stripHtml(String html) {
        if (html == null)
            return "";
        return html
                .replaceAll("<[^>]*>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .trim();
    }
}
