package com.learningnotes.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learningnotes.dto.NameRequest;
import com.learningnotes.entity.Category;
import com.learningnotes.entity.Note;
import com.learningnotes.entity.NoteTag;
import com.learningnotes.entity.Tag;
import com.learningnotes.exception.BusinessException;
import com.learningnotes.mapper.CategoryMapper;
import com.learningnotes.mapper.NoteMapper;
import com.learningnotes.mapper.NoteTagMapper;
import com.learningnotes.mapper.TagMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final NoteMapper noteMapper;
    private final NoteTagMapper noteTagMapper;

    public CatalogService(CategoryMapper categoryMapper, TagMapper tagMapper, NoteMapper noteMapper, NoteTagMapper noteTagMapper) {
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.noteMapper = noteMapper;
        this.noteTagMapper = noteTagMapper;
    }

    public List<Category> listCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder).orderByAsc(Category::getId));
    }

    public Category createCategory(NameRequest request) {
        ensureCategoryNameAvailable(request.name(), null);
        Category category = new Category();
        category.setName(request.name().trim());
        category.setSortOrder(0);
        categoryMapper.insert(category);
        return category;
    }

    public Category updateCategory(Long id, NameRequest request) {
        Category category = categoryMapper.selectById(id);
        if (category == null) throw new BusinessException("分类不存在");
        ensureCategoryNameAvailable(request.name(), id);
        category.setName(request.name().trim());
        categoryMapper.updateById(category);
        return category;
    }

    public void deleteCategory(Long id) {
        if (categoryMapper.selectById(id) == null) throw new BusinessException("分类不存在");
        long count = noteMapper.selectCount(new LambdaQueryWrapper<Note>().eq(Note::getCategoryId, id));
        if (count > 0) throw new BusinessException("该分类仍关联笔记，无法删除");
        categoryMapper.deleteById(id);
    }

    public List<Tag> listTags() {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getName));
    }

    public Tag createTag(NameRequest request) {
        ensureTagNameAvailable(request.name(), null);
        Tag tag = new Tag();
        tag.setName(request.name().trim());
        tagMapper.insert(tag);
        return tag;
    }

    @Transactional
    public void deleteTag(Long id) {
        if (tagMapper.selectById(id) == null) throw new BusinessException("标签不存在");
        noteTagMapper.delete(new LambdaQueryWrapper<NoteTag>().eq(NoteTag::getTagId, id));
        tagMapper.deleteById(id);
    }

    private void ensureCategoryNameAvailable(String name, Long currentId) {
        Category found = categoryMapper.selectOne(new LambdaQueryWrapper<Category>().eq(Category::getName, name.trim()));
        if (found != null && !found.getId().equals(currentId)) throw new BusinessException("分类名称已存在");
    }

    private void ensureTagNameAvailable(String name, Long currentId) {
        Tag found = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name.trim()));
        if (found != null && !found.getId().equals(currentId)) throw new BusinessException("标签名称已存在");
    }
}
