package com.mailsangja.db.adapter.mail;

import com.mailsangja.db.dto.MailDraftReferenceMessageResult;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.module.mail.MessageJpaRepositoryModule;
import com.mailsangja.db.port.MailDraftReferenceQueryPort;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Repository
@RequiredArgsConstructor
public class MailDraftReferenceQueryAdapter implements MailDraftReferenceQueryPort {

    private static final Set<String> BLOCK_TAGS = Set.of(
            "p", "div", "section", "article", "header", "footer", "main", "aside",
            "table", "tr", "ul", "ol", "li", "h1", "h2", "h3", "h4", "h5", "h6",
            "blockquote", "pre"
    );
    private static final Set<String> SKIP_TAGS = Set.of("script", "style", "noscript");
    private static final Pattern QUOTED_LINE_PATTERN = Pattern.compile("^>+\\s?.*");
    private static final Pattern ENGLISH_QUOTE_HEADER_PATTERN = Pattern.compile("^On\\s+.+\\s+wrote:$", Pattern.CASE_INSENSITIVE);
    private static final Pattern KOREAN_QUOTE_HEADER_PATTERN = Pattern.compile("^.+님이\\s*작성:$");
    private static final Pattern ORIGINAL_MESSAGE_PATTERN = Pattern.compile("^-+\\s*Original Message\\s*-+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORWARDED_MESSAGE_PATTERN = Pattern.compile("^-+\\s*Forwarded message\\s*-+$", Pattern.CASE_INSENSITIVE);

    private final MessageJpaRepositoryModule messageJpaRepositoryModule;

    @Override
    public List<MailDraftReferenceMessageResult> findRecentWrittenMessages(UUID userId, UUID mailAccountId, int limit) {
        List<Message> messages = messageJpaRepositoryModule.findRecentByUserIdAndMailAccountIdAndDirection(
                userId, mailAccountId, Direction.OUTBOUND, PageRequest.of(0, limit)
        );
        return toResults(messages, mailAccountId);
    }

    @Override
    public List<MailDraftReferenceMessageResult> findWrittenMessagesByHints(UUID userId, UUID mailAccountId,
                                                                            List<String> hints, int limit) {
        if (hints == null || hints.isEmpty()) {
            return List.of();
        }
        return toResults(findHintMessages(userId, mailAccountId, hints, limit), mailAccountId);
    }

    @Override
    public List<MailDraftReferenceMessageResult> findRecipientHistoryMessages(UUID userId, UUID mailAccountId,
                                                                              List<String> recipientHints, int limit) {
        if (recipientHints == null || recipientHints.isEmpty()) {
            return List.of();
        }
        return toResults(findRecipientMessages(userId, mailAccountId, recipientHints, limit), mailAccountId);
    }

    @Override
    public List<UUID> findAccountLexicalRelevantMessageIds(UUID userId, UUID mailAccountId, String tsQuery, int limit) {
        if (isBlank(tsQuery) || limit <= 0) {
            return List.of();
        }
        return toUuids(messageJpaRepositoryModule.findAccountLexicalRelevantMessageIds(
                userId.toString(), mailAccountId.toString(), tsQuery, limit
        ));
    }

    @Override
    public List<UUID> findUserLexicalRelevantMessageIds(UUID userId, String tsQuery, int limit) {
        if (isBlank(tsQuery) || limit <= 0) {
            return List.of();
        }
        return toUuids(messageJpaRepositoryModule.findUserLexicalRelevantMessageIds(
                userId.toString(), tsQuery, limit
        ));
    }

    @Override
    public List<MailDraftReferenceMessageResult> findMessagesByIds(List<UUID> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }
        return toResults(messageJpaRepositoryModule.findActiveByIdIn(messageIds));
    }

    @Override
    public List<MailDraftReferenceMessageResult> findThreadContextMessages(UUID replyMessageId) {
        return toResults(messageJpaRepositoryModule.findThreadContextByReplyMessageId(replyMessageId));
    }

    private List<Message> findHintMessages(UUID userId, UUID mailAccountId, List<String> hints, int limit) {
        List<Message> messages = new ArrayList<>();
        for (String hint : hints) {
            addHintMessages(messages, userId, mailAccountId, hint, limit);
        }
        return messages;
    }

    private List<Message> findRecipientMessages(UUID userId, UUID mailAccountId, List<String> recipientHints, int limit) {
        List<Message> messages = new ArrayList<>();
        for (String hint : recipientHints) {
            addRecipientMessages(messages, userId, mailAccountId, hint, limit);
        }
        return messages;
    }

