package ru.hvayon.IdentityProvider.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.hvayon.IdentityProvider.entities.Role;
import ru.hvayon.IdentityProvider.entities.User;
import ru.hvayon.IdentityProvider.repositories.RoleRepository;
import ru.hvayon.IdentityProvider.repositories.UserRepository;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("api")
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/v1/callback")
    public String unsecuredData() {
        return "Unsecured data";
    }

    @GetMapping("/secured")
    public String securedData() {
        return "Secured data";
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/v1/admin")
    public String adminData() {
        return "Admin data";
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/create/user")
    @ResponseStatus(HttpStatus.CREATED)
    public void adminCreateUser(@RequestBody User user) {
        System.out.println("A new user has been created: " + user.getEmail());
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Role 'USER' not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        String encodedPassword = passwordEncoder.encode(user.getPassword());
        System.out.println(encodedPassword);
        user.setPassword(encodedPassword);

        userRepository.save(user);
    }

    @PostMapping("/v1/create/user")
    @ResponseStatus(HttpStatus.CREATED)
    public void createUser(@RequestBody User user) {
        System.out.println("A new user has been created: " + user.getEmail());
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Role 'USER' not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        String encodedPassword = passwordEncoder.encode(user.getPassword());
        System.out.println(encodedPassword);
        user.setPassword(encodedPassword);

        userRepository.save(user);
    }

    @RequestMapping("/v1/user")
    public Principal user(Principal user) {
        return user;
    }

    @GetMapping("/v1/info")
    public String userData(Principal principal) {
        return principal.getName();
    }
}