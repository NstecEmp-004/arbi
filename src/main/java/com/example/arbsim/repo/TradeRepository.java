package com.example.arbsim.repo;
import com.example.arbsim.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.Instant;
public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findTop20ByOrderByTsDesc();
    List<Trade> findByTsBetweenOrderByTsAsc(Instant from, Instant to);
}
