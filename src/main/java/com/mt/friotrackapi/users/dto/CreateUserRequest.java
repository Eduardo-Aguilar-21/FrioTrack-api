package com.mt.friotrackapi.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotNull Long companyId,
        @NotBlank String username,
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull Long roleId
) {
}
