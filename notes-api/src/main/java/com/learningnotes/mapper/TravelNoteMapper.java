package com.learningnotes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learningnotes.entity.TravelNote;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TravelNoteMapper extends BaseMapper<TravelNote> {}
