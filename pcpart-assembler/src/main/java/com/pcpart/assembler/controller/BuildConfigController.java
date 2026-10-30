package com.pcpart.assembler.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pcpart.assembler.common.Result;
import com.pcpart.assembler.dto.BuildConfigView;
import com.pcpart.assembler.entity.BuildConfig;
import com.pcpart.assembler.service.BuildConfigService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/build-configs")
public class BuildConfigController {

    private final BuildConfigService service;

    public BuildConfigController(BuildConfigService service) {
        this.service = service;
    }

    @GetMapping("/page")
    public Result<IPage<BuildConfigView>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String configName) {
        return Result.ok(service.pageBy(current, size, keyword, configName));
    }

    @GetMapping("/{id}")
    public Result<BuildConfigView> get(@PathVariable Long id) {
        BuildConfigView one = service.getWithNames(id);
        if (one == null) {
            return Result.error(404, "配置单不存在");
        }
        return Result.ok(one);
    }

    @PostMapping
    public Result<BuildConfig> create(@RequestBody BuildConfig body) {
        body.setId(null);
        return Result.ok(service.saveChecked(body));
    }

    @PutMapping("/{id}")
    public Result<BuildConfig> update(@PathVariable Long id, @RequestBody BuildConfig body) {
        body.setId(id);
        service.updateChecked(body);
        return Result.ok(service.getById(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (!service.removeById(id)) {
            return Result.error(404, "配置单不存在");
        }
        return Result.ok();
    }
}
