package com.pcpart.assembler.controller;

import com.pcpart.assembler.entity.PartCpuCooler;
import com.pcpart.assembler.service.PartCpuCoolerService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coolers")
public class PartCpuCoolerController extends BasePartController<PartCpuCooler> {

    private final PartCpuCoolerService service;

    public PartCpuCoolerController(PartCpuCoolerService service) {
        this.service = service;
    }

    @Override
    protected com.baomidou.mybatisplus.extension.service.IService<PartCpuCooler> service() {
        return service;
    }

    @Override
    protected String notFoundMsg() {
        return "散热器不存在";
    }

    @Override
    protected void setId(PartCpuCooler body, Long id) {
        body.setId(id);
    }

    @Override
    protected Long getId(PartCpuCooler body) {
        return body.getId();
    }
}
