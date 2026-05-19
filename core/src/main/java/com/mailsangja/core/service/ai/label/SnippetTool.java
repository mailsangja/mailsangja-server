package com.mailsangja.core.service.ai.label;

import org.springframework.ai.tool.annotation.Tool;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SnippetTool {

    private final Map<String, String> snippetMap;

    public SnippetTool(Map<String, String> snippetMap) {
        this.snippetMap = snippetMap;
    }

    @Tool(description = "Retrieve preprocessed email snippet text for the given message IDs. "
            + "Call this ONLY when subject and fromAddress/fromName are insufficient to determine a meaningful pattern.")
    public Map<String, String> getEmailSnippets(List<String> messageIds) {
        return messageIds.stream()
                .filter(snippetMap::containsKey)
                .collect(Collectors.toMap(id -> id, snippetMap::get));
    }
}
