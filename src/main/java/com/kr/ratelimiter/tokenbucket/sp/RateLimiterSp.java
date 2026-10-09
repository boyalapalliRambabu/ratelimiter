package com.kr.ratelimiter.tokenbucket.sp;

public interface RateLimiterSp {
	RateLimitResult check(String mobileNo);
}
