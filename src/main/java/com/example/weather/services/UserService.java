package com.example.weather.services;

import com.example.weather.DTO.UserDTO;
import com.example.weather.models.User;
import com.example.weather.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User saveUser(UserDTO userDTO) {
        User userToSave = new User();
        userToSave.setEmail(userDTO.getEmail().toLowerCase());
        userToSave.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        userToSave.setRole("ROLE_USER");
        return userRepository.save(userToSave);
    }

    public String deleteUser(Long id) {
        User userToDelete = userRepository.findById(id).orElseThrow();
        String deleteMessage = String.format("User with id %d and email %s was successfully deleted",
                id, userToDelete.getEmail());
        userRepository.deleteById(id);
        return deleteMessage;
    }

    public User updateUserRole(Long userId, UserDTO userDTO) {
        User userToUpdate = userRepository.findById(userId).orElseThrow();
        userToUpdate.setRole(userDTO.getRole());
        return userRepository.save(userToUpdate);
    }

    public User updateUserPassword(Long userId, UserDTO userDTO) {
        User userToUpdate = userRepository.findById(userId).orElseThrow();
        userToUpdate.setPassword(userDTO.getPassword());
        return userRepository.save(userToUpdate);
    }
}
