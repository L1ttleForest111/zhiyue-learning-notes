package com.learningnotes.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learningnotes.dto.NoteRequest;
import com.learningnotes.dto.StatusRequest;
import com.learningnotes.entity.Category;
import com.learningnotes.entity.Note;
import com.learningnotes.entity.NoteTag;
import com.learningnotes.entity.Tag;
import com.learningnotes.exception.BusinessException;
import com.learningnotes.mapper.CategoryMapper;
import com.learningnotes.mapper.NoteMapper;
import com.learningnotes.mapper.NoteTagMapper;
import com.learningnotes.mapper.TagMapper;
import com.learningnotes.vo.DashboardView;
import com.learningnotes.vo.NoteView;
import com.learningnotes.vo.PageResult;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NoteService {
    private static final Set<String> VALID_STATUSES = Set.of("TODO", "LEARNING", "MASTERED", "REVIEW");
    private final NoteMapper noteMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final NoteTagMapper noteTagMapper;

    public NoteService(NoteMapper noteMapper, CategoryMapper categoryMapper, TagMapper tagMapper, NoteTagMapper noteTagMapper) {
        this.noteMapper = noteMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.noteTagMapper = noteTagMapper;
    }

    public PageResult<NoteView> list(String keyword, Long categoryId, Long tagId, String status, long page, long pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) throw new BusinessException("分页参数不合法");
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<Note>()
                .eq(categoryId != null, Note::getCategoryId, categoryId)
                .eq(StringUtils.hasText(status), Note::getStatus, status)
                .and(StringUtils.hasText(keyword), q -> q.like(Note::getTitle, keyword).or().like(Note::getContent, keyword))
                .orderByDesc(Note::getPinned).orderByDesc(Note::getUpdatedAt);
        if (tagId != null) {
            List<NoteTag> links = noteTagMapper.selectList(new LambdaQueryWrapper<NoteTag>().eq(NoteTag::getTagId, tagId));
            if (links.isEmpty()) return new PageResult<>(List.of(), 0, page, pageSize);
            wrapper.in(Note::getId, links.stream().map(NoteTag::getNoteId).toList());
        }
        Page<Note> result = noteMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::toView).toList(), result.getTotal(), page, pageSize);
    }

    public NoteView get(Long id) {
        return toView(requireNote(id));
    }

    @Transactional
    public NoteView create(NoteRequest request) {
        validateRequest(request);
        Note note = new Note();
        applyRequest(note, request);
        note.setPinned(false);
        note.setFavorite(false);
        noteMapper.insert(note);
        replaceTags(note.getId(), request.tagIds());
        return get(note.getId());
    }

    @Transactional
    public NoteView update(Long id, NoteRequest request) {
        validateRequest(request);
        Note note = requireNote(id);
        applyRequest(note, request);
        noteMapper.updateById(note);
        replaceTags(id, request.tagIds());
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        requireNote(id);
        noteMapper.deleteById(id);
    }

    public NoteView updatePinned(Long id, boolean value) {
        requireNote(id);
        noteMapper.update(null, new LambdaUpdateWrapper<Note>().eq(Note::getId, id).set(Note::getPinned, value));
        return get(id);
    }

    public NoteView updateFavorite(Long id, boolean value) {
        requireNote(id);
        noteMapper.update(null, new LambdaUpdateWrapper<Note>().eq(Note::getId, id).set(Note::getFavorite, value));
        return get(id);
    }

    public NoteView updateStatus(Long id, StatusRequest request) {
        if (!VALID_STATUSES.contains(request.status())) throw new BusinessException("不支持的学习状态");
        requireNote(id);
        noteMapper.update(null, new LambdaUpdateWrapper<Note>().eq(Note::getId, id)
                .set(Note::getStatus, request.status()).set(Note::getNextReviewAt, request.nextReviewAt()));
        return get(id);
    }

    public DashboardView dashboard() {
        long total = noteMapper.selectCount(new LambdaQueryWrapper<>());
        long learning = noteMapper.selectCount(new LambdaQueryWrapper<Note>().eq(Note::getStatus, "LEARNING"));
        long mastered = noteMapper.selectCount(new LambdaQueryWrapper<Note>().eq(Note::getStatus, "MASTERED"));
        long due = noteMapper.selectCount(new LambdaQueryWrapper<Note>().le(Note::getNextReviewAt, LocalDateTime.now()));
        List<Note> recent = noteMapper.selectList(new LambdaQueryWrapper<Note>().orderByDesc(Note::getUpdatedAt).last("LIMIT 5"));
        return new DashboardView(total, learning, mastered, due, recent.stream().map(this::toView).toList());
    }

    public List<NoteView> todayReviews() {
        return noteMapper.selectList(new LambdaQueryWrapper<Note>().le(Note::getNextReviewAt, LocalDateTime.now())
                .orderByAsc(Note::getNextReviewAt)).stream().map(this::toView).toList();
    }

    private void validateRequest(NoteRequest request) {
        String status = StringUtils.hasText(request.status()) ? request.status() : "LEARNING";
        if (!VALID_STATUSES.contains(status)) throw new BusinessException("不支持的学习状态");
        if (request.categoryId() != null && categoryMapper.selectById(request.categoryId()) == null) throw new BusinessException("分类不存在");
        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            long count = tagMapper.selectCount(new LambdaQueryWrapper<Tag>().in(Tag::getId, new LinkedHashSet<>(request.tagIds())));
            if (count != new LinkedHashSet<>(request.tagIds()).size()) throw new BusinessException("存在无效标签");
        }
    }

    private void applyRequest(Note note, NoteRequest request) {
        note.setTitle(request.title().trim());
        note.setContent(request.content());
        note.setCategoryId(request.categoryId());
        note.setStatus(StringUtils.hasText(request.status()) ? request.status() : "LEARNING");
        note.setNextReviewAt(request.nextReviewAt());
    }

    private void replaceTags(Long noteId, List<Long> tagIds) {
        noteTagMapper.delete(new LambdaQueryWrapper<NoteTag>().eq(NoteTag::getNoteId, noteId));
        if (tagIds == null) return;
        for (Long tagId : new LinkedHashSet<>(tagIds)) {
            NoteTag link = new NoteTag();
            link.setNoteId(noteId);
            link.setTagId(tagId);
            noteTagMapper.insert(link);
        }
    }

    private Note requireNote(Long id) {
        Note note = noteMapper.selectById(id);
        if (note == null) throw new BusinessException("笔记不存在");
        return note;
    }

    private NoteView toView(Note note) {
        Category category = note.getCategoryId() == null ? null : categoryMapper.selectById(note.getCategoryId());
        List<NoteTag> links = noteTagMapper.selectList(new LambdaQueryWrapper<NoteTag>().eq(NoteTag::getNoteId, note.getId()));
        List<Tag> tags = links.isEmpty() ? List.of() : tagMapper.selectBatchIds(links.stream().map(NoteTag::getTagId).toList());
        return new NoteView(note.getId(), note.getTitle(), note.getContent(), note.getCategoryId(), category == null ? null : category.getName(),
                note.getStatus(), note.getPinned(), note.getFavorite(), note.getNextReviewAt(), note.getCreatedAt(), note.getUpdatedAt(), tags);
    }
}
