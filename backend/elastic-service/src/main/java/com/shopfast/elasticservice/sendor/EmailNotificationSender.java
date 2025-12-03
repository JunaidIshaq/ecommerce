//package com.shopfast.elasticservice.sendor;
//
//import com.shopfast.elasticservice.document.ProductDocument;
//import com.shopfast.elasticservice.enums.NotificationChannel;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//public class EmailNotificationSender implements NotificationSender {
//
//    private final JavaMailSender mailSender;
//
//    public EmailNotificationSender(JavaMailSender mailSender) {
//        this.mailSender = mailSender;
//    }
//
//
//    @Override
//    public boolean supports(ProductDocument productDocument) {
//        return productDocument.getChannel() == NotificationChannel.EMAIL;
//    }
//
//    @Override
//    public void send(ProductDocument productDocument) {
//        log.info("Sending email productDocument id : {} to : {}", productDocument.getId(), productDocument.getRecipient());
//
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo(productDocument.getRecipient());
//        message.setSubject(productDocument.getSubject() != null ? productDocument.getSubject() : "ProductDocument");
//        message.setText(productDocument.getContent());
//
//        mailSender.send(message);
//    }
//}
