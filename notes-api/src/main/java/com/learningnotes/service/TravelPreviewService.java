package com.learningnotes.service;

import java.net.URI;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TravelPreviewService {
    public static final String RESOURCE_CSP = "default-src 'none'; "
            + "script-src 'unsafe-inline' https://map.qq.com; style-src 'unsafe-inline'; "
            + "img-src data: blob: https://*.map.qq.com https://*.map.gtimg.com https://*.map.tencent.com https://mapapi.qq.com; "
            + "connect-src https://*.map.qq.com https://*.map.gtimg.com https://*.map.tencent.com https://mapstyle.qpic.cn; "
            + "worker-src blob:; font-src data:; frame-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'";
    public static final String MAP_RESOURCE_CSP = RESOURCE_CSP.replace(
            "script-src 'unsafe-inline' https://map.qq.com",
            "script-src 'unsafe-inline' 'unsafe-eval' https://map.qq.com https://*.map.qq.com");
    public static final String CSP = "sandbox allow-scripts; " + RESOURCE_CSP;
    private final String mapKey;

    public TravelPreviewService(@Value("${travel.tencent-map-key:}") String mapKey) { this.mapKey = mapKey; }

    public boolean mapConfigured() { return !mapKey.isBlank(); }

    public String build(String html) { return build(html, RESOURCE_CSP); }

    public String buildForMap(String html) { return build(html, MAP_RESOURCE_CSP); }

    private String build(String html, String policy) {
        Document doc = Jsoup.parse(html);
        doc.outputSettings().prettyPrint(false);
        doc.select("base, iframe, frame, frameset, object, embed, form, link, meta[http-equiv], script[src]").forEach(el -> {
            if (el.normalName().equals("script") && isMapSdk(el.attr("src"))) return;
            el.remove();
        });
        for (Element script : doc.select("script")) {
            if (script.data().contains("_TMapSecurityConfig") || script.data().contains("__WB_TMAP_SECRET__")) script.remove();
            else if (script.hasAttr("src")) {
                String sdk = "https://map.qq.com/api/gljs?v=1.exp&libraries=service";
                if (mapConfigured()) sdk += "&key=" + java.net.URLEncoder.encode(mapKey, java.nio.charset.StandardCharsets.UTF_8);
                script.attr("src", sdk);
            }
        }
        for (Element el : doc.getAllElements()) {
            for (String attr : new String[]{"src", "href", "action", "formaction", "poster", "srcset", "ping", "xlink:href"}) {
                if (!el.hasAttr(attr)) continue;
                String value = el.attr(attr).trim();
                if (!value.startsWith("#") && !(attr.equals("src") && el.normalName().equals("script") && isMapSdk(value))
                        && !(attr.equals("src") && el.normalName().equals("img") && value.startsWith("data:image/"))) el.removeAttr(attr);
            }
        }
        doc.head().prependElement("meta").attr("name", "referrer").attr("content", "no-referrer");
        doc.head().prependElement("meta").attr("http-equiv", "Content-Security-Policy")
                .attr("content", policy);
        doc.head().prependElement("meta").attr("charset", "utf-8");
        if (!mapConfigured()) doc.body().prependElement("div").attr("style", "padding:12px;background:#fff3cd;color:#664d03")
                .text("地图 SDK 已启用，尚未配置个人 Key：底图与点位可能加载，但授权、配额和道路路线不保证可用。未使用 WorkBuddy 本机代理。外部链接及附件默认禁用。");
        return doc.outerHtml();
    }

    private boolean isMapSdk(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equals(uri.getScheme()) && "map.qq.com".equals(uri.getHost()) && "/api/gljs".equals(uri.getPath());
        } catch (IllegalArgumentException exception) { return false; }
    }
}
