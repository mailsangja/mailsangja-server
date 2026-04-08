package com.mailsangja.worker.controller;

import com.mailsangja.worker.dto.gmail.GooglePubsubPushRequest;
import com.mailsangja.worker.facade.GmailPushFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GmailPushController {

    private final GmailPushFacade gmailPushFacade;

    @PostMapping("/api/v1/gmail/push")
    public ResponseEntity<Void> handlePush(@RequestBody GooglePubsubPushRequest request) {
        gmailPushFacade.handlePush(request);
        return ResponseEntity.noContent().build();
    }
}
