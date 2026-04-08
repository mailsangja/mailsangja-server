package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.mail.MailAccountAuthorizeRequest;
import com.mailsangja.core.dto.mail.MailAccountAuthorizeResponse;
import com.mailsangja.core.dto.mail.MailAccountListResponse;
import com.mailsangja.db.entity.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Mail Account", description = "Mail account integration API")
public interface MailAccountControllerDocs {

    @Operation(
            summary = "Get my mail accounts",
            description = "Returns all non-deleted mail accounts connected by the authenticated user.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Mail account list retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MailAccountListResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<List<MailAccountListResponse>> getMyMailAccounts(
            @Parameter(hidden = true) @AuthUser User user
    );

    @Operation(
            summary = "Create Google OAuth authorize URL",
            description = "Stores OAuth session data for the authenticated user and returns the Google OAuth authorize URL.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authorize URL created successfully",
                    content = @Content(schema = @Schema(implementation = MailAccountAuthorizeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<MailAccountAuthorizeResponse> authorizeGoogle(
            @Parameter(hidden = true) @AuthUser User user,
            @ParameterObject MailAccountAuthorizeRequest request,
            @Parameter(hidden = true) HttpSession session
    );

    @Operation(
            summary = "Handle Google OAuth callback",
            description = "Validates the Google OAuth code and state, creates the mail account, and redirects to callbackRedirectUri.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "302",
                    description = "Mail account connected successfully and redirected to callbackRedirectUri",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid code, state, alias, icon, color, OAuth response, or missing refresh token",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "OAuth session missing or authentication required",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "OAuth initiating user does not match the authenticated user",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Mail account is already connected",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Google OAuth token exchange or user info lookup failed",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<Void> googleCallback(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "Google OAuth authorization code", required = true, example = "4/0AQSTgQ...")
            String code,
            @Parameter(description = "OAuth state stored in the session", required = true, example = "c4c6f8c2-3b2b-4c5b-9f2c-7a1d3f9a9f11")
            String state,
            @Parameter(hidden = true) HttpSession session
    );
}
