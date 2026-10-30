package com.pcpart.assembler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcpart.assembler.entity.PartCpu;
import com.pcpart.assembler.mapper.PartCpuMapper;
import org.springframework.stereotype.Service;

@Service
public class PartCpuService extends BasePartService<PartCpuMapper, PartCpu> {

    @Override
    protected LambdaQueryWrapper<PartCpu> buildQuery(PartCpu q, String keyword) {
        LambdaQueryWrapper<PartCpu> w = new LambdaQueryWrapper<>();
        if (q != null) {
            w.like(hasText(q.getName()), PartCpu::getName, q.getName());
            w.eq(hasText(q.getSocket()), PartCpu::getSocket, q.getSocket());
            w.eq(hasText(q.getManufacturer()), PartCpu::getManufacturer, q.getManufacturer());
            w.eq(hasText(q.getMicroarchitecture()), PartCpu::getMicroarchitecture, q.getMicroarchitecture());
            w.eq(q.getCoresTotal() != null, PartCpu::getCoresTotal, q.getCoresTotal());
            w.eq(q.getTdp() != null, PartCpu::getTdp, q.getTdp());
        }
        if (hasText(keyword)) {
            w.and(x -> x.like(PartCpu::getName, keyword)
                    .or().like(PartCpu::getSocket, keyword)
                    .or().like(PartCpu::getManufacturer, keyword)
                    .or().like(PartCpu::getOpendbId, keyword));
        }
        w.last("ORDER BY CASE WHEN price_usd IS NULL THEN 1 ELSE 0 END, id DESC");
        return w;
    }
}
