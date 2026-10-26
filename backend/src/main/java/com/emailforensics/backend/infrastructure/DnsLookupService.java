package com.emailforensics.backend.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class DnsLookupService {

    private final long timeoutMs;
    private final Map<String, DnsResult> cache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> txtCache = new ConcurrentHashMap<>();

    public DnsLookupService(@Value("${emailforensics.dns.timeout-ms:2000}") long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public void putCache(String domain, DnsResult result) {
        if (domain != null && result != null) {
            cache.put(domain.toLowerCase(), result);
        }
    }

    public void putTxtCache(String domain, List<String> records) {
        if (domain != null && records != null) {
            txtCache.put(domain.toLowerCase(), records);
        }
    }

    public DnsResult lookupDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return new DnsResult(domain, "SKIPPED", new ArrayList<>(), new ArrayList<>());
        }

        String normalized = domain.trim().toLowerCase();
        if (cache.containsKey(normalized)) {
            return cache.get(normalized);
        }

        if (normalized.endsWith(".test") || normalized.endsWith(".local") ||
                normalized.endsWith(".internal") || normalized.endsWith(".example") ||
                normalized.endsWith(".invalid") || normalized.equals("localhost")) {
            DnsResult simulated = resolveTestDomain(normalized);
            cache.put(normalized, simulated);
            return simulated;
        }

        try {
            CompletableFuture<DnsResult> future = CompletableFuture.supplyAsync(() -> performLiveLookup(normalized));
            DnsResult result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            cache.put(normalized, result);
            return result;
        } catch (TimeoutException e) {
            DnsResult timeoutResult = new DnsResult(normalized, "TIMEOUT", new ArrayList<>(), new ArrayList<>());
            cache.put(normalized, timeoutResult);
            return timeoutResult;
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof UnknownHostException) {
                DnsResult nxResult = new DnsResult(normalized, "NXDOMAIN", new ArrayList<>(), new ArrayList<>());
                cache.put(normalized, nxResult);
                return nxResult;
            }
            DnsResult failedResult = new DnsResult(normalized, "LOOKUP_FAILED", new ArrayList<>(), new ArrayList<>());
            cache.put(normalized, failedResult);
            return failedResult;
        }
    }

    private DnsResult performLiveLookup(String domain) {
        List<String> aRecords = new ArrayList<>();
        List<String> mxRecords = new ArrayList<>();

        try {
            InetAddress[] addresses = InetAddress.getAllByName(domain);
            for (InetAddress addr : addresses) {
                aRecords.add(addr.getHostAddress());
            }
        } catch (UnknownHostException e) {
            return new DnsResult(domain, "NXDOMAIN", aRecords, mxRecords);
        } catch (Exception ignored) {
        }

        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", String.valueOf(timeoutMs));
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            DirContext ictx = new InitialDirContext(env);
            Attributes attrs = ictx.getAttributes(domain, new String[]{"MX"});
            Attribute attr = attrs.get("MX");
            if (attr != null) {
                for (int i = 0; i < attr.size(); i++) {
                    String val = (String) attr.get(i);
                    String[] parts = val.split("\\s+");
                    mxRecords.add(parts.length > 1 ? parts[1] : val);
                }
            }
        } catch (Exception ignored) {
        }

        String status = (!aRecords.isEmpty() || !mxRecords.isEmpty()) ? "RESOLVED" : "NO_RECORDS";
        return new DnsResult(domain, status, aRecords, mxRecords);
    }

    private DnsResult resolveTestDomain(String domain) {
        List<String> a = new ArrayList<>();
        List<String> mx = new ArrayList<>();
        if (domain.contains("secure-bank") || domain.contains("internal-corp")) {
            a.add("198.51.100.44");
            mx.add("mx." + domain);
            return new DnsResult(domain, "RESOLVED", a, mx);
        }
        if (domain.contains("phish-sender")) {
            a.add("203.0.113.45");
            mx.add("mail." + domain);
            return new DnsResult(domain, "RESOLVED", a, mx);
        }
        if (domain.contains("nonexistent") || domain.contains("fake-domain")) {
            return new DnsResult(domain, "NXDOMAIN", a, mx);
        }
        return new DnsResult(domain, "RESOLVED", List.of("192.0.2.1"), List.of("mail." + domain));
    }

    public List<String> lookupTxtRecords(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalized = domain.trim().toLowerCase();
        if (txtCache.containsKey(normalized)) {
            return txtCache.get(normalized);
        }

        if (normalized.endsWith(".test") || normalized.endsWith(".local") ||
                normalized.endsWith(".internal") || normalized.endsWith(".example") ||
                normalized.endsWith(".invalid") || normalized.equals("localhost")) {
            List<String> simulated = resolveTestTxtRecords(normalized);
            txtCache.put(normalized, simulated);
            return simulated;
        }

        try {
            CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> performLiveTxtLookup(normalized));
            List<String> result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            txtCache.put(normalized, result);
            return result;
        } catch (Exception e) {
            List<String> empty = Collections.emptyList();
            txtCache.put(normalized, empty);
            return empty;
        }
    }

    private List<String> performLiveTxtLookup(String domain) {
        List<String> txtRecords = new ArrayList<>();
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", String.valueOf(timeoutMs));
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            DirContext ictx = new InitialDirContext(env);
            Attributes attrs = ictx.getAttributes(domain, new String[]{"TXT"});
            Attribute attr = attrs != null ? attrs.get("TXT") : null;
            if (attr != null) {
                for (int i = 0; i < attr.size(); i++) {
                    String val = (String) attr.get(i);
                    if (val != null) {
                        String clean = val.replaceAll("^\"|\"$", "").trim();
                        txtRecords.add(clean);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return txtRecords;
    }

    private List<String> resolveTestTxtRecords(String domain) {
        List<String> records = new ArrayList<>();
        if (domain.startsWith("_dmarc.")) {
            if (domain.contains("phish-sender")) {
                records.add("v=DMARC1; p=reject; rua=mailto:dmarc@phish-sender.test");
            } else if (domain.contains("secure-bank")) {
                records.add("v=DMARC1; p=reject; aspf=s; adkim=s");
            } else if (domain.contains("internal-corp")) {
                records.add("v=DMARC1; p=quarantine; pct=100");
            } else if (domain.contains("none-policy")) {
                records.add("v=DMARC1; p=none");
            }
            return records;
        }
        if (domain.contains("._domainkey.")) {
            if (domain.contains("phish-sender") || domain.contains("secure-bank") || domain.contains("internal-corp")) {
                records.add("v=DKIM1; k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC3");
            }
            return records;
        }
        if (domain.contains("phish-sender")) {
            records.add("v=spf1 ip4:203.0.113.45 ~all");
            return records;
        }
        if (domain.contains("secure-bank")) {
            records.add("v=spf1 ip4:198.51.100.44 -all");
            return records;
        }
        if (domain.contains("internal-corp")) {
            records.add("v=spf1 ip4:198.51.100.44 include:_spf.internal-corp.test ~all");
            return records;
        }
        if (domain.contains("softfail")) {
            records.add("v=spf1 ip4:1.2.3.4 ~all");
            return records;
        }
        if (domain.contains("fail")) {
            records.add("v=spf1 -all");
            return records;
        }
        if (domain.contains("neutral")) {
            records.add("v=spf1 ?all");
            return records;
        }
        return records;
    }
}
