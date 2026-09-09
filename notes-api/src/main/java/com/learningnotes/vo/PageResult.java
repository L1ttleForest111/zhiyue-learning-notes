package com.learningnotes.vo;

import java.util.List;

public record PageResult<T>(List<T> records, long total, long page, long pageSize) {}
