package com.pcpart.assembler.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcpart.assembler.config.DifyProperties;
import com.pcpart.assembler.dto.DifyChatResult;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Dify Service API 客户端。
 * 对应接口：POST /v1/chat-messages（blocking / streaming 两种模式）。
 */
@Component
public class DifyClient {

    private final DifyProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public DifyClient(DifyProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeout());
        factory.setReadTimeout(props.getReadTimeout());
        this.restTemplate = new RestTemplate(factory);
    }

    /** 同步问答。 */
    public DifyChatResult chat(String query, String conversationId) {
        String json = post(payload(query, conversationId, "blocking"));
        try {
            JsonNode node = mapper.readTree(json);
            return new DifyChatResult(
                    node.path("answer").asText(""),
                    node.path("conversation_id").asText(""),
                    node.path("message_id").asText(""));
        } catch (Exception e) {
            throw new IllegalStateException("解析 Dify 响应失败：" + e.getMessage(), e);
        }
    }

    /**
     * 流式问答。
     *
     * @param onEvent 每收到一条 SSE 事件回调一次（原始 JSON 字符串）
     * @param onDone  结束回调，带完整 answer 与 conversationId
     */
    public void chatStream(String query, String conversationId,
                           Consumer<String> onEvent, Consumer<DifyChatResult> onDone) {
        byte[] bytes;
        try {
            bytes = mapper.writeValueAsBytes(payload(query, conversationId, "streaming"));
        } catch (Exception e) {
            throw new IllegalStateException("序列化请求失败", e);
        }

        restTemplate.execute(endpoint(), HttpMethod.POST, request -> {
            request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            request.getHeaders().setBearerAuth(props.getApiKey());
            request.getHeaders().setAccept(List.of(MediaType.TEXT_EVENT_STREAM));
            request.getBody().write(bytes);
        }, response -> {
            String convId = conversationId == null ? "" : conversationId;
            StringBuilder answer = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String raw = line.substring(5).trim();
                    if (raw.isEmpty() || "[DONE]".equals(raw)) {
                        continue;
                    }
                    JsonNode node = mapper.readTree(raw);
                    String event = node.path("event").asText();
                    if (node.hasNonNull("conversation_id")) {
                        convId = node.get("conversation_id").asText();
                    }
                    if ("message".equals(event)) {
                        String chunk = node.path("answer").asText("");
                        if (!chunk.isEmpty()) {
                            answer.setLength(0);
                            answer.append(chunk);
                        }
                        onEvent.accept(raw);
                    } else if ("message_end".equals(event) || "workflow_finished".equals(event)) {
                        onEvent.accept(raw);
                    } else if ("error".equals(event)) {
                        throw new IllegalStateException(node.path("message").asText("Dify 流式调用失败"));
                    }
                }
            }
            onDone.accept(new DifyChatResult(answer.toString(), convId, ""));
            return null;
        });
    }

    private Map<String, Object> payload(String query, String conversationId, String mode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", Map.of());
        body.put("query", query);
        body.put("response_mode", mode);
        body.put("conversation_id", conversationId == null ? "" : conversationId);
        body.put("user", props.getUser());
        return body;
    }

    private String post(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(props.getApiKey());
        return restTemplate.postForObject(endpoint(), new HttpEntity<>(body, headers), String.class);
    }

    /** base-url 允许带不带 /v1，这里统一补齐。 */
    private String endpoint() {
        String base = props.getBaseUrl() == null ? "" : props.getBaseUrl().trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!base.endsWith("/v1")) {
            base = base + "/v1";
        }
        return base + "/chat-messages";
    }
}
