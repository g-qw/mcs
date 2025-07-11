package org.cloud.mail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MailServiceApplication {

    public static void main(String[] args) {
        // 避免在生成邮件地址时进行DNS查找, 可以显著降低发送邮件的时长，减少大概 20s 左右
        System.setProperty("mail.mime.address.usecanonicalhostname", "false");

        SpringApplication.run(MailServiceApplication.class, args);
    }
}
