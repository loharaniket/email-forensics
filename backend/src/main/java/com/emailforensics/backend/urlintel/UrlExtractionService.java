package com.emailforensics.backend.urlintel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UrlExtractionService {

    private static final Pattern URL_REGEX = Pattern.compile(
            "\\b(?i:(https?|ftp|hxxps?):\\/\\/|www\\.)[a-zA-Z0-9+&@#/%?=~_|!:,.;\\[\\]-]*[a-zA-Z0-9+&@#/%=~_|]"
    );

    public static class RawExtractedUrl {
        private String rawUrl;
        private String sourceType;
        private String anchorText;

        public RawExtractedUrl(String rawUrl, String sourceType, String anchorText) {
            this.rawUrl = rawUrl;
            this.sourceType = sourceType;
            this.anchorText = anchorText;
        }

        public String getRawUrl() {
            return rawUrl;
        }

        public String getSourceType() {
            return sourceType;
        }

        public String getAnchorText() {
            return anchorText;
        }
    }

    public List<RawExtractedUrl> extractUrls(String plainText, String htmlBody) {
        List<RawExtractedUrl> results = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        if (htmlBody != null && !htmlBody.trim().isEmpty()) {
            try {
                Document doc = Jsoup.parse(htmlBody);
                Elements anchors = doc.select("a[href]");
                for (Element a : anchors) {
                    String href = a.attr("href").trim();
                    String text = a.text().trim();
                    if (!href.isEmpty() && !href.startsWith("mailto:") && !href.startsWith("javascript:") && !href.startsWith("#")) {
                        String clean = cleanUrl(href);
                        if (!clean.isEmpty()) {
                            String key = clean + "||HTML_ANCHOR||" + text;
                            if (seen.add(key)) {
                                results.add(new RawExtractedUrl(clean, "HTML_ANCHOR", text.isEmpty() ? null : text));
                            }
                        }
                    }
                }

                Document cleanDoc = doc.clone();
                cleanDoc.select("a").remove();
                String bodyText = cleanDoc.text();
                Matcher m = URL_REGEX.matcher(bodyText);
                while (m.find()) {
                    String found = cleanUrl(m.group());
                    if (!found.isEmpty()) {
                        String key = found + "||HTML_BODY||null";
                        if (seen.add(key)) {
                            results.add(new RawExtractedUrl(found, "HTML_BODY", null));
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (plainText != null && !plainText.trim().isEmpty()) {
            Matcher m = URL_REGEX.matcher(plainText);
            while (m.find()) {
                String found = cleanUrl(m.group());
                if (!found.isEmpty()) {
                    String key = found + "||PLAIN_TEXT||null";
                    if (seen.add(key)) {
                        results.add(new RawExtractedUrl(found, "PLAIN_TEXT", null));
                    }
                }
            }
        }

        return results;
    }

    public String defang(String url) {
        if (url == null) return null;
        String s = url.trim();
        s = s.replaceAll("(?i)^hxxp://", "http://");
        s = s.replaceAll("(?i)^hxxps://", "https://");
        s = s.replaceAll("\\[\\.\\]", ".");
        s = s.replaceAll("\\(\\.\\)", ".");
        s = s.replaceAll("\\[:\\]", ":");
        return s;
    }

    private String cleanUrl(String raw) {
        if (raw == null) return "";
        String s = defang(raw.trim());

        while (s.endsWith(".") || s.endsWith(",") || s.endsWith(";") || s.endsWith(")") || s.endsWith("]") || s.endsWith(">") || s.endsWith("\"") || s.endsWith("'")) {
            if (s.endsWith(")") && s.contains("(")) {
                break;
            }
            if (s.endsWith("]") && s.contains("[")) {
                break;
            }
            s = s.substring(0, s.length() - 1).trim();
        }

        if (s.toLowerCase().startsWith("www.")) {
            s = "http://" + s;
        }

        return s;
    }
}
