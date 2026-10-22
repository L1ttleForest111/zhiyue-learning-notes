package com.learningnotes.service;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TravelPreviewServiceTest {
    @Test
    void removesWorkBuddyProxyAndForeignResourcesButKeepsItineraryScripts() {
        String html = "<html><head><script>window._TMapSecurityConfig={serviceHost:'http://127.0.0.1:1234/secret'};</script>"
                + "<script src='https://map.qq.com/api/gljs?v=1.exp'></script><script src='https://evil.example/app.js'></script>"
                + "<base href='http://127.0.0.1:8080/'><meta http-equiv='refresh' content='0;url=http://localhost:8080'></head>"
                + "<body><h1>阳朔</h1><script>document.querySelector('h1').textContent='三日行程';</script>"
                + "<iframe src='http://localhost:8080'></iframe><a href='./a.pdf'>PDF</a><img src='https://evil.example/pixel'></body></html>";
        String out = new TravelPreviewService("").build(html);
        var doc = Jsoup.parse(out);
        assertEquals(0, doc.select("base,iframe,meta[http-equiv=refresh]").size());
        assertEquals(1, doc.select("script[src]").size());
        assertEquals("https://map.qq.com/api/gljs?v=1.exp&libraries=service", doc.selectFirst("script[src]").attr("src"));
        assertTrue(out.contains("地图 SDK 已启用"));
        assertEquals(1, doc.select("meta[http-equiv=Content-Security-Policy]").size());
        String policy = doc.selectFirst("meta[http-equiv=Content-Security-Policy]").attr("content");
        assertTrue(policy.contains("default-src 'none'"));
        assertFalse(policy.contains("sandbox"));
        assertTrue(doc.head().child(1).attr("http-equiv").equals("Content-Security-Policy"));
        assertFalse(out.contains("_TMapSecurityConfig"));
        assertFalse(out.contains("evil.example"));
        assertFalse(doc.selectFirst("a").hasAttr("href"));
        assertTrue(out.contains("document.querySelector"));
        assertTrue(out.contains("阳朔"));
    }

    @Test
    void mapCompatibilityPolicyIsOnlyUsedOnDedicatedOrigin() {
        var service = new TravelPreviewService("");
        assertFalse(service.build("<h1>攻略</h1>").contains("unsafe-eval"));
        assertTrue(service.buildForMap("<h1>攻略</h1>").contains("unsafe-eval"));
        assertTrue(TravelPreviewService.MAP_RESOURCE_CSP.contains("https://mapstyle.qpic.cn"));
        assertTrue(TravelPreviewService.MAP_RESOURCE_CSP.contains("form-action 'none'"));
        assertFalse(TravelPreviewService.MAP_RESOURCE_CSP.contains("http://127.0.0.1"));
    }

    @Test
    void injectsOnlyConfiguredBrowserMapKey() {
        var service = new TravelPreviewService("example-key");
        var doc = Jsoup.parse(service.build("<script src='https://map.qq.com/api/gljs?key=old'></script>"));
        assertTrue(service.mapConfigured());
        assertEquals("https://map.qq.com/api/gljs?v=1.exp&libraries=service&key=example-key", doc.selectFirst("script[src]").attr("src"));
        assertFalse(TravelPreviewService.CSP.contains("allow-same-origin"));
        assertTrue(TravelPreviewService.CSP.contains("sandbox allow-scripts"));
        assertTrue(TravelPreviewService.CSP.contains("default-src 'none'"));
    }
}
