package com.hotel.entity;

import com.hotel.enums.HotelStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hotels")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false)
    private String address;
    @Column(nullable = false, length = 100)
    private String city;
    @Column(nullable = false, length = 100)
    private String state;
    @Column(nullable = false, length = 100)
    private String country;
    @Column(length = 10)
    private String pincode;
    @Column(name = "contact_number", length = 15)
    private String contactNumber;
    @Column(name = "email", length = 100)
    private String email;
    @Column(name = "star_rating")
    private Integer starRating; // 1-5
    @Column(columnDefinition = "TEXT")
    private String amenities; // Comma-separated or JSON string
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HotelStatus status = HotelStatus.ACTIVE;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    // One-to-Many relationship with Room
//    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    @OneToMany(mappedBy = "hotel")
    private List<Room> rooms = new ArrayList<>();
    // Total rooms count
    @Column(name = "total_rooms")
    private Integer totalRooms = 0;

    @Column(name = "available_rooms")
    private Integer availableRooms = 0;

    // Helper methods for bidirectional relationship
    public void addRoom(Room room) {
        rooms.add(room);
        room.setHotel(this);
        this.totalRooms = rooms.size();
    }

    public void removeRoom(Room room) {
        rooms.remove(room);
        room.setHotel(null);
        this.totalRooms = rooms.size();
    }
}