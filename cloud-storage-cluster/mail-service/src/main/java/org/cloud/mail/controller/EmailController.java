package org.cloud.mail.controller;

import freemarker.template.TemplateException;
import io.netty.util.internal.ThreadLocalRandom;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.cloud.mail.config.CustomThreadPoolTaskExecutor;
import org.cloud.mail.dto.ApiResponse;
import org.cloud.mail.dto.EmailRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public class EmailController {
    @Value("${spring.mail.username}")
    private String fromEmail;
    private static final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private final String VERIFICATION_CODE_PREFIX = "verification:"; // 验证码的 redis 键前缀
    private final String VERIFICATION_CODE_REQUEST_PREFIX = "verificationRequest:"; // 最新的验证码请求时间的 redis 键前缀
    private final long VERIFICATION_CODE_EXPIRE_TIME = 60; // 验证码有效期，1 分钟
    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

    private final JavaMailSender mailSender;
    private final CustomThreadPoolTaskExecutor mailExecutor;
    private final FreeMarkerConfigurer freeMarkerConfigurer;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Autowired
    public EmailController(JavaMailSender mailSender, CustomThreadPoolTaskExecutor mailExecutor, FreeMarkerConfigurer freeMarkerConfigurer, ReactiveRedisTemplate<String, String> redisTemplate) {
        this.mailSender = mailSender;
        this.mailExecutor = mailExecutor;
        this.freeMarkerConfigurer = freeMarkerConfigurer;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/check_email")
    public Mono<String> checkEmail(@Validated @RequestBody EmailRequest emailRequest) {
        return Mono.fromFuture(
            CompletableFuture.supplyAsync(
                () -> {
                    try {
                        SimpleMailMessage message = new SimpleMailMessage();
                        message.setFrom(fromEmail);
                        message.setTo(emailRequest.getEmail());
                        message.setSubject("邮箱检查");
                        message.setText("这是一封测试邮件，请勿回复，仅用于验证邮箱是否有效。");
                        mailSender.send(message);

                        return "valid";
                    } catch (Exception e) {
                        logger.error("邮件发送失败", e);
                        return "invalid";
                    }
                },
                mailExecutor
            )
        );
    }

    /**
     * 发送验证码
     * 验证码以 verification:email 为键存入 redis，有效期为 1 分钟
     * @param emailRequest 请求参数, 包含邮箱
     */
    @PostMapping("/verify")
    public Mono<ApiResponse<?>> sendVerifyEmail(@Validated @RequestBody EmailRequest emailRequest) {
        String email = emailRequest.getEmail();
        String requestKey = VERIFICATION_CODE_REQUEST_PREFIX + email;

        return redisTemplate.hasKey(requestKey)
                .flatMap(
                        exists -> {
                            // 如果有重复请求，则返回错误响应
                            if(Boolean.TRUE.equals(exists)) {
                                return Mono.just(ApiResponse.failure(400, "操作过于频繁，请稍后再试"));
                            }

                            // 如果没有重复请求，则发送邮件
                            String code = getVerificationCode();  // 生成验证码

                            // 加载模板
                            String content;
                            try {
                                content = loadTemplate(code);
                            } catch (IOException | TemplateException e) {
                                return Mono.just(ApiResponse.failure(500, "内部服务器错误"));
                            }

                            // 在线程池中执行发送邮件任务
                            CompletableFuture<Boolean> future = sendEmailAsync(email, content, code);
                            return Mono.fromFuture(future)
                                    .flatMap(
                                            result -> {
                                                if(Boolean.TRUE.equals(result)) {
                                                    return Mono.just(ApiResponse.success("验证码已发送到您的邮箱，请注意查收"));
                                                } else {
                                                    return Mono.just(ApiResponse.failure(500, "邮件发送失败，请稍后再试"));
                                                }
                                            }
                                    );

                        }
                );
    }

    // 生成6位随机验证码，包含数字和大小写字母
    private String getVerificationCode() {
        char[] code = new char[6];
        for (int i = 0; i < 6; i++) {
            code[i] = characters.charAt(ThreadLocalRandom.current().nextInt(characters.length()));
        }
        return new String(code);
    }

    // 加载模板
    private String loadTemplate(String code) throws IOException, TemplateException {
        Map<String, String> model = Map.of("code", code);
        return FreeMarkerTemplateUtils.processTemplateIntoString(
                freeMarkerConfigurer.getConfiguration().getTemplate("auth.html"), // 获取根据配置加载的模板(配置中包含一些全局变量)
                model // 额外的模板数据
        );
    }

    // 异步发送邮件
    private CompletableFuture<Boolean> sendEmailAsync(String email, String content, String code) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        mailExecutor.execute(
            () -> {
                try {
                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

                    helper.setFrom(fromEmail);
                    helper.setTo(email);
                    helper.setSubject("Cloud Storage 邮箱验证");
                    helper.setText(content, true);
                    mailSender.send(mimeMessage);

                    // 将验证码存入 redis
                    String codeKey = VERIFICATION_CODE_PREFIX + email;
                    redisTemplate.opsForValue().set(codeKey, code, Duration.ofSeconds(VERIFICATION_CODE_EXPIRE_TIME))
                        .subscribe(
                            result -> {
                                // 记录验证码请求
                                String requestKey = VERIFICATION_CODE_REQUEST_PREFIX + email;
                                redisTemplate.opsForValue().set(requestKey, String.valueOf(System.currentTimeMillis()), Duration.ofSeconds(VERIFICATION_CODE_EXPIRE_TIME))
                                    .subscribe(
                                        requestResult -> future.complete(true), // Redis操作成功，完成Future
                                        error -> {
                                            logger.error("Redis记录验证码请求失败", error);
                                            future.complete(false); // Redis操作失败，完成Future
                                        }
                                    );
                            },
                            error -> {
                                logger.error("Redis记录验证码失败", error);
                                future.complete(false); // Redis操作失败，完成Future
                            }
                        );
                } catch (MessagingException e) {
                    logger.error("邮件发送失败", e);
                    future.complete(false); // 发送失败，完成Future
                }
            }
        );
        return future;
    }
}