package com.pcpart.assembler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pcpart.assembler.dto.BuildConfigView;
import com.pcpart.assembler.entity.BuildConfig;
import com.pcpart.assembler.mapper.BuildConfigMapper;
import com.pcpart.assembler.mapper.PartCpuCoolerMapper;
import com.pcpart.assembler.mapper.PartCpuMapper;
import com.pcpart.assembler.mapper.PartGpuMapper;
import com.pcpart.assembler.mapper.PartMotherboardMapper;
import com.pcpart.assembler.mapper.PartPcCaseMapper;
import com.pcpart.assembler.mapper.PartPsuMapper;
import com.pcpart.assembler.mapper.PartRamMapper;
import com.pcpart.assembler.mapper.PartStorageMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
public class BuildConfigService extends ServiceImpl<BuildConfigMapper, BuildConfig> {

    /** 按配件名搜索配置单时，每类配件最多取多少个 opendb_id，避免 IN 列表过长。 */
    private static final int NAME_SEARCH_LIMIT = 500;

    private final PartCpuMapper cpuMapper;
    private final PartCpuCoolerMapper coolerMapper;
    private final PartGpuMapper gpuMapper;
    private final PartMotherboardMapper motherboardMapper;
    private final PartRamMapper ramMapper;
    private final PartPsuMapper psuMapper;
    private final PartStorageMapper storageMapper;
    private final PartPcCaseMapper caseMapper;

    public BuildConfigService(PartCpuMapper cpuMapper,
                              PartCpuCoolerMapper coolerMapper,
                              PartGpuMapper gpuMapper,
                              PartMotherboardMapper motherboardMapper,
                              PartRamMapper ramMapper,
                              PartPsuMapper psuMapper,
                              PartStorageMapper storageMapper,
                              PartPcCaseMapper caseMapper) {
        this.cpuMapper = cpuMapper;
        this.coolerMapper = coolerMapper;
        this.gpuMapper = gpuMapper;
        this.motherboardMapper = motherboardMapper;
        this.ramMapper = ramMapper;
        this.psuMapper = psuMapper;
        this.storageMapper = storageMapper;
        this.caseMapper = caseMapper;
    }

    @Transactional
    public BuildConfig saveChecked(BuildConfig config) {
        if (!StringUtils.hasText(config.getConfigName())) {
            throw new IllegalArgumentException("配置单名称不能为空");
        }
        save(config);
        return config;
    }

    @Transactional
    public BuildConfig updateChecked(BuildConfig config) {
        if (config.getId() == null) {
            throw new IllegalArgumentException("更新配置单必须带 id");
        }
        updateById(config);
        return config;
    }

    /**
     * 分页查询配置单，并把 8 类配件的 opendb_id 批量翻译成产品名称。
     * 关键词同时匹配配置单名称、备注、配件编号和配件名称（例如搜“4060”能命中显卡为 RTX 4060 的配置单）。
     */
    public IPage<BuildConfigView> pageBy(long current, long size, String keyword, String configName) {
        LambdaQueryWrapper<BuildConfig> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(configName), BuildConfig::getConfigName, configName);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            Set<String> cpuIds = idsByPartName(cpuMapper, kw);
            Set<String> coolerIds = idsByPartName(coolerMapper, kw);
            Set<String> gpuIds = idsByPartName(gpuMapper, kw);
            Set<String> motherboardIds = idsByPartName(motherboardMapper, kw);
            Set<String> ramIds = idsByPartName(ramMapper, kw);
            Set<String> psuIds = idsByPartName(psuMapper, kw);
            Set<String> storageIds = idsByPartName(storageMapper, kw);
            Set<String> caseIds = idsByPartName(caseMapper, kw);

