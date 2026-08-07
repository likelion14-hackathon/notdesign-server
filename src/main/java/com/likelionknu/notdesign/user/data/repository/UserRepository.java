package com.likelionknu.notdesign.user.data.repository;

import com.likelionknu.notdesign.user.data.entity.User;
import com.likelionknu.notdesign.user.data.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(SocialProvider provider, String providerId);
}
