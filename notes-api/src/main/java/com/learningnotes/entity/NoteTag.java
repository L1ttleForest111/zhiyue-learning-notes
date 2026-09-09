package com.learningnotes.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("note_tags")
public class NoteTag {
    private Long noteId;
    private Long tagId;
    public Long getNoteId() { return noteId; }
    public void setNoteId(Long noteId) { this.noteId = noteId; }
    public Long getTagId() { return tagId; }
    public void setTagId(Long tagId) { this.tagId = tagId; }
}
