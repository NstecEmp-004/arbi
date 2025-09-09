package com.example.arbsim.repo;
import com.example.arbsim.entity.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {}
