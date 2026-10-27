package com.emailforensics.backend.infrastructure.fixtures;

import com.emailforensics.backend.infrastructure.DataProvenance;
import com.emailforensics.backend.infrastructure.DnsResult;

import java.util.ArrayList;
import java.util.List;

public class DefaultDnsFixtureProvider implements DnsFixtureProvider {

    @Override
    public boolean supports(String domain) {
        if (domain == null) {
            return false;
        }
        String normalized = domain.trim().toLowerCase();
        return normalized.endsWith(".test") || normalized.endsWith(".local") ||
                normalized.endsWith(".internal") || normalized.endsWith(".example") ||
                normalized.endsWith(".invalid") || normalized.equals("localhost");
    }

    @Override
    public DnsResult getSimulatedDnsResult(String domain) {
        if (domain == null) {
            return new DnsResult(null, "SKIPPED", new ArrayList<>(), new ArrayList<>(), DataProvenance.NOT_CHECKED.name());
        }
        String normalized = domain.trim().toLowerCase();
        List<String> a = new ArrayList<>();
        List<String> mx = new ArrayList<>();

        if (normalized.contains("secure-bank") || normalized.contains("internal-corp")) {
            a.add("198.51.100.44");
            mx.add("mx." + normalized);
            return new DnsResult(normalized, "RESOLVED", a, mx, DataProvenance.SIMULATED.name());
        }
        if (normalized.contains("phish-sender")) {
            a.add("203.0.113.45");
            mx.add("mail." + normalized);
            return new DnsResult(normalized, "RESOLVED", a, mx, DataProvenance.SIMULATED.name());
        }
        if (normalized.contains("nonexistent") || normalized.contains("fake-domain")) {
            return new DnsResult(normalized, "NXDOMAIN", a, mx, DataProvenance.SIMULATED.name());
        }
        return new DnsResult(normalized, "RESOLVED", List.of("192.0.2.1"), List.of("mail." + normalized), DataProvenance.SIMULATED.name());
    }

    @Override
    public boolean supportsTxt(String domain) {
        if (domain == null) {
            return false;
        }
        String normalized = domain.trim().toLowerCase();
        return normalized.endsWith(".test") || normalized.endsWith(".local") ||
                normalized.endsWith(".internal") || normalized.endsWith(".example") ||
                normalized.endsWith(".invalid") || normalized.equals("localhost");
    }

    @Override
    public List<String> getSimulatedTxtRecords(String domain) {
        if (domain == null) {
            return new ArrayList<>();
        }
        String normalized = domain.trim().toLowerCase();
        List<String> records = new ArrayList<>();

        if (normalized.startsWith("_dmarc.")) {
            if (normalized.contains("phish-sender")) {
                records.add("v=DMARC1; p=reject; rua=mailto:dmarc@phish-sender.test");
            } else if (normalized.contains("secure-bank")) {
                records.add("v=DMARC1; p=reject; aspf=s; adkim=s");
            } else if (normalized.contains("internal-corp")) {
                records.add("v=DMARC1; p=quarantine; pct=100");
            } else if (normalized.contains("none-policy")) {
                records.add("v=DMARC1; p=none");
            }
            return records;
        }
        if (normalized.contains("._domainkey.")) {
            if (normalized.contains("phish-sender") || normalized.contains("secure-bank") || normalized.contains("internal-corp")) {
                records.add("v=DKIM1; k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC3");
            }
            return records;
        }
        if (normalized.contains("phish-sender")) {
            records.add("v=spf1 ip4:203.0.113.45 ~all");
            return records;
        }
        if (normalized.contains("secure-bank")) {
            records.add("v=spf1 ip4:198.51.100.44 -all");
            return records;
        }
        if (normalized.contains("internal-corp")) {
            records.add("v=spf1 ip4:198.51.100.44 include:_spf.internal-corp.test ~all");
            return records;
        }
        if (normalized.contains("softfail")) {
            records.add("v=spf1 ip4:1.2.3.4 ~all");
            return records;
        }
        if (normalized.contains("fail")) {
            records.add("v=spf1 -all");
            return records;
        }
        if (normalized.contains("neutral")) {
            records.add("v=spf1 ?all");
            return records;
        }
        return records;
    }
}
