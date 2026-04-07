package com.mailsangja.core.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Slice;

import java.util.List;

@Schema(description = "무한 스크롤 슬라이스 응답 래퍼")
public record SliceResponse<T>(
        @Schema(description = "조회된 데이터 목록")
        List<T> content,
        @Schema(description = "현재 페이지 번호 (0-indexed)", example = "0")
        int page,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
    public static <T> SliceResponse<T> from(Slice<T> slice) {
        return new SliceResponse<>(slice.getContent(), slice.getNumber(), slice.hasNext());
    }
}
