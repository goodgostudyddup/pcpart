package com.pcpart.assembler.controller;

import com.pcpart.assembler.entity.PartPsu;
import com.pcpart.assembler.service.PartPsuService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/psus")
public class PartPsuController extends BasePartController<PartPsu> {

    private final PartPsuService service;

    public PartPsuController(PartPsuService service) {
        this.service = service;
    }

    @Override
    protected com.baomidou.mybatisplus.extension.service.IService<PartPsu> service() {
        return service;
    }

    @Override
    protected String notFoundMsg() {
        return "电源不存在";
    }

    @Override
    protected void setId(PartPsu body, Long id) {
        body.setId(id);
    }

    @Override
    protected Long getId(PartPsu body) {
        return body.getId();
    }
}
