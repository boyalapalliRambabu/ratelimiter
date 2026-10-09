package com.kr.ratelimiter.tokenbucket.sp;

public class BucketSp {

	private double tokens;
	private long lastRefillTime;

	public BucketSp(double tokens, long lastRefillTime) {
		super();
		this.tokens = tokens;
		this.lastRefillTime = lastRefillTime;
	}

	public double getTokens() {
		return tokens;
	}

	public void setTokens(double tokens) {
		this.tokens = tokens;
	}

	public long getLastRefillTime() {
		return lastRefillTime;
	}

	public void setLastRefillTime(long lastRefillTime) {
		this.lastRefillTime = lastRefillTime;
	}

	@Override
	public String toString() {
		return "BucketSp [tokens=" + tokens + ", lastRefillTime=" + lastRefillTime + ", getTokens()=" + getTokens()
				+ ", getLastRefillTime()=" + getLastRefillTime() + ", getClass()=" + getClass() + ", hashCode()="
				+ hashCode() + ", toString()=" + super.toString() + "]";
	}

}
