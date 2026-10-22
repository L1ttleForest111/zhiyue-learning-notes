package com.learningnotes.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learningnotes.dto.TravelNoteRequest;
import com.learningnotes.entity.TravelNote;
import com.learningnotes.exception.BusinessException;
import com.learningnotes.mapper.TravelNoteMapper;
import com.learningnotes.vo.PageResult;
import com.learningnotes.vo.TravelNoteSummary;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TravelNoteService {
    public static final int MAX_HTML_BYTES = 2 * 1024 * 1024;
    private final TravelNoteMapper mapper;

    public TravelNoteService(TravelNoteMapper mapper) { this.mapper = mapper; }

    public PageResult<TravelNoteSummary> list(String keyword, long page, long pageSize) {
        if (page < 1 || page > 100000 || pageSize < 1 || pageSize > 100 || (keyword != null && keyword.length() > 200)) {
            throw new BusinessException("分页或搜索参数不合法");
        }
        LambdaQueryWrapper<TravelNote> query = new LambdaQueryWrapper<TravelNote>()
                .select(TravelNote::getId, TravelNote::getTitle, TravelNote::getDestination, TravelNote::getStartDate,
                        TravelNote::getEndDate, TravelNote::getSummary, TravelNote::getOriginalFilename,
                        TravelNote::getCreatedAt, TravelNote::getUpdatedAt)
                .and(keyword != null && !keyword.isBlank(), q -> q.like(TravelNote::getTitle, keyword)
                        .or().like(TravelNote::getDestination, keyword).or().like(TravelNote::getSummary, keyword))
                .orderByDesc(TravelNote::getUpdatedAt).orderByDesc(TravelNote::getId);
        Page<TravelNote> result = mapper.selectPage(new Page<>(page, pageSize), query);
        return new PageResult<>(result.getRecords().stream().map(TravelNoteSummary::from).toList(), result.getTotal(), page, pageSize);
    }

    public TravelNote get(Long id) {
        TravelNote note = mapper.selectById(id);
        if (note == null) throw BusinessException.notFound("旅行笔记不存在或已删除");
        return note;
    }

    @Transactional
    public TravelNote create(TravelNoteRequest request) {
        validate(request);
        TravelNote existing = mapper.selectOne(new LambdaQueryWrapper<TravelNote>().eq(TravelNote::getImportKey, request.importKey().toLowerCase(Locale.ROOT)));
        if (existing != null) {
            if (!existing.getHtmlSha256().equals(sha256(request.htmlContent()))) {
                throw BusinessException.conflict("同一导入标识对应不同攻略，请先确认是否替换原记录");
            }
            return existing;
        }
        TravelNote note = new TravelNote();
        apply(note, request);
        note.setImportKey(request.importKey().toLowerCase(Locale.ROOT));
        note.setCreatedAt(LocalDateTime.now());
        note.setDeleted(0);
        mapper.insert(note);
        return get(note.getId());
    }

    @Transactional
    public TravelNote update(Long id, TravelNoteRequest request) {
        validate(request);
        TravelNote note = get(id);
        if (!note.getImportKey().equalsIgnoreCase(request.importKey())) throw BusinessException.conflict("导入标识与当前旅行不一致");
        apply(note, request);
        mapper.update(null, new LambdaUpdateWrapper<TravelNote>().eq(TravelNote::getId, id)
                .set(TravelNote::getTitle, note.getTitle()).set(TravelNote::getDestination, note.getDestination())
                .set(TravelNote::getStartDate, note.getStartDate()).set(TravelNote::getEndDate, note.getEndDate())
                .set(TravelNote::getSummary, note.getSummary()).set(TravelNote::getJournalContent, note.getJournalContent())
                .set(TravelNote::getHtmlContent, note.getHtmlContent()).set(TravelNote::getOriginalFilename, note.getOriginalFilename())
                .set(TravelNote::getHtmlSha256, note.getHtmlSha256()).set(TravelNote::getUpdatedAt, note.getUpdatedAt()));
        return get(id);
    }

    public void delete(Long id) { get(id); mapper.deleteById(id); }

    private void validate(TravelNoteRequest request) {
        if (request.htmlContent().getBytes(StandardCharsets.UTF_8).length > MAX_HTML_BYTES) throw new BusinessException("HTML 文件最多 2 MiB（按 UTF-8 字节计算）");
        String filename = request.originalFilename().toLowerCase(Locale.ROOT);
        if ((!filename.endsWith(".html") && !filename.endsWith(".htm")) || filename.contains("/") || filename.contains("\\")) throw new BusinessException("请选择 .html 或 .htm 文件，不接受文件路径");
        if (request.startDate() != null && request.endDate() != null && request.endDate().isBefore(request.startDate())) throw new BusinessException("结束日期不能早于开始日期");
    }

    private void apply(TravelNote note, TravelNoteRequest request) {
        note.setTitle(request.title().trim());
        note.setDestination(request.destination() == null ? "" : request.destination().trim());
        note.setStartDate(request.startDate());
        note.setEndDate(request.endDate());
        note.setSummary(request.summary() == null ? "" : request.summary().trim());
        note.setJournalContent(request.journalContent() == null ? "" : request.journalContent());
        note.setHtmlContent(request.htmlContent());
        note.setOriginalFilename(request.originalFilename());
        note.setHtmlSha256(sha256(request.htmlContent()));
        note.setUpdatedAt(LocalDateTime.now());
    }

    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
