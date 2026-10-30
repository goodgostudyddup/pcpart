package com.pcpart.assembler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.util.StringUtils;

public abstract class BasePartService<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> {

    protected abstract LambdaQueryWrapper<T> buildQuery(T query, String keyword);

    public IPage<T> pageBy(long current, long size, T query, String keyword) {
        Page<T> page = new Page<>(current, size);
        return page(page, buildQuery(query, keyword));
    }

    protected boolean hasText(String s) {
        return StringUtils.hasText(s);
    }
}
