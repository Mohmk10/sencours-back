package com.sencours.repository;

import com.sencours.entity.SuspensionAppeal;
import com.sencours.enums.AppealStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SuspensionAppealRepository extends JpaRepository<SuspensionAppeal, Long> {

    List<SuspensionAppeal> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<SuspensionAppeal> findByStatusOrderByCreatedAtAsc(AppealStatus status);

    Optional<SuspensionAppeal> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, AppealStatus status);

    boolean existsByUserIdAndStatus(Long userId, AppealStatus status);
}
