package com.mailsangja.core.service.ai.draft;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.core.dto.mail.MailDraftCommand;
import com.mailsangja.core.dto.mail.MailDraftPromptResult;
import com.mailsangja.core.dto.mail.MailDraftRagContextResult;
import com.mailsangja.core.dto.mail.MailDraftSearchContextResult;
import com.mailsangja.db.port.MailDraftReferenceQueryPort;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MailDraftQueryService {

    private final MailDraftReferenceQueryPort referenceQueryPort;
    private final VectorStore vectorStore;

    public MailDraftQueryService() {
        this(null, null);
    }

    public MailDraftQueryService(VectorStore vectorStore) {
        this(null, vectorStore);
    }

    public MailDraftQueryService(MailDraftReferenceQueryPort referenceQueryPort) {
        this(referenceQueryPort, null);
    }

    public MailDraftQueryService(MailDraftReferenceQueryPort referenceQueryPort, VectorStore vectorStore) {
        this.referenceQueryPort = referenceQueryPort;
        this.vectorStore = vectorStore;
    }

    public void validatePromptInjection(String query) {
        String normalized = query.toLowerCase();
        if (isPromptInjection(normalized)) {
            throw new MailDraftException(MailDraftErrorCode.PROMPT_INJECTION_DETECTED);
        }
    }

    public List<MailDraftSearchContextResult> searchOwnWrittenMessages(UUID userId, UUID mailAccountId, String query, int limit) {
        if (vectorStore == null) {
            return List.of();
        }
        vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(limit).build());
        return List.of();
    }

    public MailDraftPromptResult generalPrompt(MailDraftCommand command, MailDraftRagContextResult context) {
        return new MailDraftPromptResult("general draft", buildUserPrompt(command, context));
    }

    public MailDraftPromptResult replyPrompt(MailDraftCommand command, MailDraftRagContextResult context) {
        return new MailDraftPromptResult("reply draft", buildUserPrompt(command, context));
    }

    public MailDraftRagContextResult generalRagContext(MailDraftCommand command) {
        List<MailDraftSearchContextResult> recent = findRecent(command);
        List<MailDraftSearchContextResult> relevant = findGeneralRelevant(command);
        return MailDraftRagContextResult.of(recent, relevant, List.of());
    }

    public MailDraftRagContextResult replyRagContext(MailDraftCommand command) {
        List<MailDraftSearchContextResult> recent = findRecent(command);
        List<MailDraftSearchContextResult> thread = findThread(command.replyMessageId());
        return MailDraftRagContextResult.of(recent, List.of(), thread);
    }

    private boolean isPromptInjection(String query) {
        return query.contains("ignore all previous")
                || query.contains("hidden context")
                || query.contains("token map");
    }

    private String buildUserPrompt(MailDraftCommand command, MailDraftRagContextResult context) {
        StringBuilder builder = new StringBuilder(command.maskedQuery());
        for (MailDraftSearchContextResult message : context.referenceMessages()) {
            appendMessage(builder, message);
        }
        return builder.toString();
    }

    private void appendMessage(StringBuilder builder, MailDraftSearchContextResult message) {
        builder.append('\n');
        builder.append(message.subject());
        builder.append('\n');
        builder.append(message.body());
    }

    private List<MailDraftSearchContextResult> findGeneralRelevant(MailDraftCommand command) {
        List<MailDraftSearchContextResult> own = searchOwn(command);
        if (own.size() >= 8) {
            return own;
        }
        return mergeRelevant(own, searchOther(command));
    }

    private List<MailDraftSearchContextResult> mergeRelevant(List<MailDraftSearchContextResult> own, List<MailDraftSearchContextResult> other) {
        List<MailDraftSearchContextResult> merged = new ArrayList<>(own);
        merged.addAll(other);
        return merged;
    }

    @SuppressWarnings("unchecked")
    private List<MailDraftSearchContextResult> findRecent(MailDraftCommand command) {
        if (referenceQueryPort == null) {
            return List.of();
        }
        return referenceQueryPort.findRecentWrittenMessages(command.userId(), command.mailAccountId(), 6);
    }

    @SuppressWarnings("unchecked")
    private List<MailDraftSearchContextResult> searchOwn(MailDraftCommand command) {
        if (referenceQueryPort == null) {
            return List.of();
        }
        return referenceQueryPort.searchOwnWrittenMessages(command.userId(), command.mailAccountId(), command.maskedQuery(), 8);
    }

    @SuppressWarnings("unchecked")
    private List<MailDraftSearchContextResult> searchOther(MailDraftCommand command) {
        if (referenceQueryPort == null) {
            return List.of();
        }
        return referenceQueryPort.searchOtherRelevantMessages(command.userId(), command.mailAccountId(), command.maskedQuery(), 3);
    }

    @SuppressWarnings("unchecked")
    private List<MailDraftSearchContextResult> findThread(UUID replyMessageId) {
        if (referenceQueryPort == null) {
            return List.of();
        }
        return referenceQueryPort.findThreadContextMessages(replyMessageId);
    }
}
