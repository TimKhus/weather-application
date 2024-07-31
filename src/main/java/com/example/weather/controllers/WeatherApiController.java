package com.example.weather.controllers;

import com.example.weather.DTO.ChangePasswordDTO;
import com.example.weather.DTO.LocationDTO;
import com.example.weather.DTO.UserDTO;
import com.example.weather.models.Location;
import com.example.weather.models.User;
import com.example.weather.models.openweather.SearchResult;
import com.example.weather.models.openweather.WeatherData;
import com.example.weather.services.LocationService;
import com.example.weather.services.UserService;
import com.example.weather.services.WeatherApiClient;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class WeatherApiController {
    private final WeatherApiClient weatherApiClient;
    private final LocationService locationService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    WeatherApiController(WeatherApiClient weatherApiClient, LocationService locationService,
                         UserService userService, PasswordEncoder passwordEncoder) {
        this.weatherApiClient = weatherApiClient;
        this.locationService = locationService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }


    @GetMapping(path = "/weather/location/{locationId}")
    public String getWeatherData(@PathVariable Long locationId,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        String userEmail = userDetails.getUsername();
        Optional<User> userOptional = userService.getUserByEmail(userEmail);
        Optional<Location> locationOptional = locationService.getLocationById(locationId);

        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "User with email %s not found".formatted(userEmail));
            return "redirect:/weather/locations";
        }

        if (locationOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "Location with ID %d not found".formatted(locationId));
            return "redirect:/weather/locations";
        }

        User user = userOptional.get();
        Location location = locationOptional.get();
        Optional<Location> userLocationOptional = user.getLocations()
                .stream()
                .filter(loc -> Objects.equals(loc.getId(), locationId))
                .findFirst();

        if (userLocationOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "Location with ID %s not found for user %s".formatted(locationId, userEmail));
            return "redirect:/weather/locations";
        }

        WeatherData weatherData = weatherApiClient.getWeatherData(location);
        model.addAttribute("location", location);
        model.addAttribute("weatherData", weatherData);
        return "weather_data";
    }

    @GetMapping(path = "/search")
    public String showSearchLocationPage(Model model) {
        model.addAttribute("locationName", "");
        return "search";
    }

    @GetMapping(path = "/search/location")
    public String showSearchLocationResultsPage(@RequestParam String locationName,
                                                @AuthenticationPrincipal UserDetails userDetails,
                                                RedirectAttributes redirectAttributes,
                                                Model model) {
        if (StringUtils.isEmpty(locationName) || userDetails == null) {
            redirectAttributes.addFlashAttribute("error",
                    "Location name or user details are missing.");
            return "redirect:/search";
        }

        try {
            String encodedLocationName = locationName.replaceAll("\\s", "_");
            ResponseEntity<List<SearchResult>> response = weatherApiClient.getLocationByName(encodedLocationName);
            if (response == null || response.getBody() == null || response.getBody().isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                        "No search results found for the location");
                return "redirect:/search";
            }
            List<SearchResult> searchResults = response.getBody();
            Optional<User> userOptional = userService.getUserByEmail(userDetails.getUsername());
            if (userOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/search";
            }
            Long userId = userOptional.get().getId();
            List<LocationDTO> locationDTOList = searchResults.stream()
                    .map(searchResult -> LocationDTO.mapFromSearchResult(searchResult, userId)).toList();
            model.addAttribute("searchResults", locationDTOList);
            return "search_results";
        } catch (EntityNotFoundException exception) {
            redirectAttributes.addFlashAttribute(locationName);
            redirectAttributes.addFlashAttribute("error",
                    "Bad request. Please enter location full name");
            return "redirect:/search";
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                redirectAttributes.addFlashAttribute("error",
                        "No results found for the location");
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "An error occurred while searching the location");
            }
            return "redirect:/search";
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred");
            return "redirect:/search";
        }
    }

    @GetMapping(path = "/weather/locations")
    public String getAllLocationsForCurrentUser(@AuthenticationPrincipal UserDetails userDetails,
                                                Model model) {
        User user = userService.getUserByEmail(userDetails.getUsername()).orElseThrow();
        List<Location> locations = locationService.getAllLocationsForUser(user.getId());
        boolean isAdmin = userDetails.getAuthorities().toArray()[0].toString().equals("ROLE_ADMIN");
        model.addAttribute("locations", locations);
        model.addAttribute("userId", user.getId());
        model.addAttribute("username", user.getEmail().split("@")[0]);
        model.addAttribute("isAdmin", isAdmin);
        return "locations";
    }

    @GetMapping(path = "/location/id/{locationId}")
    public ResponseEntity<Location> getLocationById(@PathVariable Long locationId) {
        Location location = locationService.getLocationById(locationId).orElseThrow(
                () -> new EntityNotFoundException(
                        String.format("Location with id %d not found", locationId)));
        return new ResponseEntity<>(location, HttpStatus.OK);
    }

    @PostMapping(path = "/location")
    public String saveLocation(@AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes,
                               @ModelAttribute LocationDTO locationDTO,
                               Model model) {
        try {
            Location locationToSave = new Location();
            locationToSave.setName(locationDTO.getName());
            User user = userService.getUserByEmail(userDetails.getUsername()).orElseThrow();
            locationToSave.setUser(user);
            locationToSave.setLatitude(locationDTO.getLatitude());
            locationToSave.setLongitude(locationDTO.getLongitude());
            locationToSave.setCountry(locationDTO.getCountry());
            locationToSave.setState(locationDTO.getState());
            boolean locationExists = user.getLocations().stream()
                    .anyMatch(location -> location.equals(locationToSave));
            if (locationExists) {
                redirectAttributes.addFlashAttribute("error",
                        "Location %s, %s already exists in your list".formatted(locationToSave.getName(),
                                locationToSave.getCountry()));
                Optional<Location> locationOptional = user.getLocations().stream()
                        .filter(location -> location.equals(locationToSave))
                        .findFirst();
                if (locationOptional.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error",
                            "An unexpected error occurred, please try again");
                    return "redirect:/search";
                }
                return "redirect:/weather/location/%d".formatted(locationOptional.get().getId());
            }
            locationService.saveLocation(user.getId(), locationToSave);
            return "redirect:/weather/location/%d".formatted(locationToSave.getId());
        } catch (NullPointerException exception) {
            model.addAttribute(locationDTO);
            model.addAttribute("error", exception.getMessage());
            return "/search";
        }
    }

    @PostMapping(path = "/location/delete/{locationId}")
    public String deleteLocationById(@PathVariable Long locationId,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {

        Optional<Location> locationOptional = locationService.getLocationById(locationId);
        if (locationOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Location not found");
            return "redirect://weather/locations";
        }

        Location locationToDelete = locationOptional.get();
        if (locationToDelete.getUser().getEmail().equals(userDetails.getUsername())) {
            locationService.deleteLocation(locationId);
            return "redirect:/weather/locations";

        } else {
            redirectAttributes.addFlashAttribute("error", "You cannot delete this location");
            return "redirect:/weather/location/{locationId}";
        }

    }

    @GetMapping(path = "/admin/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String getAllUsers(@AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("username", userDetails.getUsername().split("@")[0]);
        model.addAttribute("usersList", users);
        return "admin";
    }

    @GetMapping(path = "/admin/user/id/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String getUserById(@PathVariable Long userId,
                              Model model) {
        User user = userService.getUserById(userId).orElseThrow(
                () -> new EntityNotFoundException(
                        String.format("No user with id %d", userId)));
        model.addAttribute("user", user);
        return "user_info";
    }

    @PostMapping(path = "/register")
    public String saveUser(@Valid @ModelAttribute UserDTO userDTO,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            List<String> errorMessages = bindingResult.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                            .collect(Collectors.toList());
            model.addAttribute("error", errorMessages);
            return "register";
        }
        try {
            userService.saveUser(userDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
            return "redirect:/login";
        } catch (EntityNotFoundException exception) {
            model.addAttribute("error", exception.getMessage());
            return "/register";
        } catch (DataIntegrityViolationException exception) {
            model.addAttribute("error", "User with this email already exists");
            return "/register";
        } catch (Exception exception) {
            model.addAttribute("error", "An unexpected error occurred");
            return "/register";
        }
    }

    @PostMapping(path = "/user/delete/{userId}")
    public String deleteUserById(@PathVariable Long userId,
                                 RedirectAttributes redirectAttributes) {
        try {
            User userToDelete = userService.getUserById(userId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            String.format("User with id %d not found", userId)));
            System.out.println("Deleting user: " + userToDelete);
            userService.deleteUser(userId);

            redirectAttributes.addFlashAttribute("successDeleteUserMessage",
                    "User with email %s deleted successfully".formatted(userToDelete.getEmail()));
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorDeleteUserMessage", "Error deleting user");
            return "redirect:/weather/locations";
        }
    }

    @GetMapping("/delete_user_confirmation")
    public String deleteConfirmation(@AuthenticationPrincipal UserDetails userDetails,
                                     Model model) {
        Optional<User> userOptional = userService.getUserByEmail(userDetails.getUsername());
        if (userOptional.isEmpty()) {
            return "locations";
        }
        Long userId = userOptional.get().getId();
        model.addAttribute("userId", userId);
        model.addAttribute("username", userDetails.getUsername().split("@")[0]);
        return "delete_user_confirmation";
    }

    @PostMapping("/admin/block/user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String blockUser(@PathVariable Long userId,
                            RedirectAttributes redirectAttributes) {
        Optional<User> userToBlock = userService.getUserById(userId);
        if (userToBlock.isPresent()) {
            UserDTO userDTO = new UserDTO();
            userDTO.setRole("ROLE_BLOCKED");
            userService.updateUserRole(userId, userDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "User with ID %d and email %s is blocked".formatted(userId, userToBlock.get().getEmail()));
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "User with ID %d not found".formatted(userId));
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/unblock/user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String unblockUser(@PathVariable Long userId,
                              RedirectAttributes redirectAttributes) {
        Optional<User> userToUnblock = userService.getUserById(userId);
        if (userToUnblock.isPresent()) {
            UserDTO userDTO = new UserDTO();
            userDTO.setRole("ROLE_USER");
            userService.updateUserRole(userId, userDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "User with ID %d and email %s is user".formatted(userId, userToUnblock.get().getEmail()));
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "User with ID %d not found".formatted(userId));
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/user/blocked")
    public String blockedUserHome() {
        return "blocked_user";
    }

    @PostMapping("/admin/admin/user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String giveUserAdminRole(@PathVariable Long userId,
                                    RedirectAttributes redirectAttributes) {
        Optional<User> userToGiveAdminRole = userService.getUserById(userId);
        if (userToGiveAdminRole.isPresent()) {
            UserDTO userDTO = new UserDTO();
            userDTO.setRole("ROLE_ADMIN");
            userService.updateUserRole(userId, userDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "User with ID %d and email %s is admin".formatted(userId, userToGiveAdminRole.get().getEmail()));
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "User with ID %d not found".formatted(userId));
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/user/change/password")
    public String changePasswordPage() {
        return "change_password";
    }

    @PostMapping("/user/change/password")
    public String changePasswordForUser(@Valid @ModelAttribute ChangePasswordDTO changePasswordDTO,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        RedirectAttributes redirectAttributes,
                                        Model model) {
        Optional<User> userOptional = userService.getUserByEmail(userDetails.getUsername());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (!passwordEncoder.matches(changePasswordDTO.getCurrentPassword(), user.getPassword())) {
                model.addAttribute("error", "Current password is wrong");
                return "change_password";
            }

            if (!changePasswordDTO.getNewPassword().equals(changePasswordDTO.getConfirmedPassword())) {
                model.addAttribute("error", "Confirmed password is wrong");
                return "change_password";
            }

            UserDTO userDTO = new UserDTO();
            userDTO.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
            userService.updateUserPassword(user.getId(), userDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Password successfully changed!");
            return "redirect:/weather/locations";
        } else {
            model.addAttribute("error", "User not found");
            return "change_password";
        }
    }
}
