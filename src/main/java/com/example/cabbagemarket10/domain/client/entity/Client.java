package com.example.cabbagemarket10.domain.client.entity;

import com.example.cabbagemarket10.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "client",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_client_email",
                columnNames = "email"
        )
)
@SQLDelete(sql = "UPDATE client SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Client extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(nullable = false)
    private String name;

    @Column
    private String phone;

    @Column(nullable = false)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(nullable = false)
    private boolean isVerified;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Builder
    private Client(
            String email,
            String password,
            String nickname,
            String name,
            String phone
    ) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.name = name;
        this.phone = phone;
        this.profileImageUrl = "";
        this.status = AccountStatus.ACTIVE;
        this.isVerified = false;
        this.isDeleted = false;
    }

    public static Client create(
            String email,
            String encodedPassword,
            String nickname,
            String name,
            String phone
    ) {
        return Client.builder()
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .name(name)
                .phone(phone)
                .build();
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }
}