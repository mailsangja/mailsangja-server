package com.mailsangja.core.service.ai.review;

import com.mailsangja.core.dto.mail.LlmMailReviewIssueResult;
import com.mailsangja.core.dto.mail.MailReviewCommand;
import com.mailsangja.core.dto.mail.MailReviewField;
import com.mailsangja.core.dto.mail.MailReviewIssueResult;
import com.mailsangja.core.dto.mail.MailReviewSegment;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MailReviewQueryService {

    private static final int HASH_LENGTH = 16;

    public List<MailReviewSegment> createSegments(MailReviewCommand command) {
        List<MailReviewSegment> segments = new ArrayList<>();
        appendSubject(segments, command.subject());
        appendBody(segments, command.body());
        return List.copyOf(segments);
    }

    public Map<String, MailReviewSegment> toSegmentMap(List<MailReviewSegment> segments) {
        Map<String, MailReviewSegment> values = new LinkedHashMap<>();
        for (MailReviewSegment segment : segments) {
            values.put(segment.segmentId(), segment);
        }
        return values;
    }

    public List<MailReviewIssueResult> verifyIssues(List<LlmMailReviewIssueResult> issues,
                                                    Map<String, MailReviewSegment> segmentsById) {
        List<MailReviewIssueResult> verified = new ArrayList<>();
        for (LlmMailReviewIssueResult issue : issues) {
            String segmentId = text(issue.segmentId());
            String originalText = text(issue.originalText());
            if (segmentId.isBlank() || originalText.isBlank()) {
                continue;
            }
            MailReviewSegment segment = segmentsById.get(segmentId);
            if (segment == null) {
                continue;
            }
            Match match = findMatch(segment.text(), originalText, issue);
            if (match == null) {
                continue;
            }
            verified.add(toResult(issue, segment, match, segmentId, originalText));
        }
        return List.copyOf(removeOverlaps(verified));
    }

    private void appendSubject(List<MailReviewSegment> segments, String subject) {
        if (subject.isBlank()) {
            return;
        }
        segments.add(createSegment(MailReviewField.SUBJECT, 0, subject, 0, subject.length()));
    }

    private void appendBody(List<MailReviewSegment> segments, String body) {
        int index = 0;
        int lineStart = 0;
        while (lineStart < body.length()) {
            int lineEnd = findLineEnd(body, lineStart);
            int nextLineStart = nextLineStart(body, lineEnd);
            String rawLine = body.substring(lineStart, lineEnd);
            TrimmedLine line = trimLine(rawLine, lineStart);
            if (!line.text().isBlank()) {
                segments.add(createSegment(MailReviewField.BODY, index, line.text(), line.startOffset(), line.endOffset()));
                index++;
            }
            lineStart = nextLineStart;
        }
    }

    private int findLineEnd(String text, int start) {
        int end = text.indexOf('\n', start);
        if (end < 0) {
            return text.length();
        }
        if (end > start && text.charAt(end - 1) == '\r') {
            return end - 1;
        }
        return end;
    }

    private int nextLineStart(String text, int lineEnd) {
        int next = lineEnd;
        if (next < text.length() && text.charAt(next) == '\r') {
            next++;
        }
        if (next < text.length() && text.charAt(next) == '\n') {
            next++;
        }
        return next;
    }

    private TrimmedLine trimLine(String line, int lineGlobalStart) {
        int start = 0;
        int end = line.length();
        while (start < end && Character.isWhitespace(line.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(line.charAt(end - 1))) {
            end--;
        }
        return new TrimmedLine(line.substring(start, end), lineGlobalStart + start, lineGlobalStart + end);
    }

    private MailReviewSegment createSegment(MailReviewField field, int index, String text, int startOffset, int endOffset) {
        String hash = hash(text);
        return new MailReviewSegment(segmentId(field, index, hash), field, index, hash, text, startOffset, endOffset);
    }

    private String segmentId(MailReviewField field, int index, String hash) {
        return field.name() + ":" + "%03d".formatted(index) + ":" + hash;
    }

    private String hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes).substring(0, HASH_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    private Match findMatch(String segmentText, String originalText, LlmMailReviewIssueResult issue) {
        Match contextMatch = findByContext(segmentText, originalText, issue);
        if (contextMatch != null) {
            return contextMatch;
        }
        return findUnique(segmentText, originalText);
    }

    private Match findByContext(String segmentText, String originalText, LlmMailReviewIssueResult issue) {
        String contextBefore = text(issue.contextBefore());
        String contextAfter = text(issue.contextAfter());
        if (contextBefore.isBlank() && contextAfter.isBlank()) {
            return null;
        }
        String target = contextBefore + originalText + contextAfter;
        Match match = findUnique(segmentText, target);
        if (match == null) {
            return null;
        }
        int startOffset = match.startOffset() + contextBefore.length();
        return new Match(startOffset, startOffset + originalText.length());
    }

    private Match findUnique(String text, String target) {
        if (target == null || target.isEmpty()) {
            return null;
        }
        int first = text.indexOf(target);
        if (first < 0) {
            return null;
        }
        int second = text.indexOf(target, first + target.length());
        if (second >= 0) {
            return null;
        }
        return new Match(first, first + target.length());
    }

    private MailReviewIssueResult toResult(LlmMailReviewIssueResult issue,
                                           MailReviewSegment segment,
                                           Match match,
                                           String segmentId,
                                           String originalText) {
        return new MailReviewIssueResult(
                segmentId,
                segment.field(),
                issue.type(),
                issue.severity(),
                segment.text(),
                originalText,
                text(issue.replacementText()),
                match.startOffset(),
                match.endOffset(),
                segment.globalStartOffset() + match.startOffset(),
                segment.globalStartOffset() + match.endOffset(),
                text(issue.reason())
        );
    }

    private String text(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private List<MailReviewIssueResult> removeOverlaps(List<MailReviewIssueResult> issues) {
        List<MailReviewIssueResult> sorted = issues.stream()
                .sorted(Comparator.comparing(MailReviewIssueResult::field)
                        .thenComparingInt(MailReviewIssueResult::globalStartOffset)
                        .thenComparingInt(issue -> issue.globalEndOffset() - issue.globalStartOffset()))
                .toList();
        List<MailReviewIssueResult> selected = new ArrayList<>();
        for (MailReviewIssueResult issue : sorted) {
            if (selected.stream().noneMatch(existing -> overlaps(existing, issue))) {
                selected.add(issue);
            }
        }
        return selected;
    }

    private boolean overlaps(MailReviewIssueResult left, MailReviewIssueResult right) {
        if (left.field() != right.field()) {
            return false;
        }
        return left.globalStartOffset() < right.globalEndOffset()
                && right.globalStartOffset() < left.globalEndOffset();
    }

    private record TrimmedLine(String text, int startOffset, int endOffset) {
    }

    private record Match(int startOffset, int endOffset) {
    }
}
