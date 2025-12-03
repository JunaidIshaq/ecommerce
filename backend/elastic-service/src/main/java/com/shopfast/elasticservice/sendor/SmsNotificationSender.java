//package com.shopfast.elasticservice.sendor;
//
//import com.shopfast.elasticservice.enums.NotificationChannel;
//import com.shopfast.elasticservice.document.ProductDocument;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//public class SmsNotificationSender implements NotificationSender {
//
//    @Override
//    public boolean supports(ProductDocument productDocument) {
//        return productDocument.getChannel() == NotificationChannel.SMS;
//    }
//
//    @Override
//    public void send(ProductDocument productDocument) {
//        // TODO integrate with SMS provider
//        log.info("Simulating SMS send to {} with content : {}", productDocument.getRecipient(), productDocument.getContent());
//    }
//}
