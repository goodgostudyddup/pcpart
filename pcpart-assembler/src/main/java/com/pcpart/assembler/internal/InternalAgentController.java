package com.pcpart.assembler.internal;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcpart.assembler.common.Result;
import com.pcpart.assembler.compatibility.CompatibilityBuild;
import com.pcpart.assembler.compatibility.CompatibilityResult;
import com.pcpart.assembler.compatibility.CompatibilityRule;
import com.pcpart.assembler.compatibility.CompatibilityRuleInfo;
import com.pcpart.assembler.compatibility.CompatibilityService;
import com.pcpart.assembler.dto.BuildConfigView;
import com.pcpart.assembler.internal.dto.InternalPartDetail;
import com.pcpart.assembler.internal.dto.InternalSearchRequest;
import com.pcpart.assembler.mapper.BuildConfigMapper;
import com.pcpart.assembler.mapper.PartCpuCoolerMapper;
import com.pcpart.assembler.mapper.PartCpuMapper;
import com.pcpart.assembler.mapper.PartGpuMapper;
import com.pcpart.assembler.mapper.PartMotherboardMapper;
import com.pcpart.assembler.mapper.PartPcCaseMapper;
import com.pcpart.assembler.mapper.PartPsuMapper;
import com.pcpart.assembler.mapper.PartRamMapper;
import com.pcpart.assembler.mapper.PartStorageMapper;
import com.pcpart.assembler.service.BuildConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalAgentController {

    private static final Set<String> CATEGORIES = new HashSet<>(Arrays.asList(
            "cpu", "cooler", "gpu", "motherboard", "ram", "psu", "storage", "case"));

    private static final Map<String, Set<String>> ALLOWED_FILTERS = new HashMap<>();

    static {
        ALLOWED_FILTERS.put("cpu", new HashSet<>(Arrays.asList(
                "name", "manufacturer", "series", "variant", "socket", "microarchitecture",
                "core_family", "cores_total", "cores_performance", "cores_efficiency",
                "threads", "tdp", "integrated_graphics", "memory_max", "release_year", "opendb_id")));
        ALLOWED_FILTERS.put("cooler", new HashSet<>(Arrays.asList(
                "name", "manufacturer", "series", "variant", "water_cooled", "radiator_size",
                "fanless", "fan_size", "fan_quantity", "cpu_sockets", "release_year", "opendb_id")));
        ALLOWED_FILTERS.put("gpu", new HashSet<>(Arrays.asList(
                "name", "manufacturer", "series", "variant", "chipset_manufacturer", "chipset",
                "memory_gb", "memory_type", "tdp", "gpu_interface", "cooling",
                "frame_sync", "release_year", "opendb_id")));
        ALLOWED_FILTERS.put("motherboard", new HashSet<>(Arrays.asList(
                "name", "manufacturer", "series", "variant", "socket", "chipset", "form_factor",
                "memory_max", "memory_type", "memory_slots", "wireless",
                "ecc_support", "raid_support", "color", "release_year", "opendb_id")));
        ALLOWED_FILTERS.put("ram", new HashSet<>(Arrays.asList(
                "name", "manufacturer", "series", "variant", "ram_type", "speed_mt",
                "capacity_gb", "modules_qty", "module_gb", "form_factor",
                "cas_latency", "color", "release_year", "opendb_id")));
        ALLOWED_FILTERS.put("psu", new HashSet<>(Arrays.asList(
                "name", "manufacturer", "series", "variant", "wattage", "form_factor",
                "efficiency", "modular", "length_mm", "fanless", "release_year", "opendb_id")));
        ALLOWED_FILTERS.put("storage", new HashSet<>(Arrays.asList(
                "name", "manufacturer", "series", "variant", "storage_type", "capacity_gb",
                "form_factor", "hard_interface", "nvme", "cache_mb", "release_year", "opendb_id")));
        ALLOWED_FILTERS.put("pc_case", new HashSet<>(Arrays.asList(
                "name", "manufacturer", "series", "variant", "form_factor", "color",
                "side_panel", "power_supply", "max_gpu_length", "release_year", "opendb_id")));
        ALLOWED_FILTERS.put("case", ALLOWED_FILTERS.get("pc_case"));
    }

    private final CompatibilityService compatibilityService;
    private final PartCpuMapper cpuMapper;
    private final PartCpuCoolerMapper coolerMapper;
    private final PartGpuMapper gpuMapper;
    private final PartMotherboardMapper motherboardMapper;
    private final PartRamMapper ramMapper;
    private final PartPsuMapper psuMapper;
    private final PartStorageMapper storageMapper;
    private final PartPcCaseMapper caseMapper;
    private final BuildConfigMapper buildConfigMapper;
    private final BuildConfigService buildConfigService;
    private final ObjectMapper objectMapper;

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "ok");
        data.put("rules", CompatibilityRule.values().length);
        return Result.ok(data);
    }

    @GetMapping("/compatibility/rules")
    public Result<List<CompatibilityRuleInfo>> rules() {
        List<CompatibilityRuleInfo> list = new ArrayList<>();
        for (CompatibilityRule r : CompatibilityRule.values()) {
            list.add(r.toInfo());
        }
        return Result.ok(list);
    }

    @PostMapping("/compatibility/check")
    public Result<CompatibilityResult> check(@RequestBody CompatibilityBuild build) {
        return Result.ok(compatibilityService.check(build));
    }

    @PostMapping("/parts/{category}/search")
    public Result<IPage<Map<String, Object>>> search(
            @PathVariable String category,
            @RequestBody(required = false) InternalSearchRequest request) {
        com.baomidou.mybatisplus.core.mapper.BaseMapper<?> mapper = mapperOf(category);
        String table = tableOf(category);
        InternalSearchRequest req = request == null ? new InternalSearchRequest() : request;
        long current = Math.max(1, req.getCurrent());
        long size = Math.min(100, Math.max(1, req.getSize()));

        QueryWrapper<Object> wrapper = new QueryWrapper<>();
        wrapper.select("*");
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.and(w -> w.like("name", req.getKeyword().trim())
                    .or().like("manufacturer", req.getKeyword().trim())
                    .or().like("opendb_id", req.getKeyword().trim()));
        }
        Map<String, Object> filters = req.getFilters() == null ? new HashMap<>() : req.getFilters();
        Set<String> allowed = ALLOWED_FILTERS.getOrDefault(normalizeCategory(category), new HashSet<>());
        for (Map.Entry<String, Object> e : filters.entrySet()) {
            String col = toColumn(e.getKey());
            if (!allowed.contains(col) || e.getValue() == null) {
                continue;
            }
            String text = String.valueOf(e.getValue()).trim();
            if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
                continue;
            }
            if (col.equals("name")) {
                wrapper.like("name", text);
            } else {
                wrapper.eq(col, coerceNumber(col, text));
            }
        }
        if (Boolean.TRUE.equals(req.getRequirePriced())) {
            wrapper.isNotNull("price_cny");
        }
        if (req.getPriceCnyMin() != null) {
            wrapper.ge("price_cny", req.getPriceCnyMin());
        }
        if (req.getPriceCnyMax() != null) {
            wrapper.le("price_cny", req.getPriceCnyMax());
        }
        if (req.getPriceUsdMin() != null) {
            wrapper.ge("price_usd", req.getPriceUsdMin());
        }
        if (req.getPriceUsdMax() != null) {
            wrapper.le("price_usd", req.getPriceUsdMax());
        }
        wrapper.orderBy(true, false, "CASE WHEN price_cny IS NULL THEN 1 ELSE 0 END");
        String sortBy = toColumn(req.getSortBy());
        if (StringUtils.hasText(sortBy) && allowed.contains(sortBy)) {
            boolean asc = !"desc".equalsIgnoreCase(String.valueOf(req.getSortOrder()));
            wrapper.orderBy(true, asc, sortBy);
        } else {
            wrapper.orderByAsc("price_cny");
            wrapper.orderByDesc("id");
        }

        IPage<Map<String, Object>> page = new Page<>(current, size);
        try {
            java.lang.reflect.Method m = mapper.getClass()
                    .getMethod("selectMapsPage", IPage.class, com.baomidou.mybatisplus.core.conditions.Wrapper.class);
            Object out = m.invoke(mapper, page, wrapper);
            @SuppressWarnings("unchecked")
            IPage<Map<String, Object>> typed = (IPage<Map<String, Object>>) out;
            return Result.ok(sanitizePage(category, typed));
        } catch (Exception e) {
            return Result.error("查询 " + table + " 失败: " + e.getMessage());
        }
    }

    @GetMapping("/parts/{category}/{identifier}")
    public Result<InternalPartDetail> detail(
            @PathVariable String category,
            @PathVariable String identifier) {
        com.baomidou.mybatisplus.core.mapper.BaseMapper<?> mapper = mapperOf(category);
        Map<String, Object> row = findRow(mapper, identifier);
        if (row == null) {
            return Result.error(404, category + " 不存在: " + identifier);
        }
        InternalPartDetail detail = new InternalPartDetail();
        detail.setIdField(String.valueOf(row.getOrDefault("opendb_id", row.get("id"))));
        detail.setFields(row);
        Object raw = row.get("raw_json");
        String rawJson = raw == null ? null : String.valueOf(raw);
        detail.setMemoryTypes(extractStringList(rawJson, "specifications", "memory", "types"));
        detail.setSupportedFormFactors(extractStringList(rawJson, "supported_motherboard_form_factors"));
        return Result.ok(detail);
    }

    @PostMapping("/build-configs/validate")
    public Result<CompatibilityResult> validateBuildConfig(@RequestBody Map<String, Object> body) {
        Object id = body == null ? null : body.get("id");
        if (id == null || String.valueOf(id).trim().isEmpty()) {
            return Result.error(400, "缺少配置单 id");
        }
        com.pcpart.assembler.entity.BuildConfig cfg;
        try {
            cfg = buildConfigMapper.selectById(Long.parseLong(String.valueOf(id).trim()));
        } catch (NumberFormatException e) {
            return Result.error(400, "配置单 id 格式错误");
        }
        if (cfg == null) {
            return Result.error(404, "配置单不存在");
        }
        CompatibilityBuild build = new CompatibilityBuild();
        build.setCpuId(cfg.getCpuId());
        build.setCoolerId(cfg.getCoolerId());
        build.setMotherboardId(cfg.getMotherboardId());
        build.setRamId(cfg.getRamId());
        build.setGpuId(cfg.getGpuId());
        build.setPsuId(cfg.getPsuId());
        build.setStorageId(cfg.getStorageId());
        build.setCaseId(cfg.getCaseId());
        return Result.ok(compatibilityService.check(build));
    }

    @PostMapping("/build-configs/save")
    public Result<Map<String, Object>> saveBuildConfig(@RequestBody Map<String, Object> body) {
        if (body == null) {
            return Result.error(400, "缺少配置单参数");
        }
        Object name = body.get("configName");
        if (name == null || String.valueOf(name).trim().isEmpty()) {
            return Result.error(400, "配置单名称不能为空");
        }
        CompatibilityBuild build = new CompatibilityBuild();
        build.setCpuId(strOrNull(body.get("cpuId")));
        build.setCoolerId(strOrNull(body.get("coolerId")));
        build.setMotherboardId(strOrNull(body.get("motherboardId")));
        build.setRamId(strOrNull(body.get("ramId")));
        build.setGpuId(strOrNull(body.get("gpuId")));
        build.setPsuId(strOrNull(body.get("psuId")));
        build.setStorageId(strOrNull(body.get("storageId")));
        build.setCaseId(strOrNull(body.get("caseId")));
        CompatibilityResult checked = compatibilityService.check(build);
        if (!checked.isCompatible()) {
            Map<String, Object> data = new HashMap<>();
            data.put("compatible", false);
            data.put("violations", checked.getViolations());
            data.put("warnings", checked.getWarnings());
            Result<Map<String, Object>> fail = Result.error(400, "兼容性校验不通过，已拒绝保存");
            fail.setData(data);
            return fail;
        }
        com.pcpart.assembler.entity.BuildConfig cfg = new com.pcpart.assembler.entity.BuildConfig();
        cfg.setConfigName(String.valueOf(name).trim());
        cfg.setCpuId(build.getCpuId());
        cfg.setCoolerId(build.getCoolerId());
        cfg.setMotherboardId(build.getMotherboardId());
        cfg.setRamId(build.getRamId());
        cfg.setGpuId(build.getGpuId());
        cfg.setPsuId(build.getPsuId());
        cfg.setStorageId(build.getStorageId());
        cfg.setCaseId(build.getCaseId());
        Object remark = body.get("remark");
        if (remark != null && StringUtils.hasText(String.valueOf(remark))) {
            cfg.setRemark(String.valueOf(remark));
        }
        try {
            buildConfigMapper.insert(cfg);
        } catch (Exception e) {
            return Result.error("保存配置单失败: " + e.getMessage());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", cfg.getId());
        data.put("compatible", true);
        data.put("violations", checked.getViolations());
        data.put("warnings", checked.getWarnings());
        return Result.ok(data);
    }

    @GetMapping("/build-configs/{id}/detail")
    public Result<Map<String, Object>> detailBuildConfig(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return Result.error(400, "配置单 id 不能为空");
        }
        BuildConfigView cfg = buildConfigService.getWithNames(id);
        if (cfg == null) {
            return Result.error(404, "配置单不存在");
        }
        Map<String, Object> out = new HashMap<>();
        out.put("id", cfg.getId());
        out.put("configName", cfg.getConfigName());
        out.put("cpuId", cfg.getCpuId());
        out.put("cpuName", cfg.getCpuName());
        out.put("coolerId", cfg.getCoolerId());
        out.put("coolerName", cfg.getCoolerName());
        out.put("motherboardId", cfg.getMotherboardId());
        out.put("motherboardName", cfg.getMotherboardName());
        out.put("ramId", cfg.getRamId());
        out.put("ramName", cfg.getRamName());
        out.put("gpuId", cfg.getGpuId());
        out.put("gpuName", cfg.getGpuName());
        out.put("psuId", cfg.getPsuId());
        out.put("psuName", cfg.getPsuName());
        out.put("storageId", cfg.getStorageId());
        out.put("storageName", cfg.getStorageName());
        out.put("caseId", cfg.getCaseId());
        out.put("caseName", cfg.getCaseName());
        out.put("remark", cfg.getRemark());
        return Result.ok(out);
    }

    private String strOrNull(Object v) {
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private com.baomidou.mybatisplus.core.mapper.BaseMapper<?> mapperOf(String category) {
        switch (normalizeCategory(category)) {
            case "cpu":
                return cpuMapper;
            case "cooler":
                return coolerMapper;
            case "gpu":
                return gpuMapper;
            case "motherboard":
                return motherboardMapper;
            case "ram":
                return ramMapper;
            case "psu":
                return psuMapper;
            case "storage":
                return storageMapper;
            case "case":
            case "pc_case":
                return caseMapper;
            default:
                throw new IllegalArgumentException("未知配件类别: " + category + "，支持 " + CATEGORIES);
        }
    }

    private String tableOf(String category) {
        switch (normalizeCategory(category)) {
            case "cpu":
                return "part_cpu";
            case "cooler":
                return "part_cpu_cooler";
            case "gpu":
                return "part_gpu";
            case "motherboard":
                return "part_motherboard";
            case "ram":
                return "part_ram";
            case "psu":
                return "part_psu";
            case "storage":
                return "part_storage";
            case "case":
            case "pc_case":
                return "part_pc_case";
            default:
                throw new IllegalArgumentException("未知配件类别: " + category);
        }
    }

    private String normalizeCategory(String category) {
        if (!StringUtils.hasText(category)) {
            throw new IllegalArgumentException("缺少配件类别");
        }
        String c = category.trim().toLowerCase().replace("-", "_");
        if (c.equals("cases") || c.equals("case") || c.equals("pc_case") || c.equals("pccase")) {
            return "case";
        }
        if (c.equals("cpus") || c.equals("cpu")) {
            return "cpu";
        }
        if (c.equals("coolers") || c.equals("cooler") || c.equals("cpu_cooler") || c.equals("cpucooler")) {
            return "cooler";
        }
        if (c.equals("gpus") || c.equals("gpu")) {
            return "gpu";
        }
        if (c.equals("motherboards") || c.equals("motherboard") || c.equals("mainboard")) {
            return "motherboard";
        }
        if (c.equals("rams") || c.equals("ram") || c.equals("memory")) {
            return "ram";
        }
        if (c.equals("psus") || c.equals("psu") || c.equals("power")) {
            return "psu";
        }
        if (c.equals("storages") || c.equals("storage") || c.equals("disk")) {
            return "storage";
        }
        throw new IllegalArgumentException("未知配件类别: " + category + "，支持 " + CATEGORIES);
    }

    private Map<String, Object> findRow(com.baomidou.mybatisplus.core.mapper.BaseMapper<?> mapper, String identifier) {
        String id = identifier == null ? "" : identifier.trim();
        if (id.isEmpty()) {
            return null;
        }
        try {
            long numeric = Long.parseLong(id);
            QueryWrapper<Object> w = new QueryWrapper<>();
            w.eq("id", numeric);
            return rawSelectOne(mapper, w);
        } catch (NumberFormatException e) {
            QueryWrapper<Object> w = new QueryWrapper<>();
            w.eq("opendb_id", id);
            return rawSelectOne(mapper, w);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> rawSelectOne(com.baomidou.mybatisplus.core.mapper.BaseMapper<?> mapper, QueryWrapper<Object> wrapper) {
        try {
            java.lang.reflect.Method m = mapper.getClass()
                    .getMethod("selectMaps", com.baomidou.mybatisplus.core.conditions.Wrapper.class);
            List<Map<String, Object>> rows = (List<Map<String, Object>>) m.invoke(mapper, wrapper);
            return rows == null || rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            throw new IllegalStateException("查询配件详情失败: " + e.getMessage());
        }
    }

    private IPage<Map<String, Object>> sanitizePage(String category, IPage<Map<String, Object>> page) {
        if (page == null || page.getRecords() == null) {
            return page;
        }
        for (Map<String, Object> row : page.getRecords()) {
            row.remove("raw_json");
        }
        return page;
    }

    private List<String> extractStringList(String rawJson, String... path) {
        List<String> out = new ArrayList<>();
        if (!StringUtils.hasText(rawJson)) {
            return out;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(rawJson);
            for (String p : path) {
                node = node.path(p);
            }
            if (node.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode n : node) {
                    if (n.isTextual() && StringUtils.hasText(n.asText())) {
                        out.add(n.asText());
                    }
                }
            }
        } catch (Exception e) {
            return out;
        }
        return out;
    }

    private String toColumn(String key) {
        if (!StringUtils.hasText(key)) {
            return "";
        }
        String k = key.trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < k.length(); i++) {
            char c = k.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    private Object coerceNumber(String col, String text) {
        Set<String> numeric = new HashSet<>(Arrays.asList(
                "cores_total", "cores_performance", "cores_efficiency", "threads",
                "tdp", "memory_max", "memory_gb", "memory_slots", "speed_mt",
                "capacity_gb", "modules_qty", "module_gb", "cas_latency",
                "wattage", "length_mm", "radiator_size", "fan_size", "fan_quantity",
                "max_gpu_length", "release_year", "water_cooled", "fanless",
                "nvme", "cache_mb", "ecc_support", "raid_support"));
        if (!numeric.contains(col)) {
            return text;
        }
        try {
            if (text.contains(".")) {
                return new java.math.BigDecimal(text);
            }
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return text;
        }
    }
}
