package com.pcpart.assembler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcpart.assembler.entity.PartPcCase;
import com.pcpart.assembler.mapper.PartPcCaseMapper;
import org.springframework.stereotype.Service;

@Service
public class PartPcCaseService extends BasePartService<PartPcCaseMapper, PartPcCase> {

    @Override
    protected LambdaQueryWrapper<PartPcCase> buildQuery(PartPcCase q, String keyword) {
        LambdaQueryWrapper<PartPcCase> w = new LambdaQueryWrapper<>();
        if (q != null) {
            w.like(hasText(q.getName()), PartPcCase::getName, q.getName());
            w.eq(hasText(q.getFormFactor()), PartPcCase::getFormFactor, q.getFormFactor());
            w.eq(hasText(q.getManufacturer()), PartPcCase::getManufacturer, q.getManufacturer());
        }
        if (hasText(keyword)) {
            w.and(x -> x.like(PartPcCase::getName, keyword)
                    .or().like(PartPcCase::getFormFactor, keyword)
                    .or().like(PartPcCase::getOpendbId, keyword));
        }
        w.last("ORDER BY CASE WHEN price_usd IS NULL THEN 1 ELSE 0 END, id DESC");
        return w;
    }
}
