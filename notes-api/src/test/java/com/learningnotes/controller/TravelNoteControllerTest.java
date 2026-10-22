package com.learningnotes.controller;

import com.learningnotes.entity.TravelNote;
import com.learningnotes.service.TravelNoteService;
import com.learningnotes.service.TravelPreviewService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TravelNoteControllerTest {
    @Test void previewDocumentReturnsSanitizedHtmlInJson() {
        TravelNoteService service = mock(TravelNoteService.class);
        TravelNote note = new TravelNote(); note.setHtmlContent("<h1>阳朔</h1><iframe src='http://localhost:8080'></iframe>");
        when(service.get(1L)).thenReturn(note);
        var response = new TravelNoteController(service, new TravelPreviewService(""), mock(com.learningnotes.service.TravelPreviewServer.class)).previewDocument(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().code());
        String html = response.getBody().data().get("htmlContent");
        assertTrue(html.contains("阳朔"));
        assertFalse(html.contains("<iframe"));
        assertTrue(html.contains("Content-Security-Policy"));
        assertEquals("no-store", response.getHeaders().getFirst("Cache-Control"));
    }
    @Test void previewUsesSandboxCspEvenWhenOpenedOutsideAnIframe() {
        TravelNoteService service = mock(TravelNoteService.class);
        TravelNote note = new TravelNote(); note.setHtmlContent("<h1>阳朔</h1>");
        when(service.get(1L)).thenReturn(note);
        var controller = new TravelNoteController(service, new TravelPreviewService(""), mock(com.learningnotes.service.TravelPreviewServer.class));
        var response = controller.preview(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(TravelPreviewService.CSP, response.getHeaders().getFirst("Content-Security-Policy"));
        assertEquals("no-store", response.getHeaders().getFirst("Cache-Control"));
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals("no-referrer", response.getHeaders().getFirst("Referrer-Policy"));
        assertTrue(response.getBody().contains("阳朔"));
    }
}
