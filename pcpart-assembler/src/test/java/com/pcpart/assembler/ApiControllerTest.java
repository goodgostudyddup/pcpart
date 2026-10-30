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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String uniq(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void indexPageOk() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void cpuApiCrud() throws Exception {
        String opendb = uniq("cpu-api");
        String name = "API CPU " + opendb;
        String body = "{\"opendbId\":\"" + opendb + "\",\"name\":\"" + name
                + "\",\"socket\":\"AM5\",\"coresTotal\":6,\"tdp\":65,\"rawJson\":\"{}\"}";

        MvcResult created = mockMvc.perform(post("/api/cpus")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode createdData = objectMapper.readTree(created.getResponse().getContentAsString()).get("data");
        long id = createdData.get("id").asLong();

        mockMvc.perform(get("/api/cpus/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(name));

        mockMvc.perform(get("/api/cpus/page")
                        .param("current", "1").param("size", "5").param("keyword", opendb))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(put("/api/cpus/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opendbId\":\"" + opendb + "\",\"name\":\"" + name
                                + "\",\"socket\":\"AM5\",\"coresTotal\":6,\"tdp\":105,\"rawJson\":\"{}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tdp").value(105));

        mockMvc.perform(delete("/api/cpus/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/cpus/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void buildConfigApiCrud() throws Exception {
        MvcResult cpuPage = mockMvc.perform(get("/api/cpus/page").param("current", "1").param("size", "1"))
                .andExpect(status().isOk()).andReturn();
        String cpuOpendb = objectMapper.readTree(cpuPage.getResponse().getContentAsString())
                .get("data").get("records").get(0).get("opendbId").asText();

        String cfgName = uniq("CFG");
        String body = objectMapper.createObjectNode()
                .put("configName", cfgName)
                .put("cpuId", cpuOpendb)
                .put("remark", "api test")
                .toString();

        MvcResult created = mockMvc.perform(post("/api/build-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(get("/api/build-configs/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configName").value(cfgName))
                .andExpect(jsonPath("$.data.cpuId").value(cpuOpendb))
                .andExpect(jsonPath("$.data.cpuName").isNotEmpty());

        mockMvc.perform(put("/api/build-configs/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("api test", "updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.remark").value("updated"));

        mockMvc.perform(delete("/api/build-configs/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void otherPartApisPageOk() throws Exception {
        String[] urls = {
                "/api/coolers/page", "/api/gpus/page", "/api/motherboards/page",
                "/api/cases/page", "/api/psus/page", "/api/rams/page",
                "/api/storages/page", "/api/build-configs/page"
        };
        for (String url : urls) {
            mockMvc.perform(get(url).param("current", "1").param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
        assertNotNull(urls);
    }
}
