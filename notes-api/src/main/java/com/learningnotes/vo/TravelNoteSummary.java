package com.learningnotes.vo;

import com.learningnotes.entity.TravelNote;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TravelNoteSummary(Long id, String title, String destination, LocalDate startDate,
                                LocalDate endDate, String summary, String originalFilename,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static TravelNoteSummary from(TravelNote note) {
        return new TravelNoteSummary(note.getId(), note.getTitle(), note.getDestination(), note.getStartDate(),
                note.getEndDate(), note.getSummary(), note.getOriginalFilename(), note.getCreatedAt(), note.getUpdatedAt());
    }
}
