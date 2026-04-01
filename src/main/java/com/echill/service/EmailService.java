package com.echill.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailService {
    JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    @NonFinal
    String fromEmail;

    @PostConstruct
    public void checkMailConnection() {
        try {
            if (mailSender instanceof JavaMailSenderImpl) {
                ((JavaMailSenderImpl) mailSender).testConnection();
                log.info("✅ Kết nối máy chủ Email (SMTP Gmail) thành công!");
            }
        } catch (Exception e) {
            log.error("❌ Kết nối máy chủ Email thất bại. Kiểm tra lại Mật khẩu ứng dụng: {}", e.getMessage());
        }
    }

    @Async("emailTaskExecutor")
    public void sendOtpEmailAsync (String to, String fullName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Mã xác thực đăng ký tài khoản Echill");

            // Template HTML cho Email (Tối ưu giao diện hiển thị)
            String htmlContent = String.format(
                    "<div style='font-family: Arial, sans-serif; padding: 20px; border: 1px solid #ddd; border-radius: 8px; max-width: 500px; margin: auto;'>" +
                            "<h2 style='color: #4CAF50; text-align: center;'>Chào mừng đến với Echill!</h2>" +
                            "<p>Xin chào <b>%s</b>,</p>" +
                            "<p>Cảm ơn bạn đã đăng ký tài khoản. Đây là mã xác thực (OTP) của bạn:</p>" +
                            "<div style='text-align: center; margin: 20px 0;'>" +
                            "<span style='font-size: 28px; font-weight: bold; background: #f4f4f4; padding: 10px 20px; border-radius: 5px; letter-spacing: 5px; color: #333;'>%s</span>" +
                            "</div>" +
                            "<p style='color: red; font-size: 13px;'><i>* Mã này có hiệu lực trong vòng 5 phút. Vui lòng không chia sẻ cho bất kỳ ai.</i></p>" +
                            "<hr style='border-top: 1px solid #eee;'/>" +
                            "<p style='font-size: 12px; color: #888; text-align: center;'>Đội ngũ hỗ trợ Echill</p>" +
                            "</div>",
                    fullName, otp
            );

            helper.setText(htmlContent, true); // true để bật chế độ render HTML
            mailSender.send(message);

            log.info("📧 Đã gửi email OTP thành công tới {}", to);

        } catch (Exception e) {
            // LƯU Ý QUAN TRỌNG: Bắt Exception ở đây để nó ghi Log ra thôi.
            // Tuyệt đối không throw tiếp, vì nó sẽ làm chết cái Thread gửi mail ngầm.
            log.error("❌ Lỗi khi gửi email tới {}: {}", to, e.getMessage());
        }
    }

    @Async("emailTaskExecutor")
    public void sendForgotPasswordEmailAsync(String to, String fullName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            // 💥 Đổi tiêu đề email
            helper.setSubject("Mã xác thực Khôi phục mật khẩu Echill");

            // 💥 Template HTML chuyên dụng cho Quên mật khẩu
            String htmlContent = String.format(
                    "<div style='font-family: Arial, sans-serif; padding: 20px; border: 1px solid #ddd; border-radius: 8px; max-width: 500px; margin: auto;'>" +
                            "<h2 style='color: #FF9800; text-align: center;'>Yêu cầu Khôi phục mật khẩu</h2>" +
                            "<p>Xin chào <b>%s</b>,</p>" +
                            "<p>Chúng tôi vừa nhận được yêu cầu khôi phục mật khẩu cho tài khoản Echill của bạn. Đây là mã xác thực (OTP) của bạn:</p>" +
                            "<div style='text-align: center; margin: 20px 0;'>" +
                            "<span style='font-size: 28px; font-weight: bold; background: #fef0d9; padding: 10px 20px; border-radius: 5px; letter-spacing: 5px; color: #d84315; border: 1px dashed #FF9800;'>%s</span>" +
                            "</div>" +
                            "<p style='color: red; font-size: 13px;'><i>* Mã này có hiệu lực trong vòng 5 phút.<br/>Nếu bạn <b>không</b> yêu cầu đổi mật khẩu, vui lòng bỏ qua email này. Tài khoản của bạn vẫn an toàn.</i></p>" +
                            "<hr style='border-top: 1px solid #eee;'/>" +
                            "<p style='font-size: 12px; color: #888; text-align: center;'>Đội ngũ hỗ trợ bảo mật Echill</p>" +
                            "</div>",
                    fullName, otp
            );

            helper.setText(htmlContent, true); // true để bật chế độ render HTML
            mailSender.send(message);

            log.info("📧 Đã gửi email Reset Password thành công tới {}", to);

        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi email Reset Password tới {}: {}", to, e.getMessage());
        }
    }
}
