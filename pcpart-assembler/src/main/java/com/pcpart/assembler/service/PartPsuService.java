package com.pcpart.assembler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcpart.assembler.entity.PartPsu;
import com.pcpart.assembler.mapper.PartPsuMapper;
import org.springframework.stereotype.Service;

@Service
public class PartPsuService extends BasePartService<PartPsuMapper, PartPsu> {

    @Override
    protected LambdaQueryWrapper<PartPsu> buildQuery(PartPsu q, String keyword) {
        LambdaQueryWrapper<PartPsu> w = new LambdaQueryWrapper<>();
        if (q != null) {
            w.like(hasText(q.getName()), PartPsu::getName, q.getName());
            w.eq(q.getWattage() != null, PartPsu::getWattage, q.getWattage());
            w.eq(hasText(q.getEfficiency()), PartPsu::getEfficiency, q.getEfficiency());
            w.eq(hasText(q.getModular()), PartPsu::getModular, q.getModular());
        }
        if (hasText(keyword)) {
            w.and(x -> x.like(PartPsu::getName, keyword)
                    .or().like(PartPsu::getEfficiency, keyword)
                    .or().like(PartPsu::getOpendbId, keyword));
        }
        w.last("ORDER BY CASE WHEN price_usd IS NULL THEN 1 ELSE 0 END, id DESC");
        return w;
    }
}
