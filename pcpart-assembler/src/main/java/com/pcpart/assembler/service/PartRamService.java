package com.pcpart.assembler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcpart.assembler.entity.PartRam;
import com.pcpart.assembler.mapper.PartRamMapper;
import org.springframework.stereotype.Service;

@Service
public class PartRamService extends BasePartService<PartRamMapper, PartRam> {

    @Override
    protected LambdaQueryWrapper<PartRam> buildQuery(PartRam q, String keyword) {
        LambdaQueryWrapper<PartRam> w = new LambdaQueryWrapper<>();
        if (q != null) {
            w.like(hasText(q.getName()), PartRam::getName, q.getName());
            w.eq(hasText(q.getRamType()), PartRam::getRamType, q.getRamType());
            w.eq(q.getSpeedMt() != null, PartRam::getSpeedMt, q.getSpeedMt());
            w.eq(q.getCapacityGb() != null, PartRam::getCapacityGb, q.getCapacityGb());
        }
        if (hasText(keyword)) {
            w.and(x -> x.like(PartRam::getName, keyword)
                    .or().like(PartRam::getRamType, keyword)
                    .or().like(PartRam::getOpendbId, keyword));
        }
        w.last("ORDER BY CASE WHEN price_usd IS NULL THEN 1 ELSE 0 END, id DESC");
        return w;
    }
}
