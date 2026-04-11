package com.mailsangja.worker.scheduler;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.config.properties.GmailWatchRenewalProperties;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GmailWatchRenewalScheduler {

    private final GmailWatchRenewalProperties gmailWatchRenewalProperties;
    private final MailAccountQueryService mailAccountQueryService;

    @Scheduled(cron = "${mailsangja.gmail.watch-renewal.cron}")
    public void scheduleRenewalTargets() {
        if (!gmailWatchRenewalProperties.isEnabled()) {
            return;
        }

        LocalDateTime renewalThreshold = mailAccountQueryService.getKstNow()
                .plus(gmailWatchRenewalProperties.getRenewalWindow());

        List<MailAccount> targetMailAccounts = mailAccountQueryService.findRenewalTargetGmailAccounts(
                renewalThreshold,
                gmailWatchRenewalProperties.getBatchSize()
        );

        log.info(
                "Discovered Gmail watch renewal targets count={} renewalThreshold={} batchSize={}",
                targetMailAccounts.size(),
                renewalThreshold,
                gmailWatchRenewalProperties.getBatchSize()
        );
    }
}
