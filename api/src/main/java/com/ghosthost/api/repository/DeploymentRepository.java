package com.ghosthost.api.repository;

import com.ghosthost.api.entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeploymentRepository extends JpaRepository<Deployment, String> {
    List<Deployment> findByUserIdOrderByCreatedAtDesc(Long userId);
}
