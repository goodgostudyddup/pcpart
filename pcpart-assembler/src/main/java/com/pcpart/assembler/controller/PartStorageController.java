package com.pcpart.assembler.controller;

import com.pcpart.assembler.entity.PartStorage;
import com.pcpart.assembler.service.PartStorageService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/storages")
public class PartStorageController extends BasePartController<PartStorage> {

    private final PartStorageService service;

    public PartStorageController(PartStorageService service) {
        this.service = service;
    }

    @Override
    protected com.baomidou.mybatisplus.extension.service.IService<PartStorage> service() {
        return service;
    }

    @Override
    protected String notFoundMsg() {
        return "硬盘不存在";
    }

    @Override
    protected void setId(PartStorage body, Long id) {
        body.setId(id);
    }

    @Override
    protected Long getId(PartStorage body) {
        return body.getId();
    }
}
