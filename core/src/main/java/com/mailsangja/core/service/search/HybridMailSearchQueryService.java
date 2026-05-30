package com.mailsangja.core.service.search;

import com.mailsangja.core.dto.search.HybridMailSearchItemResult;
import com.mailsangja.core.dto.search.HybridMailSearchMatchType;
import com.mailsangja.core.dto.search.HybridMailSearchResult;
import com.mailsangja.core.dto.search.HybridMailSearchScope;
import com.mailsangja.db.entity.contact.Contact;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.port.ContactRepositoryPort;
import com.mailsangja.db.port.MailSearchRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridMailSearchQueryService {

    private static final int CANDIDATE_MULTIPLIER = 5;
    private static final int MIN_CANDIDATE_LIMIT = 40;
    private static final int RRF_K = 60;
    private static final double VECTOR_RRF_WEIGHT = 0.6;
    private static final double LEXICAL_RRF_WEIGHT = 0.4;

    private final MailSearchRepositoryPort mailSearchRepositoryPort;
    private final ContactRepositoryPort contactRepositoryPort;
    private final VectorStore vectorStore;
    private final HybridSearchLexicalQueryBuilder lexicalQueryBuilder;

    public HybridMailSearchResult search(
            UUID userId,
            String query,
            HybridMailSearchScope scope,
            UUID mailAccountId,
            List<UUID> labelIds,
            Boolean read,
            int size
    ) {
        int resultLimit = Math.max(size, 1);
        int candidateLimit = Math.max(MIN_CANDIDATE_LIMIT, resultLimit * CANDIDATE_MULTIPLIER);
        Direction direction = scope.direction();
        List<UUID> vectorIds = findVectorMessageIds(userId, query, direction, mailAccountId, candidateLimit);
        List<UUID> lexicalIds = findLexicalMessageIds(userId, query, direction, mailAccountId, labelIds, read, candidateLimit);
        Map<UUID, RankedMessage> ranked = rankHybrid(vectorIds, lexicalIds, candidateLimit);
        List<UUID> rankedIds = ranked.keySet().stream().toList();
        List<Message> messages = mailSearchRepositoryPort.findHybridMessagesByIds(
                userId, rankedIds, mailAccountId, direction, labelIds, read
        );
        List<HybridMailSearchItemResult> items = orderByRank(messages, ranked, resultLimit);
        return new HybridMailSearchResult(items, findContactNamesByEmails(userId, items));
    }

    private List<UUID> findVectorMessageIds(
            UUID userId,
            String query,
            Direction direction,
            UUID mailAccountId,
            int limit
    ) {
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(limit)
                    .filterExpression(vectorFilter(userId, direction, mailAccountId))
                    .build();
            return vectorStore.similaritySearch(request).stream()
                    .map(this::messageId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
        } catch (RuntimeException exception) {
            log.warn("Hybrid mail vector search failed. userId={} direction={} mailAccountId={}",
                    userId, direction, mailAccountId, exception);
            return List.of();
        }
    }

    private Filter.Expression vectorFilter(UUID userId, Direction direction, UUID mailAccountId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        if (mailAccountId == null && direction == null) {
            return builder.eq("UserId", userId.toString()).build();
        }
        if (mailAccountId != null && direction != null) {
            return builder.and(
                    builder.and(
                            builder.eq("UserId", userId.toString()),
                            builder.eq("MailAccountId", mailAccountId.toString())
                    ),
                    builder.eq("Direction", direction.name())
            ).build();
        }
        if (mailAccountId != null) {
            return builder.and(
                    builder.eq("UserId", userId.toString()),
                    builder.eq("MailAccountId", mailAccountId.toString())
            ).build();
        }
        return builder.and(
                builder.eq("UserId", userId.toString()),
                builder.eq("Direction", direction.name())
        ).build();
    }

    private UUID messageId(Document document) {
        Object value = document.getMetadata().get("MessageId");
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException exception) {
            log.warn("Hybrid mail vector document has invalid MessageId metadata. messageId={}", value);
            return null;
        }
    }

    private List<UUID> findLexicalMessageIds(
            UUID userId,
            String query,
            Direction direction,
            UUID mailAccountId,
            List<UUID> labelIds,
            Boolean read,
            int limit
    ) {
        String tsQuery = lexicalQueryBuilder.build(query);
        if (tsQuery.isBlank()) {
            return List.of();
        }
        try {
            return mailSearchRepositoryPort.findHybridLexicalMessageIds(
                    userId, mailAccountId, direction, tsQuery, labelIds, read, limit
            );
        } catch (RuntimeException exception) {
            log.warn("Hybrid mail lexical search failed. userId={} direction={} mailAccountId={}",
                    userId, direction, mailAccountId, exception);
            return List.of();
        }
    }

    private Map<UUID, RankedMessage> rankHybrid(List<UUID> vectorIds, List<UUID> lexicalIds, int limit) {
        Map<UUID, RankedMessage> scores = new LinkedHashMap<>();
        addRrfScores(scores, vectorIds, HybridMailSearchMatchType.VECTOR, VECTOR_RRF_WEIGHT);
        addRrfScores(scores, lexicalIds, HybridMailSearchMatchType.LEXICAL, LEXICAL_RRF_WEIGHT);
        return scores.entrySet().stream()
                .sorted(Map.Entry.<UUID, RankedMessage>comparingByValue(
                        Comparator.comparingDouble(RankedMessage::score)
                ).reversed())
                .limit(limit)
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Map::putAll);
    }

    private void addRrfScores(
            Map<UUID, RankedMessage> scores,
            List<UUID> ids,
            HybridMailSearchMatchType matchType,
            double weight
    ) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (int index = 0; index < ids.size(); index++) {
            UUID id = ids.get(index);
            RankedMessage ranked = scores.computeIfAbsent(id, ignored -> new RankedMessage());
            ranked.addScore(weight / (RRF_K + index + 1));
            ranked.addMatchType(matchType);
        }
    }

    private List<HybridMailSearchItemResult> orderByRank(
            List<Message> messages,
            Map<UUID, RankedMessage> ranked,
            int limit
    ) {
        Map<UUID, Message> messageById = new LinkedHashMap<>();
        for (Message message : messages) {
            messageById.put(message.getId(), message);
        }
        List<HybridMailSearchItemResult> results = new ArrayList<>();
        for (Map.Entry<UUID, RankedMessage> entry : ranked.entrySet()) {
            Message message = messageById.get(entry.getKey());
            if (message == null) {
                continue;
            }
            RankedMessage score = entry.getValue();
            results.add(new HybridMailSearchItemResult(message, score.matchTypes(), score.score()));
            if (results.size() >= limit) {
                break;
            }
        }
        return results;
    }

    private Map<String, String> findContactNamesByEmails(UUID userId, List<HybridMailSearchItemResult> items) {
        List<String> emails = items.stream()
                .flatMap(item -> participantEmails(item.message()).stream())
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .toList();
        if (emails.isEmpty()) {
            return Map.of();
        }
        return contactRepositoryPort.findAllByUserIdAndEmailInAndDeletedAtIsNull(userId, emails)
                .stream()
                .collect(java.util.stream.Collectors.toMap(Contact::getEmail, Contact::getName));
    }

    private List<String> participantEmails(Message message) {
        List<String> emails = new ArrayList<>();
        emails.add(message.getFromAddress());
        if (message.getToAddresses() != null) {
            emails.addAll(message.getToAddresses());
        }
        return emails;
    }

    private static final class RankedMessage {
        private double score;
        private final EnumSet<HybridMailSearchMatchType> matchTypes = EnumSet.noneOf(HybridMailSearchMatchType.class);

        private void addScore(double value) {
            score += value;
        }

        private void addMatchType(HybridMailSearchMatchType matchType) {
            matchTypes.add(matchType);
        }

        private double score() {
            return score;
        }

        private List<HybridMailSearchMatchType> matchTypes() {
            return List.copyOf(matchTypes);
        }
    }
}
