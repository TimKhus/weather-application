package com.example.weather.services;

import com.example.weather.models.User;
import com.example.weather.repositories.UserRepository;
import com.example.weather.config.MyUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WeatherAppUserDetailService implements UserDetailsService {

    @Autowired
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String username = email.toLowerCase();
        Optional<User> user = userRepository.findByEmail(username);
                return user.map(MyUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User %s not found".formatted(username)));
    }
}
