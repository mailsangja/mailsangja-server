package com.mailsangja.db.entity.label;

import com.mailsangja.db.entity.mail.Message;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "message_labels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageLabel {

    @EmbeddedId
    private MessageLabelId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("messageId")
    @JoinColumn(name = "message_id")
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("labelId")
    @JoinColumn(name = "label_id")
    private Label label;

    public static MessageLabel of(Message message, Label label) {
        MessageLabel ml = new MessageLabel();
        ml.id = new MessageLabelId(message.getId(), label.getId());
        ml.message = message;
        ml.label = label;
        return ml;
    }
}
