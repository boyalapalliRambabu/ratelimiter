package com.kr.ratelimiter.tokenbucket.sp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

	@GetMapping("/api/v1/products")
	public String getProducts() {
		return "Product data from database";
	}
}
