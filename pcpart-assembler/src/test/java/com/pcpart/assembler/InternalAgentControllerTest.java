package com.pcpart.assembler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InternalAgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthOk() throws Exception {
        mockMvc.perform(get("/internal/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ok"))
                .andExpect(jsonPath("$.data.rules").value(10));
    }

    @Test
    void rulesListTenHard() throws Exception {
        MvcResult result = mockMvc.perform(get("/internal/compatibility/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertEquals(10, data.size());
        for (JsonNode r : data) {
            assertEquals("hard", r.get("severity").asText());
        }
    }

    @Test
    void checkEmptyBuildCompatible() throws Exception {
        mockMvc.perform(post("/internal/compatibility/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpuId\":null,\"motherboardId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.compatible").value(true));
    }

    @Test
    void checkRealSeededParts() throws Exception {
        MvcResult cpuPage = mockMvc.perform(post("/internal/parts/cpu/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"current\":1,\"size\":1}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode records = objectMapper.readTree(cpuPage.getResponse().getContentAsString())
                .get("data").get("records");
        assertTrue(records.size() >= 1);
        String cpuOpendb = records.get(0).get("opendb_id").asText();

        MvcResult mbPage = mockMvc.perform(post("/internal/parts/motherboard/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"current\":1,\"size\":1}"))
                .andExpect(status().isOk())
                .andReturn();
        String mbOpendb = objectMapper.readTree(mbPage.getResponse().getContentAsString())
                .get("data").get("records").get(0).get("opendb_id").asText();

        String body = objectMapper.createObjectNode()
                .put("cpuId", cpuOpendb)
                .put("motherboardId", mbOpendb)
                .toString();
        MvcResult checked = mockMvc.perform(post("/internal/compatibility/check")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode data = objectMapper.readTree(checked.getResponse().getContentAsString()).get("data");
        assertTrue(data.has("compatible"));
        assertTrue(data.has("violations"));
    }

    @Test
    void searchAllCategoriesPageOk() throws Exception {
        String[] categories = {"cpu", "cooler", "gpu", "motherboard", "ram", "psu", "storage", "case"};
        for (String c : categories) {
            mockMvc.perform(post("/internal/parts/" + c + "/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"current\":1,\"size\":2}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray());
        }
    }

    @Test
    void searchUnknownCategoryReturns500() throws Exception {
        mockMvc.perform(post("/internal/parts/unknown/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"current\":1,\"size\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void searchWithFiltersAndPrice() throws Exception {
        com.fasterxml.jackson.databind.node.ObjectNode body = objectMapper.createObjectNode();
        body.put("current", 1);
        body.put("size", 5);
        body.put("keyword", "Intel");
        body.set("filters", objectMapper.createObjectNode().put("socket", "LGA1700"));
        mockMvc.perform(post("/internal/parts/cpu/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/internal/parts/cpu/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"current\":1,\"size\":5,\"requirePriced\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void detailByOpendbId() throws Exception {
        MvcResult cpuPage = mockMvc.perform(post("/internal/parts/cpu/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"current\":1,\"size\":1}"))
                .andExpect(status().isOk())
                .andReturn();
        String opendb = objectMapper.readTree(cpuPage.getResponse().getContentAsString())
                .get("data").get("records").get(0).get("opendb_id").asText();

        mockMvc.perform(get("/internal/parts/cpu/" + opendb))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fields").exists())
                .andExpect(jsonPath("$.data.memoryTypes").isArray());
    }

    @Test
    void detailNotFound404() throws Exception {
        mockMvc.perform(get("/internal/parts/cpu/no-such-id-zzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void validateBuildConfigMissingId() throws Exception {
        mockMvc.perform(post("/internal/build-configs/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void saveBuildConfigRequiresName() throws Exception {
        mockMvc.perform(post("/internal/build-configs/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configName\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void saveBuildConfigRejectsIncompatible() throws Exception {
        MvcResult cpuPage = mockMvc.perform(post("/internal/parts/cpu/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"current\":1,\"size\":1}"))
                .andExpect(status().isOk())
                .andReturn();
        String cpuOpendb = objectMapper.readTree(cpuPage.getResponse().getContentAsString())
                .get("data").get("records").get(0).get("opendb_id").asText();

        MvcResult mbPage = mockMvc.perform(post("/internal/parts/motherboard/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"current\":1,\"size\":1}"))
                .andExpect(status().isOk())
                .andReturn();
        String mbOpendb = objectMapper.readTree(mbPage.getResponse().getContentAsString())
                .get("data").get("records").get(0).get("opendb_id").asText();

        String body = objectMapper.createObjectNode()
                .put("configName", "AGENT-TEST-")
                .put("cpuId", cpuOpendb)
                .put("motherboardId", mbOpendb + "-not-exist")
                .toString();
        mockMvc.perform(post("/internal/compatibility/check")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String bad = objectMapper.createObjectNode()
                .put("configName", "AGENT-TEST-BAD")
                .put("cpuId", cpuOpendb)
                .put("motherboardId", mbOpendb)
                .toString();
        MvcResult checked = mockMvc.perform(post("/internal/compatibility/check")
                        .contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isOk())
                .andReturn();
        boolean compatible = objectMapper.readTree(checked.getResponse().getContentAsString())
                .get("data").get("compatible").asBoolean();
        MvcResult saved = mockMvc.perform(post("/internal/build-configs/save")
                        .contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isOk())
                .andReturn();
        int code = objectMapper.readTree(saved.getResponse().getContentAsString()).get("code").asInt();
        assertEquals(compatible ? 200 : 400, code);
    }

    @Test
    void detailBuildConfigReturnsIds() throws Exception {
        String saveBody = "{\"configName\":\"AGENT-DETAIL-TEST\",\"cpuId\":\"cpu-1\",\"motherboardId\":\"mb-1\"}";
        MvcResult saved = mockMvc.perform(post("/internal/build-configs/save")
                        .contentType(MediaType.APPLICATION_JSON).content(saveBody))
                .andExpect(status().isOk())
                .andReturn();
        long configId = objectMapper.readTree(saved.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        mockMvc.perform(get("/internal/build-configs/" + configId + "/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(configId))
                .andExpect(jsonPath("$.data.configName").value("AGENT-DETAIL-TEST"))
                .andExpect(jsonPath("$.data.cpuId").value("cpu-1"))
                .andExpect(jsonPath("$.data.motherboardId").value("mb-1"));
    }

    @Test
    void detailBuildConfigNotFound() throws Exception {
        mockMvc.perform(get("/internal/build-configs/999999/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void detailBuildConfigInvalidId() throws Exception {
        mockMvc.perform(get("/internal/build-configs/0/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
