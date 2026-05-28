package com.mailsangja.core.facade;

import com.mailsangja.core.dto.mail.MailReviewCommand;
import com.mailsangja.core.dto.mail.MailReviewRequest;
import com.mailsangja.core.dto.mail.MailReviewResponse;
import com.mailsangja.core.dto.mail.MailReviewResult;
import com.mailsangja.core.service.ai.review.MailReviewCommandService;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailReviewFacade {

    private final MailReviewCommandService mailReviewCommandService;

    public MailReviewResponse review(User user, MailReviewRequest request) {
        MailReviewResult result = mailReviewCommandService.review(MailReviewCommand.of(user.getId(), request), user.getPlan());
        return MailReviewResponse.from(result);
    }
}
