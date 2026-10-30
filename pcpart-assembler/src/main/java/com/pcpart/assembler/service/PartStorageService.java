package com.pcpart.assembler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcpart.assembler.entity.PartStorage;
import com.pcpart.assembler.mapper.PartStorageMapper;
import org.springframework.stereotype.Service;

@Service
public class PartStorageService extends BasePartService<PartStorageMapper, PartStorage> {

    @Override
    protected LambdaQueryWrapper<PartStorage> buildQuery(PartStorage q, String keyword) {
        LambdaQueryWrapper<PartStorage> w = new LambdaQueryWrapper<>();
        if (q != null) {
            w.like(hasText(q.getName()), PartStorage::getName, q.getName());
            w.eq(hasText(q.getStorageType()), PartStorage::getStorageType, q.getStorageType());
            w.eq(q.getCapacityGb() != null, PartStorage::getCapacityGb, q.getCapacityGb());
            w.eq(hasText(q.getFormFactor()), PartStorage::getFormFactor, q.getFormFactor());
        }
        if (hasText(keyword)) {
            w.and(x -> x.like(PartStorage::getName, keyword)
                    .or().like(PartStorage::getStorageType, keyword)
                    .or().like(PartStorage::getOpendbId, keyword));
        }
        w.last("ORDER BY CASE WHEN price_usd IS NULL THEN 1 ELSE 0 END, id DESC");
        return w;
    }
}
