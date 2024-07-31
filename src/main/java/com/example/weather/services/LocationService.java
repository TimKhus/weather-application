package com.example.weather.services;

import com.example.weather.models.Location;
import com.example.weather.models.User;
import com.example.weather.repositories.LocationRepository;
import com.example.weather.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LocationService {
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;

    @Autowired
    LocationService(LocationRepository locationRepository,
                    UserRepository userRepository) {
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
    }

    public List<Location> getAllLocationsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found, id: %s".formatted(userId)));
        return user.getLocations();
    }

    public Optional<Location> getLocationByName(String name) {
        return locationRepository.findByName(name);
    }

    public Optional<Location> getLocationById(Long id) {
        return locationRepository.findById(id);
    }

    public Location saveLocation(Long userId, Location location) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found, id: %s".formatted(userId)));
        location.setUser(user);
        return locationRepository.save(location);
    }

    public String deleteLocation(Long id) {
        Location locationToDelete = locationRepository.findById(id).orElseThrow();
        String deleteMessage = String.format("Location with id %d and name %s was successfully deleted",
                id, locationToDelete.getName());
        locationRepository.deleteById(id);
        return deleteMessage;
    }
}
