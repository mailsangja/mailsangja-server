package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.dto.auth.UserInfoResponse;
import com.mailsangja.core.dto.user.UpdateDefaultAccountRequest;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.core.service.user.UserCommandService;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserCommandService userCommandService;
    private final MailAccountQueryService mailAccountQueryService;

    public UserInfoResponse getUserInfo(User user) {
        return UserInfoResponse.from(user);
    }

    public void updateDefaultAccount(User user, UpdateDefaultAccountRequest request) {
        MailAccount mailAccount = mailAccountQueryService.findActiveById(request.mailAccountId());

        if (!mailAccount.getUser().getId().equals(user.getId())) {
            throw new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_NOT_FOUND);
        }
        userCommandService.updateDefaultAccount(user, request.mailAccountId());
    }
}
