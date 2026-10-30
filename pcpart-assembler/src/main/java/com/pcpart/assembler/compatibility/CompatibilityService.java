package com.pcpart.assembler.compatibility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcpart.assembler.entity.PartCpu;
import com.pcpart.assembler.entity.PartCpuCooler;
import com.pcpart.assembler.entity.PartGpu;
import com.pcpart.assembler.entity.PartMotherboard;
import com.pcpart.assembler.entity.PartPcCase;
import com.pcpart.assembler.entity.PartPsu;
import com.pcpart.assembler.entity.PartRam;
import com.pcpart.assembler.service.PartCpuCoolerService;
import com.pcpart.assembler.service.PartCpuService;
import com.pcpart.assembler.service.PartGpuService;
import com.pcpart.assembler.service.PartMotherboardService;
import com.pcpart.assembler.service.PartPcCaseService;
import com.pcpart.assembler.service.PartPsuService;
import com.pcpart.assembler.service.PartRamService;
import com.pcpart.assembler.service.PartStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompatibilityService {

    private final PartCpuService cpuService;
    private final PartCpuCoolerService coolerService;
    private final PartMotherboardService motherboardService;
    private final PartRamService ramService;
    private final PartGpuService gpuService;
    private final PartPsuService psuService;
    private final PartStorageService storageService;
    private final PartPcCaseService caseService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CompatibilityResult check(CompatibilityBuild build) {
        CompatibilityResult result = CompatibilityResult.ok();
        if (build == null) {
            addViolation(result, CompatibilityRule.R1, "配置单为空，无法校验");
            markRuleStatus(result, null, null, null, null, null, null, null);
            result.setCompatible(false);
            return result;
        }

        PartCpu cpu = lookupCpu(build.getCpuId());
        PartMotherboard mb = lookupMotherboard(build.getMotherboardId());
        PartRam ram = lookupRam(build.getRamId());
        PartGpu gpu = lookupGpu(build.getGpuId());
        PartPsu psu = lookupPsu(build.getPsuId());
        PartPcCase pcCase = lookupCase(build.getCaseId());
        PartCpuCooler cooler = lookupCooler(build.getCoolerId());

        checkR1(result, cpu, mb);
        checkR2(result, cpu, mb);
        checkR3(result, cpu, mb);
        checkR4(result, ram, mb);
        checkR5(result, ram, mb);
        checkR6(result, ram, mb, build.getRamQty());
        checkR7(result, gpu, pcCase);
        checkR8(result, cooler, pcCase);
        checkR9(result, mb, pcCase);
        checkR10(result, cpu, gpu, psu);

        markRuleStatus(result, cpu, mb, ram, gpu, psu, pcCase, cooler);
        result.setCompatible(result.getViolations().isEmpty());
        return result;
    }

    private void markRuleStatus(CompatibilityResult result, PartCpu cpu, PartMotherboard mb, PartRam ram,
                                PartGpu gpu, PartPsu psu, PartPcCase pcCase, PartCpuCooler cooler) {
        Map<CompatibilityRule, Boolean> applicable = new LinkedHashMap<>();
        applicable.put(CompatibilityRule.R1, cpu != null && mb != null);
        applicable.put(CompatibilityRule.R2, cpu != null && mb != null);
        applicable.put(CompatibilityRule.R3, cpu != null && mb != null);
        applicable.put(CompatibilityRule.R4, ram != null && mb != null);
        applicable.put(CompatibilityRule.R5, ram != null && mb != null);
        applicable.put(CompatibilityRule.R6, ram != null && mb != null);
        applicable.put(CompatibilityRule.R7, gpu != null && pcCase != null);
        applicable.put(CompatibilityRule.R8, cooler != null && pcCase != null);
        applicable.put(CompatibilityRule.R9, mb != null && pcCase != null);
        applicable.put(CompatibilityRule.R10, psu != null && (cpu != null || gpu != null));
        int evaluated = 0;
        int skipped = 0;
        for (Map.Entry<CompatibilityRule, Boolean> entry : applicable.entrySet()) {
            String id = entry.getKey().getId();
            String status;
            if (!entry.getValue()) {
                status = "skipped";
                skipped++;
            } else if (hasRule(result.getViolations(), id)) {
                status = "violation";
                evaluated++;
            } else if (hasRule(result.getWarnings(), id)) {
                status = "warning";
                evaluated++;
            } else {
                status = "pass";
                evaluated++;
            }
            result.getRuleStatus().put(id, status);
        }
        result.setRulesTotal(applicable.size());
        result.setRulesEvaluated(evaluated);
        result.setRulesSkipped(skipped);
        result.setCheckedAt(OffsetDateTime.now().toString());
    }

    private boolean hasRule(List<CompatibilityViolation> list, String ruleId) {
        for (CompatibilityViolation item : list) {
            if (ruleId.equals(item.getRuleId())) {
                return true;
            }
        }
        return false;
    }

    private void checkR1(CompatibilityResult result, PartCpu cpu, PartMotherboard mb) {
        if (cpu == null || mb == null) {
            return;
        }
        String c = normSocket(cpu.getSocket());
        String m = normSocket(mb.getSocket());
        if (!StringUtils.hasText(c) || !StringUtils.hasText(m)) {
            addWarning(result, CompatibilityRule.R1, "CPU 或主板插槽数据缺失，跳过 R1 校验");
            return;
        }
        if (!c.equals(m)) {
            addViolation(result, CompatibilityRule.R1,
                    "CPU 插槽 " + cpu.getSocket() + " 与主板插槽 " + mb.getSocket() + " 不一致");
        }
    }

    private void checkR2(CompatibilityResult result, PartCpu cpu, PartMotherboard mb) {
        if (cpu == null || mb == null) {
            return;
        }
        String cpuVendor = normVendor(cpu.getManufacturer());
        String chipsetVendor = chipsetVendor(mb.getChipset());
        if (!StringUtils.hasText(cpuVendor) || !StringUtils.hasText(chipsetVendor)) {
            addWarning(result, CompatibilityRule.R2, "CPU 厂商或主板芯片组厂商未知，跳过 R2 校验");
            return;
        }
        if (!cpuVendor.equals(chipsetVendor)) {
            addViolation(result, CompatibilityRule.R2,
                    "主板芯片组 " + mb.getChipset() + " 与 CPU 厂商 " + cpu.getManufacturer() + " 不匹配");
        }
    }

    private void checkR3(CompatibilityResult result, PartCpu cpu, PartMotherboard mb) {
        if (cpu == null || mb == null) {
            return;
        }
        Set<String> cpuTypes = cpuMemoryTypes(cpu.getRawJson());
        String mbType = normMem(mb.getMemoryType());
        if (cpuTypes.isEmpty() || !StringUtils.hasText(mbType)) {
            addWarning(result, CompatibilityRule.R3, "CPU 内存类型或主板内存类型缺失，跳过 R3 校验");
            return;
        }
        if (!cpuTypes.contains(mbType)) {
            addViolation(result, CompatibilityRule.R3,
                    "CPU 支持内存 " + cpuTypes + "，主板需要 " + mb.getMemoryType());
        }
    }

    private void checkR4(CompatibilityResult result, PartRam ram, PartMotherboard mb) {
        if (ram == null || mb == null) {
            return;
        }
        String r = normMem(ram.getRamType());
        String m = normMem(mb.getMemoryType());
        if (!StringUtils.hasText(r) || !StringUtils.hasText(m)) {
            addWarning(result, CompatibilityRule.R4, "内存或主板内存类型缺失，跳过 R4 校验");
            return;
        }
        if (!r.equals(m)) {
            addViolation(result, CompatibilityRule.R4,
                    "内存类型 " + ram.getRamType() + " 与主板内存类型 " + mb.getMemoryType() + " 不一致");
        }
    }

    private void checkR5(CompatibilityResult result, PartRam ram, PartMotherboard mb) {
        if (ram == null || mb == null) {
            return;
        }
        if (ram.getModulesQty() == null || mb.getMemorySlots() == null) {
            addWarning(result, CompatibilityRule.R5, "内存条数或主板插槽数缺失，跳过 R5 校验");
            return;
        }
        if (ram.getModulesQty() > mb.getMemorySlots()) {
            addViolation(result, CompatibilityRule.R5,
                    "内存条数 " + ram.getModulesQty() + " 超过主板插槽数 " + mb.getMemorySlots());
        }
    }

    private void checkR6(CompatibilityResult result, PartRam ram, PartMotherboard mb, Long ramQty) {
        if (ram == null || mb == null) {
            return;
        }
        if (ram.getCapacityGb() == null || mb.getMemoryMax() == null) {
            addWarning(result, CompatibilityRule.R6, "内存容量或主板最大内存缺失，跳过 R6 校验");
            return;
        }
        long kits = ramQty == null || ramQty < 1 ? 1 : ramQty;
        long total = (long) ram.getCapacityGb() * kits;
        if (total > mb.getMemoryMax()) {
            addViolation(result, CompatibilityRule.R6,
                    "内存总容量 " + total + "GB 超过主板最大内存 " + mb.getMemoryMax() + "GB");
        }
    }

    private void checkR7(CompatibilityResult result, PartGpu gpu, PartPcCase pcCase) {
        if (gpu == null || pcCase == null) {
            return;
        }
        if (gpu.getLengthMm() == null || pcCase.getMaxGpuLength() == null) {
            addWarning(result, CompatibilityRule.R7, "显卡长度或机箱最大显卡长度缺失，跳过 R7 校验");
            return;
        }
        if (gpu.getLengthMm().compareTo(BigDecimal.valueOf(pcCase.getMaxGpuLength())) > 0) {
            addViolation(result, CompatibilityRule.R7,
                    "显卡长度 " + gpu.getLengthMm() + "mm 超过机箱上限 " + pcCase.getMaxGpuLength() + "mm");
        }
    }

    private void checkR8(CompatibilityResult result, PartCpuCooler cooler, PartPcCase pcCase) {
        if (cooler == null || pcCase == null) {
            return;
        }
        if (cooler.getHeight() == null || pcCase.getMaxCoolerHeight() == null) {
            addWarning(result, CompatibilityRule.R8, "散热器高度或机箱最大散热器高度缺失，跳过 R8 校验");
            return;
        }
        if (cooler.getHeight().compareTo(pcCase.getMaxCoolerHeight()) > 0) {
            addViolation(result, CompatibilityRule.R8,
                    "散热器高度 " + cooler.getHeight() + "mm 超过机箱上限 " + pcCase.getMaxCoolerHeight() + "mm");
        }
    }

    private void checkR9(CompatibilityResult result, PartMotherboard mb, PartPcCase pcCase) {
        if (mb == null || pcCase == null) {
            return;
        }
        String mbForm = normForm(mb.getFormFactor());
        Set<String> supported = caseSupportedForms(pcCase.getRawJson());
        if (!StringUtils.hasText(mbForm) || supported.isEmpty()) {
            addWarning(result, CompatibilityRule.R9, "主板板型或机箱支持板型数据缺失，跳过 R9 校验");
            return;
        }
        if (!supported.contains(mbForm)) {
            addViolation(result, CompatibilityRule.R9,
                    "主板板型 " + mb.getFormFactor() + " 不在机箱支持范围 " + supported + " 内");
        }
    }

    private void checkR10(CompatibilityResult result, PartCpu cpu, PartGpu gpu, PartPsu psu) {
        if (psu == null || (cpu == null && gpu == null)) {
            return;
        }
        if (psu.getWattage() == null) {
            addWarning(result, CompatibilityRule.R10, "电源额定功率缺失，跳过 R10 校验");
            return;
        }
        int cpuTdp = cpu != null && cpu.getTdp() != null ? cpu.getTdp() : 0;
        int gpuTdp = gpu != null && gpu.getTdp() != null ? gpu.getTdp() : 0;
        if (cpuTdp == 0 && gpuTdp == 0) {
            addWarning(result, CompatibilityRule.R10, "CPU/GPU 功耗数据缺失，跳过 R10 校验");
            return;
        }
        double required = (cpuTdp + gpuTdp) * 1.4;
        if (psu.getWattage() < required) {
            addViolation(result, CompatibilityRule.R10,
                    "电源额定 " + psu.getWattage() + "W 低于需求 " + (int) Math.ceil(required)
                            + "W（(CPU " + cpuTdp + "W + 显卡 " + gpuTdp + "W) × 1.4）");
        }
    }

    private PartCpu lookupCpu(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        PartCpu byId = tryGetById(cpuService, id, PartCpu.class);
        if (byId != null) {
            return byId;
        }
        return cpuService.lambdaQuery().eq(PartCpu::getOpendbId, id.trim()).one();
    }

    private PartMotherboard lookupMotherboard(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        PartMotherboard byId = tryGetById(motherboardService, id, PartMotherboard.class);
        if (byId != null) {
            return byId;
        }
        return motherboardService.lambdaQuery().eq(PartMotherboard::getOpendbId, id.trim()).one();
    }

    private PartRam lookupRam(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        PartRam byId = tryGetById(ramService, id, PartRam.class);
        if (byId != null) {
            return byId;
        }
        return ramService.lambdaQuery().eq(PartRam::getOpendbId, id.trim()).one();
    }

    private PartGpu lookupGpu(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        PartGpu byId = tryGetById(gpuService, id, PartGpu.class);
        if (byId != null) {
            return byId;
        }
        return gpuService.lambdaQuery().eq(PartGpu::getOpendbId, id.trim()).one();
    }

    private PartPsu lookupPsu(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        PartPsu byId = tryGetById(psuService, id, PartPsu.class);
        if (byId != null) {
            return byId;
        }
        return psuService.lambdaQuery().eq(PartPsu::getOpendbId, id.trim()).one();
    }

    private PartPcCase lookupCase(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        PartPcCase byId = tryGetById(caseService, id, PartPcCase.class);
        if (byId != null) {
            return byId;
        }
        return caseService.lambdaQuery().eq(PartPcCase::getOpendbId, id.trim()).one();
    }

    private PartCpuCooler lookupCooler(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        PartCpuCooler byId = tryGetById(coolerService, id, PartCpuCooler.class);
        if (byId != null) {
            return byId;
        }
        return coolerService.lambdaQuery().eq(PartCpuCooler::getOpendbId, id.trim()).one();
    }

    @SuppressWarnings("unchecked")
    private <T> T tryGetById(com.baomidou.mybatisplus.extension.service.IService<T> service, String id, Class<T> type) {
        try {
            long numeric = Long.parseLong(id.trim());
            return service.getById(numeric);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String normSocket(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        return s.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    static String normMem(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        return s.trim().toUpperCase();
    }

    static String normForm(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        return s.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    static String normVendor(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String u = s.trim().toUpperCase();
        if (u.contains("INTEL")) {
            return "INTEL";
        }
        if (u.contains("AMD")) {
            return "AMD";
        }
        if (u.contains("NVIDIA")) {
            return "NVIDIA";
        }
        if (u.contains("VIA")) {
            return "VIA";
        }
        return u;
    }

    static String chipsetVendor(String chipset) {
        if (!StringUtils.hasText(chipset)) {
            return null;
        }
        String u = chipset.trim().toUpperCase();
        if (u.contains("INTEL")) {
            return "INTEL";
        }
        if (u.contains("AMD")) {
            return "AMD";
        }
        if (u.contains("NVIDIA")) {
            return "NVIDIA";
        }
        if (u.contains("VIA")) {
            return "VIA";
        }
        return null;
    }

    Set<String> cpuMemoryTypes(String rawJson) {
        Set<String> types = new HashSet<>();
        if (!StringUtils.hasText(rawJson)) {
            return types;
        }
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode arr = root.path("specifications").path("memory").path("types");
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    String t = normMem(n.asText());
                    if (StringUtils.hasText(t)) {
                        types.add(t);
                    }
                }
            }
        } catch (Exception e) {
            return types;
        }
        return types;
    }

    Set<String> caseSupportedForms(String rawJson) {
        Set<String> forms = new HashSet<>();
        if (!StringUtils.hasText(rawJson)) {
            return forms;
        }
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode arr = root.path("supported_motherboard_form_factors");
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    String f = normForm(n.asText());
                    if (StringUtils.hasText(f)) {
                        forms.add(f);
                    }
                }
            }
        } catch (Exception e) {
            return forms;
        }
        return forms;
    }

    private void addViolation(CompatibilityResult result, CompatibilityRule rule, String message) {
        result.getViolations().add(new CompatibilityViolation(
                rule.getId(), rule.getDescription(), rule.getSeverity(), message));
    }

    private void addWarning(CompatibilityResult result, CompatibilityRule rule, String message) {
        result.getWarnings().add(new CompatibilityViolation(
                rule.getId(), rule.getDescription(), rule.getSeverity(), message));
    }
}
