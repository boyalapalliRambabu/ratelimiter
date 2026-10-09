package com.kr.ratelimiter.tokenbucket.sp;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TokenBucketRateLimiterSp implements RateLimiterSp {

	private final int capacity;
	private final double refillPerSecond;
	private final Clock clock;

	// A bucket idle for this long would be completely full again anyway, so
	// dropping it loses no information: a new request just gets a fresh full
	// bucket.
	private final long idleEvictionMillis;

	private final Map<String, BucketSp> buckets = new ConcurrentHashMap<>();

	public TokenBucketRateLimiterSp(@Value("${ratelimit.capacity:5}") int capacity,
			@Value("${ratelimit.refill-per-second:0.1}") double refillPerSecond, Clock clock) {

		if (capacity < 1 || refillPerSecond <= 0) {
			throw new IllegalArgumentException("capacity must be >= 1 and refill-per-second must be > 0");
		}
		this.capacity = capacity;
		this.refillPerSecond = refillPerSecond;
		this.clock = clock;
		this.idleEvictionMillis = (long) Math.ceil(capacity / refillPerSecond * 1000);
	}

	@Override
	public RateLimitResult check(String clientId) {

		BucketSp bucket = buckets.computeIfAbsent(clientId, id -> new BucketSp(capacity, clock.millis()));

		// Per-client lock: different clients never wait on each other.
		synchronized (bucket) {

			refill(bucket);

			if (bucket.getTokens() >= 1) {
				bucket.setTokens(bucket.getTokens() - 1);
				return RateLimitResult.permit();
			}

			double missingTokens = 1 - bucket.getTokens();
			long retryAfterSeconds = (long) Math.ceil(missingTokens / refillPerSecond);
			return RateLimitResult.reject(Math.max(1, retryAfterSeconds));
		}
	}

	private void refill(BucketSp bucket) {

		long now = clock.millis();
		long elapsedMillis = now - bucket.getLastRefillTime();

		if (elapsedMillis <= 0) {
			return; // no time passed, or the clock moved backwards
		}

		double tokensToAdd = refillPerSecond * (elapsedMillis / 1000.0);
		bucket.setTokens(Math.min(capacity, bucket.getTokens() + tokensToAdd));
		bucket.setLastRefillTime(now);
	}

	/**
	 * Removes buckets that have been idle long enough to be full. Without this the
	 * map grows by one entry per client forever.
	 */
	@Scheduled(fixedDelayString = "${ratelimit.eviction-interval-ms:60000}")
	public void evictIdleBuckets() {

		System.out.println("idleEvictionMillis " + idleEvictionMillis);
		long now = clock.millis();

		buckets.entrySet().removeIf(entry -> {
			BucketSp bucket = entry.getValue();
			synchronized (bucket) {
				System.out.println("Removed");

				System.out.println(now - bucket.getLastRefillTime());
				return now - bucket.getLastRefillTime() >= idleEvictionMillis;
			}
		});
	}

	int bucketCount() {
		return buckets.size();
	}

}
