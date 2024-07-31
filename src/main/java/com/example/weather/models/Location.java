package com.example.weather.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;

import java.util.Objects;

@Data
@ToString(exclude = "user")
@Entity
@Table(name = "locations")
public class Location {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Objects.equals(name, location.name) && Objects.equals(user, location.user) && Objects.equals(longitude, location.longitude) && Objects.equals(latitude, location.latitude);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, user, longitude, latitude);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @NotNull
    @Column(name = "longitude")
    private Double longitude;

    @NotNull
    @Column(name = "latitude")
    private Double latitude;

    @NotNull
    @Column(name = "country")
    private String country;

    @Column(name = "state")
    private String state;
}