package com.example.arbsim.repo;
import com.example.arbsim.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findBySymbol(String symbol);
}
