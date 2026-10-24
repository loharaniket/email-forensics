package com.emailforensics.backend.infrastructure;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.regex.Pattern;

@Component
public class IpClassifier {

    private static final Pattern IPV4_PATTERN = Pattern.compile("^([0-9]{1,3}\\.){3}[0-9]{1,3}$");
    private static final Pattern IPV6_PATTERN = Pattern.compile("^[a-fA-F0-9:]+$");

    public boolean isIpAddress(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        String clean = str.trim();
        if (clean.startsWith("[") && clean.endsWith("]")) {
            clean = clean.substring(1, clean.length() - 1);
        }
        return isIpv4(clean) || isIpv6(clean);
    }

    public boolean isIpv4(String str) {
        if (str == null) return false;
        String clean = str.trim();
        if (!IPV4_PATTERN.matcher(clean).matches()) {
            return false;
        }
        String[] parts = clean.split("\\.");
        if (parts.length != 4) return false;
        for (String p : parts) {
            try {
                int octet = Integer.parseInt(p);
                if (octet < 0 || octet > 255) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public boolean isIpv6(String str) {
        if (str == null) return false;
        String clean = str.trim();
        if (clean.startsWith("[") && clean.endsWith("]")) {
            clean = clean.substring(1, clean.length() - 1);
        }
        if (!clean.contains(":") || !IPV6_PATTERN.matcher(clean).matches()) {
            return false;
        }
        try {
            InetAddress addr = InetAddress.getByName(clean);
            return addr.getAddress().length == 16;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isPrivateOrReserved(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        String clean = ip.trim();
        if (clean.startsWith("[") && clean.endsWith("]")) {
            clean = clean.substring(1, clean.length() - 1);
        }

        if (isIpv4(clean)) {
            String[] parts = clean.split("\\.");
            int o1 = Integer.parseInt(parts[0]);
            int o2 = Integer.parseInt(parts[1]);
            int o3 = Integer.parseInt(parts[2]);

            if (o1 == 10) return true;
            if (o1 == 127) return true;
            if (o1 == 0) return true;
            if (o1 == 192 && o2 == 168) return true;
            if (o1 == 172 && (o2 >= 16 && o2 <= 31)) return true;
            if (o1 == 169 && o2 == 254) return true;
            if (o1 == 100 && (o2 >= 64 && o2 <= 127)) return true;
            if (o1 == 192 && o2 == 0 && o3 == 2) return true;
            if (o1 == 198 && o2 == 51 && o3 == 100) return true;
            if (o1 == 203 && o2 == 0 && o3 == 113) return true;
            if (o1 >= 224) return true;

            return false;
        }

        if (isIpv6(clean)) {
            String lower = clean.toLowerCase();
            if (lower.equals("::1") || lower.equals("0:0:0:0:0:0:0:1")) return true;
            if (lower.startsWith("fe8") || lower.startsWith("fe9") || lower.startsWith("fea") || lower.startsWith("feb")) return true;
            if (lower.startsWith("fc") || lower.startsWith("fd")) return true;
            if (lower.equals("::")) return true;
        }

        return false;
    }
}
