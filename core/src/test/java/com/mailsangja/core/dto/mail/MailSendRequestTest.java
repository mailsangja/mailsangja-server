package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MailSendRequestTest {

    private static final int MB = 1024 * 1024;

    @Test
    void validate_정상요청이면통과한다() {
        MailSendRequest request = createRequest(
                "\"Sender\" <sender@example.com>",
                "\"Reply\" <reply@example.com>",
                List.of("\"To\" <to@example.com>"),
                List.of("\"Cc\" <cc@example.com>"),
                List.of("\"Bcc\" <bcc@example.com>"),
                "제목",
                "본문",
                List.of(new MockMultipartFile("attachments", "file.txt", "text/plain", "hello".getBytes()))
        );

        assertDoesNotThrow(() -> request.validate());
    }

    @Test
    void validate_발신주소가유효하지않으면실패한다() {
        MailSendRequest request = createRequest(
                "invalid-address",
                null,
                List.of("to@example.com"),
                null,
                null,
                "제목",
                "본문",
                null
        );

        assertError(request, MailSendErrorCode.INVALID_SENDER_ADDRESS);
    }

    @Test
    void validate_replyTo가유효하지않으면실패한다() {
        MailSendRequest request = createRequest(
                "sender@example.com",
                "invalid-reply-to",
                List.of("to@example.com"),
                null,
                null,
                "제목",
                "본문",
                null
        );

        assertError(request, MailSendErrorCode.INVALID_REPLY_TO_ADDRESS);
    }

    @Test
    void validate_수신자가없으면실패한다() {
        MailSendRequest request = createRequest(
                "sender@example.com",
                null,
                List.of(),
                null,
                null,
                "제목",
                "본문",
                null
        );

        assertError(request, MailSendErrorCode.EMPTY_RECIPIENT);
    }

    @Test
    void validate_수신주소가유효하지않으면실패한다() {
        MailSendRequest request = createRequest(
                "sender@example.com",
                null,
                List.of("invalid-recipient"),
                null,
                null,
                "제목",
                "본문",
                null
        );

        assertError(request, MailSendErrorCode.INVALID_RECIPIENT_ADDRESS);
    }

    @Test
    void validate_toCcBcc에중복수신자가있으면실패한다() {
        MailSendRequest request = createRequest(
                "sender@example.com",
                null,
                List.of("to@example.com"),
                List.of("\"Duplicate\" <TO@example.com>"),
                null,
                "제목",
                "본문",
                null
        );

        assertError(request, MailSendErrorCode.DUPLICATE_RECIPIENT_ADDRESS);
    }

    @Test
    void validate_제목에개행문자가있으면실패한다() {
        MailSendRequest request = createRequest(
                "sender@example.com",
                null,
                List.of("to@example.com"),
                null,
                null,
                "제목\n추가",
                "본문",
                null
        );

        assertError(request, MailSendErrorCode.INVALID_MAIL_SUBJECT);
    }

    @Test
    void validate_제목과본문이모두비어있으면실패한다() {
        MailSendRequest request = createRequest(
                "sender@example.com",
                null,
                List.of("to@example.com"),
                null,
                null,
                " ",
                "",
                null
        );

        assertError(request, MailSendErrorCode.EMPTY_SUBJECT_AND_CONTENT);
    }

    @Test
    void validate_첨부파일개수가제한을초과하면실패한다() {
        List<MultipartFile> attachments = IntStream.range(0, 11)
                .mapToObj(index -> new MockMultipartFile(
                        "attachments",
                        "file-" + index + ".txt",
                        "text/plain",
                        "content".getBytes()
                ))
                .map(MultipartFile.class::cast)
                .toList();
        MailSendRequest request = createRequest(
                "sender@example.com",
                null,
                List.of("to@example.com"),
                null,
                null,
                "제목",
                "본문",
                attachments
        );

        assertError(request, MailSendErrorCode.ATTACHMENT_COUNT_EXCEEDED);
    }

    @Test
    void validate_빈첨부파일이면실패한다() {
        MailSendRequest request = createRequest(
                "sender@example.com",
                null,
                List.of("to@example.com"),
                null,
                null,
                "제목",
                "본문",
                List.of(new MockMultipartFile("attachments", "file.txt", "text/plain", new byte[0]))
        );

        assertError(request, MailSendErrorCode.EMPTY_ATTACHMENT_FILE);
    }

    @Test
    void validate_첨부파일명이비어있으면실패한다() {
        MailSendRequest request = createRequest(
                "sender@example.com",
                null,
                List.of("to@example.com"),
                null,
                null,
                "제목",
                "본문",
                List.of(new MockMultipartFile("attachments", "", "text/plain", "content".getBytes()))
        );

        assertError(request, MailSendErrorCode.INVALID_ATTACHMENT_FILENAME);
    }

    @Test
    void validate_단일첨부파일크기가제한을초과하면실패한다() {
        MailSendRequest request = createRequest(
                "sender@example.com",
                null,
                List.of("to@example.com"),
                null,
                null,
                "제목",
                "본문",
                List.of(new MockMultipartFile("attachments", "file.bin", "application/octet-stream", new byte[10 * MB + 1]))
        );

        assertError(request, MailSendErrorCode.ATTACHMENT_SIZE_EXCEEDED);
    }

    @Test
    void validate_전체첨부파일크기가제한을초과하면실패한다() {
        MailSendRequest request = createRequest(
                "sender@example.com",
                null,
                List.of("to@example.com"),
                null,
                null,
                "제목",
                "본문",
                List.of(
                        new MockMultipartFile("attachments", "a.bin", "application/octet-stream", new byte[7 * MB]),
                        new MockMultipartFile("attachments", "b.bin", "application/octet-stream", new byte[7 * MB]),
                        new MockMultipartFile("attachments", "c.bin", "application/octet-stream", new byte[7 * MB])
                )
        );

        assertError(request, MailSendErrorCode.ATTACHMENT_SIZE_EXCEEDED);
    }

    @Test
    void validate_본문이미지와cid가정상매칭되면통과한다() {
        MailSendRequest request = new MailSendRequest(
                "sender@example.com",
                null,
                List.of("to@example.com"),
                null,
                null,
                "제목",
                "<p>본문</p><img src=\"cid:inline-1\">",
                null,
                List.of(new MockMultipartFile("inlineImages", "image.png", "image/png", "image".getBytes())),
                List.of("inline-1")
        );

        assertDoesNotThrow(() -> request.validate());
    }

    @Test
    void validate_본문이미지와cid개수가다르면실패한다() {
        MailSendRequest request = new MailSendRequest(
                "sender@example.com",
                null,
                List.of("to@example.com"),
                null,
                null,
                "제목",
                "<p>본문</p><img src=\"cid:inline-1\">",
                null,
                List.of(new MockMultipartFile("inlineImages", "image.png", "image/png", "image".getBytes())),
                List.of()
        );

        assertError(request, MailSendErrorCode.INLINE_IMAGE_COUNT_MISMATCH);
    }

    @Test
    void validate_본문에없는cid면실패한다() {
        MailSendRequest request = new MailSendRequest(
                "sender@example.com",
                null,
                List.of("to@example.com"),
                null,
                null,
                "제목",
                "<p>본문</p>",
                null,
                List.of(new MockMultipartFile("inlineImages", "image.png", "image/png", "image".getBytes())),
                List.of("inline-1")
        );

        assertError(request, MailSendErrorCode.INLINE_IMAGE_CID_NOT_FOUND);
    }

    @Test
    void validate_본문cid에대응하는이미지파일이없으면실패한다() {
        MailSendRequest request = new MailSendRequest(
                "sender@example.com",
                null,
                List.of("to@example.com"),
                null,
                null,
                "제목",
                "<p>본문</p><img src=\"cid:inline-1\">",
                null,
                null,
                null
        );

        assertError(request, MailSendErrorCode.INLINE_IMAGE_COUNT_MISMATCH);
    }

    @Test
    void validate_본문cid중일부만업로드되면실패한다() {
        MailSendRequest request = new MailSendRequest(
                "sender@example.com",
                null,
                List.of("to@example.com"),
                null,
                null,
                "제목",
                "<p>본문</p><img src=\"cid:inline-1\"><img src=\"cid:inline-2\">",
                null,
                List.of(new MockMultipartFile("inlineImages", "image.png", "image/png", "image".getBytes())),
                List.of("inline-1")
        );

        assertError(request, MailSendErrorCode.INLINE_IMAGE_COUNT_MISMATCH);
    }

    @Test
    void validate_본문이미지cid가중복되면실패한다() {
        MailSendRequest request = new MailSendRequest(
                "sender@example.com",
                null,
                List.of("to@example.com"),
                null,
                null,
                "제목",
                "<p>본문</p><img src=\"cid:inline-1\">",
                null,
                List.of(
                        new MockMultipartFile("inlineImages", "a.png", "image/png", "image".getBytes()),
                        new MockMultipartFile("inlineImages", "b.png", "image/png", "image".getBytes())
                ),
                List.of("inline-1", "inline-1")
        );

        assertError(request, MailSendErrorCode.DUPLICATE_INLINE_IMAGE_CID);
    }

    @Test
    void validate_본문이미지가image타입이아니면실패한다() {
        MailSendRequest request = new MailSendRequest(
                "sender@example.com",
                null,
                List.of("to@example.com"),
                null,
                null,
                "제목",
                "<p>본문</p><img src=\"cid:inline-1\">",
                null,
                List.of(new MockMultipartFile("inlineImages", "file.txt", "text/plain", "image".getBytes())),
                List.of("inline-1")
        );

        assertError(request, MailSendErrorCode.INVALID_INLINE_IMAGE_TYPE);
    }

    private void assertError(MailSendRequest request, MailSendErrorCode expectedErrorCode) {
        MailSendException exception = assertThrows(MailSendException.class, request::validate);
        assertEquals(expectedErrorCode, exception.getErrorCode());
    }

    private MailSendRequest createRequest(
            String from,
            String replyTo,
            List<String> to,
            List<String> cc,
            List<String> bcc,
            String subject,
            String content,
            List<MultipartFile> attachments
    ) {
        return new MailSendRequest(from, replyTo, to, cc, bcc, subject, content, attachments);
    }
}
