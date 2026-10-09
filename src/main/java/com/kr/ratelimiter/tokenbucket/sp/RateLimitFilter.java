package com.kr.ratelimiter.tokenbucket.sp;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

	private final RateLimiterSp rateLimiterSp;

	public RateLimitFilter(RateLimiterSp rateLimiterSp) {
		this.rateLimiterSp = rateLimiterSp;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String mobileNo = getMobileNo(request);

		RateLimitResult result = rateLimiterSp.check(mobileNo);

		if (!result.allowed()) {
			response.setStatus(429);
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
			response.setContentType("application/json");
			response.getWriter().write("""
					{"error":"Too Many Requests","message":"Rate limit exceeded","retryAfterSeconds":%d}
					""".formatted(result.retryAfterSeconds()));

			return;
		}
		filterChain.doFilter(request, response);
	}

	private String getMobileNo(HttpServletRequest request) {

		String mobileNo = request.getHeader("mobileNo");

		if (mobileNo == null || mobileNo.isBlank()) {
			return request.getRemoteAddr();
		}

		return mobileNo;
	}

}
