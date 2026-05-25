package com.swiftlink.repository;

import com.swiftlink.model.ClickEvent;

import java.util.List;

/**
 * Cloud-agnostic persistence contract for click analytics.
 * AWS: DynamoDbAnalyticsRepository  |  GCP: FirestoreAnalyticsRepository  |  Azure: CosmosDbAnalyticsRepository
 */
public interface AnalyticsRepository {

    void save(ClickEvent event);

    List<ClickEvent> findByShortCode(String shortCode, int limit);

    long countByShortCode(String shortCode);
}
