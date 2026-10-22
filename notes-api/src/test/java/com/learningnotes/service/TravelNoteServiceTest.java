package com.learningnotes.service;

import com.learningnotes.dto.TravelNoteRequest;
import com.learningnotes.entity.TravelNote;
import com.learningnotes.exception.BusinessException;
import com.learningnotes.mapper.TravelNoteMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TravelNoteServiceTest {
    private TravelNoteMapper mapper;
    private TravelNoteService service;
    private final String key = "12345678-1234-1234-1234-123456789012";

    @BeforeEach void setup() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), ""), TravelNote.class);
        mapper = mock(TravelNoteMapper.class);
        service = new TravelNoteService(mapper);
    }

    private TravelNoteRequest request(String html, LocalDate start, LocalDate end) {
        return new TravelNoteRequest(" 阳朔三日 ", "阳朔", start, end, "摘要", "# 心得", html, "攻略.html", key);
    }

    @Test void savesHtmlVerbatimAndWritesOnlyTravelMapper() {
        TravelNote returned = new TravelNote(); returned.setId(8L);
        when(mapper.selectById(8L)).thenReturn(returned);
        doAnswer(call -> { ((TravelNote)call.getArgument(0)).setId(8L); return 1; }).when(mapper).insert(any(TravelNote.class));
        assertSame(returned, service.create(request("<html><body>三日游</body></html>", null, null)));
        ArgumentCaptor<TravelNote> saved = ArgumentCaptor.forClass(TravelNote.class);
        verify(mapper).insert(saved.capture());
        assertEquals("阳朔三日", saved.getValue().getTitle());
        assertEquals("<html><body>三日游</body></html>", saved.getValue().getHtmlContent());
        assertEquals(key, saved.getValue().getImportKey());
        assertEquals(64, saved.getValue().getHtmlSha256().length());
    }

    @Test void rejectsInvalidDatesBeforeDatabaseWrite() {
        assertThrows(BusinessException.class, () -> service.create(request("<h1>攻略</h1>", LocalDate.of(2026,10,24), LocalDate.of(2026,10,23))));
        verifyNoInteractions(mapper);
    }

    @Test void enforcesUtf8ByteLimit() {
        assertThrows(BusinessException.class, () -> service.create(request("旅".repeat(800000), null, null)));
        verifyNoInteractions(mapper);
    }

    @Test void missingOrDeletedTravelCannotBeViewed() {
        assertThrows(BusinessException.class, () -> service.get(404L));
    }

    @Test void rejectsInvalidPagination() {
        assertThrows(BusinessException.class, () -> service.list(null, 0, 20));
        assertThrows(BusinessException.class, () -> service.list(null, 1, 101));
        verifyNoInteractions(mapper);
    }

    @Test void sameImportKeyReturnsExistingRecordWithoutDuplicateWrite() throws Exception {
        String html = "<h1>攻略</h1>";
        TravelNote saved = new TravelNote(); saved.setId(5L);
        saved.setHtmlSha256(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(html.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        when(mapper.selectOne(any())).thenReturn(saved);
        assertSame(saved, service.create(request(html, null, null)));
        assertThrows(BusinessException.class, () -> service.create(request("<h1>其他攻略</h1>", null, null)));
        verify(mapper, never()).insert(any(TravelNote.class));
    }

    @Test void updateKeepsImportIdentityAndCanClearDates() {
        TravelNote note = new TravelNote(); note.setId(1L); note.setImportKey(key); note.setStartDate(LocalDate.now());
        when(mapper.selectById(1L)).thenReturn(note);
        assertSame(note, service.update(1L, request("<h1>攻略</h1>", null, null)));
        assertNull(note.getStartDate());
        assertNull(note.getEndDate());
        verify(mapper).update(isNull(), any());
        verify(mapper, never()).insert(any(TravelNote.class));
    }

    @Test void deletionRequiresExistingRecord() {
        TravelNote note = new TravelNote(); note.setId(1L);
        when(mapper.selectById(1L)).thenReturn(note);
        service.delete(1L);
        verify(mapper).deleteById(1L);
    }
}
