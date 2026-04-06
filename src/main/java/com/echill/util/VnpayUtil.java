package com.echill.util;

import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
public final  class VnpayUtil {

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "CF-Connecting-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
    };

    /**
     * Lấy IP thực của Client xuyên qua Load Balancer, Cloudflare, Nginx...
     */
    public static String getIpAddress(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                String realIp = ip.split(",")[0].trim();
                if (!realIp.isEmpty()) {
                    return realIp;
                }
            }
        }

        String ip = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

    /**
     * Build chuỗi Query String chuẩn VNPAY (UTF-8)
     */
    public static String buildQueryUrl(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                hashData.append(fieldName)
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8))
                        .append('&');
            }
        }
        if (!hashData.isEmpty()) {
            hashData.setLength(hashData.length() - 1);
        }

        return hashData.toString();
    }

    /**
     * Lọc rác và băm toàn bộ payload
     */
    public static String hashAllFields(Map<String, String> fields, String secretKey) {
        Map<String, String> filtered = new HashMap<>(fields);
        filtered.remove("vnp_SecureHash");
        filtered.remove("vnp_SecureHashType");

        return hmacSHA512(secretKey, buildQueryUrl(filtered));
    }

    /**
     * Thuật toán HMAC SHA-512 siêu tốc
     */
    public static String hmacSHA512(final String key, final String data) {
        if (key == null || data == null) {
            throw new IllegalArgumentException("Key and data for HMAC-SHA512 cannot be null");
        }

        try {
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);

            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            char[] hexChars = new char[result.length * 2];
            for (int j = 0; j < result.length; j++) {
                int v = result[j] & 0xFF;
                hexChars[j * 2] = HEX_ARRAY[v >>> 4];
                hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
            }
            return new String(hexChars);

        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            log.error("Error generating HMAC SHA512 for data: {}", data, ex);
            throw new AppException(ErrorEnum.VNPAY_SIGNATURE_GENERATION_FAILED);
        }
    }

    /**
     * Xác thực chữ ký
     */
    public static boolean verifySignature(Map<String, String> fields, String secureHash, String secretKey) {
        if (secureHash == null || secureHash.isEmpty()) return false;

        Map<String, String> filtered = new HashMap<>(fields);
        filtered.remove("vnp_SecureHash");
        filtered.remove("vnp_SecureHashType");

        String signValue = hmacSHA512(secretKey, buildQueryUrl(filtered));
        return signValue.equals(secureHash);
    }
}