package com.mailsangja.db.entity.label;

import com.mailsangja.db.entity.common.BaseEntity;
import com.mailsangja.db.entity.mail.Thread;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "thread_labels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ThreadLabel extends BaseEntity {

    @EmbeddedId
    private ThreadLabelId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("threadId")
    @JoinColumn(name = "thread_id")
    private Thread thread;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("labelId")
    @JoinColumn(name = "label_id")
    private Label label;

    public static ThreadLabel of(Thread thread, Label label) {
        ThreadLabel tl = new ThreadLabel();
        tl.id = new ThreadLabelId(thread.getId(), label.getId());
        tl.thread = thread;
        tl.label = label;
        return tl;
    }
}
