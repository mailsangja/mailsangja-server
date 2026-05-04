package com.mailsangja.core.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "마커 기반 무한 스크롤 응답 래퍼")
public record MarkerSliceResponse<T>(
        @Schema(description = "조회된 데이터 목록")
        List<T> content,
        @Schema(description = "다음 요청에 사용할 마커 (마지막 스레드의 threadId), null이면 마지막 페이지", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID nextMarker,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,
        @Schema(description = "현재 목록 조건에서 읽지 않은 항목 수", example = "12")
        long unreadCount
) {
    public static <T> MarkerSliceResponse<T> of(List<T> content, UUID nextMarker, boolean hasNext) {
        return new MarkerSliceResponse<>(content, nextMarker, hasNext, 0L);
    }

    public static <T> MarkerSliceResponse<T> of(List<T> content, UUID nextMarker, boolean hasNext, long unreadCount) {
        return new MarkerSliceResponse<>(content, nextMarker, hasNext, unreadCount);
    }
}
