package com.pcpart.assembler.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalPartDetail {

    private String idField;
    private Map<String, Object> fields;
    private List<String> memoryTypes;
    private List<String> supportedFormFactors;
}
