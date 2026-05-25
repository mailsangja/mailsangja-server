package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.ai.AiPlaygroundErrorCode;
import com.mailsangja.core.common.exception.ai.AiPlaygroundException;
import com.mailsangja.core.dto.ai.AiModelListResponse;
import com.mailsangja.core.dto.ai.AiPlaygroundChatRequest;
import com.mailsangja.core.dto.ai.AiPlaygroundChatResponse;
import com.mailsangja.core.dto.ai.AiUsageItemResponse;
import com.mailsangja.core.dto.ai.AiUsageListResponse;
import com.mailsangja.core.dto.ai.AiUsageType;
import com.mailsangja.core.service.ai.AiPlaygroundCommandService;
import com.mailsangja.core.service.ai.AiQueryService;
import com.mailsangja.core.service.ai.AiUsageQueryService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AiFacade {

    private final AiPlaygroundCommandService aiPlaygroundCommandService;
    private final AiQueryService aiQueryService;
    private final AiUsageQueryService aiUsageQueryService;
    private final MailAccountQueryService mailAccountQueryService;

    public AiPlaygroundChatResponse chat(User user, AiPlaygroundChatRequest request) {
        validateAdmin(user);
        validateRegisteredMailAccount(user);
        return AiPlaygroundChatResponse.from(aiPlaygroundCommandService.chat(user.getId(), request));
    }

    public AiModelListResponse getModels() {
        return AiModelListResponse.from(aiQueryService.getModels());
    }


    private void validateAdmin(User user) {
        if (user.getRole() != Role.ADMIN) {
            throw new AiPlaygroundException(AiPlaygroundErrorCode.FORBIDDEN_USER);
        }
    }

    public AiUsageListResponse getUsages(User user, List<AiUsageType> types) {
        Set<AiUsageType> requestedTypes = resolveRequestedTypes(types);
        List<AiUsageItemResponse> usages = new ArrayList<>();
        if (requestedTypes.contains(AiUsageType.MAIL_DRAFT)) {
            usages.add(aiUsageQueryService.getMailDraftUsage(user.getId()));
        }
        if (requestedTypes.contains(AiUsageType.MAIL_REVIEW)) {
            usages.add(aiUsageQueryService.getMailReviewUsage(user.getId()));
        }
        if (requestedTypes.contains(AiUsageType.LABEL_SUGGESTION)) {
            usages.add(aiUsageQueryService.getLabelSuggestionUsage(user.getId()));
        }
        return AiUsageListResponse.of(usages);
    }

    private Set<AiUsageType> resolveRequestedTypes(List<AiUsageType> types) {
        if (types == null || types.isEmpty()) {
            return EnumSet.allOf(AiUsageType.class);
        }
        return EnumSet.copyOf(types);
    }

    private void validateRegisteredMailAccount(User user) {
        if (mailAccountQueryService.findAllActiveByUserId(user.getId()).isEmpty()) {
            throw new AiPlaygroundException(AiPlaygroundErrorCode.MAIL_ACCOUNT_REQUIRED);
        }
    }
}
