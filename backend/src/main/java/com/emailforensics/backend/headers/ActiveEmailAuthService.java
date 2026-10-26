package com.emailforensics.backend.headers;

import com.emailforensics.backend.dto.headers.ReceivedHopDto;
import com.emailforensics.backend.infrastructure.DnsLookupService;
import com.emailforensics.backend.infrastructure.DnsResult;
import com.emailforensics.backend.infrastructure.IpClassifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ActiveEmailAuthService {

    private final DnsLookupService dnsLookupService;
    private final IpClassifier ipClassifier;

    private static final Pattern DKIM_SELECTOR_PATTERN = Pattern.compile("(?i)\\bs=([a-zA-Z0-9._-]+)");
    private static final Pattern DKIM_DOMAIN_PATTERN = Pattern.compile("(?i)\\bd=([a-zA-Z0-9.-]+)");
    private static final Pattern DKIM_ALGO_PATTERN = Pattern.compile("(?i)\\ba=([a-zA-Z0-9-]+)");

    private static final Pattern DMARC_POLICY_PATTERN = Pattern.compile("(?i)\\bp=([a-zA-Z]+)");
    private static final Pattern DMARC_SUBPOLICY_PATTERN = Pattern.compile("(?i)\\bsp=([a-zA-Z]+)");
    private static final Pattern DMARC_PCT_PATTERN = Pattern.compile("(?i)\\bpct=([0-9]{1,3})");
    private static final Pattern DMARC_ASPF_PATTERN = Pattern.compile("(?i)\\baspf=([rsRS])");
    private static final Pattern DMARC_ADKIM_PATTERN = Pattern.compile("(?i)\\badkim=([rsRS])");

    public static class SpfEvaluationResult {
        private final String status;
        private final String spfRecord;
        private final String matchedMechanism;
        private final String evaluatedIp;
        private final String domain;

        public SpfEvaluationResult(String status, String spfRecord, String matchedMechanism, String evaluatedIp, String domain) {
            this.status = status;
            this.spfRecord = spfRecord;
            this.matchedMechanism = matchedMechanism;
            this.evaluatedIp = evaluatedIp;
            this.domain = domain;
        }

        public String getStatus() {
            return status;
        }

        public String getSpfRecord() {
            return spfRecord;
        }

        public String getMatchedMechanism() {
            return matchedMechanism;
        }

        public String getEvaluatedIp() {
            return evaluatedIp;
        }

        public String getDomain() {
            return domain;
        }
    }

    public static class DkimEvaluationResult {
        private final String status;
        private final String dkimRecord;
        private final String selector;
        private final String signingDomain;
        private final String algorithm;

        public DkimEvaluationResult(String status, String dkimRecord, String selector, String signingDomain, String algorithm) {
            this.status = status;
            this.dkimRecord = dkimRecord;
            this.selector = selector;
            this.signingDomain = signingDomain;
            this.algorithm = algorithm;
        }

        public String getStatus() {
            return status;
        }

        public String getDkimRecord() {
            return dkimRecord;
        }

        public String getSelector() {
            return selector;
        }

        public String getSigningDomain() {
            return signingDomain;
        }

        public String getAlgorithm() {
            return algorithm;
        }
    }

    public static class DmarcEvaluationResult {
        private final String status;
        private final String dmarcRecord;
        private final String policy;
        private final String subdomainPolicy;
        private final Integer percentage;
        private final boolean spfAligned;
        private final boolean dkimAligned;

        public DmarcEvaluationResult(String status, String dmarcRecord, String policy, String subdomainPolicy,
                                   Integer percentage, boolean spfAligned, boolean dkimAligned) {
            this.status = status;
            this.dmarcRecord = dmarcRecord;
            this.policy = policy;
            this.subdomainPolicy = subdomainPolicy;
            this.percentage = percentage;
            this.spfAligned = spfAligned;
            this.dkimAligned = dkimAligned;
        }

        public String getStatus() {
            return status;
        }

        public String getDmarcRecord() {
            return dmarcRecord;
        }

        public String getPolicy() {
            return policy;
        }

        public String getSubdomainPolicy() {
            return subdomainPolicy;
        }

        public Integer getPercentage() {
            return percentage;
        }

        public boolean isSpfAligned() {
            return spfAligned;
        }

        public boolean isDkimAligned() {
            return dkimAligned;
        }
    }

    public static class ActiveAuthResult {
        private final SpfEvaluationResult spfResult;
        private final DkimEvaluationResult dkimResult;
        private final DmarcEvaluationResult dmarcResult;
        private final String summary;
        private final String dnsObservationJson;

        public ActiveAuthResult(SpfEvaluationResult spfResult, DkimEvaluationResult dkimResult,
                                DmarcEvaluationResult dmarcResult, String summary, String dnsObservationJson) {
            this.spfResult = spfResult;
            this.dkimResult = dkimResult;
            this.dmarcResult = dmarcResult;
            this.summary = summary;
            this.dnsObservationJson = dnsObservationJson;
        }

        public SpfEvaluationResult getSpfResult() {
            return spfResult;
        }

        public DkimEvaluationResult getDkimResult() {
            return dkimResult;
        }

        public DmarcEvaluationResult getDmarcResult() {
            return dmarcResult;
        }

        public String getSummary() {
            return summary;
        }

        public String getDnsObservationJson() {
            return dnsObservationJson;
        }

        public String getActiveSpfResult() {
            return spfResult != null ? spfResult.getStatus() : "UNAVAILABLE";
        }

        public String getActiveSpfRecord() {
            return spfResult != null ? spfResult.getSpfRecord() : null;
        }

        public String getActiveSpfRelayIp() {
            return spfResult != null ? spfResult.getEvaluatedIp() : null;
        }

        public String getActiveDkimStatus() {
            return dkimResult != null ? dkimResult.getStatus() : "UNAVAILABLE";
        }

        public String getActiveDkimRecord() {
            return dkimResult != null ? dkimResult.getDkimRecord() : null;
        }

        public String getActiveDkimSelector() {
            return dkimResult != null ? dkimResult.getSelector() : null;
        }

        public String getActiveDmarcStatus() {
            return dmarcResult != null ? dmarcResult.getStatus() : "UNAVAILABLE";
        }

        public String getActiveDmarcPolicy() {
            return dmarcResult != null ? dmarcResult.getDmarcRecord() : null;
        }
    }

    public ActiveEmailAuthService(DnsLookupService dnsLookupService, IpClassifier ipClassifier) {
        this.dnsLookupService = dnsLookupService;
        this.ipClassifier = ipClassifier;
    }

    public ActiveAuthResult performActiveAuthentication(String fromDomain, String returnPathDomain,
                                                       String dkimHeader, List<ReceivedHopDto> hops) {
        String relayIp = extractFirstRelayIp(hops);
        SpfEvaluationResult spfResult = evaluateSpf(fromDomain, relayIp);
        DkimEvaluationResult dkimResult = verifyDkim(dkimHeader, fromDomain);
        DmarcEvaluationResult dmarcResult = evaluateDmarc(fromDomain, returnPathDomain, spfResult, dkimResult);

        String summary = String.format(
                "Active DNS Email Authentication Verification: SPF=%s (Relay=%s, Record=%s), DKIM=%s (Selector=%s), DMARC=%s (Policy=%s).",
                spfResult.getStatus(),
                relayIp != null ? relayIp : "None",
                spfResult.getSpfRecord() != null ? spfResult.getSpfRecord() : "None",
                dkimResult.getStatus(),
                dkimResult.getSelector() != null ? dkimResult.getSelector() : "None",
                dmarcResult.getStatus(),
                dmarcResult.getPolicy() != null ? dmarcResult.getPolicy() : "None"
        );

        String observationsJson = buildObservationsJson(spfResult, dkimResult, dmarcResult);

        return new ActiveAuthResult(spfResult, dkimResult, dmarcResult, summary, observationsJson);
    }

    public SpfEvaluationResult evaluateSpf(String domain, String relayIp) {
        if (domain == null || domain.trim().isEmpty()) {
            return new SpfEvaluationResult("NONE", null, null, relayIp, domain);
        }

        String normDomain = domain.trim().toLowerCase(Locale.ROOT);
        List<String> txtRecords = dnsLookupService.lookupTxtRecords(normDomain);

        List<String> spfRecords = new ArrayList<>();
        for (String txt : txtRecords) {
            if (txt.toLowerCase(Locale.ROOT).startsWith("v=spf1")) {
                spfRecords.add(txt);
            }
        }

        if (spfRecords.isEmpty()) {
            return new SpfEvaluationResult("NONE", null, null, relayIp, normDomain);
        }

        if (spfRecords.size() > 1) {
            return new SpfEvaluationResult("PERMERROR", spfRecords.get(0), "MULTIPLE_SPF_RECORDS", relayIp, normDomain);
        }

        String spfRecord = spfRecords.get(0);
        if (relayIp == null || relayIp.trim().isEmpty()) {
            return new SpfEvaluationResult("SKIPPED_NO_RELAY_IP", spfRecord, null, relayIp, normDomain);
        }

        String cleanIp = relayIp.trim();
        if (cleanIp.startsWith("[") && cleanIp.endsWith("]")) {
            cleanIp = cleanIp.substring(1, cleanIp.length() - 1);
        }

        return evaluateSpfRecord(spfRecord, cleanIp, normDomain, 0);
    }

    private SpfEvaluationResult evaluateSpfRecord(String spfRecord, String relayIp, String domain, int depth) {
        if (depth > 3) {
            return new SpfEvaluationResult("PERMERROR", spfRecord, "MAX_INCLUDE_DEPTH_EXCEEDED", relayIp, domain);
        }

        String[] tokens = spfRecord.split("\\s+");
        String defaultQualifier = "NEUTRAL";

        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i].trim();
            if (token.isEmpty()) {
                continue;
            }

            char qualifier = '+';
            String mechanism = token;
            if (token.startsWith("+") || token.startsWith("-") || token.startsWith("~") || token.startsWith("?")) {
                qualifier = token.charAt(0);
                mechanism = token.substring(1);
            }

            String mechLower = mechanism.toLowerCase(Locale.ROOT);

            if (mechLower.equals("all")) {
                String qualResult = mapQualifier(qualifier);
                return new SpfEvaluationResult(qualResult, spfRecord, token, relayIp, domain);
            }

            if (mechLower.startsWith("ip4:")) {
                String cidrOrIp = mechanism.substring(4).trim();
                if (matchesIpv4Cidr(relayIp, cidrOrIp)) {
                    return new SpfEvaluationResult(mapQualifier(qualifier), spfRecord, token, relayIp, domain);
                }
            } else if (mechLower.startsWith("ip6:")) {
                String cidrOrIp = mechanism.substring(4).trim();
                if (relayIp.equalsIgnoreCase(cidrOrIp)) {
                    return new SpfEvaluationResult(mapQualifier(qualifier), spfRecord, token, relayIp, domain);
                }
            } else if (mechLower.startsWith("a")) {
                String aDomain = mechLower.length() > 2 && mechLower.charAt(1) == ':' ? mechanism.substring(2).trim() : domain;
                DnsResult dnsA = dnsLookupService.lookupDomain(aDomain);
                if (dnsA != null && dnsA.getARecords() != null && dnsA.getARecords().contains(relayIp)) {
                    return new SpfEvaluationResult(mapQualifier(qualifier), spfRecord, token, relayIp, domain);
                }
            } else if (mechLower.startsWith("mx")) {
                String mxDomain = mechLower.length() > 3 && mechLower.charAt(2) == ':' ? mechanism.substring(3).trim() : domain;
                DnsResult dnsMx = dnsLookupService.lookupDomain(mxDomain);
                if (dnsMx != null && dnsMx.getMxRecords() != null) {
                    for (String mxHost : dnsMx.getMxRecords()) {
                        DnsResult mxHostDns = dnsLookupService.lookupDomain(mxHost);
                        if (mxHostDns != null && mxHostDns.getARecords() != null && mxHostDns.getARecords().contains(relayIp)) {
                            return new SpfEvaluationResult(mapQualifier(qualifier), spfRecord, token, relayIp, domain);
                        }
                    }
                }
            } else if (mechLower.startsWith("include:")) {
                String includedDomain = mechanism.substring(8).trim();
                List<String> incTxt = dnsLookupService.lookupTxtRecords(includedDomain);
                for (String t : incTxt) {
                    if (t.toLowerCase(Locale.ROOT).startsWith("v=spf1")) {
                        SpfEvaluationResult incRes = evaluateSpfRecord(t, relayIp, includedDomain, depth + 1);
                        if ("PASS".equalsIgnoreCase(incRes.getStatus())) {
                            return new SpfEvaluationResult(mapQualifier(qualifier), spfRecord, token, relayIp, domain);
                        }
                    }
                }
            } else if (mechLower.startsWith("redirect=")) {
                String redirectDomain = mechanism.substring(9).trim();
                return evaluateSpf(redirectDomain, relayIp);
            }
        }

        return new SpfEvaluationResult(defaultQualifier, spfRecord, "DEFAULT_FALLTHROUGH", relayIp, domain);
    }

    public DkimEvaluationResult verifyDkim(String dkimHeader, String fromDomain) {
        if (dkimHeader == null || dkimHeader.trim().isEmpty()) {
            return new DkimEvaluationResult("SIGNATURE_ABSENT", null, null, null, null);
        }

        String selector = null;
        String dkimDomain = null;
        String algorithm = "rsa-sha256";

        Matcher selMatcher = DKIM_SELECTOR_PATTERN.matcher(dkimHeader);
        if (selMatcher.find()) {
            selector = selMatcher.group(1).trim();
        }

        Matcher domMatcher = DKIM_DOMAIN_PATTERN.matcher(dkimHeader);
        if (domMatcher.find()) {
            dkimDomain = domMatcher.group(1).trim().toLowerCase(Locale.ROOT);
        }

        Matcher algoMatcher = DKIM_ALGO_PATTERN.matcher(dkimHeader);
        if (algoMatcher.find()) {
            algorithm = algoMatcher.group(1).trim().toLowerCase(Locale.ROOT);
        }

        if (selector == null || dkimDomain == null) {
            return new DkimEvaluationResult("INVALID_DKIM_TAGS", null, selector, dkimDomain, algorithm);
        }

        String queryHost = selector + "._domainkey." + dkimDomain;
        List<String> txtRecords = dnsLookupService.lookupTxtRecords(queryHost);

        for (String txt : txtRecords) {
            String lower = txt.toLowerCase(Locale.ROOT);
            if (lower.contains("v=dkim1") || lower.contains("p=")) {
                return new DkimEvaluationResult("PASS", txt, selector, dkimDomain, algorithm);
            }
        }

        return new DkimEvaluationResult("SELECTOR_NOT_FOUND", null, selector, dkimDomain, algorithm);
    }

    public DmarcEvaluationResult evaluateDmarc(String fromDomain, String returnPathDomain,
                                            SpfEvaluationResult spfResult, DkimEvaluationResult dkimResult) {
        if (fromDomain == null || fromDomain.trim().isEmpty()) {
            return new DmarcEvaluationResult("NONE", null, "none", null, 100, false, false);
        }

        String normFrom = fromDomain.trim().toLowerCase(Locale.ROOT);
        String dmarcHost = "_dmarc." + normFrom;
        List<String> txtRecords = dnsLookupService.lookupTxtRecords(dmarcHost);

        String dmarcRecord = null;
        for (String txt : txtRecords) {
            if (txt.toLowerCase(Locale.ROOT).startsWith("v=dmarc1")) {
                dmarcRecord = txt;
                break;
            }
        }

        if (dmarcRecord == null && normFrom.contains(".")) {
            int dotIdx = normFrom.indexOf('.');
            String parentDomain = normFrom.substring(dotIdx + 1);
            if (parentDomain.contains(".")) {
                List<String> parentTxt = dnsLookupService.lookupTxtRecords("_dmarc." + parentDomain);
                for (String txt : parentTxt) {
                    if (txt.toLowerCase(Locale.ROOT).startsWith("v=dmarc1")) {
                        dmarcRecord = txt;
                        break;
                    }
                }
            }
        }

        if (dmarcRecord == null) {
            return new DmarcEvaluationResult("RECORD_ABSENT", null, "none", null, 100, false, false);
        }

        String policy = "none";
        Matcher pMatcher = DMARC_POLICY_PATTERN.matcher(dmarcRecord);
        if (pMatcher.find()) {
            policy = pMatcher.group(1).toLowerCase(Locale.ROOT);
        }

        String subPolicy = null;
        Matcher spMatcher = DMARC_SUBPOLICY_PATTERN.matcher(dmarcRecord);
        if (spMatcher.find()) {
            subPolicy = spMatcher.group(1).toLowerCase(Locale.ROOT);
        }

        int pct = 100;
        Matcher pctMatcher = DMARC_PCT_PATTERN.matcher(dmarcRecord);
        if (pctMatcher.find()) {
            try {
                pct = Integer.parseInt(pctMatcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }

        String aspf = "r";
        Matcher aspfMatcher = DMARC_ASPF_PATTERN.matcher(dmarcRecord);
        if (aspfMatcher.find()) {
            aspf = aspfMatcher.group(1).toLowerCase(Locale.ROOT);
        }

        String adkim = "r";
        Matcher adkimMatcher = DMARC_ADKIM_PATTERN.matcher(dmarcRecord);
        if (adkimMatcher.find()) {
            adkim = adkimMatcher.group(1).toLowerCase(Locale.ROOT);
        }

        boolean spfAligned = checkAlignment(normFrom, returnPathDomain, aspf);
        boolean dkimAligned = dkimResult != null && checkAlignment(normFrom, dkimResult.getSigningDomain(), adkim);

        boolean spfPass = spfResult != null && "PASS".equalsIgnoreCase(spfResult.getStatus()) && spfAligned;
        boolean dkimPass = dkimResult != null && "PASS".equalsIgnoreCase(dkimResult.getStatus()) && dkimAligned;

        if (spfPass || dkimPass) {
            return new DmarcEvaluationResult("PASS", dmarcRecord, policy, subPolicy, pct, spfAligned, dkimAligned);
        }

        String status = "FAIL";
        if ("reject".equals(policy)) {
            status = "POLICY_REJECT";
        } else if ("quarantine".equals(policy)) {
            status = "POLICY_QUARANTINE";
        } else {
            status = "POLICY_NONE";
        }

        return new DmarcEvaluationResult(status, dmarcRecord, policy, subPolicy, pct, spfAligned, dkimAligned);
    }

    private boolean checkAlignment(String fromDomain, String otherDomain, String mode) {
        if (fromDomain == null || otherDomain == null) {
            return false;
        }
        String f = fromDomain.trim().toLowerCase(Locale.ROOT);
        String o = otherDomain.trim().toLowerCase(Locale.ROOT);

        if ("s".equalsIgnoreCase(mode)) {
            return f.equals(o);
        }

        if (f.equals(o)) {
            return true;
        }

        return f.endsWith("." + o) || o.endsWith("." + f);
    }

    private String mapQualifier(char qualifier) {
        switch (qualifier) {
            case '+':
                return "PASS";
            case '-':
                return "FAIL";
            case '~':
                return "SOFTFAIL";
            case '?':
                return "NEUTRAL";
            default:
                return "PASS";
        }
    }

    private boolean matchesIpv4Cidr(String relayIp, String cidrOrIp) {
        if (relayIp == null || cidrOrIp == null) {
            return false;
        }
        String cleanRelay = relayIp.trim();
        String cleanCidr = cidrOrIp.trim();
        if (!cleanCidr.contains("/")) {
            return cleanRelay.equalsIgnoreCase(cleanCidr);
        }
        String[] parts = cleanCidr.split("/");
        if (parts.length != 2) {
            return false;
        }
        try {
            long ipNum = ipv4ToLong(cleanRelay);
            long targetNum = ipv4ToLong(parts[0]);
            int prefix = Integer.parseInt(parts[1]);
            if (prefix < 0 || prefix > 32) {
                return false;
            }
            long mask = prefix == 0 ? 0L : (-1L << (32 - prefix)) & 0xFFFFFFFFL;
            return (ipNum & mask) == (targetNum & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private long ipv4ToLong(String ip) {
        String[] octets = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) | (Integer.parseInt(octets[i]) & 0xFF);
        }
        return result & 0xFFFFFFFFL;
    }

    private String extractFirstRelayIp(List<ReceivedHopDto> hops) {
        if (hops == null || hops.isEmpty()) {
            return null;
        }
        for (ReceivedHopDto hop : hops) {
            String ip = hop.getHopIp();
            if (ip != null && !ip.isBlank() && !ip.equals("127.0.0.1") && !ip.equals("::1")) {
                return ip;
            }
        }
        return hops.get(0).getHopIp();
    }

    private String buildObservationsJson(SpfEvaluationResult spf, DkimEvaluationResult dkim, DmarcEvaluationResult dmarc) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"activeSpf\":{");
        sb.append("\"status\":\"").append(spf != null ? spf.getStatus() : "NONE").append("\",");
        sb.append("\"record\":").append(spf != null && spf.getSpfRecord() != null ? "\"" + escapeJson(spf.getSpfRecord()) + "\"" : "null").append(",");
        sb.append("\"mechanism\":").append(spf != null && spf.getMatchedMechanism() != null ? "\"" + escapeJson(spf.getMatchedMechanism()) + "\"" : "null").append(",");
        sb.append("\"evaluatedIp\":").append(spf != null && spf.getEvaluatedIp() != null ? "\"" + escapeJson(spf.getEvaluatedIp()) + "\"" : "null");
        sb.append("},");

        sb.append("\"activeDkim\":{");
        sb.append("\"status\":\"").append(dkim != null ? dkim.getStatus() : "SIGNATURE_ABSENT").append("\",");
        sb.append("\"record\":").append(dkim != null && dkim.getDkimRecord() != null ? "\"" + escapeJson(dkim.getDkimRecord()) + "\"" : "null").append(",");
        sb.append("\"selector\":").append(dkim != null && dkim.getSelector() != null ? "\"" + escapeJson(dkim.getSelector()) + "\"" : "null").append(",");
        sb.append("\"signingDomain\":").append(dkim != null && dkim.getSigningDomain() != null ? "\"" + escapeJson(dkim.getSigningDomain()) + "\"" : "null");
        sb.append("},");

        sb.append("\"activeDmarc\":{");
        sb.append("\"status\":\"").append(dmarc != null ? dmarc.getStatus() : "RECORD_ABSENT").append("\",");
        sb.append("\"record\":").append(dmarc != null && dmarc.getDmarcRecord() != null ? "\"" + escapeJson(dmarc.getDmarcRecord()) + "\"" : "null").append(",");
        sb.append("\"policy\":\"").append(dmarc != null ? dmarc.getPolicy() : "none").append("\",");
        sb.append("\"spfAligned\":").append(dmarc != null && dmarc.isSpfAligned()).append(",");
        sb.append("\"dkimAligned\":").append(dmarc != null && dmarc.isDkimAligned());
        sb.append("}");

        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
