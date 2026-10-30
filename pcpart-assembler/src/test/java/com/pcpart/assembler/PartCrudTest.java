package com.pcpart.assembler;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pcpart.assembler.entity.BuildConfig;
import com.pcpart.assembler.entity.PartCpu;
import com.pcpart.assembler.entity.PartGpu;
import com.pcpart.assembler.entity.PartMotherboard;
import com.pcpart.assembler.entity.PartPcCase;
import com.pcpart.assembler.entity.PartPsu;
import com.pcpart.assembler.entity.PartRam;
import com.pcpart.assembler.entity.PartStorage;
import com.pcpart.assembler.service.BuildConfigService;
import com.pcpart.assembler.service.PartCpuService;
import com.pcpart.assembler.service.PartGpuService;
import com.pcpart.assembler.service.PartMotherboardService;
import com.pcpart.assembler.service.PartPcCaseService;
import com.pcpart.assembler.service.PartPsuService;
import com.pcpart.assembler.service.PartRamService;
import com.pcpart.assembler.service.PartStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PartCrudTest {

    @Autowired
    private PartCpuService cpuService;
    @Autowired
    private PartGpuService gpuService;
    @Autowired
    private PartMotherboardService motherboardService;
    @Autowired
    private PartRamService ramService;
    @Autowired
    private PartPsuService psuService;
    @Autowired
    private PartStorageService storageService;
    @Autowired
    private PartPcCaseService caseService;
    @Autowired
    private BuildConfigService buildConfigService;

    private String uniq(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void cpuCrudAndPage() {
        String opendb = uniq("cpu-test");
        PartCpu cpu = new PartCpu();
        cpu.setOpendbId(opendb);
        cpu.setName("TEST CPU " + opendb);
        cpu.setSocket("LGA1700");
        cpu.setCoresTotal(8);
        cpu.setThreads(16);
        cpu.setTdp(65);
        cpu.setRawJson("{\"test\":true}");
        cpu.setPriceUsd(new java.math.BigDecimal("449.00"));
        cpu.setPriceCny(new java.math.BigDecimal("3143.00"));
        assertTrue(cpuService.save(cpu));
        assertNotNull(cpu.getId());

        assertEquals(cpu.getName(), cpuService.getById(cpu.getId()).getName());
        assertEquals(0, new java.math.BigDecimal("449.00").compareTo(cpuService.getById(cpu.getId()).getPriceUsd()));

        cpu.setTdp(125);
        assertTrue(cpuService.updateById(cpu));
        assertEquals(125, cpuService.getById(cpu.getId()).getTdp());

        PartCpu probe = new PartCpu();
        probe.setSocket("LGA1700");
        assertTrue(cpuService.pageBy(1, 10, probe, null).getTotal() >= 1);
        assertTrue(cpuService.pageBy(1, 10, null, opendb).getTotal() >= 1);

        assertTrue(cpuService.removeById(cpu.getId()));
        assertNull(cpuService.getById(cpu.getId()));
    }

    @Test
    void gpuRamMotherboardCrud() {
        String opendb = uniq("gpu-test");
        PartGpu gpu = new PartGpu();
        gpu.setOpendbId(opendb);
        gpu.setName("TEST GPU " + opendb);
        gpu.setChipset("GeForce RTX TEST");
        gpu.setMemoryGb(16);
        gpu.setRawJson("{}");
        assertTrue(gpuService.save(gpu));
        assertTrue(gpuService.pageBy(1, 10, null, "RTX TEST").getTotal() >= 1);

        String ramDb = uniq("ram-test");
        PartRam ram = new PartRam();
        ram.setOpendbId(ramDb);
        ram.setName("TEST RAM " + ramDb);
        ram.setRamType("DDR5");
        ram.setSpeedMt(6400);
        ram.setCapacityGb(32);
        ram.setRawJson("{}");
        assertTrue(ramService.save(ram));

        String mbDb = uniq("mb-test");
        PartMotherboard mb = new PartMotherboard();
        mb.setOpendbId(mbDb);
        mb.setName("TEST MB " + mbDb);
        mb.setSocket("AM5");
        mb.setRawJson("{}");
        assertTrue(motherboardService.save(mb));
        PartMotherboard probe = new PartMotherboard();
        probe.setSocket("AM5");
        assertTrue(motherboardService.pageBy(1, 5, probe, null).getTotal() >= 1);

        assertTrue(gpuService.removeById(gpu.getId()));
        assertTrue(ramService.removeById(ram.getId()));
        assertTrue(motherboardService.removeById(mb.getId()));
    }

    @Test
    void psuStorageCaseCrud() {
        String psuDb = uniq("psu-test");
        PartPsu psu = new PartPsu();
        psu.setOpendbId(psuDb);
        psu.setName("TEST PSU " + psuDb);
        psu.setWattage(750);
        psu.setRawJson("{}");
        assertTrue(psuService.save(psu));
        PartPsu probe = new PartPsu();
        probe.setWattage(750);
        assertTrue(psuService.pageBy(1, 10, probe, null).getTotal() >= 1);

        String stDb = uniq("st-test");
        PartStorage st = new PartStorage();
        st.setOpendbId(stDb);
        st.setName("TEST SSD " + stDb);
        st.setStorageType("SSD");
        st.setCapacityGb(1000);
        st.setRawJson("{}");
        assertTrue(storageService.save(st));

        String caseDb = uniq("case-test");
        PartPcCase pcCase = new PartPcCase();
        pcCase.setOpendbId(caseDb);
        pcCase.setName("TEST CASE " + caseDb);
        pcCase.setFormFactor("ATX Mid Tower");
        pcCase.setRawJson("{}");
        assertTrue(caseService.save(pcCase));

        assertTrue(psuService.removeById(psu.getId()));
        assertTrue(storageService.removeById(st.getId()));
        assertTrue(caseService.removeById(pcCase.getId()));
    }

    @Test
    void buildConfigCrud() {
        PartCpu firstCpu = cpuService.pageBy(1, 1, null, null).getRecords().get(0);
        PartGpu firstGpu = gpuService.pageBy(1, 1, null, null).getRecords().get(0);

        BuildConfig cfg = new BuildConfig();
        cfg.setConfigName("TEST-CFG-" + UUID.randomUUID().toString().substring(0, 8));
        cfg.setCpuId(firstCpu.getOpendbId());
        cfg.setGpuId(firstGpu.getOpendbId());
        cfg.setRemark("test");
        BuildConfig saved = buildConfigService.saveChecked(cfg);
        assertNotNull(saved.getId());

        assertEquals(cfg.getConfigName(), buildConfigService.getById(saved.getId()).getConfigName());
        assertTrue(buildConfigService.pageBy(1, 10, "TEST-CFG", null).getTotal() >= 1);

        saved.setRemark("updated");
        buildConfigService.updateChecked(saved);
        assertEquals("updated", buildConfigService.getById(saved.getId()).getRemark());

        assertThrows(IllegalArgumentException.class, () -> buildConfigService.saveChecked(new BuildConfig()));
        BuildConfig noId = new BuildConfig();
        noId.setConfigName("x");
        assertThrows(IllegalArgumentException.class, () -> buildConfigService.updateChecked(noId));

        assertTrue(buildConfigService.removeById(saved.getId()));
    }

    @Test
    void seededDataPresent() {
        assertTrue(cpuService.pageBy(1, 5, null, null).getTotal() >= 100);
        assertTrue(gpuService.pageBy(1, 5, null, null).getTotal() >= 100);
        assertTrue(ramService.pageBy(1, 5, null, null).getTotal() >= 100);
        IPage<PartCpu> kw = cpuService.pageBy(1, 5, null, "AMD");
        assertTrue(kw.getTotal() >= 1);
        PartCpu priced = cpuService.lambdaQuery().isNotNull(PartCpu::getPriceUsd).last("LIMIT 1").one();
        assertNotNull(priced);
        assertNotNull(priced.getPriceCny());
        IPage<PartCpu> firstPage = cpuService.pageBy(1, 5, null, null);
        assertNotNull(firstPage.getRecords().get(0).getPriceUsd());
    }
}
