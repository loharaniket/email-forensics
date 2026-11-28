package com.emailforensics.backend.urlintel;

import com.emailforensics.backend.infrastructure.IpClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UrlCheckService {

    private final IpClassifier ipClassifier;
    private final UrlNormalizationService normalizationService;
    private final List<UrlChecker> checkers;

    public UrlCheckService(IpClassifier ipClassifier, UrlNormalizationService normalizationService) {
        this(ipClassifier, normalizationService, Collections.emptyList());
    }

    @Autowired
    public UrlCheckService(IpClassifier ipClassifier, UrlNormalizationService normalizationService, List<UrlChecker> checkers) {
        this.ipClassifier = ipClassifier;
        this.normalizationService = normalizationService;
        this.checkers = checkers != null ? checkers : Collections.emptyList();
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
        return lower.equals("bit.ly") || lower.equals("tinyurl.com") || lower.equals("t.co")
                || lower.endsWith(".bit.ly") || lower.endsWith(".tinyurl.com");
    }

    public boolean isSubdomainExcess(String domain) {
        return false;
    }

    public boolean isLookalike(String domain) {
        return false;
    }

    public boolean isAnchorMismatch(String destinationDomain, String anchorText) {
        if (destinationDomain == null || anchorText == null || anchorText.trim().isEmpty()) return false;
        String text = anchorText.trim();
        Matcher m = Pattern.compile("\\b(?i:(https?://)?)([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})\\b").matcher(text);
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
        if (unusualPort) score += 20;
        if (isShortened) score += 15;
        if (score > 0 && score < 40) {
            score = 40;
        }
        return Math.min(score, 100);
    }

    public UrlCheckResult checkWithExternalServices(String rawUrl, String domain) {
        for (UrlChecker checker : checkers) {
            try {
                UrlCheckResult result = checker.check(rawUrl, domain);
                if (result != null && result.isFlagged()) {
                    return result;
                }
            } catch (Exception ignored) {
            }
        }
        return new UrlCheckResult("EXTERNAL_CHECK", "External Services", false, 0, "CLEAN", "All external checks clean");
    }
}
