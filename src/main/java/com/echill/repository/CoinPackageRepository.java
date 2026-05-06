package com.echill.repository;

import com.echill.entity.CoinPackage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoinPackageRepository extends JpaRepository<CoinPackage,Long> {
    boolean existsByName(String name);

    Page<CoinPackage> findAllByIsActiveTrue(Pageable pageable);

    Optional<CoinPackage> findByIdAndIsActiveTrue(Long id);
}
