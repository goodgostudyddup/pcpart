package com.pcpart.assembler.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pcpart.assembler.common.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public abstract class BasePartController<T> {

    protected abstract com.baomidou.mybatisplus.extension.service.IService<T> service();

    protected abstract String notFoundMsg();

    protected abstract void setId(T body, Long id);

    protected abstract Long getId(T body);

    @GetMapping("/page")
    public Result<IPage<T>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            T query) {
        IPage<T> data = ((com.pcpart.assembler.service.BasePartService<?, T>) service()).pageBy(current, size, query, keyword);
        return Result.ok(data);
    }

    @GetMapping("/{id}")
    public Result<T> get(@PathVariable Long id) {
        T one = service().getById(id);
        if (one == null) {
            return Result.error(404, notFoundMsg());
        }
        return Result.ok(one);
    }

    @PostMapping
    public Result<T> create(@RequestBody T body) {
        setId(body, null);
        service().save(body);
        return Result.ok(body);
    }

    @PutMapping("/{id}")
    public Result<T> update(@PathVariable Long id, @RequestBody T body) {
        setId(body, id);
        if (!service().updateById(body)) {
            return Result.error(404, notFoundMsg());
        }
        return Result.ok(service().getById(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (!service().removeById(id)) {
            return Result.error(404, notFoundMsg());
        }
        return Result.ok();
    }
}
