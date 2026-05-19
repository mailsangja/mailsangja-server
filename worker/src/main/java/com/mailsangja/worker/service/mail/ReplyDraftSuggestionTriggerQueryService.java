package com.mailsangja.worker.service.mail;

import com.mailsangja.worker.config.properties.ReplyDraftSuggestionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReplyDraftSuggestionTriggerQueryService {

    private final ReplyDraftSuggestionProperties replyDraftSuggestionProperties;

    public boolean isEligible(int threadMessageCount) {
        return threadMessageCount >= replyDraftSuggestionProperties.getMinThreadMessageCount();
    }
}
