package com.shopfast.productservice.job;

import com.shopfast.productservice.util.ProductDataSeeder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class Schedular {

    private ProductDataSeeder productDataSeeder;

    public Schedular(ProductDataSeeder productDataSeeder) {
        this.productDataSeeder = productDataSeeder;
    }

    // runs at 00 minutes, 00 seconds of every 2nd hour: 00:00, 02:00, 04:00, ...
    @Scheduled(cron = "0 0 */2 * * *")
    public void everyTwoHours() {
        log.info("Job Running every two hour : {}", LocalDateTime.now());
        productDataSeeder.seed();
    }
}
