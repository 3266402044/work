package com.classpulse.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

public final class Utils {
    private static final SecureRandom RANDOM = new SecureRandom();

    private Utils() {
    }

    public static String randomDigits(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    public static String randomToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Haversine 公式，返回米，保留两位小数 */
    public static BigDecimal distanceMeters(BigDecimal lon1, BigDecimal lat1, BigDecimal lon2, BigDecimal lat2) {
        double radLat1 = Math.toRadians(lat1.doubleValue());
        double radLat2 = Math.toRadians(lat2.doubleValue());
        double deltaLat = radLat2 - radLat1;
        double deltaLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double meters = 6371000.0 * c;
        return BigDecimal.valueOf(meters).setScale(2, RoundingMode.HALF_UP);
    }

    public static List<String> splitModes(String modes) {
        if (modes == null || modes.isBlank()) {
            return List.of();
        }
        return List.of(modes.split(",")).stream().map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public static double rate(long part, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(part * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
