package com.mailsangja.core.service.google;

import com.mailsangja.core.config.properties.GoogleOAuthProperties;
import com.mailsangja.core.dto.mail.GoogleMailAccountResult;
import com.mailsangja.core.dto.mail.GoogleOAuthTokenResult;
import com.mailsangja.core.dto.mail.GoogleUserInfoResult;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleOAuthQueryServiceTest {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Test
    void getGoogleMailAccountResult_토큰만료시각을KST기준으로계산한다() {
        GoogleOAuthQueryService service = new FakeGoogleOAuthQueryService();
        LocalDateTime lowerBound = LocalDateTime.now(KST_ZONE_ID).plusSeconds(3600);

        GoogleMailAccountResult result = service.getGoogleMailAccountResult("auth-code");

        LocalDateTime upperBound = LocalDateTime.now(KST_ZONE_ID).plusSeconds(3600);

        assertEquals("user@example.com", result.emailAddress());
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        assertTrue(!result.accessTokenExpiresAt().isBefore(lowerBound));
        assertTrue(!result.accessTokenExpiresAt().isAfter(upperBound));
    }

    private static final class FakeGoogleOAuthQueryService extends GoogleOAuthQueryService {

        private FakeGoogleOAuthQueryService() {
            super(new GoogleOAuthProperties(), RestClient.builder().build());
        }

        @Override
        public GoogleOAuthTokenResult exchangeCodeForToken(String code) {
            return new GoogleOAuthTokenResult("access-token", "refresh-token", 3600L, "scope", "Bearer");
        }

        @Override
        public GoogleUserInfoResult fetchUserInfo(String accessToken) {
            return new GoogleUserInfoResult("user@example.com", true);
        }
    }
}
