package com.swiftlink.repository;

import com.swiftlink.model.UrlMapping;

import java.util.Optional;

/**
 * Cloud-agnostic persistence contract for URL mappings.
 * AWS: DynamoDbUrlRepository  |  GCP: FirestoreUrlRepository  |  Azure: CosmosDbUrlRepository
 */
public interface UrlRepository {

    UrlMapping save(UrlMapping urlMapping);

    Optional<UrlMapping> findByShortCode(String shortCode);

    void deleteByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    UrlMapping incrementClickCount(String shortCode);
}
