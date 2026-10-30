package com.pcpart.assembler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcpart.assembler.entity.PartGpu;
import com.pcpart.assembler.mapper.PartGpuMapper;
import org.springframework.stereotype.Service;

@Service
public class PartGpuService extends BasePartService<PartGpuMapper, PartGpu> {

    @Override
    protected LambdaQueryWrapper<PartGpu> buildQuery(PartGpu q, String keyword) {
        LambdaQueryWrapper<PartGpu> w = new LambdaQueryWrapper<>();
        if (q != null) {
            w.like(hasText(q.getName()), PartGpu::getName, q.getName());
            w.like(hasText(q.getChipset()), PartGpu::getChipset, q.getChipset());
            w.eq(hasText(q.getChipsetManufacturer()), PartGpu::getChipsetManufacturer, q.getChipsetManufacturer());
            w.eq(q.getMemoryGb() != null, PartGpu::getMemoryGb, q.getMemoryGb());
        }
        if (hasText(keyword)) {
            w.and(x -> x.like(PartGpu::getName, keyword)
                    .or().like(PartGpu::getChipset, keyword)
                    .or().like(PartGpu::getOpendbId, keyword));
        }
        w.last("ORDER BY CASE WHEN price_usd IS NULL THEN 1 ELSE 0 END, id DESC");
        return w;
    }
}
