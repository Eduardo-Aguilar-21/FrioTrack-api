package com.mt.friotrackapi.users.repository;

import com.mt.friotrackapi.users.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    List<UserEntity> findByCompanyIdOrderByIdAsc(Long companyId);
    Optional<UserEntity> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);
    boolean existsByCompanyIdAndUsernameIgnoreCaseOrCompanyIdAndEmailIgnoreCase(Long companyId1, String username, Long companyId2, String email);
    boolean existsByCompanyIdAndUsernameIgnoreCaseAndIdNot(Long companyId, String username, Long id);
    boolean existsByCompanyIdAndEmailIgnoreCaseAndIdNot(Long companyId, String email, Long id);
}
