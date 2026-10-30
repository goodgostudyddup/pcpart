package com.pcpart.assembler;

import com.pcpart.assembler.compatibility.CompatibilityBuild;
import com.pcpart.assembler.compatibility.CompatibilityResult;
import com.pcpart.assembler.compatibility.CompatibilityRule;
import com.pcpart.assembler.compatibility.CompatibilityService;
import com.pcpart.assembler.entity.PartCpu;
import com.pcpart.assembler.entity.PartCpuCooler;
import com.pcpart.assembler.entity.PartGpu;
import com.pcpart.assembler.entity.PartMotherboard;
import com.pcpart.assembler.entity.PartPcCase;
import com.pcpart.assembler.entity.PartPsu;
import com.pcpart.assembler.entity.PartRam;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CompatibilityServiceTest {

    @Autowired
    private CompatibilityService compatibilityService;

    @Autowired
    private com.pcpart.assembler.service.PartCpuService cpuService;

    @Autowired
    private com.pcpart.assembler.service.PartMotherboardService motherboardService;

    @Autowired
    private com.pcpart.assembler.service.PartRamService ramService;

    @Autowired
    private com.pcpart.assembler.service.PartGpuService gpuService;

    @Autowired
    private com.pcpart.assembler.service.PartPsuService psuService;

    @Autowired
    private com.pcpart.assembler.service.PartPcCaseService caseService;

    @Autowired
    private com.pcpart.assembler.service.PartCpuCoolerService coolerService;

    private String uniq(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void seedCompatibleSet(Holder h) {
        PartCpu cpu = new PartCpu();
        cpu.setOpendbId(uniq("cpu-r"));
        cpu.setName("R CPU " + cpu.getOpendbId());
        cpu.setManufacturer("Intel");
        cpu.setSocket("LGA1700");
        cpu.setTdp(65);
        cpu.setRawJson("{\"specifications\":{\"memory\":{\"types\":[\"DDR5\"]}}}");
        cpuService.save(cpu);

        PartMotherboard mb = new PartMotherboard();
        mb.setOpendbId(uniq("mb-r"));
        mb.setName("R MB " + mb.getOpendbId());
        mb.setSocket("LGA1700");
        mb.setChipset("Intel B760");
        mb.setFormFactor("ATX");
        mb.setMemoryType("DDR5");
        mb.setMemorySlots(4);
        mb.setMemoryMax(128);
        mb.setRawJson("{\"socket\":\"LGA1700\"}");
        motherboardService.save(mb);

        PartRam ram = new PartRam();
        ram.setOpendbId(uniq("ram-r"));
        ram.setName("R RAM " + ram.getOpendbId());
        ram.setRamType("DDR5");
        ram.setModulesQty(2);
        ram.setCapacityGb(32);
        ram.setRawJson("{}");
        ramService.save(ram);

        PartGpu gpu = new PartGpu();
        gpu.setOpendbId(uniq("gpu-r"));
        gpu.setName("R GPU " + gpu.getOpendbId());
        gpu.setTdp(200);
        gpu.setLengthMm(new BigDecimal("300"));
        gpu.setRawJson("{}");
        gpuService.save(gpu);

        PartPsu psu = new PartPsu();
        psu.setOpendbId(uniq("psu-r"));
        psu.setName("R PSU " + psu.getOpendbId());
        psu.setWattage(750);
        psu.setRawJson("{}");
        psuService.save(psu);

        PartPcCase pcCase = new PartPcCase();
        pcCase.setOpendbId(uniq("case-r"));
        pcCase.setName("R CASE " + pcCase.getOpendbId());
        pcCase.setMaxGpuLength(400);
        pcCase.setMaxCoolerHeight(new BigDecimal("170"));
        pcCase.setRawJson("{\"supported_motherboard_form_factors\":[\"ATX\",\"Micro ATX\",\"Mini-ITX\"]}");
        caseService.save(pcCase);

        PartCpuCooler cooler = new PartCpuCooler();
        cooler.setOpendbId(uniq("cooler-r"));
        cooler.setName("R COOLER " + cooler.getOpendbId());
        cooler.setHeight(new BigDecimal("150"));
        cooler.setRawJson("{}");
        coolerService.save(cooler);

        h.cpu = cpu.getOpendbId();
        h.mb = mb.getOpendbId();
        h.ram = ram.getOpendbId();
        h.gpu = gpu.getOpendbId();
        h.psu = psu.getOpendbId();
        h.pcCase = pcCase.getOpendbId();
        h.cooler = cooler.getOpendbId();
    }

    private CompatibilityBuild fullBuild(Holder h) {
        CompatibilityBuild b = new CompatibilityBuild();
        b.setCpuId(h.cpu);
        b.setMotherboardId(h.mb);
        b.setRamId(h.ram);
        b.setGpuId(h.gpu);
        b.setPsuId(h.psu);
        b.setCaseId(h.pcCase);
        b.setCoolerId(h.cooler);
        return b;
    }

    private static class Holder {
        String cpu;
        String mb;
        String ram;
        String gpu;
        String psu;
        String pcCase;
        String cooler;
    }

    @Test
    void rulesCountIsTenAndAllHard() {
        assertEquals(10, CompatibilityRule.values().length);
        for (CompatibilityRule r : CompatibilityRule.values()) {
            assertEquals("hard", r.getSeverity());
        }
    }

    @Test
    void compatibleBuildPasses() {
        Holder h = new Holder();
        seedCompatibleSet(h);
        CompatibilityResult result = compatibilityService.check(fullBuild(h));
        assertTrue(result.isCompatible(), "期望兼容，实际: " + result.getViolations());
        assertTrue(result.getViolations().isEmpty());
    }

    @Test
    void r1SocketMismatch() {
        Holder h = new Holder();
        seedCompatibleSet(h);
        PartMotherboard mb = motherboardService.lambdaQuery()
                .eq(PartMotherboard::getOpendbId, h.mb).one();
        mb.setSocket("AM5");
        motherboardService.updateById(mb);
        CompatibilityResult result = compatibilityService.check(fullBuild(h));
        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.getRuleId().equals("R1")));
    }

    @Test
    void r2ChipsetVendorMismatch() {
        Holder h = new Holder();
        seedCompatibleSet(h);
        PartMotherboard mb = motherboardService.lambdaQuery()
                .eq(PartMotherboard::getOpendbId, h.mb).one();
        mb.setChipset("AMD B650");
        motherboardService.updateById(mb);
        CompatibilityResult result = compatibilityService.check(fullBuild(h));
        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.getRuleId().equals("R2")));
    }

    @Test
    void r3CpuMemoryTypeMismatch() {
        Holder h = new Holder();
        seedCompatibleSet(h);
        PartMotherboard mb = motherboardService.lambdaQuery()
                .eq(PartMotherboard::getOpendbId, h.mb).one();
        mb.setMemoryType("DDR4");
        motherboardService.updateById(mb);
        PartRam ram = ramService.lambdaQuery().eq(PartRam::getOpendbId, h.ram).one();
        ram.setRamType("DDR4");
        ramService.updateById(ram);
        CompatibilityResult result = compatibilityService.check(fullBuild(h));
        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.getRuleId().equals("R3")));
    }

    @Test
    void r4RamTypeMismatch() {
        Holder h = new Holder();
        seedCompatibleSet(h);
        PartRam ram = ramService.lambdaQuery().eq(PartRam::getOpendbId, h.ram).one();
        ram.setRamType("DDR4");
        ramService.updateById(ram);
        CompatibilityResult result = compatibilityService.check(fullBuild(h));
        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.getRuleId().equals("R4")));
    }

    @Test
    void r5ModulesExceedSlots() {
        Holder h = new Holder();
        seedCompatibleSet(h);
        PartRam ram = ramService.lambdaQuery().eq(PartRam::getOpendbId, h.ram).one();
        ram.setModulesQty(8);
        ramService.updateById(ram);
        CompatibilityResult result = compatibilityService.check(fullBuild(h));
        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.getRuleId().equals("R5")));
    }

    @Test
    void r6CapacityExceedsMax() {
        Holder h = new Holder();
        seedCompatibleSet(h);
        CompatibilityBuild b = fullBuild(h);
        b.setRamQty(8L);
        CompatibilityResult result = compatibilityService.check(b);
        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.getRuleId().equals("R6")));
    }

    @Test
    void r7GpuTooLong() {
        Holder h = new Holder();
        seedCompatibleSet(h);
        PartPcCase pcCase = caseService.lambdaQuery().eq(PartPcCase::getOpendbId, h.pcCase).one();
        pcCase.setMaxGpuLength(200);
        caseService.updateById(pcCase);
        CompatibilityResult result = compatibilityService.check(fullBuild(h));
        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.getRuleId().equals("R7")));
    }

    @Test
    void r8CoolerTooTall() {
        Holder h = new Holder();
        seedCompatibleSet(h);
        PartCpuCooler cooler = coolerService.lambdaQuery().eq(PartCpuCooler::getOpendbId, h.cooler).one();
        cooler.setHeight(new BigDecimal("200"));
        coolerService.updateById(cooler);
        CompatibilityResult result = compatibilityService.check(fullBuild(h));
        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.getRuleId().equals("R8")));
    }

    @Test
    void r9FormFactorNotSupported() {
        Holder h = new Holder();
        seedCompatibleSet(h);
        PartMotherboard mb = motherboardService.lambdaQuery()
                .eq(PartMotherboard::getOpendbId, h.mb).one();
        mb.setFormFactor("EATX");
        motherboardService.updateById(mb);
        CompatibilityResult result = compatibilityService.check(fullBuild(h));
        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.getRuleId().equals("R9")));
    }

    @Test
    void r10PsuInsufficient() {
        Holder h = new Holder();
        seedCompatibleSet(h);
        PartPsu psu = psuService.lambdaQuery().eq(PartPsu::getOpendbId, h.psu).one();
        psu.setWattage(300);
        psuService.updateById(psu);
        CompatibilityResult result = compatibilityService.check(fullBuild(h));
        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.getRuleId().equals("R10")));
    }

    @Test
    void partialBuildSkipsMissingParts() {
        CompatibilityBuild b = new CompatibilityBuild();
        CompatibilityResult result = compatibilityService.check(b);
        assertTrue(result.isCompatible());
        b.setCpuId("不存在的id-xxx");
        b.setMotherboardId("不存在的id-yyy");
        CompatibilityResult result2 = compatibilityService.check(b);
        assertTrue(result2.isCompatible());
        assertNotNull(result2.getWarnings());
    }

    @Test
    void missingDataProducesWarningsNotViolations() {
        CompatibilityBuild b = new CompatibilityBuild();
        CompatibilityResult result = compatibilityService.check(b);
        assertTrue(result.isCompatible());
        assertTrue(result.getViolations().isEmpty());
        assertNotNull(result.getWarnings());
    }

    @Test
    void ruleIdsCovered() {
        Holder h = new Holder();
        seedCompatibleSet(h);
        CompatibilityBuild bad = fullBuild(h);
        PartMotherboard mb = motherboardService.lambdaQuery()
                .eq(PartMotherboard::getOpendbId, h.mb).one();
        mb.setSocket("AM5");
        motherboardService.updateById(mb);
        CompatibilityResult result = compatibilityService.check(bad);
        assertEquals(
                java.util.Set.of("R1"),
                result.getViolations().stream().map(v -> v.getRuleId()).collect(Collectors.toSet()));
    }
}
