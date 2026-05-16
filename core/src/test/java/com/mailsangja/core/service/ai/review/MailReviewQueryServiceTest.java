package com.mailsangja.core.service.ai.review;

import com.mailsangja.core.dto.mail.LlmMailReviewIssueResult;
import com.mailsangja.core.dto.mail.MailReviewCommand;
import com.mailsangja.core.dto.mail.MailReviewField;
import com.mailsangja.core.dto.mail.MailReviewIssueResult;
import com.mailsangja.core.dto.mail.MailReviewIssueType;
import com.mailsangja.core.dto.mail.MailReviewSegment;
import com.mailsangja.core.dto.mail.MailReviewSeverity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MailReviewQueryServiceTest {

    private final MailReviewQueryService service = new MailReviewQueryService();

    @Test
    void 제목과본문라인을segment로분리하고본문globalOffset을보존한다() {
        // given
        MailReviewCommand command = new MailReviewCommand(
                UUID.randomUUID(),
                "회의 일정 확입 요청",
                "안녕하새요.\n  내일 회의 가능하신지 확인 부탁드림니다.\n",
                0,
                List.of()
        );

        // when
        List<MailReviewSegment> segments = service.createSegments(command);

        // then
        assertEquals(3, segments.size());
        assertEquals(MailReviewField.SUBJECT, segments.get(0).field());
        assertEquals("회의 일정 확입 요청", segments.get(0).text());
        assertEquals("안녕하새요.", segments.get(1).text());
        assertEquals(0, segments.get(1).globalStartOffset());
        assertEquals("내일 회의 가능하신지 확인 부탁드림니다.", segments.get(2).text());
        assertEquals(9, segments.get(2).globalStartOffset());
        assertTrue(segments.get(2).segmentId().startsWith("BODY:001:"));
    }

    @Test
    void segmentId로원문을찾고localGlobalOffset을계산한다() {
        // given
        MailReviewSegment segment = new MailReviewSegment(
                "BODY:001:hash",
                MailReviewField.BODY,
                1,
                "hash",
                "내일 회의 가능하신지 확인 부탁드림니다.",
                9,
                31
        );
        LlmMailReviewIssueResult issue = new LlmMailReviewIssueResult(
                "BODY:001:hash",
                MailReviewIssueType.SPELLING,
                MailReviewSeverity.LOW,
                "부탁드림니다",
                "부탁드립니다",
                "확인 ",
                ".",
                "맞춤법 오류입니다."
        );

        // when
        List<MailReviewIssueResult> result = service.verifyIssues(List.of(issue), Map.of(segment.segmentId(), segment));

        // then
        assertEquals(1, result.size());
        assertEquals(15, result.getFirst().localStartOffset());
        assertEquals(24, result.getFirst().globalStartOffset());
    }

    @Test
    void 같은원문이반복되면context가없을때후보를버린다() {
        // given
        MailReviewSegment segment = new MailReviewSegment(
                "BODY:000:hash",
                MailReviewField.BODY,
                0,
                "hash",
                "확인 부탁드림니다. 다시 확인 부탁드림니다.",
                0,
                24
        );
        LlmMailReviewIssueResult issue = new LlmMailReviewIssueResult(
                "BODY:000:hash",
                MailReviewIssueType.SPELLING,
                MailReviewSeverity.LOW,
                "부탁드림니다",
                "부탁드립니다",
                "",
                "",
                "맞춤법 오류입니다."
        );

        // when
        List<MailReviewIssueResult> result = service.verifyIssues(List.of(issue), Map.of(segment.segmentId(), segment));

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    void 필수필드가null이면후보를버린다() {
        // given
        MailReviewSegment segment = new MailReviewSegment(
                "BODY:000:hash",
                MailReviewField.BODY,
                0,
                "hash",
                "안뇽하세요.",
                0,
                6
        );
        LlmMailReviewIssueResult issue = mock(LlmMailReviewIssueResult.class);
        when(issue.segmentId()).thenReturn(null);
        when(issue.originalText()).thenReturn("안뇽");

        // when
        List<MailReviewIssueResult> result = service.verifyIssues(List.of(issue), Map.of(segment.segmentId(), segment));

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    void context필드가null이면context없이원문으로검증한다() {
        // given
        MailReviewSegment segment = new MailReviewSegment(
                "BODY:000:hash",
                MailReviewField.BODY,
                0,
                "hash",
                "안뇽하세요.",
                0,
                6
        );
        LlmMailReviewIssueResult issue = mock(LlmMailReviewIssueResult.class);
        when(issue.segmentId()).thenReturn("BODY:000:hash");
        when(issue.originalText()).thenReturn("안뇽");
        when(issue.replacementText()).thenReturn("안녕");
        when(issue.contextBefore()).thenReturn(null);
        when(issue.contextAfter()).thenReturn(null);
        when(issue.reason()).thenReturn("오타입니다.");
        when(issue.type()).thenReturn(MailReviewIssueType.SPELLING);
        when(issue.severity()).thenReturn(MailReviewSeverity.LOW);

        // when
        List<MailReviewIssueResult> result = service.verifyIssues(List.of(issue), Map.of(segment.segmentId(), segment));

        // then
        assertEquals(1, result.size());
        assertEquals(0, result.getFirst().localStartOffset());
        assertEquals(2, result.getFirst().localEndOffset());
    }
}
