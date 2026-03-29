package com.echill.event.listener;

import com.echill.event.UserAuthEvent;
import com.echill.service.EmailService;
import com.echill.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserAuthEventListener {

    private final OtpService otpService;
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleUserAuthEvent(UserAuthEvent event) {
        String otp = otpService.generateAndSaveOtp(event.email(), event.isForgot());
        if (event.isForgot()) {
            emailService.sendForgotPasswordEmailAsync(event.email(), event.fullName(), otp);
        } else {
            emailService.sendOtpEmailAsync(event.email(), event.fullName(), otp);
        }
    }
}