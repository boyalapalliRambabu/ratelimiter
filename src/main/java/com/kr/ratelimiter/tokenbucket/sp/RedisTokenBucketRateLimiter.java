package com.kr.ratelimiter.tokenbucket.sp;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Distributed token bucket. Every app instance calls the same Redis key, so the
 * limit is global, not per server. The refill-and-consume step runs as a Lua
 * script, which Redis executes as one indivisible step: two servers can never
 * both read the last token.
 *
 * Turn on with ratelimit.store=redis. The in-memory version stays the default.
 */
@Component
@ConditionalOnProperty(name = "ratelimit.store", havingValue = "redis")
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RedisTokenBucketRateLimiter implements RateLimiterSp {

	private static final Logger log = LoggerFactory.getLogger(RedisTokenBucketRateLimiter.class);

	private final StringRedisTemplate redis;
	private final DefaultRedisScript<List> script = new DefaultRedisScript<>();
	private final int capacity;
	private final double refillPerSecond;
	private final boolean failOpen;

	public RedisTokenBucketRateLimiter(StringRedisTemplate redis, @Value("${ratelimit.capacity:5}") int capacity,
			@Value("${ratelimit.refill-per-second:0.1}") double refillPerSecond,
			@Value("${ratelimit.fail-open:true}") boolean failOpen) {

		if (capacity < 1 || refillPerSecond <= 0) {
			throw new IllegalArgumentException("capacity must be >= 1 and refill-per-second must be > 0");
		}
		this.redis = redis;
		this.capacity = capacity;
		this.refillPerSecond = refillPerSecond;
		this.failOpen = failOpen;

		this.script.setLocation(new ClassPathResource("token_bucket.lua"));
		this.script.setResultType(List.class);
	}

	@Override
	public RateLimitResult check(String clientId) {

		try {
			List<Object> result = (List<Object>) redis.execute(script, List.of("ratelimit:" + clientId),
					String.valueOf(capacity), String.valueOf(refillPerSecond));

			if (result == null || result.size() < 2) {
				return onRedisFailure("empty response from Redis script");
			}

			boolean allowed = ((Number) result.get(0)).longValue() == 1;
			long retryAfterSeconds = ((Number) result.get(1)).longValue();

			return allowed ? RateLimitResult.permit() : RateLimitResult.reject(Math.max(1, retryAfterSeconds));

		} catch (DataAccessException e) {
			return onRedisFailure(e.getMessage());
		}
	}

	/**
	 * Redis is down or slow. Fail open: let traffic through, so a limiter outage
	 * does not become a full outage (right for most APIs). Fail closed: block
	 * everything (right for things like OTP sending, where abuse costs money).
	 */
	private RateLimitResult onRedisFailure(String reason) {

		if (failOpen) {
			log.warn("Rate limiter store unavailable, failing OPEN: {}", reason);
			return RateLimitResult.permit();
		}
		log.error("Rate limiter store unavailable, failing CLOSED: {}", reason);
		return RateLimitResult.reject(1);
	}
}
