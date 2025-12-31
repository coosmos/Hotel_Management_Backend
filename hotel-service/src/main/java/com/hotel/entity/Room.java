package com.hotel.entity;

import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms",
        uniqueConstraints = @UniqueConstraint(columnNames = {"hotel_id", "room_number"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    private RoomType roomType;

    @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @Column(name = "max_occupancy", nullable = false)
    private Integer maxOccupancy;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "bed_type", length = 50)
    private String bedType; // King, Queen, Twin, etc.

    @Column(name = "room_size") // in sq ft
    private Integer roomSize;

    @Column(columnDefinition = "TEXT")
    private String amenities; // AC, WiFi, TV, Mini Bar, etc.

    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status = RoomStatus.AVAILABLE;
    // For future booking integration
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Audit fields
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Last status change tracking (useful for cleaning schedules, maintenance logs)
    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @Column(name = "status_changed_by")
    private Long statusChangedBy;

    // Helper method to update status with audit trail
    public void updateStatus(RoomStatus newStatus, Long userId) {
        if (this.status != newStatus) {
            this.status = newStatus;
            this.statusChangedAt = LocalDateTime.now();
            this.statusChangedBy = userId;
            this.updatedBy = userId;
        }
    }
}