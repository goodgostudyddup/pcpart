package com.pcpart.assembler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcpart.assembler.entity.PartMotherboard;
import com.pcpart.assembler.mapper.PartMotherboardMapper;
import org.springframework.stereotype.Service;

@Service
public class PartMotherboardService extends BasePartService<PartMotherboardMapper, PartMotherboard> {

    @Override
    protected LambdaQueryWrapper<PartMotherboard> buildQuery(PartMotherboard q, String keyword) {
        LambdaQueryWrapper<PartMotherboard> w = new LambdaQueryWrapper<>();
        if (q != null) {
            w.like(hasText(q.getName()), PartMotherboard::getName, q.getName());
            w.eq(hasText(q.getSocket()), PartMotherboard::getSocket, q.getSocket());
            w.eq(hasText(q.getFormFactor()), PartMotherboard::getFormFactor, q.getFormFactor());
            w.like(hasText(q.getChipset()), PartMotherboard::getChipset, q.getChipset());
        }
        if (hasText(keyword)) {
            w.and(x -> x.like(PartMotherboard::getName, keyword)
                    .or().like(PartMotherboard::getSocket, keyword)
                    .or().like(PartMotherboard::getChipset, keyword)
                    .or().like(PartMotherboard::getOpendbId, keyword));
        }
        w.last("ORDER BY CASE WHEN price_usd IS NULL THEN 1 ELSE 0 END, id DESC");
        return w;
    }
}
