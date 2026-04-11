package com.mailsangja.worker.controller;

import com.mailsangja.worker.controller.docs.GmailPushControllerDocs;
import com.mailsangja.worker.dto.gmail.GooglePubsubPushRequest;
import com.mailsangja.worker.facade.GmailPushFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GmailPushController implements GmailPushControllerDocs {

    private final GmailPushFacade gmailPushFacade;

    @Override
    @PostMapping("/api/v1/gmail/push")
    public ResponseEntity<Void> handlePush(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody GooglePubsubPushRequest request
    ) {
        gmailPushFacade.handlePush(authorization, request);
        return ResponseEntity.noContent().build();
    }
}