    private void addHintMessages(List<Message> messages, UUID userId, UUID mailAccountId, String hint, int limit) {
        int remaining = limit - messages.size();
        if (remaining <= 0 || hint == null || hint.isBlank()) {
            return;
        }
        List<Message> found = messageJpaRepositoryModule.findWrittenByUserIdAndMailAccountIdAndHint(
                userId.toString(), mailAccountId.toString(), hint, PageRequest.of(0, remaining)
        );
        addUniqueMessages(messages, found, limit);
    }

    private void addRecipientMessages(List<Message> messages, UUID userId, UUID mailAccountId, String hint, int limit) {
        int remaining = limit - messages.size();
        if (remaining <= 0 || hint == null || hint.isBlank()) {
            return;
        }
        List<Message> found = messageJpaRepositoryModule.findRecipientHistoryByUserIdAndMailAccountIdAndHint(
                userId.toString(), mailAccountId.toString(), hint, PageRequest.of(0, remaining)
        );
        addUniqueMessages(messages, found, limit);
    }

    private void addUniqueMessages(List<Message> messages, List<Message> found, int limit) {
        for (Message message : found) {
            addUniqueMessage(messages, message, limit);
        }
    }

    private void addUniqueMessage(List<Message> messages, Message message, int limit) {
        if (messages.size() < limit && !containsMessage(messages, message)) {
            messages.add(message);
        }
    }

    private boolean containsMessage(List<Message> messages, Message message) {
        for (Message value : messages) {
            if (value.getId().equals(message.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<UUID> toUuids(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .map(UUID::fromString)
                .toList();
    }

    private List<MailDraftReferenceMessageResult> toResults(List<Message> messages) {
        return messages.stream()
                .map(this::toResult)
                .toList();
    }

    private List<MailDraftReferenceMessageResult> toResults(List<Message> messages, UUID mailAccountId) {
        return messages.stream()
                .map(message -> toResult(message, mailAccountId))
                .toList();
    }

    private MailDraftReferenceMessageResult toResult(Message message) {
        return new MailDraftReferenceMessageResult(
                message.getId(),
                message.getThread().getMailAccount().getId(),
                message.getDirection(),
                message.getSubject(),
                bodyOf(message)
        );
    }

    private MailDraftReferenceMessageResult toResult(Message message, UUID mailAccountId) {
        return new MailDraftReferenceMessageResult(
                message.getId(),
                mailAccountId,
                message.getDirection(),
                message.getSubject(),
                bodyOf(message)
        );
    }

    private String bodyOf(Message message) {
        if (message.getBodyText() != null && !message.getBodyText().isBlank()) {
            return cleanText(message.getBodyText());
        }
        return cleanHtml(message.getBodyHtml());
    }

    private String cleanText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return removeQuotedLines(normalizeLineEndings(text));
    }

    private String cleanHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        org.jsoup.nodes.Document document = Jsoup.parse(html);
        document.select(String.join(",", SKIP_TAGS)).remove();
        document.select("blockquote,.gmail_quote,.gmail_attr,.yahoo_quoted,.moz-cite-prefix,div[type=cite]").remove();

        StringBuilder builder = new StringBuilder();
        appendNodeText(document.body(), builder);
        return cleanText(builder.toString());
    }

    private String normalizeLineEndings(String text) {
        return text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\u00A0', ' ');
    }

    private String removeQuotedLines(String text) {
        StringBuilder builder = new StringBuilder();
        for (String line : text.split("\n")) {
            String compactLine = line.replaceAll("[\\t\\x0B\\f ]+", " ").trim();
            if (compactLine.isBlank()) {
                continue;
            }
            if (isQuoteStart(compactLine)) {
                break;
            }
            if (QUOTED_LINE_PATTERN.matcher(compactLine).matches()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(compactLine);
        }
        return builder.toString();
    }

    private boolean isQuoteStart(String line) {
        return ENGLISH_QUOTE_HEADER_PATTERN.matcher(line).matches()
                || KOREAN_QUOTE_HEADER_PATTERN.matcher(line).matches()
                || ORIGINAL_MESSAGE_PATTERN.matcher(line).matches()
                || FORWARDED_MESSAGE_PATTERN.matcher(line).matches();
    }

    private void appendNodeText(Node node, StringBuilder builder) {
        if (node instanceof TextNode textNode) {
            builder.append(textNode.getWholeText());
            return;
        }
        if (!(node instanceof Element element)) {
            for (Node childNode : node.childNodes()) {
                appendNodeText(childNode, builder);
            }
            return;
        }

        String tagName = element.tagName();
        if (SKIP_TAGS.contains(tagName)) {
            return;
        }
        if ("br".equals(tagName)) {
            appendNewLine(builder);
            return;
        }
        for (Node childNode : element.childNodes()) {
            appendNodeText(childNode, builder);
        }
        if (BLOCK_TAGS.contains(tagName)) {
            appendNewLine(builder);
        }
    }

    private void appendNewLine(StringBuilder builder) {
        if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != '\n') {
            builder.append('\n');
        }
    }
}
