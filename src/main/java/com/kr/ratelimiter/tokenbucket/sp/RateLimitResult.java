package com.kr.ratelimiter.tokenbucket.sp;

/**
 * The answer to "allow or reject?", plus how long a rejected client should
 * wait. Returning this instead of a bare boolean is what lets the filter send a
 * correct Retry-After header.
 */
public record RateLimitResult(boolean allowed, long retryAfterSeconds) {

	public static RateLimitResult permit() {
		return new RateLimitResult(true, 0);
	}

	public static RateLimitResult reject(long retryAfterSeconds) {
		return new RateLimitResult(false, retryAfterSeconds);
	}
}
