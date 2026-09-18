package com.ztech.crm.access.repository;

import com.ztech.crm.access.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countByTenantIdAndRoleAndActiveTrue(Long tenantId, com.ztech.crm.access.domain.Role role);
}
