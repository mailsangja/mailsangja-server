package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.ai.AiPlaygroundErrorCode;
import com.mailsangja.core.common.exception.ai.AiPlaygroundException;
import com.mailsangja.core.dto.ai.AiPlaygroundChatRequest;
import com.mailsangja.core.dto.ai.AiPlaygroundChatResponse;
import com.mailsangja.core.dto.ai.AiModelListResponse;
import com.mailsangja.core.service.ai.AiPlaygroundCommandService;
import com.mailsangja.core.service.ai.AiQueryService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiFacade {

    private final AiPlaygroundCommandService aiPlaygroundCommandService;
    private final AiQueryService aiQueryService;
    private final MailAccountQueryService mailAccountQueryService;

    public AiPlaygroundChatResponse chat(User user, AiPlaygroundChatRequest request) {
        validateAdmin(user);
        validateRegisteredMailAccount(user);
        return AiPlaygroundChatResponse.from(aiPlaygroundCommandService.chat(request));
    }

    public AiModelListResponse getModels() {
        return AiModelListResponse.from(aiQueryService.getModels());
    }

    private void validateAdmin(User user) {
        if (user.getRole() != Role.ADMIN) {
            throw new AiPlaygroundException(AiPlaygroundErrorCode.FORBIDDEN_USER);
        }
    }

    private void validateRegisteredMailAccount(User user) {
        if (mailAccountQueryService.findAllActiveByUserId(user.getId()).isEmpty()) {
            throw new AiPlaygroundException(AiPlaygroundErrorCode.MAIL_ACCOUNT_REQUIRED);
        }
    }
}
