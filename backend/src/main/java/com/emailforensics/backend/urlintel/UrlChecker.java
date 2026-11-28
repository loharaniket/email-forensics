package com.emailforensics.backend.urlintel;

public interface UrlChecker {
    String getCheckName();
    UrlCheckResult check(String url, String domain);
}
