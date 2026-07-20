package com.mt.friotrackapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.friotrackapi.auth.service.AuthTokenService;
import com.mt.friotrackapi.companies.entity.CompanyEntity;
import com.mt.friotrackapi.roles.entity.RoleEntity;
import com.mt.friotrackapi.users.dto.UserResponse;
import com.mt.friotrackapi.users.entity.UserEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RolePermissionIntegrationTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired EntityManager entityManager;
    @Autowired AuthTokenService authTokenService;

    private Long companyOneId;
    private Long companyTwoId;
    private Long adminId;
    private Long userId;
    private Long serviceAdminId;

    @BeforeEach
    void setUp() {
        entityManager.createNativeQuery("delete from users").executeUpdate();
        entityManager.createNativeQuery("delete from companies").executeUpdate();
        entityManager.createNativeQuery("delete from roles").executeUpdate();
        entityManager.createNativeQuery("insert into roles(id, name, description) values (1, 'USER', 'Usuario'), (2, 'ADMIN', 'Administrador'), (4, 'SA', 'Super administrador')").executeUpdate();

        CompanyEntity companyOne = new CompanyEntity("Frio Uno", "frio-uno", "ACTIVE", 3, 10);
        CompanyEntity companyTwo = new CompanyEntity("Frio Dos", "frio-dos", "ACTIVE", 3, 10);
        entityManager.persist(companyOne);
        entityManager.persist(companyTwo);
        entityManager.flush();
        companyOneId = companyOne.getId();
        companyTwoId = companyTwo.getId();

        RoleEntity userRole = entityManager.find(RoleEntity.class, 1L);
        RoleEntity adminRole = entityManager.find(RoleEntity.class, 2L);
        RoleEntity saRole = entityManager.find(RoleEntity.class, 4L);

        UserEntity admin = new UserEntity(companyOne, "admin", "Admin", "admin@test.pe", "hash", adminRole, "ACTIVE");
        UserEntity user = new UserEntity(companyOne, "user", "User", "user@test.pe", "hash", userRole, "ACTIVE");
        UserEntity serviceAdmin = new UserEntity(companyOne, "sa", "SA", "sa@test.pe", "hash", saRole, "ACTIVE");
        entityManager.persist(admin);
        entityManager.persist(user);
        entityManager.persist(serviceAdmin);
        entityManager.flush();
        adminId = admin.getId();
        userId = user.getId();
        serviceAdminId = serviceAdmin.getId();
    }

    @Test
    void apiRequestsRequireJwt() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotCreateUsersOrCompanies() throws Exception {
        String token = token(userId, companyOneId, "user", "USER");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("companyId", companyOneId, "username", "blocked", "name", "Blocked", "email", "blocked@test.pe", "password", "secret123", "roleId", 1))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/companies")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Nope", "taxId", "nope", "status", "ACTIVE", "warningOfflineMinutes", 3, "criticalOfflineMinutes", 10))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateUsersOnlyInsideOwnCompany() throws Exception {
        String token = token(adminId, companyOneId, "admin", "ADMIN");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("companyId", companyTwoId, "username", "cross", "name", "Cross", "email", "cross@test.pe", "password", "secret123", "roleId", 1))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("companyId", companyOneId, "username", "inside", "name", "Inside", "email", "inside@test.pe", "password", "secret123", "roleId", 1))))
                .andExpect(status().isOk());
    }

    @Test
    void serviceAdminCanCreateCompanies() throws Exception {
        String token = token(serviceAdminId, companyOneId, "sa", "SA");

        mockMvc.perform(post("/api/companies")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Nueva Empresa", "taxId", "20123456789", "status", "ACTIVE", "warningOfflineMinutes", 3, "criticalOfflineMinutes", 10))))
                .andExpect(status().isOk());
    }

    private String token(Long id, Long companyId, String username, String role) {
        return authTokenService.createToken(new UserResponse(id, companyId, "Test", username, username, username + "@test.pe", role, "ACTIVE"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
