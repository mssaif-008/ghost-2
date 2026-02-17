package com.ghosthost.api.repository;

import com.ghosthost.api.entity.BuildJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BuildJobRepository extends JpaRepository<BuildJob, Long> {
    List<BuildJob> findByDeploymentIdOrderByStartedAtAsc(String deploymentId);
}
