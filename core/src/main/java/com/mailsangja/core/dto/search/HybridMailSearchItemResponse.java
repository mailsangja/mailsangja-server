package com.mailsangja.core.dto.search;

import com.mailsangja.core.dto.inbox.MailAddressResponse;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Message;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "하이브리드 메일 검색 결과 항목")
public record HybridMailSearchItemResponse(
        @Schema(description = "메시지 ID")
        UUID messageId,
        @Schema(description = "스레드 ID")
        UUID threadId,
        @Schema(description = "메일 계정 ID")
        UUID mailAccountId,
        @Schema(description = "메시지 방향")
        Direction direction,
        @Schema(description = "제목")
        String subject,
        @Schema(description = "발신자")
        MailAddressResponse from,
        @Schema(description = "수신자 목록")
        List<MailAddressResponse> to,
        @Schema(description = "스니펫")
        String snippet,
        @Schema(description = "읽음 여부")
        boolean read,
        @Schema(description = "별표 여부")
        boolean star,
        @Schema(description = "발송/수신 시각")
        LocalDateTime sentAt,
        @Schema(description = "검색 매칭 방식")
        List<HybridMailSearchMatchType> matchedBy,
        @Schema(description = "RRF 기반 검색 점수")
        double score
) {
    public static HybridMailSearchItemResponse from(
            HybridMailSearchItemResult result,
            Map<String, String> contactNameByEmail
    ) {
        Message message = result.message();
        return new HybridMailSearchItemResponse(
                message.getId(),
                message.getThread().getId(),
                message.getThread().getMailAccount().getId(),
                message.getDirection(),
                message.getSubject(),
                MailAddressResponse.of(message.getFromName(), message.getFromAddress(), contactNameByEmail),
                toMailAddressResponses(message.getToAddresses(), message.getToNames(), contactNameByEmail),
                message.getSnippet(),
                message.isRead(),
                message.isStar(),
                message.getSentAt(),
                result.matchedBy(),
                result.score()
        );
    }

    private static List<MailAddressResponse> toMailAddressResponses(
            List<String> emails,
            List<String> names,
            Map<String, String> contactNameByEmail
    ) {
        if (emails == null || emails.isEmpty()) {
            return List.of();
        }
        return java.util.stream.IntStream.range(0, emails.size())
                .mapToObj(index -> MailAddressResponse.of(
                        resolveName(names, index),
                        emails.get(index),
                        contactNameByEmail
                ))
                .toList();
    }

    private static String resolveName(List<String> names, int index) {
        if (names == null || index >= names.size()) {
            return null;
        }
        return names.get(index);
    }
}
