package com.kr.ratelimiter.tokenbucket;


import java.time.Clock;

public class Main {

	public static void main(String[] args) {

		RateLimiter rateLimiter = new TokenBucketRateLimiter(5, // capacity
				2, // 2 tokens/sec
				Clock.systemUTC());

		String clientId = "user-101";

		for (int i = 1; i <= 7; i++) {

			boolean allowed = rateLimiter.allow(clientId);

			System.out.println("Request " + i + " -> " + (allowed ? "ALLOWED" : "REJECTED"));
		}
	}
}