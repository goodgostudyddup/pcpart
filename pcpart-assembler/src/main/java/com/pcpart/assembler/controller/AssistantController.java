package com.pcpart.assembler.controller;

import com.pcpart.assembler.client.DifyClient;
import com.pcpart.assembler.common.Result;
import com.pcpart.assembler.dto.DifyChatResult;
import com.pcpart.assembler.entity.BuildConfig;
import com.pcpart.assembler.service.AssistantService;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

/**
 * AI 装机助手入口。前端只与本控制器交互，由后端代理访问 Dify，
 * 避免把 Dify 的 app key 暴露在浏览器里。
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /** 同步问答。 */
    @PostMapping("/chat")
    public Result<DifyChatResult> chat(@RequestBody ChatRequest request) {
        return Result.ok(assistantService.chat(request.getQuery(), request.getConversationId()));
    }

    /** 流式问答，SSE 转发 Dify 的增量事件。 */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String query,
                             @RequestParam(required = false) String conversationId) {
        SseEmitter emitter = new SseEmitter(300_000L);
        DifyClient client = assistantService.getDifyClient();

        CompletableFuture.runAsync(() -> {
            try {
                client.chatStream(query, conversationId,
                        raw -> {
                            try {
                                emitter.send(SseEmitter.event().data(raw, MediaType.TEXT_PLAIN));
                            } catch (Exception ignored) {
                                // 前端已断开
                            }
                        },
                        done -> {
                            try {
                                emitter.send(SseEmitter.event().name("done")
                                        .data(done, MediaType.APPLICATION_JSON));
                                emitter.complete();
                            } catch (Exception ignored) {
                                // ignore
                            }
                        });
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data(e.getMessage() == null ? "AI 服务异常" : e.getMessage(),
                                    MediaType.TEXT_PLAIN));
                } catch (Exception ignored) {
                    // ignore
                }
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    /** 把 AI 生成的配置单落库（会自动跑 R1-R10 校验）。 */
    @PostMapping("/save-config")
    public Result<BuildConfig> saveConfig(@RequestBody SaveRequest request) {
        return Result.ok(assistantService.saveFromAnswer(request.getAnswer(), request.getConfigName()));
    }

    @Data
    public static class ChatRequest {
        private String query;
        private String conversationId;
    }

    @Data
    public static class SaveRequest {
        private String answer;
        private String configName;
    }
}
