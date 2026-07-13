package com.likelionknu.notdesign.common.security;

import com.likelionknu.notdesign.common.security.exception.AuthenticationInfoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {
    public static String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AuthenticationInfoException();
        }
        return authentication.getName();
    }
}
