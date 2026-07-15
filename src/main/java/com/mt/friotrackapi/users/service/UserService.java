package com.mt.friotrackapi.users.service;

import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.common.exception.AuthException;
import com.mt.friotrackapi.companies.dto.CompanyResponse;
import com.mt.friotrackapi.companies.service.CompanyService;
import com.mt.friotrackapi.roles.dto.RoleResponse;
import com.mt.friotrackapi.roles.service.RoleService;
import com.mt.friotrackapi.users.dto.CreateUserRequest;
import com.mt.friotrackapi.users.dto.UserResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final RoleService roleService;
    private final CompanyService companyService;
    private final List<UserAccount> users = new ArrayList<>(List.of(
            new UserAccount(1L, 1L, "FrioTrack Demo", "admin", "Administrador", "admin@friotrack.pe", "secret123", "ADMIN", "ACTIVE"),
            new UserAccount(2L, 1L, "FrioTrack Demo", "operador", "Operador Lima", "operador@friotrack.pe", "secret123", "OPERADOR", "ACTIVE"),
            new UserAccount(3L, 1L, "FrioTrack Demo", "supervisor", "Supervisor", "supervisor@friotrack.pe", "secret123", "LECTOR", "ACTIVE"),
            new UserAccount(4L, 2L, "Cadena Fria Norte", "admin-norte", "Administrador Norte", "admin@norte.pe", "secret123", "ADMIN", "ACTIVE")
    ));

    public UserService(RoleService roleService, CompanyService companyService) {
        this.roleService = roleService;
        this.companyService = companyService;
    }

    public List<UserResponse> findAll(Long companyId) {
        if (companyId == null) {
            return users.stream().map(UserAccount::toResponse).toList();
        }

        companyService.findById(companyId);
        return users.stream()
                .filter(user -> user.companyId().equals(companyId))
                .map(UserAccount::toResponse)
                .toList();
    }

    public UserResponse findById(Long id) {
        return findAccountById(id).toResponse();
    }

    public UserResponse findByUsernameOrEmail(String access) {
        return findAccountByUsernameOrEmail(access).toResponse();
    }

    public UserResponse authenticate(String access, String password) {
        UserAccount account = findAccountByUsernameOrEmail(access);
        if (!account.password().equals(password)) {
            throw new AuthException("Credenciales invalidas");
        }
        return account.toResponse();
    }

    public UserResponse demoAdmin() {
        return findById(1L);
    }

    public UserResponse create(CreateUserRequest request) {
        CompanyResponse company = companyService.findById(request.companyId());
        RoleResponse role = roleService.findById(request.roleId());

        boolean exists = users.stream()
                .anyMatch(user -> user.companyId().equals(request.companyId())
                        && (user.username().equalsIgnoreCase(request.username())
                        || user.email().equalsIgnoreCase(request.email())));

        if (exists) {
            throw new ApiException("El usuario o correo ya existe en la empresa");
        }

        UserAccount user = new UserAccount(
                (long) users.size() + 1,
                company.id(),
                company.name(),
                request.username(),
                request.name(),
                request.email(),
                request.password(),
                role.name(),
                "ACTIVE"
        );
        users.add(user);
        return user.toResponse();
    }

    private UserAccount findAccountById(Long id) {
        return users.stream()
                .filter(user -> user.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ApiException("Usuario no encontrado"));
    }

    private UserAccount findAccountByUsernameOrEmail(String access) {
        return users.stream()
                .filter(user -> user.username().equalsIgnoreCase(access) || user.email().equalsIgnoreCase(access))
                .findFirst()
                .orElseThrow(() -> new ApiException("Usuario no encontrado"));
    }

    private record UserAccount(
            Long id,
            Long companyId,
            String companyName,
            String username,
            String name,
            String email,
            String password,
            String role,
            String status
    ) {
        private UserResponse toResponse() {
            return new UserResponse(id, companyId, companyName, username, name, email, role, status);
        }
    }
}
