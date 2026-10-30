package com.pcpart.assembler.controller;

import com.pcpart.assembler.entity.PartMotherboard;
import com.pcpart.assembler.service.PartMotherboardService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/motherboards")
public class PartMotherboardController extends BasePartController<PartMotherboard> {

    private final PartMotherboardService service;

    public PartMotherboardController(PartMotherboardService service) {
        this.service = service;
    }

    @Override
    protected com.baomidou.mybatisplus.extension.service.IService<PartMotherboard> service() {
        return service;
    }

    @Override
    protected String notFoundMsg() {
        return "主板不存在";
    }

    @Override
    protected void setId(PartMotherboard body, Long id) {
        body.setId(id);
    }

    @Override
    protected Long getId(PartMotherboard body) {
        return body.getId();
    }
}
