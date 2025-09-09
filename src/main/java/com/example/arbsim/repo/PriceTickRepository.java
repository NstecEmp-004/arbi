package com.example.arbsim.repo;
import com.example.arbsim.entity.PriceTick;
import com.example.arbsim.entity.Market;
import com.example.arbsim.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.Instant;
public interface PriceTickRepository extends JpaRepository<PriceTick, Long> {
    List<PriceTick> findTop1ByMarketAndAssetOrderByTsDesc(Market m, Asset a);
    List<PriceTick> findByAssetAndTsBetweenOrderByTsAsc(Asset asset, Instant from, Instant to);
}
