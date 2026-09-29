package com.example.shopping.user.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EntityListeners;

import com.example.shopping.user.listener.UserEntityListener;

import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DynamicUpdate
@EntityListeners(UserEntityListener.class)
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, name = "full_name")
    private String fullName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean emailVerificationRequired = false;

    @Column(name = "role")
    @Builder.Default
    private String role = "USER";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "avatar_url", length = 2048)
    private String avatarUrl;

    @Column(length = 30)
    private String phone;

    @Column(name = "date_of_birth")
    private java.time.LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    @Column(length = 500)
    private String address;

    @Column(name = "employee_id", length = 50, unique = true)
    private String employeeId;

    @Column
    private String department;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "office_location")
    private String officeLocation;

    @Column
    private String manager;

    @Builder.Default
    @Column(name = "employee_status", nullable = false, length = 20)
    private String employeeStatus = "ACTIVE";

   
}
