package com.pcpart.assembler.controller;

import com.pcpart.assembler.entity.PartRam;
import com.pcpart.assembler.service.PartRamService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rams")
public class PartRamController extends BasePartController<PartRam> {

    private final PartRamService service;

    public PartRamController(PartRamService service) {
        this.service = service;
    }

    @Override
    protected com.baomidou.mybatisplus.extension.service.IService<PartRam> service() {
        return service;
    }

    @Override
    protected String notFoundMsg() {
        return "内存不存在";
    }

    @Override
    protected void setId(PartRam body, Long id) {
        body.setId(id);
    }

    @Override
    protected Long getId(PartRam body) {
        return body.getId();
    }
}
