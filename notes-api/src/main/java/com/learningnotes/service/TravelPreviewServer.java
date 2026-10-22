package com.learningnotes.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TravelPreviewServer implements InitializingBean, DisposableBean {
    private record Ticket(Long noteId, Instant expiresAt) {}
    private final Map<String, Ticket> tickets = new HashMap<>();
    private final TravelNoteService notes;
    private final TravelPreviewService preview;
    private final int port;
    private HttpServer server;
    private ExecutorService executor;

    public TravelPreviewServer(TravelNoteService notes, TravelPreviewService preview,
            @Value("${travel.preview-port:8082}") int port) {
        this.notes = notes;
        this.preview = preview;
        this.port = port;
    }

    @Override
    public void afterPropertiesSet() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 16);
        executor = Executors.newFixedThreadPool(2);
        server.setExecutor(executor);
        server.createContext("/", this::serve);
        server.start();
    }

    public synchronized String issue(Long noteId) {
        notes.get(noteId);
        tickets.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(Instant.now()));
        if (tickets.size() >= 128) throw new com.learningnotes.exception.BusinessException("打开的预览过多，请稍后重试");
        String token = UUID.randomUUID().toString();
        tickets.put(token, new Ticket(noteId, Instant.now().plusSeconds(600)));
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/preview/" + token;
    }

    private void serve(HttpExchange exchange) throws IOException {
        try {
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
            exchange.getResponseHeaders().set("Permissions-Policy", "geolocation=(), camera=(), microphone=()");
            String host = exchange.getRequestHeaders().getFirst("Host");
            if (!("127.0.0.1:" + server.getAddress().getPort()).equals(host)) {
                send(exchange, 403, "预览来源不合法"); return;
            }
            if (!exchange.getRequestMethod().equals("GET")) { send(exchange, 405, "仅允许读取预览"); return; }
            String path = exchange.getRequestURI().getPath();
            Ticket ticket;
            synchronized (this) { ticket = path.startsWith("/preview/") ? tickets.get(path.substring(9)) : null; }
            if (ticket == null || !ticket.expiresAt().isAfter(Instant.now())) { send(exchange, 404, "预览已过期，请关闭后重新打开"); return; }
            String html;
            try { html = preview.buildForMap(notes.get(ticket.noteId()).getHtmlContent()); }
            catch (RuntimeException exception) { send(exchange, 404, "攻略不可用，请关闭后重新打开"); return; }
            exchange.getResponseHeaders().set("Content-Security-Policy", "sandbox allow-scripts allow-same-origin; "
                    + TravelPreviewService.MAP_RESOURCE_CSP
                    + "; frame-ancestors http://localhost:5173 http://localhost:5174 http://127.0.0.1:5173 http://127.0.0.1:5174");
            exchange.getResponseHeaders().set("Content-Type", "text/html;charset=UTF-8");
            send(exchange, 200, html);
        } finally { exchange.close(); }
    }

    private void send(HttpExchange exchange, int status, String text) throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        if (status != 200) exchange.getResponseHeaders().set("Content-Type", "text/plain;charset=UTF-8");
        exchange.sendResponseHeaders(status, data.length);
        exchange.getResponseBody().write(data);
    }

    @Override
    public void destroy() {
        if (server != null) server.stop(0);
        if (executor != null) executor.shutdownNow();
        synchronized (this) { tickets.clear(); }
    }
}
