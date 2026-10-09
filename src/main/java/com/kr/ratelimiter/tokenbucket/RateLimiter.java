package com.kr.ratelimiter.tokenbucket;

public interface RateLimiter {

	boolean allow(String clientId);

}
