package com.mailsangja.worker.service.label;

import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.port.LabelRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabelQueryService {

    // ⚠️ Label 재분류 유스케이스에서 mail 도메인(Message) 읽기가 필요해 cross-domain Repository를 함께 참조한다.
    private final LabelRepositoryPort labelRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;

    public List<Label> findAllActiveByUserId(UUID userId) {
        return labelRepositoryPort.findAllByUserIdAndDeletedAtIsNull(userId);
    }

    public Set<UUID> findActiveLabelIdsByUserId(UUID userId) {
        return findAllActiveByUserId(userId).stream()
                .map(Label::getId)
                .collect(Collectors.toSet());
    }

    /**
     * 사용자의 활성 메시지를 labels 컬렉션과 함께 로드한다.
     * hasAttachment 정보는 별도 쿼리로 조회하여 MessageFeatureContext에서 제공한다.
     *
     * @return labels가 초기화된 메시지 목록
     */
    public List<Message> findAllActiveMessagesWithLabelsByUserId(UUID userId) {
        return messageRepositoryPort.findAllByUserIdAndDeletedAtIsNullWithLabels(userId);
    }

    /**
     * 사용자의 메시지 중 첨부파일이 있는 메시지 ID 집합을 반환한다.
     * labels와 attachments를 동시에 fetch join하면 MultipleBagFetchException이 발생하므로 분리한다.
     */
    public Set<UUID> findMessageIdsWithAttachmentsByUserId(UUID userId) {
        List<Message> messagesWithAttachments = messageRepositoryPort.findAllByUserIdAndDeletedAtIsNullWithAttachments(userId);
        return messagesWithAttachments.stream()
                .filter(m -> m.getAttachments() != null && !m.getAttachments().isEmpty())
                .map(Message::getId)
                .collect(Collectors.toSet());
    }
}
