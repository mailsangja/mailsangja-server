package com.mailsangja.worker.handler.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import com.mailsangja.worker.service.trash.GmailHistoryDeleteApplyCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageRestoredHistoryEventHandlerTest {

    @Mock private MailAccountQueryService mailAccountQueryService;
    @Mock private GmailHistoryDeleteApplyCommandService gmailHistoryDeleteApplyCommandService;

    private MessageRestoredHistoryEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MessageRestoredHistoryEventHandler(mailAccountQueryService, gmailHistoryDeleteApplyCommandService);
    }

    @Test
    void handle_동기화가능계정을조회해복원을위임한다() {
        UUID mailAccountId = UUID.randomUUID();
        MailAccount mailAccount = MailAccount.builder().id(mailAccountId).build();
        GmailHistoryEvent event = new GmailHistoryEvent(
                GmailHistoryEventType.MESSAGE_RESTORED,
                mailAccountId,
                "message-1",
                "thread-1",
                "history-1"
        );

        when(mailAccountQueryService.findSyncableMailAccountById(mailAccountId)).thenReturn(mailAccount);

        handler.handle(event);

        assertEquals(GmailHistoryEventType.MESSAGE_RESTORED, handler.supports());
        verify(gmailHistoryDeleteApplyCommandService).applyMessageRestored(mailAccount, event);
    }
}
