package com.mailsangja.db.entity.label;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MessageLabelId implements Serializable {

    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "label_id")
    private UUID labelId;
}
