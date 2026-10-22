package com.learningnotes.controller;

import com.learningnotes.common.ApiResponse;
import com.learningnotes.dto.TravelNoteRequest;
import com.learningnotes.entity.TravelNote;
import com.learningnotes.service.TravelNoteService;
import com.learningnotes.service.TravelPreviewService;
import com.learningnotes.service.TravelPreviewServer;
import com.learningnotes.vo.PageResult;
import com.learningnotes.vo.TravelNoteSummary;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/travel-notes")
public class TravelNoteController {
    private final TravelNoteService service;
    private final TravelPreviewService preview;
    private final TravelPreviewServer previewServer;

    public TravelNoteController(TravelNoteService service, TravelPreviewService preview, TravelPreviewServer previewServer) {
        this.service = service; this.preview = preview; this.previewServer = previewServer;
    }

    @GetMapping
    public ApiResponse<PageResult<TravelNoteSummary>> list(@RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.list(keyword, page, pageSize));
    }

    @GetMapping("/capabilities")
    public ApiResponse<Map<String, Object>> capabilities() {
        return ApiResponse.success(Map.of("mapConfigured", preview.mapConfigured(), "maxHtmlBytes", TravelNoteService.MAX_HTML_BYTES));
    }

    @GetMapping("/{id}")
    public ApiResponse<TravelNote> get(@PathVariable Long id) { return ApiResponse.success(service.get(id)); }

    @PostMapping
    public ApiResponse<TravelNote> create(@Valid @RequestBody TravelNoteRequest request) { return ApiResponse.success(service.create(request)); }

    @PutMapping("/{id}")
    public ApiResponse<TravelNote> update(@PathVariable Long id, @Valid @RequestBody TravelNoteRequest request) { return ApiResponse.success(service.update(id, request)); }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) { service.delete(id); return ApiResponse.success(); }

    @GetMapping("/{id}/map-preview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> mapPreview(@PathVariable Long id) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(ApiResponse.success(Map.of("previewUrl", previewServer.issue(id), "mapConfigured", preview.mapConfigured())));
    }

    @GetMapping("/{id}/preview-document")
    public ResponseEntity<ApiResponse<Map<String, String>>> previewDocument(@PathVariable Long id) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(ApiResponse.success(Map.of("htmlContent", preview.build(service.get(id).getHtmlContent()))));
    }

    @GetMapping(value = "/{id}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> preview(@PathVariable Long id) {
        return ResponseEntity.ok().header("Content-Security-Policy", TravelPreviewService.CSP)
                .header("X-Content-Type-Options", "nosniff").header("Referrer-Policy", "no-referrer")
                .header("Cache-Control", "no-store").header("Permissions-Policy", "geolocation=(), camera=(), microphone=()")
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .body(preview.build(service.get(id).getHtmlContent()));
    }
}
