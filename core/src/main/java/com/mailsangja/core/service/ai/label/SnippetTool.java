package com.mailsangja.core.service.ai.label;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class SnippetTool {

    private final Map<String, String> snippetMap;

    public SnippetTool(Map<String, String> snippetMap) {
        this.snippetMap = snippetMap;
    }

    @Tool(description = "Retrieve preprocessed email snippet text for the given message IDs. "
            + "Call this ONLY when subject and fromAddress/fromName are insufficient to determine a meaningful pattern.")
    public Map<String, String> getEmailSnippets(List<String> messageIds) {
        if (messageIds == null) {
            return Map.of();
        }
        Map<String, String> result = messageIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .filter(snippetMap::containsKey)
                .collect(Collectors.toMap(id -> id, snippetMap::get));
        log.info("SnippetTool invoked by LLM: requested={}, returned={}", messageIds.size(), result.size());
        return result;
    }
}
