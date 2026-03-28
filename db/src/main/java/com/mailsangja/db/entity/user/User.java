package com.mailsangja.db.entity.user;

import com.mailsangja.db.entity.common.BaseEntity;
import com.mailsangja.db.entity.user.enums.Plan;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    private Plan plan;

    @Column(name = "credit_usage", nullable = false)
    private int creditUsage;

    @Column(name = "default_account", columnDefinition = "CHAR(36)")
    private UUID defaultAccount;

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updatePlan(Plan plan) {
        this.plan = plan;
    }

    public void updateDefaultAccount(UUID defaultAccount) {
        this.defaultAccount = defaultAccount;
    }

    public void addCreditUsage(int amount) {
        this.creditUsage += amount;
    }
}
