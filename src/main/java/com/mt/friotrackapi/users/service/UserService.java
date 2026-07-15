package com.mt.friotrackapi.users.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.common.exception.AuthException;
import com.mt.friotrackapi.companies.dto.CompanyResponse;
import com.mt.friotrackapi.companies.service.CompanyService;
import com.mt.friotrackapi.roles.dto.RoleResponse;
import com.mt.friotrackapi.roles.service.RoleService;
import com.mt.friotrackapi.users.dto.CreateUserRequest;
import com.mt.friotrackapi.users.dto.UserResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final RoleService roleService;
    private final CompanyService companyService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path storePath = Path.of(System.getProperty("user.dir"), "data", "users.json");
    private final List<UserAccount> users = new ArrayList<>();

    public UserService(RoleService roleService, CompanyService companyService) {
        this.roleService = roleService;
        this.companyService = companyService;
        loadUsers();
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
        if (request.password() == null || request.password().isBlank()) {
            throw new ApiException("La contraseña es obligatoria");
        }

        boolean exists = users.stream()
                .anyMatch(user -> user.companyId().equals(request.companyId())
                        && (user.username().equalsIgnoreCase(request.username())
                        || user.email().equalsIgnoreCase(request.email())));

        if (exists) {
            throw new ApiException("El usuario o correo ya existe en la empresa");
        }

        UserAccount user = new UserAccount(
                nextId(),
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
        saveUsers();
        return user.toResponse();
    }


    public UserResponse update(Long id, CreateUserRequest request) {
        UserAccount current = findAccountById(id);
        CompanyResponse company = companyService.findById(request.companyId());
        RoleResponse role = roleService.findById(request.roleId());

        boolean exists = users.stream()
                .anyMatch(user -> !user.id().equals(id)
                        && user.companyId().equals(request.companyId())
                        && (user.username().equalsIgnoreCase(request.username())
                        || user.email().equalsIgnoreCase(request.email())));

        if (exists) {
            throw new ApiException("El usuario o correo ya existe en la empresa");
        }

        String password = request.password() == null || request.password().isBlank()
                ? current.password()
                : request.password();

        UserAccount updated = new UserAccount(
                current.id(),
                company.id(),
                company.name(),
                request.username(),
                request.name(),
                request.email(),
                password,
                role.name(),
                current.status()
        );
        users.set(users.indexOf(current), updated);
        saveUsers();
        return updated.toResponse();
    }

    private Long nextId() {
        return users.stream().mapToLong(UserAccount::id).max().orElse(0L) + 1;
    }

    private void loadUsers() {
        try {
            if (Files.exists(storePath)) {
                users.addAll(objectMapper.readValue(storePath.toFile(), new TypeReference<List<UserAccount>>() {}));
                return;
            }

            users.addAll(defaultUsers());
            saveUsers();
        } catch (IOException ex) {
            throw new ApiException("No se pudo cargar usuarios persistidos");
        }
    }

    private void saveUsers() {
        try {
            Files.createDirectories(storePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storePath.toFile(), users);
        } catch (IOException ex) {
            throw new ApiException("No se pudo persistir usuarios");
        }
    }

    private List<UserAccount> defaultUsers() {
        return List.of(
                new UserAccount(1L, 1L, "FrioTrack Demo", "admin", "Administrador", "admin@friotrack.pe", "secret123", "ADMIN", "ACTIVE"),
                new UserAccount(2L, 1L, "FrioTrack Demo", "operador", "Operador Lima", "operador@friotrack.pe", "secret123", "OPERADOR", "ACTIVE"),
                new UserAccount(3L, 1L, "FrioTrack Demo", "supervisor", "Supervisor", "supervisor@friotrack.pe", "secret123", "LECTOR", "ACTIVE"),
                new UserAccount(4L, 2L, "Cadena Fria Norte", "admin-norte", "Administrador Norte", "admin@norte.pe", "secret123", "ADMIN", "ACTIVE")
        );
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

    public record UserAccount(
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
