package com.pcpart.assembler.controller;

import com.pcpart.assembler.entity.PartCpu;
import com.pcpart.assembler.service.PartCpuService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cpus")
public class PartCpuController extends BasePartController<PartCpu> {

    private final PartCpuService service;

    public PartCpuController(PartCpuService service) {
        this.service = service;
    }

    @Override
    protected com.baomidou.mybatisplus.extension.service.IService<PartCpu> service() {
        return service;
    }

    @Override
    protected String notFoundMsg() {
        return "CPU 不存在";
    }

    @Override
    protected void setId(PartCpu body, Long id) {
        body.setId(id);
    }

    @Override
    protected Long getId(PartCpu body) {
        return body.getId();
    }
}
