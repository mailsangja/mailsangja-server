package com.mailsangja.core.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "하이브리드 메일 검색 응답")
public record HybridMailSearchResponse(
        @Schema(description = "검색 결과 목록")
        List<HybridMailSearchItemResponse> content
) {
    public HybridMailSearchResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public static HybridMailSearchResponse of(List<HybridMailSearchItemResponse> content) {
        return new HybridMailSearchResponse(content);
    }
}
