package com.core.auction_system.repository;

/**
 * Marker interface retained for compatibility. Auction-system no longer uses a local JPA
 * users table; user information should be fetched from auth-svc via `UserClient`.
 * <p>
 * This file exists to avoid compile-time breakages in code that still references the
 * repository, but actual persistence should not use it.
 */
@Deprecated
public interface UserRepository {
}
