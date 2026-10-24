package com.emailforensics.backend.urlintel;

import com.emailforensics.backend.infrastructure.IpClassifier;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UrlHeuristicsService {

    private final IpClassifier ipClassifier;
    private final UrlNormalizationService normalizationService;

    private static final Set<String> SHORTENER_DOMAINS = new HashSet<>(Arrays.asList(
            "bit.ly", "tinyurl.com", "t.co", "is.gd", "ow.ly", "buff.ly",
            "goo.gl", "cutt.ly", "rb.gy", "shorturl.at", "clck.ru",
            "rebrand.ly", "snip.ly", "tiny.cc", "v.gd", "qr.net",
            "1url.com", "bc.vc", "shorte.st"
    ));

    private static final Set<String> HIGH_VALUE_BRANDS = new HashSet<>(Arrays.asList(
            "paypal", "microsoft", "google", "apple", "amazon", "chase",
            "wellsfargo", "netflix", "bankofamerica", "dhl", "fedex",
            "outlook", "office365", "dropbox", "facebook", "instagram"
    ));

    private static final Pattern DOMAIN_IN_TEXT_PATTERN = Pattern.compile(
            "\\b(?i:(https?://)?)([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})\\b"
    );

    public UrlHeuristicsService(IpClassifier ipClassifier, UrlNormalizationService normalizationService) {
        this.ipClassifier = ipClassifier;
        this.normalizationService = normalizationService;
    }

    public boolean isIpUrl(String domain) {
        if (domain == null) return false;
        return ipClassifier.isIpAddress(domain);
    }

    public boolean isUnusualPort(int port) {
        if (port == -1) return false;
        return port != 80 && port != 443 && port != 8080;
    }

    public boolean isShortened(String domain) {
        if (domain == null) return false;
        String lower = domain.toLowerCase(Locale.ROOT);
        return SHORTENER_DOMAINS.contains(lower) ||
                SHORTENER_DOMAINS.contains(normalizationService.extractRegisteredDomain(lower));
    }

    public boolean isSubdomainExcess(String domain) {
        if (domain == null || ipClassifier.isIpAddress(domain)) return false;
        String registered = normalizationService.extractRegisteredDomain(domain);
        if (registered.isEmpty() || !domain.endsWith(registered)) return false;

        String prefix = domain.substring(0, domain.length() - registered.length());
        if (prefix.endsWith(".")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        if (prefix.isEmpty()) return false;

        String[] subparts = prefix.split("\\.");
        return subparts.length >= 3;
    }

    public boolean isLookalike(String domain) {
        if (domain == null || ipClassifier.isIpAddress(domain)) return false;
        String lower = domain.toLowerCase(Locale.ROOT);

        if (lower.startsWith("xn--") || lower.contains(".xn--")) {
            return true;
        }

        String registered = normalizationService.extractRegisteredDomain(lower);
        String nameWithoutTld = registered.contains(".")
                ? registered.substring(0, registered.indexOf('.'))
                : registered;

        for (String brand : HIGH_VALUE_BRANDS) {
            if (nameWithoutTld.equals(brand)) {
                continue;
            }

            if (nameWithoutTld.contains(brand + "-") || nameWithoutTld.contains("-" + brand) ||
                    nameWithoutTld.contains(brand + "verify") || nameWithoutTld.contains(brand + "login") ||
                    nameWithoutTld.contains(brand + "security") || nameWithoutTld.contains(brand + "support") ||
                    nameWithoutTld.contains(brand + "portal")) {
                return true;
            }

            String dehomoglyphed = nameWithoutTld
                    .replace('0', 'o')
                    .replace('1', 'l')
                    .replace("vv", "w")
                    .replace("rn", "m");

            if (dehomoglyphed.equals(brand) || dehomoglyphed.contains(brand)) {
                return true;
            }
        }

        return false;
    }

    public boolean isAnchorMismatch(String destinationDomain, String anchorText) {
        if (destinationDomain == null || anchorText == null || anchorText.trim().isEmpty()) {
            return false;
        }

        String text = anchorText.trim();
        Matcher m = DOMAIN_IN_TEXT_PATTERN.matcher(text);
        if (m.find()) {
            String anchorDomain = m.group(2).toLowerCase(Locale.ROOT);
            String normDest = destinationDomain.toLowerCase(Locale.ROOT);
            if (!normDest.equals(anchorDomain) && !normDest.endsWith("." + anchorDomain)) {
                String regDest = normalizationService.extractRegisteredDomain(normDest);
                String regAnchor = normalizationService.extractRegisteredDomain(anchorDomain);
                return !regDest.equalsIgnoreCase(regAnchor);
            }
        }

        return false;
    }

    public int calculateRiskScore(boolean anchorMismatch, boolean isIpUrl, boolean lookalike,
                                  boolean unusualPort, boolean subdomainExcess, boolean isShortened) {
        int score = 0;
        if (anchorMismatch) score += 40;
        if (isIpUrl) score += 35;
        if (lookalike) score += 30;
        if (unusualPort) score += 20;
        if (subdomainExcess) score += 15;
        if (isShortened) score += 15;

        return Math.min(score, 100);
    }
}
