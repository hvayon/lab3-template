package ru.hvayon.IdentityProvider.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hvayon.IdentityProvider.entities.Role;
import ru.hvayon.IdentityProvider.repositories.RoleRepository;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    public Role getUserRole() {
        return roleRepository.findByName("ROLE_USER").get();
    }
}
