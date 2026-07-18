package com.mt.friotrackapi.users.entity;

import com.mt.friotrackapi.companies.entity.CompanyEntity;
import com.mt.friotrackapi.roles.entity.RoleEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_company_username", columnNames = {"company_id", "username"}),
        @UniqueConstraint(name = "uk_users_company_email", columnNames = {"company_id", "email"})
})
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Column(nullable = false)
    private String status;

    protected UserEntity() {}

    public UserEntity(CompanyEntity company, String username, String name, String email, String password, RoleEntity role, String status) {
        this.company = company;
        this.username = username;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = status;
    }

    public Long getId() { return id; }
    public CompanyEntity getCompany() { return company; }
    public String getUsername() { return username; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public RoleEntity getRole() { return role; }
    public String getStatus() { return status; }

    public void update(CompanyEntity company, String username, String name, String email, String password, RoleEntity role) {
        this.company = company;
        this.username = username;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public void setStatus(String status) { this.status = status; }
    public void setPassword(String password) { this.password = password; }
}
