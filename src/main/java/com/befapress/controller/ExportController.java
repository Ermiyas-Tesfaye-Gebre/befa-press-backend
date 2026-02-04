package com.befapress.controller;

import com.befapress.service.PdfExportService;
import com.befapress.service.WordExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
@Tag(name = "Export", description = "Export APIs for PDF and Word documents")
public class ExportController {

    private final PdfExportService pdfExportService;
    private final WordExportService wordExportService;

    // ========== News Exports ==========

    @GetMapping("/news/{id}/pdf")
    @Operation(summary = "Export news article to PDF")
    public ResponseEntity<byte[]> exportNewsToPdf(@PathVariable Long id) {
        byte[] pdfContent = pdfExportService.exportNewsToPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "news-" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfContent);
    }

    @GetMapping("/news/{id}/word")
    @Operation(summary = "Export news article to Word document")
    public ResponseEntity<byte[]> exportNewsToWord(@PathVariable Long id) {
        byte[] docContent = wordExportService.exportNewsToWord(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDispositionFormData("attachment", "news-" + id + ".docx");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok()
                .headers(headers)
                .body(docContent);
    }

    // ========== Opinion Exports ==========

    @GetMapping("/opinion/{id}/pdf")
    @Operation(summary = "Export opinion article to PDF")
    public ResponseEntity<byte[]> exportOpinionToPdf(@PathVariable Long id) {
        byte[] pdfContent = pdfExportService.exportOpinionToPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "opinion-" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfContent);
    }

    @GetMapping("/opinion/{id}/word")
    @Operation(summary = "Export opinion article to Word document")
    public ResponseEntity<byte[]> exportOpinionToWord(@PathVariable Long id) {
        byte[] docContent = wordExportService.exportOpinionToWord(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDispositionFormData("attachment", "opinion-" + id + ".docx");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok()
                .headers(headers)
                .body(docContent);
    }
}
