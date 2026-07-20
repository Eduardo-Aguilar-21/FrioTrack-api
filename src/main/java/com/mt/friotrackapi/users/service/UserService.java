package com.mt.friotrackapi.users.service;

import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.common.exception.AuthException;
import com.mt.friotrackapi.companies.entity.CompanyEntity;
import com.mt.friotrackapi.companies.service.CompanyService;
import com.mt.friotrackapi.roles.entity.RoleEntity;
import com.mt.friotrackapi.roles.service.RoleService;
import com.mt.friotrackapi.users.dto.CreateUserRequest;
import com.mt.friotrackapi.users.dto.UserResponse;
import com.mt.friotrackapi.users.entity.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final RoleService roleService;
    private final CompanyService companyService;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public UserService(RoleService roleService, CompanyService companyService, PasswordEncoder passwordEncoder) {
        this.roleService = roleService;
        this.companyService = companyService;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> findAll(Long companyId) {
        if (companyId == null) {
            return entityManager.createQuery("select u from UserEntity u order by u.id", UserEntity.class).getResultList().stream().map(this::toResponse).toList();
        }

        companyService.findById(companyId);
        return entityManager.createQuery("select u from UserEntity u where u.company.id = :companyId order by u.id", UserEntity.class).setParameter("companyId", companyId).getResultList().stream().map(this::toResponse).toList();
    }

    public UserResponse findById(Long id) {
        return toResponse(entityById(id));
    }

    public UserResponse findByUsernameOrEmail(String access) {
        return toResponse(findAccountByUsernameOrEmail(access));
    }

    @Transactional
    public UserResponse authenticate(String access, String password) {
        UserEntity account = findAccountByUsernameOrEmail(access);
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new AuthException("Usuario inactivo");
        }

        if (isPasswordHash(account.getPassword()) && passwordEncoder.matches(password, account.getPassword())) {
            return toResponse(account);
        }

        if (!isPasswordHash(account.getPassword()) && account.getPassword().equals(password)) {
            account.setPassword(passwordEncoder.encode(account.getPassword()));
            return toResponse(account);
        }

        throw new AuthException("Credenciales invalidas");
    }

    public UserResponse demoAdmin() {
        return findById(1L);
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        CompanyEntity company = companyService.entityById(request.companyId());
        RoleEntity role = roleService.entityById(request.roleId());
        if (request.password() == null || request.password().isBlank()) {
            throw new ApiException("La contraseña es obligatoria");
        }

        boolean exists = count("select count(u) from UserEntity u where u.company.id = :companyId and (lower(u.username) = lower(:username) or lower(u.email) = lower(:email))", request.companyId(), request.username(), request.email(), null) > 0;
        if (exists) {
            throw new ApiException("El usuario o correo ya existe en la empresa");
        }

        UserEntity user = new UserEntity(
                company,
                request.username(),
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                role,
                "ACTIVE"
        );
        entityManager.persist(user);
        return toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, CreateUserRequest request) {
        UserEntity current = entityById(id);
        CompanyEntity company = companyService.entityById(request.companyId());
        RoleEntity role = roleService.entityById(request.roleId());

        boolean exists = count("select count(u) from UserEntity u where u.company.id = :companyId and u.id <> :id and (lower(u.username) = lower(:username) or lower(u.email) = lower(:email))", request.companyId(), request.username(), request.email(), id) > 0;
        if (exists) {
            throw new ApiException("El usuario o correo ya existe en la empresa");
        }

        String password = request.password() == null || request.password().isBlank()
                ? current.getPassword()
                : passwordEncoder.encode(request.password());

        current.update(company, request.username(), request.name(), request.email(), password, role);
        return toResponse(current);
    }


    @Transactional
    public void changePassword(Long id, String currentPassword, String newPassword) {
        UserEntity current = entityById(id);
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new AuthException("La contraseña actual es obligatoria");
        }
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 6) {
            throw new ApiException("La nueva contraseña debe tener al menos 6 caracteres");
        }

        boolean validCurrent = isPasswordHash(current.getPassword())
                ? passwordEncoder.matches(currentPassword, current.getPassword())
                : current.getPassword().equals(currentPassword);

        if (!validCurrent) {
            throw new AuthException("La contraseña actual no es correcta");
        }

        current.setPassword(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public UserResponse setStatus(Long id, String status) {
        UserEntity current = entityById(id);
        current.setStatus(normalizeStatus(status));
        return toResponse(current);
    }

    private UserEntity entityById(Long id) {
        UserEntity user = entityManager.find(UserEntity.class, id);
        if (user == null) {
            throw new ApiException("Usuario no encontrado");
        }
        return user;
    }

    private UserEntity findAccountByUsernameOrEmail(String access) {
        return entityManager.createQuery("select u from UserEntity u where lower(u.username) = lower(:access) or lower(u.email) = lower(:access)", UserEntity.class)
                .setParameter("access", access)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new ApiException("Usuario no encontrado"));
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "INACTIVE";
        }
        String normalized = status.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.equals("ACTIVE") ? "ACTIVE" : "INACTIVE";
    }

    private boolean isPasswordHash(String password) {
        return password != null && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }

    private long count(String query, Long companyId, String username, String email, Long id) {
        var typedQuery = entityManager.createQuery(query, Long.class)
                .setParameter("companyId", companyId)
                .setParameter("username", username)
                .setParameter("email", email);
        if (id != null) {
            typedQuery.setParameter("id", id);
        }
        return typedQuery.getSingleResult();
    }

    private UserResponse toResponse(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getCompany().getId(),
                user.getCompany().getName(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getRole().getName(),
                user.getStatus()
        );
    }
}
