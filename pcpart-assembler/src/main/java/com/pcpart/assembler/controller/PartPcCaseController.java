package com.pcpart.assembler.controller;

import com.pcpart.assembler.entity.PartPcCase;
import com.pcpart.assembler.service.PartPcCaseService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cases")
public class PartPcCaseController extends BasePartController<PartPcCase> {

    private final PartPcCaseService service;

    public PartPcCaseController(PartPcCaseService service) {
        this.service = service;
    }

    @Override
    protected com.baomidou.mybatisplus.extension.service.IService<PartPcCase> service() {
        return service;
    }

    @Override
    protected String notFoundMsg() {
        return "机箱不存在";
    }

    @Override
    protected void setId(PartPcCase body, Long id) {
        body.setId(id);
    }

    @Override
    protected Long getId(PartPcCase body) {
        return body.getId();
    }
}
