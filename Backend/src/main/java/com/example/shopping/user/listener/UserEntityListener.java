package com.example.shopping.user.listener;

import java.time.LocalDateTime;
import jakarta.persistence.PrePersist;
import com.example.shopping.user.entity.UserEntity;
import com.example.shopping.user.normalization.UserNameNormalizer;

public class UserEntityListener {
    @PrePersist
    public void onCreate(UserEntity user) {
        user.setFullName(UserNameNormalizer.resolveFullName(user.getFullName(), user.getFirstName(), user.getLastName()));
        user.setCreatedAt(LocalDateTime.now());
    }
}
