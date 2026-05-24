package com.mailsangja.worker.service.ai.embedding;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.worker.config.properties.MailEmbeddingMetadataHashProperties;
import com.mailsangja.worker.common.exception.embedding.EmbeddingErrorCode;
import com.mailsangja.worker.common.exception.embedding.EmbeddingException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class MailEmbeddingQueryService {

    static final int EMBEDDING_CHUNK_SIZE_TOKENS = 1_800;
    private static final int MIN_CHUNK_SIZE_CHARS = 350;
    private static final int MIN_CHUNK_LENGTH_TO_EMBED = 5;
    private static final int MAX_CHUNK_COUNT = 1_000;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
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

    private final TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
            .withChunkSize(EMBEDDING_CHUNK_SIZE_TOKENS)
            .withMinChunkSizeChars(MIN_CHUNK_SIZE_CHARS)
            .withMinChunkLengthToEmbed(MIN_CHUNK_LENGTH_TO_EMBED)
            .withMaxNumChunks(MAX_CHUNK_COUNT)
            .withKeepSeparator(true)
            .build();
    private final MailEmbeddingMetadataHashProperties metadataHashProperties;

    public MailEmbeddingQueryService(MailEmbeddingMetadataHashProperties metadataHashProperties) {
        this.metadataHashProperties = metadataHashProperties;
    }

    public String extractEmbeddableText(Message message) {
        if (message == null) {
            return "";
        }
        if (!isBlank(message.getBodyText())) {
            return cleanText(message.getBodyText());
        }
        if (!isBlank(message.getBodyHtml())) {
            return cleanHtml(message.getBodyHtml());
        }
        return "";
    }

    public UUID createDocumentId(Message message) {
        validateMessage(message);
        String identity = "mail-embedding:%s:%s".formatted(
                message.getThread().getMailAccount().getProvider(),
                providerMessageId(message)
        );
        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
    }

    public List<String> splitTextForEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Document document = Document.builder()
                .text(text)
                .build();
        return tokenTextSplitter.split(document).stream()
                .map(Document::getText)
                .filter(chunk -> chunk != null && !chunk.isBlank())
                .toList();
    }

    public UUID createChunkDocumentId(UUID documentId, int chunkIndex) {
        if (documentId == null || chunkIndex <= 0) {
            throw new EmbeddingException(EmbeddingErrorCode.INVALID_MAIL_EMBEDDING_DOCUMENT);
        }
        String identity = "mail-embedding-chunk:%s:%d".formatted(documentId, chunkIndex);
        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
    }

    public Document buildDocument(Message message, UUID documentId, String maskedText) {
        validateMessage(message);
        validateDocumentInput(documentId, maskedText);
        return buildDocument(message, documentId, maskedText, documentId, 0, 1);
    }

    public Document buildDocument(
            Message message,
            UUID documentId,
            String maskedText,
            UUID rootDocumentId,
            int chunkIndex,
            int chunkCount
    ) {
        validateMessage(message);
        validateDocumentInput(documentId, maskedText);
        validateChunkInput(rootDocumentId, chunkIndex, chunkCount);
        return Document.builder()
                .id(documentId.toString())
                .text(maskedText)
                .metadata(buildMetadata(message, rootDocumentId, chunkIndex, chunkCount))
                .build();
    }

    private void validateMessage(Message message) {
        if (message == null
                || message.getThread() == null
                || message.getThread().getMailAccount() == null
                || message.getThread().getMailAccount().getProvider() == null
                || message.getId() == null
                || message.getDirection() == null
                || isBlank(message.getFromAddress())) {
            throw new EmbeddingException(EmbeddingErrorCode.INVALID_MAIL_EMBEDDING_MESSAGE);
        }
    }

    private void validateDocumentInput(UUID documentId, String maskedText) {
        if (documentId == null || maskedText == null || maskedText.isBlank()) {
            throw new EmbeddingException(EmbeddingErrorCode.INVALID_MAIL_EMBEDDING_DOCUMENT);
        }
    }

    private void validateChunkInput(UUID rootDocumentId, int chunkIndex, int chunkCount) {
        if (rootDocumentId == null || chunkIndex < 0 || chunkCount <= 0 || chunkIndex >= chunkCount) {
            throw new EmbeddingException(EmbeddingErrorCode.INVALID_MAIL_EMBEDDING_DOCUMENT);
        }
    }

    private Map<String, Object> buildMetadata(Message message, UUID rootDocumentId, int chunkIndex, int chunkCount) {
        Thread thread = message.getThread();
        MailAccount mailAccount = thread.getMailAccount();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("RootDocumentId", rootDocumentId.toString());
        metadata.put("ChunkIndex", chunkIndex);
        metadata.put("ChunkCount", chunkCount);
        metadata.put("UserId", mailAccount.getUser().getId().toString());
        metadata.put("MailAccountId", mailAccount.getId().toString());
        metadata.put("MessageId", message.getId().toString());
        metadata.put("ThreadId", thread.getId().toString());
        metadata.put("Direction", message.getDirection().name());
        addAddressMetadata(metadata, message);
        addSearchHashMetadata(metadata, message);
        return metadata;
    }

    private void addAddressMetadata(Map<String, Object> metadata, Message message) {
        putIfNotNull(metadata, "SentAt", sentAt(message));
        metadata.put("FromMailAddress", message.getFromAddress());
        metadata.put("ToMailAddress", toMailAddresses(message));
    }

    private void addSearchHashMetadata(Map<String, Object> metadata, Message message) {
        metadata.put("FromHash", hashValue(message.getFromAddress()));
        metadata.put("ToHashes", hashValues(message.getToAddresses()));
        metadata.put("CcHashes", hashValues(message.getCcAddresses()));
        metadata.put("RecipientHashes", recipientHashes(message));
        metadata.put("ParticipantNameHashes", participantNameHashes(message));
    }

    private void putIfNotNull(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }

    private List<String> recipientHashes(Message message) {
        ArrayList<String> hashes = new ArrayList<>();
        addHashes(hashes, message.getToAddresses());
        addHashes(hashes, message.getCcAddresses());
        return List.copyOf(hashes);
    }

    private List<String> participantNameHashes(Message message) {
        ArrayList<String> hashes = new ArrayList<>();
        addHash(hashes, message.getFromName());
        addHashes(hashes, message.getToNames());
        addHashes(hashes, message.getCcNames());
        return List.copyOf(hashes);
    }

    private List<String> hashValues(List<String> values) {
        ArrayList<String> hashes = new ArrayList<>();
        addHashes(hashes, values);
        return List.copyOf(hashes);
    }

    private void addHashes(List<String> hashes, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            addHash(hashes, value);
        }
    }

    private void addHash(List<String> hashes, String value) {
        String hash = hashValue(value);
        if (hash != null && !hashes.contains(hash)) {
            hashes.add(hash);
        }
    }

    private String hashValue(String value) {
        String normalized = normalizeHashSource(value);
        if (normalized.isBlank()) {
            return null;
        }
        validateMetadataHashSecret();
        return hmacSha256(normalized);
    }

    private void validateMetadataHashSecret() {
        if (metadataHashSecret().isBlank()) {
            throw new EmbeddingException(EmbeddingErrorCode.METADATA_HASH_SECRET_NOT_CONFIGURED);
        }
    }

    private String normalizeHashSource(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }

    private String metadataHashSecret() {
        String secret = metadataHashProperties.getSecret();
        return secret == null ? "" : secret;
    }

    private String hmacSha256(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(metadataHashSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String sentAt(Message message) {
        if (message.getSentAt() == null) {
            return null;
        }
        return message.getSentAt().toString();
    }

    private List<String> toMailAddresses(Message message) {
        if (message.getToAddresses() == null) {
            return List.of();
        }
        return message.getToAddresses();
    }

    private String providerMessageId(Message message) {
        String providerMessageId = message.getGmailMessageId();
        if (providerMessageId == null || providerMessageId.isBlank()) {
            throw new EmbeddingException(EmbeddingErrorCode.INVALID_MAIL_EMBEDDING_MESSAGE);
        }
        return providerMessageId;
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
