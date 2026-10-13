package dev.joaovictor.pixledger.persistence;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.joaovictor.pixledger.domain.Closing;

public interface ClosingRepository extends JpaRepository<Closing, LocalDate> {
}
