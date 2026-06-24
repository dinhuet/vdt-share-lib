package com.pm.be.repository.anomaly;

import com.pm.be.entity.anomaly.AnomalyBaselineEntity;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalyTimeBucketType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AnomalyBaselineRepository extends JpaRepository<AnomalyBaselineEntity, UUID> {
    Optional<AnomalyBaselineEntity> findByMetricAndScopeTypeAndScopeKeyAndTimeBucketTypeAndTimeBucketAndHistoryDaysAndAggregationAndWindowSeconds(
            String metric,
            AnomalyScopeType scopeType,
            String scopeKey,
            AnomalyTimeBucketType timeBucketType,
            String timeBucket,
            Integer historyDays,
            String aggregation,
            Integer windowSeconds);
}
