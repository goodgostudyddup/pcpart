package com.pcpart.assembler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcpart.assembler.entity.PartCpuCooler;
import com.pcpart.assembler.mapper.PartCpuCoolerMapper;
import org.springframework.stereotype.Service;

@Service
public class PartCpuCoolerService extends BasePartService<PartCpuCoolerMapper, PartCpuCooler> {

    @Override
    protected LambdaQueryWrapper<PartCpuCooler> buildQuery(PartCpuCooler q, String keyword) {
        LambdaQueryWrapper<PartCpuCooler> w = new LambdaQueryWrapper<>();
        if (q != null) {
            w.like(hasText(q.getName()), PartCpuCooler::getName, q.getName());
            w.eq(hasText(q.getManufacturer()), PartCpuCooler::getManufacturer, q.getManufacturer());
            w.eq(q.getWaterCooled() != null, PartCpuCooler::getWaterCooled, q.getWaterCooled());
            w.eq(q.getRadiatorSize() != null, PartCpuCooler::getRadiatorSize, q.getRadiatorSize());
        }
        if (hasText(keyword)) {
            w.and(x -> x.like(PartCpuCooler::getName, keyword)
                    .or().like(PartCpuCooler::getManufacturer, keyword)
                    .or().like(PartCpuCooler::getOpendbId, keyword));
        }
        w.last("ORDER BY CASE WHEN price_usd IS NULL THEN 1 ELSE 0 END, id DESC");
        return w;
    }
}