            w.and(x -> {
                x.like(BuildConfig::getConfigName, kw)
                        .or().like(BuildConfig::getRemark, kw)
                        .or().like(BuildConfig::getCpuId, kw)
                        .or().like(BuildConfig::getCoolerId, kw)
                        .or().like(BuildConfig::getMotherboardId, kw)
                        .or().like(BuildConfig::getRamId, kw)
                        .or().like(BuildConfig::getGpuId, kw)
                        .or().like(BuildConfig::getPsuId, kw)
                        .or().like(BuildConfig::getStorageId, kw)
                        .or().like(BuildConfig::getCaseId, kw);
                if (!cpuIds.isEmpty()) {
                    x.or().in(BuildConfig::getCpuId, cpuIds);
                }
                if (!coolerIds.isEmpty()) {
                    x.or().in(BuildConfig::getCoolerId, coolerIds);
                }
                if (!gpuIds.isEmpty()) {
                    x.or().in(BuildConfig::getGpuId, gpuIds);
                }
                if (!motherboardIds.isEmpty()) {
                    x.or().in(BuildConfig::getMotherboardId, motherboardIds);
                }
                if (!ramIds.isEmpty()) {
                    x.or().in(BuildConfig::getRamId, ramIds);
                }
                if (!psuIds.isEmpty()) {
                    x.or().in(BuildConfig::getPsuId, psuIds);
                }
                if (!storageIds.isEmpty()) {
                    x.or().in(BuildConfig::getStorageId, storageIds);
                }
                if (!caseIds.isEmpty()) {
                    x.or().in(BuildConfig::getCaseId, caseIds);
                }
            });
        }
        w.orderByDesc(BuildConfig::getId);

        Page<BuildConfig> source = page(new Page<>(current, size), w);
        Page<BuildConfigView> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        target.setRecords(toViews(source.getRecords()));
        return target;
    }

    /** 按主键查询配置单，并带上各配件名称；不存在返回 null。 */
    public BuildConfigView getWithNames(Long id) {
        BuildConfig one = getById(id);
        if (one == null) {
            return null;
        }
        return toViews(Collections.singletonList(one)).get(0);
    }

    /**
     * 把配置单实体转成带配件名称的视图。名称按 opendb_id 每类只查一次，避免逐行 N+1 查询。
     */
    public List<BuildConfigView> toViews(List<BuildConfig> configs) {
        List<BuildConfigView> out = new ArrayList<>();
        if (configs == null || configs.isEmpty()) {
            return out;
        }
        Map<String, String> cpuNames = loadNames(cpuMapper, collectIds(configs, BuildConfig::getCpuId));
        Map<String, String> coolerNames = loadNames(coolerMapper, collectIds(configs, BuildConfig::getCoolerId));
        Map<String, String> gpuNames = loadNames(gpuMapper, collectIds(configs, BuildConfig::getGpuId));
        Map<String, String> motherboardNames = loadNames(motherboardMapper, collectIds(configs, BuildConfig::getMotherboardId));
        Map<String, String> ramNames = loadNames(ramMapper, collectIds(configs, BuildConfig::getRamId));
        Map<String, String> psuNames = loadNames(psuMapper, collectIds(configs, BuildConfig::getPsuId));
        Map<String, String> storageNames = loadNames(storageMapper, collectIds(configs, BuildConfig::getStorageId));
        Map<String, String> caseNames = loadNames(caseMapper, collectIds(configs, BuildConfig::getCaseId));

        for (BuildConfig config : configs) {
            BuildConfigView view = new BuildConfigView();
            BeanUtils.copyProperties(config, view);
            view.setCpuName(cpuNames.get(config.getCpuId()));
            view.setCoolerName(coolerNames.get(config.getCoolerId()));
            view.setGpuName(gpuNames.get(config.getGpuId()));
            view.setMotherboardName(motherboardNames.get(config.getMotherboardId()));
            view.setRamName(ramNames.get(config.getRamId()));
            view.setPsuName(psuNames.get(config.getPsuId()));
            view.setStorageName(storageNames.get(config.getStorageId()));
            view.setCaseName(caseNames.get(config.getCaseId()));
            out.add(view);
        }
        return out;
    }

    private Set<String> collectIds(List<BuildConfig> configs, Function<BuildConfig, String> getter) {
        Set<String> ids = new HashSet<>();
        for (BuildConfig config : configs) {
            String id = getter.apply(config);
            if (StringUtils.hasText(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * 按 opendb_id 批量查询配件名称。只取 opendb_id / name 两列，直接读 Map，避免为 8 张表各建一套实体适配。
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> loadNames(BaseMapper<?> mapper, Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<Object> w = new QueryWrapper<>();
        w.select("opendb_id", "name").in("opendb_id", ids);
        List<Map<String, Object>> rows = ((BaseMapper<Object>) mapper).selectMaps(w);
        Map<String, String> names = new HashMap<>(Math.max(16, rows.size() * 2));
        for (Map<String, Object> row : rows) {
            Object opendbId = row.get("opendb_id");
            if (opendbId == null) {
                continue;
            }
            Object name = row.get("name");
            names.put(String.valueOf(opendbId), name == null ? null : String.valueOf(name));
        }
        return names;
    }

    /** 按配件名称模糊匹配出 opendb_id 集合，用于“关键词搜配置单”时命中配件名。 */
    @SuppressWarnings("unchecked")
    private Set<String> idsByPartName(BaseMapper<?> mapper, String keyword) {
        QueryWrapper<Object> w = new QueryWrapper<>();
        w.select("opendb_id").like("name", keyword).last("LIMIT " + NAME_SEARCH_LIMIT);
        List<Map<String, Object>> rows = ((BaseMapper<Object>) mapper).selectMaps(w);
        Set<String> ids = new HashSet<>();
        for (Map<String, Object> row : rows) {
            Object opendbId = row.get("opendb_id");
            if (opendbId != null) {
                ids.add(String.valueOf(opendbId));
            }
        }
        return ids;
    }
}
