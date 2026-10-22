package com.learningnotes.service;

import com.learningnotes.entity.TravelNote;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TravelPreviewServerTest {
    @Test void dedicatedOriginHasOnlyTicketedReadOnlyPreviewAndResourcePolicy() throws Exception {
        TravelNoteService notes = mock(TravelNoteService.class);
        TravelNote note = new TravelNote(); note.setHtmlContent("<h1>阳朔</h1>");
        when(notes.get(1L)).thenReturn(note);
        TravelPreviewServer server = new TravelPreviewServer(notes, new TravelPreviewService(""), 0);
        server.afterPropertiesSet();
        HttpClient client = HttpClient.newHttpClient();
        try {
            String url = server.issue(1L);
            var result = client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, result.statusCode());
            assertTrue(result.body().contains("阳朔"));
            assertTrue(result.headers().firstValue("Content-Security-Policy").orElseThrow().contains("sandbox allow-scripts allow-same-origin"));
            assertTrue(result.headers().firstValue("Content-Security-Policy").orElseThrow().contains("frame-ancestors"));
            assertFalse(result.headers().firstValue("Access-Control-Allow-Origin").isPresent());
            var forbidden = client.send(HttpRequest.newBuilder(URI.create(url)).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(405, forbidden.statusCode());
            URI api = URI.create(url).resolve("/api/notes");
            assertEquals(404, client.send(HttpRequest.newBuilder(api).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            when(notes.get(1L)).thenThrow(new RuntimeException("deleted"));
            assertEquals(404, client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        } finally { server.destroy(); }
    }
}
