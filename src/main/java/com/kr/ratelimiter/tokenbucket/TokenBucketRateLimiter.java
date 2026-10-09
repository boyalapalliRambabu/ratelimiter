package com.kr.ratelimiter.tokenbucket;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenBucketRateLimiter implements RateLimiter {

	private final int capacity;
	private final double refillRatePerSecond;
	private final Clock clock;

	private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

	public TokenBucketRateLimiter(int capacity, double refillRatePerSecond, Clock clock) {

		if (capacity <= 0) {
			throw new IllegalArgumentException("Capacity must be greater than 0");
		}

		if (refillRatePerSecond <= 0) {
			throw new IllegalArgumentException("Refill rate must be greater than 0");
		}

		this.capacity = capacity;
		this.refillRatePerSecond = refillRatePerSecond;
		this.clock = clock;
	}

	@Override
	public boolean allow(String clientId) {

		Bucket bucket = buckets.computeIfAbsent(clientId, id -> new Bucket(capacity, clock.millis()));

		synchronized (bucket) {
			refill(bucket);
			if (bucket.getTokens() >= 1) {

				bucket.setTokens(bucket.getTokens() - 1);

				return true;
			}
			return false;
		}

	}

	private void refill(Bucket bucket) {

		long now = clock.millis();

		long elapsedMillis = now - bucket.getLastRefillTime();

		if (elapsedMillis <= 0) {
			return;
		}
		double elapsedSeconds = elapsedMillis / 1000.0;

		double tokensToAdd = elapsedSeconds * refillRatePerSecond;
		double newTokens = Math.min(capacity, bucket.getTokens() + tokensToAdd);
		bucket.setTokens(newTokens);
		bucket.setLastRefillTime(now);
	}

}
