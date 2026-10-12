package dev.joaovictor.pixledger.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.joaovictor.pixledger.domain.PixLimit;

public interface PixLimitRepository extends JpaRepository<PixLimit, UUID> {
}
