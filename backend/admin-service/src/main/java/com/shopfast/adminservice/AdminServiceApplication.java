package com.shopfast.adminservice;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@EnableDiscoveryClient
@EnableCaching
@SpringBootApplication
@EnableFeignClients(basePackages = "com.shopfast.adminservice.client")
public class AdminServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdminServiceApplication.class, args);
	}

    // ✅ Test Redis connection at startup
//    @Bean
//    public CommandLineRunner testRedisConnection(RedisConnectionFactory factory, StringRedisTemplate redisTemplate) {
//        return args -> {
//            try {
//                // Simple ping test
//                RedisConnection connection = factory.getConnection();
//                String pong = connection.ping();
//                System.out.println("✅ Connected to Redis: " + pong);
//
//                // Write a test key
//                redisTemplate.opsForValue().set("test-key", "connected");
//                String value = redisTemplate.opsForValue().get("test-key");
//                System.out.println("✅ Redis write/read successful. test-key=" + value);
//
//                connection.close();
//            } catch (Exception e) {
//                System.err.println("❌ Redis connection failed: " + e.getMessage());
//            }
//        };
//    }
//
}
