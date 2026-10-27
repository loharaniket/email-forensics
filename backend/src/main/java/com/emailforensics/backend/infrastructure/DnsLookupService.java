package com.emailforensics.backend.infrastructure;

import com.emailforensics.backend.infrastructure.fixtures.DefaultDnsFixtureProvider;
import com.emailforensics.backend.infrastructure.fixtures.DnsFixtureProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
    private final DnsFixtureProvider fixtureProvider;
    private final Environment environment;
    private final Map<String, DnsResult> cache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> txtCache = new ConcurrentHashMap<>();

    @Autowired
    public DnsLookupService(
            @Value("${emailforensics.dns.timeout-ms:2000}") long timeoutMs,
            @Autowired(required = false) DnsFixtureProvider fixtureProvider,
            @Autowired(required = false) Environment environment) {
        this.timeoutMs = timeoutMs;
        this.fixtureProvider = fixtureProvider;
        this.environment = environment;
    }

    public DnsLookupService(long timeoutMs) {
        this(timeoutMs, new DefaultDnsFixtureProvider(), null);
    }

    public DnsLookupService(long timeoutMs, DnsFixtureProvider fixtureProvider) {
        this(timeoutMs, fixtureProvider, null);
    }

    private boolean isProduction() {
        return environment != null && environment.acceptsProfiles(Profiles.of("prod", "production"));
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
            return new DnsResult(domain, "SKIPPED", new ArrayList<>(), new ArrayList<>(), DataProvenance.NOT_CHECKED.name());
        }

        String normalized = domain.trim().toLowerCase();
        if (cache.containsKey(normalized)) {
            return cache.get(normalized);
        }

        if (!isProduction() && fixtureProvider != null && fixtureProvider.supports(normalized)) {
            DnsResult simulated = fixtureProvider.getSimulatedDnsResult(normalized);
            cache.put(normalized, simulated);
            return simulated;
        }

        try {
            CompletableFuture<DnsResult> future = CompletableFuture.supplyAsync(() -> performLiveLookup(normalized));
            DnsResult result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            result.setDataMode(DataProvenance.REAL.name());
            cache.put(normalized, result);
            return result;
        } catch (TimeoutException e) {
            DnsResult timeoutResult = new DnsResult(normalized, "TIMEOUT", new ArrayList<>(), new ArrayList<>(), DataProvenance.UNAVAILABLE.name());
            cache.put(normalized, timeoutResult);
            return timeoutResult;
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof UnknownHostException) {
                DnsResult nxResult = new DnsResult(normalized, "NXDOMAIN", new ArrayList<>(), new ArrayList<>(), DataProvenance.REAL.name());
                cache.put(normalized, nxResult);
                return nxResult;
            }
            DnsResult failedResult = new DnsResult(normalized, "LOOKUP_FAILED", new ArrayList<>(), new ArrayList<>(), DataProvenance.UNAVAILABLE.name());
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
            return new DnsResult(domain, "NXDOMAIN", aRecords, mxRecords, DataProvenance.REAL.name());
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
        return new DnsResult(domain, status, aRecords, mxRecords, DataProvenance.REAL.name());
    }

    public List<String> lookupTxtRecords(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalized = domain.trim().toLowerCase();
        if (txtCache.containsKey(normalized)) {
            return txtCache.get(normalized);
        }

        if (!isProduction() && fixtureProvider != null && fixtureProvider.supportsTxt(normalized)) {
            List<String> simulated = fixtureProvider.getSimulatedTxtRecords(normalized);
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
}
