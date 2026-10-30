package com.pcpart.assembler.compatibility;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CompatibilityRule {

    R1("R1", "CPU.socket 必须等于 Motherboard.socket", "hard"),
    R2("R2", "Motherboard.chipset 必须支持 CPU", "hard"),
    R3("R3", "CPU 支持的内存类型必须包含 Motherboard.memory_type", "hard"),
    R4("R4", "Memory.memory_type 必须等于 Motherboard.memory_type", "hard"),
    R5("R5", "Memory.modules 不得超过 Motherboard.memory_slots", "hard"),
    R6("R6", "内存总容量不得超过 Motherboard.max_memory", "hard"),
    R7("R7", "VideoCard.length 不得超过 Case.max_gpu_length", "hard"),
    R8("R8", "Cooler.height 不得超过 Case.max_cooler_height", "hard"),
    R9("R9", "Motherboard.form_factor 必须在 Case 支持的板型范围内", "hard"),
    R10("R10", "PSU.wattage 不得低于 (CPU.tdp + VideoCard.tdp) * 1.4", "hard");

    private final String id;
    private final String description;
    private final String severity;

    public CompatibilityRuleInfo toInfo() {
        return new CompatibilityRuleInfo(id, description, severity);
    }
}
