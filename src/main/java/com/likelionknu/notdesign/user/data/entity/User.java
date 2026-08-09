package com.likelionknu.notdesign.user.data.entity;

import com.likelionknu.notdesign.user.data.enums.SocialProvider;
import com.likelionknu.notdesign.user.data.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "user",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_provider",
                columnNames = {"provider", "provider_id"}
        )
)
public class User implements UserDetails {
    private static final long serialVersionUID = 6014984039564979072L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column
    private SocialProvider provider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "measurement_agreed_at")
    private LocalDateTime measurementAgreedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void agreeMeasurement() {
        this.measurementAgreedAt = LocalDateTime.now();
    }

    /**
     * 소셜 로그인 시 최신 프로필(닉네임)로 갱신한다.
     * 카카오 등 외부 계정의 닉네임은 변경될 수 있으므로 로그인마다 최신값으로 맞춘다.
     *
     * @param name 최신 닉네임
     */
    public void updateProfile(String name) {
        this.name = name;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.role.getValue()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
