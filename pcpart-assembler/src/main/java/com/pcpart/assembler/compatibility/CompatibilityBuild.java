package com.pcpart.assembler.compatibility;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompatibilityBuild {

    private String cpuId;
    private String coolerId;
    private String motherboardId;
    private String ramId;
    private Long ramQty;
    private String gpuId;
    private String psuId;
    private String storageId;
    private String caseId;
}
