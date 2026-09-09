package com.learningnotes.vo;

import java.util.List;

public record DashboardView(long totalNotes, long learningNotes, long masteredNotes, long dueReviews, List<NoteView> recentNotes) {}
