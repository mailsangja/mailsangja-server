package com.mailsangja.core.service.ai.label;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SnippetToolTest {

    @Test
    void 요청한ID가모두존재하면모두반환한다() {
        Map<String, String> snippetMap = Map.of(
                "msg-001", "스니펫1",
                "msg-002", "스니펫2"
        );
        SnippetTool tool = new SnippetTool(snippetMap);

        Map<String, String> result = tool.getEmailSnippets(List.of("msg-001", "msg-002"));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                "msg-001", "스니펫1",
                "msg-002", "스니펫2"
        ));
    }

    @Test
    void 존재하지않는ID는결과에서제외된다() {
        Map<String, String> snippetMap = Map.of("msg-001", "스니펫1");
        SnippetTool tool = new SnippetTool(snippetMap);

        Map<String, String> result = tool.getEmailSnippets(List.of("msg-001", "msg-999"));

        assertThat(result).containsOnlyKeys("msg-001");
        assertThat(result).doesNotContainKey("msg-999");
    }

    @Test
    void 모두존재하지않는ID요청시빈맵반환() {
        Map<String, String> snippetMap = Map.of("msg-001", "스니펫");
        SnippetTool tool = new SnippetTool(snippetMap);

        Map<String, String> result = tool.getEmailSnippets(List.of("msg-999", "msg-888"));

        assertThat(result).isEmpty();
    }

    @Test
    void 빈ID목록요청시빈맵반환() {
        SnippetTool tool = new SnippetTool(Map.of("msg-001", "스니펫"));

        Map<String, String> result = tool.getEmailSnippets(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void 빈snippetMap에서조회시빈맵반환() {
        SnippetTool tool = new SnippetTool(Map.of());

        Map<String, String> result = tool.getEmailSnippets(List.of("msg-001"));

        assertThat(result).isEmpty();
    }
}
