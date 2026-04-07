package com.mailsangja.core.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "페이지 응답 래퍼")
public record PageResponse<T>(
        @Schema(description = "조회된 데이터 목록")
        List<T> content,
        @Schema(description = "현재 페이지 번호 (0-indexed)", example = "0")
        int page,
        @Schema(description = "페이지 크기", example = "20")
        int size,
        @Schema(description = "전체 요소 수", example = "100")
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "5")
        int totalPages,
        @Schema(description = "마지막 페이지 여부", example = "false")
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
