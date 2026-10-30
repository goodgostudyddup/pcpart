package com.pcpart.assembler.compatibility;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class CompatibilityResult {

    private boolean compatible;
    private List<CompatibilityViolation> violations = new ArrayList<>();
    private List<CompatibilityViolation> warnings = new ArrayList<>();
    /** 每条规则的执行结果：pass / violation / warning / skipped。 */
    private Map<String, String> ruleStatus = new LinkedHashMap<>();
    private int rulesTotal;
    private int rulesEvaluated;
    private int rulesSkipped;
    private String checkedAt;
    private boolean cached = false;

    public static CompatibilityResult ok() {
        return new CompatibilityResult();
    }
}
