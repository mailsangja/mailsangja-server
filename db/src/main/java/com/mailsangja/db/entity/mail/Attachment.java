package com.mailsangja.db.entity.mail;

import com.mailsangja.db.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "attachments")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Attachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    // Gmail attachment ID (다운로드 시 사용)
    @Column(name = "gmail_attachment_id", length = 1024)
    private String gmailAttachmentId;

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "mime_type", nullable = false, length = 255)
    private String mimeType;

    // 첨부파일 크기 (bytes)
    @Column(name = "size")
    private Integer size;
}
