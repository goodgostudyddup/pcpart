package com.pcpart.assembler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcpart.assembler.client.DifyClient;
import com.pcpart.assembler.dto.DifyChatResult;
import com.pcpart.assembler.entity.BuildConfig;
import com.pcpart.assembler.entity.PartCpu;
import com.pcpart.assembler.entity.PartCpuCooler;
import com.pcpart.assembler.entity.PartGpu;
import com.pcpart.assembler.entity.PartMotherboard;
import com.pcpart.assembler.entity.PartPcCase;
import com.pcpart.assembler.entity.PartPsu;
import com.pcpart.assembler.entity.PartRam;
import com.pcpart.assembler.entity.PartStorage;
import com.pcpart.assembler.mapper.PartCpuCoolerMapper;
import com.pcpart.assembler.mapper.PartCpuMapper;
import com.pcpart.assembler.mapper.PartGpuMapper;
import com.pcpart.assembler.mapper.PartMotherboardMapper;
import com.pcpart.assembler.mapper.PartPcCaseMapper;
import com.pcpart.assembler.mapper.PartPsuMapper;
import com.pcpart.assembler.mapper.PartRamMapper;
import com.pcpart.assembler.mapper.PartStorageMapper;
import com.pcpart.assembler.util.BuildSheetParser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Agent 与本地配件库之间的粘合层：
 * 1) 把用户问题转给 Dify 上的装机工作流；
 * 2) 把回答里的配置表格解析成 8 个型号，回查本地 opendb_id；
 * 3) 交给 BuildConfigService 做 R1-R10 校验后落库。
 */
@Service
public class AssistantService {

    private final DifyClient difyClient;
    private final BuildConfigService buildConfigService;
    private final PartCpuMapper cpuMapper;
    private final PartCpuCoolerMapper coolerMapper;
    private final PartGpuMapper gpuMapper;
    private final PartMotherboardMapper motherboardMapper;
    private final PartRamMapper ramMapper;
    private final PartPsuMapper psuMapper;
    private final PartStorageMapper storageMapper;
    private final PartPcCaseMapper caseMapper;

    public AssistantService(DifyClient difyClient,
                            BuildConfigService buildConfigService,
                            PartCpuMapper cpuMapper,
                            PartCpuCoolerMapper coolerMapper,
                            PartGpuMapper gpuMapper,
                            PartMotherboardMapper motherboardMapper,
                            PartRamMapper ramMapper,
                            PartPsuMapper psuMapper,
                            PartStorageMapper storageMapper,
                            PartPcCaseMapper caseMapper) {
        this.difyClient = difyClient;
        this.buildConfigService = buildConfigService;
        this.cpuMapper = cpuMapper;
        this.coolerMapper = coolerMapper;
        this.gpuMapper = gpuMapper;
        this.motherboardMapper = motherboardMapper;
        this.ramMapper = ramMapper;
        this.psuMapper = psuMapper;
        this.storageMapper = storageMapper;
        this.caseMapper = caseMapper;
    }

