package com.learningnotes.controller;

import com.learningnotes.common.ApiResponse;
import com.learningnotes.service.NoteService;
import com.learningnotes.vo.DashboardView;
import com.learningnotes.vo.NoteView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private final NoteService noteService;
    public DashboardController(NoteService noteService) { this.noteService = noteService; }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardView> dashboard() { return ApiResponse.success(noteService.dashboard()); }

    @GetMapping("/reviews/today")
    public ApiResponse<List<NoteView>> reviews() { return ApiResponse.success(noteService.todayReviews()); }
}
