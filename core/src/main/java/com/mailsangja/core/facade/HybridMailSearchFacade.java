package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.inbox.InboxErrorCode;
import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.core.dto.search.HybridMailSearchItemResponse;
import com.mailsangja.core.dto.search.HybridMailSearchResponse;
import com.mailsangja.core.dto.search.HybridMailSearchResult;
import com.mailsangja.core.dto.search.HybridMailSearchScope;
import com.mailsangja.core.service.search.HybridMailSearchQueryService;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HybridMailSearchFacade {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final HybridMailSearchQueryService hybridMailSearchQueryService;

    public HybridMailSearchResponse search(
            User user,
            String query,
            HybridMailSearchScope scope,
            UUID mailAccountId,
            List<UUID> labelIds,
            Boolean read,
            Integer size
    ) {
        validateRequest(user, query, size);
        HybridMailSearchResult result = hybridMailSearchQueryService.search(
                user.getId(),
                query.trim(),
                scope == null ? HybridMailSearchScope.ALL : scope,
                mailAccountId,
                labelIds,
                read,
                resolveSize(size)
        );
        return HybridMailSearchResponse.of(result.items().stream()
                .map(item -> HybridMailSearchItemResponse.from(item, result.contactNameByEmail()))
                .toList());
    }

    private void validateRequest(User user, String query, Integer size) {
        if (user == null || query == null || query.isBlank() || resolveSize(size) <= 0 || resolveSize(size) > MAX_SIZE) {
            throw new InboxException(InboxErrorCode.INVALID_SEARCH_REQUEST);
        }
    }

    private int resolveSize(Integer size) {
        return size == null ? DEFAULT_SIZE : size;
    }
}
