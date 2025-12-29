package com.app.entity;

import com.app.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "hotel_id")
    private Long hotelId;

    @Column(length = 100)
    private String fullName;

    @Column(length = 15)
    private String phoneNumber;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    public Long getId() {return id;}
    public String getUsername() {return username;}
    public String getEmail() {return email;}
    public String getPassword() {return password;}
    public Role getRole() {return role;}
    public Long getHotelId() {return hotelId;}
    public String getFullName() {return fullName;}
    public String getPhoneNumber() {return phoneNumber;}
    public Boolean getActive() {return active;}
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
