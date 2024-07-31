package com.example.weather.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserDTO {
    private String role;
    @NotNull
    @Email(message = "Email should be valid")
    private String email;
    @NotNull
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,}$",
            message = "Password must be at least 6 characters long and contain at least one lowercase letter, " +
                    "one uppercase letter, and one digit.")
    private String password;
}
