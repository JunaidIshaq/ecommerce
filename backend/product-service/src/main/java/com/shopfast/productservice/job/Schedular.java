package com.shopfast.productservice.job;

import com.shopfast.productservice.util.ProductDataSeeder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
public class Schedular {

    private ProductDataSeeder productDataSeeder;

    public Schedular(ProductDataSeeder productDataSeeder) {
        this.productDataSeeder = productDataSeeder;
    }

//    // Runs every hour
//    @Scheduled(cron = "0 0 * * * *")
//    public void everyTwoHours() throws IOException {
//        log.info("Job Running every hour : {}", LocalDateTime.now());
//        productDataSeeder.seed();
//    }
}
