package com.pcpart.assembler.controller;

import com.pcpart.assembler.entity.PartGpu;
import com.pcpart.assembler.service.PartGpuService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gpus")
public class PartGpuController extends BasePartController<PartGpu> {

    private final PartGpuService service;

    public PartGpuController(PartGpuService service) {
        this.service = service;
    }

    @Override
    protected com.baomidou.mybatisplus.extension.service.IService<PartGpu> service() {
        return service;
    }

    @Override
    protected String notFoundMsg() {
        return "显卡不存在";
    }

    @Override
    protected void setId(PartGpu body, Long id) {
        body.setId(id);
    }

    @Override
    protected Long getId(PartGpu body) {
        return body.getId();
    }
}
