package com.emailforensics.backend.urlintel;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class UrlNormalizationService {

    private static final Set<String> TRACKING_PARAMS = new HashSet<>(Arrays.asList(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "fbclid", "gclid", "msclkid", "mc_cid", "mc_eid"
    ));

    private static final Set<String> TWO_LEVEL_TLDS = new HashSet<>(Arrays.asList(
            "co.uk", "org.uk", "gov.uk", "ac.uk", "me.uk",
            "com.au", "net.au", "org.au", "edu.au",
            "co.nz", "net.nz", "org.nz",
            "co.jp", "ne.jp", "ac.jp",
            "co.in", "net.in", "org.in", "gen.in",
            "com.br", "net.br", "org.br",
            "com.cn", "net.cn", "org.cn"
    ));

    public static class NormalizedUrlResult {
        private String normalizedUrl;
        private String domain;
        private String registeredDomain;
        private int port;

        public NormalizedUrlResult(String normalizedUrl, String domain, String registeredDomain, int port) {
            this.normalizedUrl = normalizedUrl;
            this.domain = domain;
            this.registeredDomain = registeredDomain;
            this.port = port;
        }

        public String getNormalizedUrl() {
            return normalizedUrl;
        }

        public String getDomain() {
            return domain;
        }

        public String getRegisteredDomain() {
            return registeredDomain;
        }

        public int getPort() {
            return port;
        }
    }

    public NormalizedUrlResult normalize(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return new NormalizedUrlResult("", "", "", -1);
        }

        String input = rawUrl.trim();
        if (!input.contains("://")) {
            input = "http://" + input;
        }

        try {
            URI uri = URI.create(input);
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : "http";
            String host = uri.getHost();
            int port = uri.getPort();

            if (host == null) {
                int start = input.indexOf("://") + 3;
                int end = input.indexOf('/', start);
                int colon = input.indexOf(':', start);
                if (end == -1) end = input.length();
                if (colon != -1 && colon < end) {
                    host = input.substring(start, colon);
                    try {
                        port = Integer.parseInt(input.substring(colon + 1, end));
                    } catch (Exception ignored) {
                    }
                } else {
                    host = input.substring(start, end);
                }
            }

            if (host != null) {
                host = host.toLowerCase(Locale.ROOT);
                while (host.endsWith(".")) {
                    host = host.substring(0, host.length() - 1);
                }
            } else {
                host = "";
            }

            boolean isDefaultPort = (scheme.equals("http") && port == 80) ||
                    (scheme.equals("https") && port == 443);
            int effectivePort = isDefaultPort ? -1 : port;

            String path = uri.getRawPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }

            String query = uri.getRawQuery();
            String cleanQuery = filterTrackingParams(query);

            StringBuilder sb = new StringBuilder();
            sb.append(scheme).append("://").append(host);
            if (effectivePort != -1) {
                sb.append(":").append(effectivePort);
            }
            sb.append(path);
            if (cleanQuery != null && !cleanQuery.isEmpty()) {
                sb.append("?").append(cleanQuery);
            }

            String registeredDomain = extractRegisteredDomain(host);

            return new NormalizedUrlResult(sb.toString(), host, registeredDomain, effectivePort);

        } catch (Exception e) {
            String cleanHost = extractHostSimple(input);
            return new NormalizedUrlResult(input, cleanHost, extractRegisteredDomain(cleanHost), -1);
        }
    }

    public String extractRegisteredDomain(String host) {
        if (host == null || host.isEmpty()) {
            return "";
        }
        String clean = host.toLowerCase(Locale.ROOT);
        String[] parts = clean.split("\\.");
        if (parts.length <= 1) {
            return clean;
        }

        if (parts.length >= 3) {
            String lastTwo = parts[parts.length - 2] + "." + parts[parts.length - 1];
            if (TWO_LEVEL_TLDS.contains(lastTwo)) {
                return parts[parts.length - 3] + "." + lastTwo;
            }
        }

        return parts[parts.length - 2] + "." + parts[parts.length - 1];
    }

    private String filterTrackingParams(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        String[] pairs = query.split("&");
        StringBuilder sb = new StringBuilder();
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            String key = kv[0].toLowerCase(Locale.ROOT);
            if (!TRACKING_PARAMS.contains(key)) {
                if (!sb.isEmpty()) {
                    sb.append("&");
                }
                sb.append(pair);
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private String extractHostSimple(String url) {
        try {
            int start = url.indexOf("://");
            start = start == -1 ? 0 : start + 3;
            int end = url.indexOf('/', start);
            if (end == -1) end = url.indexOf('?', start);
            if (end == -1) end = url.indexOf('#', start);
            if (end == -1) end = url.length();
            String hostPort = url.substring(start, end);
            int colon = hostPort.indexOf(':');
            return colon == -1 ? hostPort.toLowerCase(Locale.ROOT) : hostPort.substring(0, colon).toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return "";
        }
    }
}
