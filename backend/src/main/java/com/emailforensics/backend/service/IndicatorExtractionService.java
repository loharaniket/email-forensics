package com.emailforensics.backend.service;

import com.emailforensics.backend.dto.ParsedHeaderResult;
import com.emailforensics.backend.entity.Indicator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IndicatorExtractionService {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>\"]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern IPV4_PATTERN = Pattern.compile("\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$");

    public List<Indicator> extractIndicators(Long emailId, ParsedHeaderResult parsed) {
        List<Indicator> indicators = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        if (parsed == null || emailId == null) {
            return indicators;
        }

        extractHeaderIndicators(emailId, parsed, indicators, seen);
        extractBodyIndicators(emailId, parsed.getBodyText(), indicators, seen);
        extractHtmlIndicators(emailId, parsed.getBodyHtml(), indicators, seen);

        return indicators;
    }

    private void extractHeaderIndicators(Long emailId, ParsedHeaderResult parsed,
                                         List<Indicator> indicators, Set<String> seen) {
        addDomainIndicator(emailId, parsed.getFromDomain(), "HEADER", indicators, seen);
        addDomainIndicator(emailId, parsed.getReplyToDomain(), "HEADER", indicators, seen);
        addDomainIndicator(emailId, parsed.getReturnPathDomain(), "HEADER", indicators, seen);

        List<Map<String, String>> hops = parsed.getReceivedHops();
        if (hops != null) {
            for (Map<String, String> hop : hops) {
                String ip = hop.get("ip");
                if (ip != null && isPublicIp(ip)) {
                    addIpIndicator(emailId, ip, "HEADER", indicators, seen);
                }
            }
        }
    }

    private void extractBodyIndicators(Long emailId, String bodyText,
                                       List<Indicator> indicators, Set<String> seen) {
        if (bodyText == null || bodyText.isBlank()) {
            return;
        }

        Matcher urlMatcher = URL_PATTERN.matcher(bodyText);
        while (urlMatcher.find()) {
            String rawUrl = urlMatcher.group();
            processExtractedUrl(emailId, rawUrl, "BODY", indicators, seen);
        }

        Matcher ipMatcher = IPV4_PATTERN.matcher(bodyText);
        while (ipMatcher.find()) {
            String ip = ipMatcher.group();
            if (isPublicIp(ip)) {
                addIpIndicator(emailId, ip, "BODY", indicators, seen);
            }
        }
    }

    private void extractHtmlIndicators(Long emailId, String bodyHtml,
                                       List<Indicator> indicators, Set<String> seen) {
        if (bodyHtml == null || bodyHtml.isBlank()) {
            return;
        }

        Document document = Jsoup.parse(bodyHtml);
        extractAttributes(document.select("a[href]"), "href", emailId, indicators, seen);
        extractAttributes(document.select("form[action]"), "action", emailId, indicators, seen);
        extractAttributes(document.select("img[src]"), "src", emailId, indicators, seen);

        Matcher ipMatcher = IPV4_PATTERN.matcher(document.text());
        while (ipMatcher.find()) {
            String ip = ipMatcher.group();
            if (isPublicIp(ip)) {
                addIpIndicator(emailId, ip, "HTML", indicators, seen);
            }
        }
    }

    private void extractAttributes(Elements elements, String attr, Long emailId,
                                   List<Indicator> indicators, Set<String> seen) {
        for (Element element : elements) {
            String val = element.attr(attr).trim();
            if (val.startsWith("http://") || val.startsWith("https://")) {
                processExtractedUrl(emailId, val, "HTML", indicators, seen);
            }
        }
    }

    private void processExtractedUrl(Long emailId, String rawUrl, String origin,
                                     List<Indicator> indicators, Set<String> seen) {
        String normalizedUrl = normalizeUrl(rawUrl);
        if (normalizedUrl == null) {
            return;
        }

        addIndicator(emailId, "URL", rawUrl, normalizedUrl, origin, indicators, seen);
        String host = extractHostFromUrl(normalizedUrl);
        if (host != null) {
            if (isPublicIp(host)) {
                addIpIndicator(emailId, host, origin, indicators, seen);
            } else if (isValidDomain(host)) {
                addDomainIndicator(emailId, host, origin, indicators, seen);
            }
        }
    }

    private void addDomainIndicator(Long emailId, String domain, String origin,
                                    List<Indicator> indicators, Set<String> seen) {
        if (domain == null || domain.isBlank()) {
            return;
        }
        String normalized = normalizeDomain(domain);
        if (isValidDomain(normalized)) {
            addIndicator(emailId, "DOMAIN", domain.trim(), normalized, origin, indicators, seen);
        }
    }

    private void addIpIndicator(Long emailId, String ip, String origin,
                                List<Indicator> indicators, Set<String> seen) {
        String normalized = normalizeIp(ip);
        if (normalized != null) {
            addIndicator(emailId, "IP", ip.trim(), normalized, origin, indicators, seen);
        }
    }

    private void addIndicator(Long emailId, String type, String value, String normalizedValue,
                              String origin, List<Indicator> indicators, Set<String> seen) {
        String key = type + "|" + origin + "|" + normalizedValue;
        if (seen.add(key)) {
            Indicator ind = new Indicator(emailId, type, value, normalizedValue, origin);
            indicators.add(ind);
        }
    }

    public String normalizeDomain(String domain) {
        if (domain == null) {
            return null;
        }
        String cleaned = domain.trim().toLowerCase(Locale.ROOT);
        int colonIndex = cleaned.indexOf(':');
        if (colonIndex > 0) {
            cleaned = cleaned.substring(0, colonIndex);
        }
        while (cleaned.endsWith(".")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }

    public String normalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        String unescaped = Parser.unescapeEntities(rawUrl.trim(), false);
        try {
            URI uri = URI.create(unescaped);
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : "http";
            String host = uri.getHost() != null ? uri.getHost().toLowerCase(Locale.ROOT) : "";
            if (host.isEmpty()) {
                return null;
            }
            int port = uri.getPort();
            String portPart = (port > 0 && port != 80 && port != 443) ? ":" + port : "";
            String path = uri.getRawPath() != null ? uri.getRawPath() : "";
            String query = uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "";
            return scheme + "://" + host + portPart + path + query;
        } catch (Exception e) {
            return fallbackNormalizeUrl(unescaped);
        }
    }

    private String fallbackNormalizeUrl(String url) {
        int hashIdx = url.indexOf('#');
        String noFragment = hashIdx > 0 ? url.substring(0, hashIdx) : url;
        int schemeEnd = noFragment.indexOf("://");
        if (schemeEnd > 0) {
            String scheme = noFragment.substring(0, schemeEnd).toLowerCase(Locale.ROOT);
            String rest = noFragment.substring(schemeEnd + 3);
            int slashIdx = rest.indexOf('/');
            if (slashIdx > 0) {
                String host = rest.substring(0, slashIdx).toLowerCase(Locale.ROOT);
                return scheme + "://" + host + rest.substring(slashIdx);
            }
            return scheme + "://" + rest.toLowerCase(Locale.ROOT);
        }
        return noFragment;
    }

    public String normalizeIp(String ip) {
        if (ip == null) {
            return null;
        }
        String cleaned = ip.replaceAll("[\\[\\]\\s]", "").trim();
        return cleaned.toLowerCase(Locale.ROOT);
    }

    public boolean isValidDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return false;
        }
        return DOMAIN_PATTERN.matcher(domain).matches();
    }

    public boolean isPublicIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String cleaned = normalizeIp(ip);
        if (IPV4_PATTERN.matcher(cleaned).matches()) {
            return !isPrivateIpv4(cleaned);
        }
        if (cleaned.contains(":")) {
            try {
                InetAddress address = InetAddress.getByName(cleaned);
                return !address.isLoopbackAddress()
                        && !address.isLinkLocalAddress()
                        && !address.isSiteLocalAddress()
                        && !address.isMulticastAddress()
                        && !address.isAnyLocalAddress();
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private boolean isPrivateIpv4(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return true;
        }
        try {
            int a = Integer.parseInt(parts[0]);
            int b = Integer.parseInt(parts[1]);
            if (a == 0 || a == 10 || a == 127) {
                return true;
            }
            if (a == 169 && b == 254) {
                return true;
            }
            if (a == 172 && (b >= 16 && b <= 31)) {
                return true;
            }
            if (a == 192 && b == 168) {
                return true;
            }
            if (a >= 224) {
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private String extractHostFromUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return host != null ? host.toLowerCase(Locale.ROOT) : null;
        } catch (Exception e) {
            int schemeEnd = url.indexOf("://");
            if (schemeEnd > 0) {
                String afterScheme = url.substring(schemeEnd + 3);
                int slashIdx = afterScheme.indexOf('/');
                String host = slashIdx > 0 ? afterScheme.substring(0, slashIdx) : afterScheme;
                int colonIdx = host.indexOf(':');
                return colonIdx > 0 ? host.substring(0, colonIdx) : host;
            }
            return null;
        }
    }
}
