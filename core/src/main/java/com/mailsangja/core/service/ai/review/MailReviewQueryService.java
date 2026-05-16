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
            MailReviewSegment segment = segmentsById.get(issue.segmentId());
            if (segment == null) {
                continue;
            }
            Match match = findMatch(segment.text(), issue);
            if (match == null) {
                continue;
            }
            verified.add(toResult(issue, segment, match));
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

    private Match findMatch(String segmentText, LlmMailReviewIssueResult issue) {
        Match contextMatch = findByContext(segmentText, issue);
        if (contextMatch != null) {
            return contextMatch;
        }
        return findUnique(segmentText, issue.originalText());
    }

    private Match findByContext(String segmentText, LlmMailReviewIssueResult issue) {
        if (issue.contextBefore().isBlank() && issue.contextAfter().isBlank()) {
            return null;
        }
        String target = issue.contextBefore() + issue.originalText() + issue.contextAfter();
        Match match = findUnique(segmentText, target);
        if (match == null) {
            return null;
        }
        int startOffset = match.startOffset() + issue.contextBefore().length();
        return new Match(startOffset, startOffset + issue.originalText().length());
    }

    private Match findUnique(String text, String target) {
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

    private MailReviewIssueResult toResult(LlmMailReviewIssueResult issue, MailReviewSegment segment, Match match) {
        return new MailReviewIssueResult(
                issue.segmentId(),
                segment.field(),
                issue.type(),
                issue.severity(),
                segment.text(),
                issue.originalText(),
                issue.replacementText(),
                match.startOffset(),
                match.endOffset(),
                segment.globalStartOffset() + match.startOffset(),
                segment.globalStartOffset() + match.endOffset(),
                issue.reason()
        );
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
