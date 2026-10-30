package com.pcpart.assembler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dify 一次问答的返回结果。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DifyChatResult {

    /** 模型生成的完整回答（Markdown） */
    private String answer;

    /** 会话 id，多轮会话时回传给 Dify */
    private String conversationId;

    /** 本次消息 id */
    private String messageId;
}
