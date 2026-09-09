package com.learningnotes.controller;

import com.learningnotes.common.ApiResponse;
import com.learningnotes.dto.BooleanRequest;
import com.learningnotes.dto.NoteRequest;
import com.learningnotes.dto.StatusRequest;
import com.learningnotes.service.NoteService;
import com.learningnotes.vo.NoteView;
import com.learningnotes.vo.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    private final NoteService noteService;

    public NoteController(NoteService noteService) { this.noteService = noteService; }

    @GetMapping
    public ApiResponse<PageResult<NoteView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(noteService.list(keyword, categoryId, tagId, status, page, pageSize));
    }

    @PostMapping
    public ApiResponse<NoteView> create(@Valid @RequestBody NoteRequest request) { return ApiResponse.success(noteService.create(request)); }

    @GetMapping("/{id}")
    public ApiResponse<NoteView> get(@PathVariable Long id) { return ApiResponse.success(noteService.get(id)); }

    @PutMapping("/{id}")
    public ApiResponse<NoteView> update(@PathVariable Long id, @Valid @RequestBody NoteRequest request) { return ApiResponse.success(noteService.update(id, request)); }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) { noteService.delete(id); return ApiResponse.success(); }

    @PatchMapping("/{id}/pin")
    public ApiResponse<NoteView> pin(@PathVariable Long id, @Valid @RequestBody BooleanRequest request) { return ApiResponse.success(noteService.updatePinned(id, request.value())); }

    @PatchMapping("/{id}/favorite")
    public ApiResponse<NoteView> favorite(@PathVariable Long id, @Valid @RequestBody BooleanRequest request) { return ApiResponse.success(noteService.updateFavorite(id, request.value())); }

    @PatchMapping("/{id}/status")
    public ApiResponse<NoteView> status(@PathVariable Long id, @Valid @RequestBody StatusRequest request) { return ApiResponse.success(noteService.updateStatus(id, request)); }
}