    public DifyChatResult chat(String query, String conversationId) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("问题不能为空");
        }
        return difyClient.chat(query, conversationId);
    }

    public DifyClient getDifyClient() {
        return difyClient;
    }

    /** 把 AI 回答解析成配置单并保存到本地库（会自动跑 R1-R10 校验）。 */
    public BuildConfig saveFromAnswer(String answer, String configName) {
        Map<String, String> picked = BuildSheetParser.parse(answer);
        if (picked.isEmpty()) {
            throw new IllegalArgumentException("回答里没有识别到配置表格，无法保存");
        }

        BuildConfig cfg = new BuildConfig();
        cfg.setConfigName(StringUtils.hasText(configName) ? configName : ("AI 配置单（" + picked.size() + " 类部件）"));
        cfg.setCpuId(resolveCpu(picked.get("cpu")));
        cfg.setCoolerId(resolveCooler(picked.get("cooler")));
        cfg.setMotherboardId(resolveMotherboard(picked.get("mb")));
        cfg.setRamId(resolveRam(picked.get("ram")));
        cfg.setGpuId(resolveGpu(picked.get("gpu")));
        cfg.setPsuId(resolvePsu(picked.get("psu")));
        cfg.setStorageId(resolveStorage(picked.get("ssd")));
        cfg.setCaseId(resolveCase(picked.get("case")));
        cfg.setRemark("由 AI 助手生成；型号：" + String.join(" / ", picked.values()));
        return buildConfigService.saveChecked(cfg);
    }

    private String clean(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String s = name.trim();
        if (s.contains("缺失") || "—".equals(s) || "-".equals(s)) {
            return null;
        }
        return s.length() > 60 ? s.substring(0, 60) : s;
    }

    private String resolveCpu(String name) {
        String n = clean(name);
        if (n == null) {
            return null;
        }
        PartCpu row = cpuMapper.selectOne(new LambdaQueryWrapper<PartCpu>()
                .eq(PartCpu::getName, n).last("LIMIT 1"));
        if (row == null) {
            row = cpuMapper.selectOne(new LambdaQueryWrapper<PartCpu>()
                    .like(PartCpu::getName, n).last("LIMIT 1"));
        }
        return row == null ? null : row.getOpendbId();
    }

    private String resolveCooler(String name) {
        String n = clean(name);
        if (n == null) {
            return null;
        }
        PartCpuCooler row = coolerMapper.selectOne(new LambdaQueryWrapper<PartCpuCooler>()
                .eq(PartCpuCooler::getName, n).last("LIMIT 1"));
        if (row == null) {
            row = coolerMapper.selectOne(new LambdaQueryWrapper<PartCpuCooler>()
                    .like(PartCpuCooler::getName, n).last("LIMIT 1"));
        }
        return row == null ? null : row.getOpendbId();
    }

    private String resolveGpu(String name) {
        String n = clean(name);
        if (n == null) {
            return null;
        }
        PartGpu row = gpuMapper.selectOne(new LambdaQueryWrapper<PartGpu>()
                .eq(PartGpu::getName, n).last("LIMIT 1"));
        if (row == null) {
            row = gpuMapper.selectOne(new LambdaQueryWrapper<PartGpu>()
                    .like(PartGpu::getName, n).last("LIMIT 1"));
        }
        return row == null ? null : row.getOpendbId();
    }

    private String resolveMotherboard(String name) {
        String n = clean(name);
        if (n == null) {
            return null;
        }
        PartMotherboard row = motherboardMapper.selectOne(new LambdaQueryWrapper<PartMotherboard>()
                .eq(PartMotherboard::getName, n).last("LIMIT 1"));
        if (row == null) {
            row = motherboardMapper.selectOne(new LambdaQueryWrapper<PartMotherboard>()
                    .like(PartMotherboard::getName, n).last("LIMIT 1"));
        }
        return row == null ? null : row.getOpendbId();
    }

    private String resolveRam(String name) {
        String n = clean(name);
        if (n == null) {
            return null;
        }
        PartRam row = ramMapper.selectOne(new LambdaQueryWrapper<PartRam>()
                .eq(PartRam::getName, n).last("LIMIT 1"));
        if (row == null) {
            row = ramMapper.selectOne(new LambdaQueryWrapper<PartRam>()
                    .like(PartRam::getName, n).last("LIMIT 1"));
        }
        return row == null ? null : row.getOpendbId();
    }

    private String resolvePsu(String name) {
        String n = clean(name);
        if (n == null) {
            return null;
        }
        PartPsu row = psuMapper.selectOne(new LambdaQueryWrapper<PartPsu>()
                .eq(PartPsu::getName, n).last("LIMIT 1"));
        if (row == null) {
            row = psuMapper.selectOne(new LambdaQueryWrapper<PartPsu>()
                    .like(PartPsu::getName, n).last("LIMIT 1"));
        }
        return row == null ? null : row.getOpendbId();
    }

    private String resolveStorage(String name) {
        String n = clean(name);
        if (n == null) {
            return null;
        }
        PartStorage row = storageMapper.selectOne(new LambdaQueryWrapper<PartStorage>()
                .eq(PartStorage::getName, n).last("LIMIT 1"));
        if (row == null) {
            row = storageMapper.selectOne(new LambdaQueryWrapper<PartStorage>()
                    .like(PartStorage::getName, n).last("LIMIT 1"));
        }
        return row == null ? null : row.getOpendbId();
    }

    private String resolveCase(String name) {
        String n = clean(name);
        if (n == null) {
            return null;
        }
        PartPcCase row = caseMapper.selectOne(new LambdaQueryWrapper<PartPcCase>()
                .eq(PartPcCase::getName, n).last("LIMIT 1"));
        if (row == null) {
            row = caseMapper.selectOne(new LambdaQueryWrapper<PartPcCase>()
                    .like(PartPcCase::getName, n).last("LIMIT 1"));
        }
        return row == null ? null : row.getOpendbId();
    }
}
