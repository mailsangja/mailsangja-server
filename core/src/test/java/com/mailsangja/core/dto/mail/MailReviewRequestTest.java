package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailReviewException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MailReviewRequestTest {

    @Test
    void attachmentNames의빈값은무시한다() {
        // when
        MailReviewRequest request = new MailReviewRequest(
                "자료 전달드립니다",
                "요청하신 자료를 첨부드립니다.",
                1,
                List.of("", " 자료.pdf ", " ")
        );

        // then
        assertEquals(List.of("자료.pdf"), request.attachmentNames());
    }

    @Test
    void attachmentNames가null이면빈목록으로처리한다() {
        // when
        MailReviewRequest request = new MailReviewRequest(
                "자료 전달드립니다",
                "요청하신 자료를 첨부드립니다.",
                0,
                null
        );

        // then
        assertEquals(List.of(), request.attachmentNames());
    }

    @Test
    void attachmentCount가음수이면실패한다() {
        // when & then
        assertThrows(MailReviewException.class, () -> new MailReviewRequest(
                "자료 전달드립니다",
                "요청하신 자료를 첨부드립니다.",
                -1,
                List.of()
        ));
    }

    @Test
    void attachmentCount가0이어도attachmentNames가있으면파일명개수로보정한다() {
        // when
        MailReviewRequest request = new MailReviewRequest(
                "자료 전달드립니다",
                "요청하신 자료를 첨부드립니다.",
                0,
                List.of("자료.pdf", "이미지.png")
        );

        // then
        assertEquals(2, request.attachmentCount());
    }

    @Test
    void attachmentCount가양수이고attachmentNames가비어있어도허용한다() {
        // when
        MailReviewRequest request = new MailReviewRequest(
                "자료 전달드립니다",
                "요청하신 자료를 첨부드립니다.",
                1,
                List.of()
        );

        // then
        assertEquals(1, request.attachmentCount());
        assertEquals(List.of(), request.attachmentNames());
    }
}
