package com.pcpart.assembler.compatibility;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompatibilityViolation {

    private String ruleId;
    private String description;
    private String severity;
    private String message;
}
