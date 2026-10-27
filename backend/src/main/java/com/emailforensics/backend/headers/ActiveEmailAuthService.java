package com.emailforensics.backend.headers;

import com.emailforensics.backend.dto.headers.ReceivedHopDto;
import com.emailforensics.backend.infrastructure.DnsLookupService;
import com.emailforensics.backend.infrastructure.DnsResult;
import com.emailforensics.backend.infrastructure.IpClassifier;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
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
    private static final Pattern DKIM_HEADERS_PATTERN = Pattern.compile("(?i)\\bh=([a-zA-Z0-9._:-]+)");
    private static final Pattern DKIM_BODY_HASH_PATTERN = Pattern.compile("(?i)\\bbh=([a-zA-Z0-9+/=]+)");
    private static final Pattern DKIM_SIG_PATTERN = Pattern.compile("(?i)\\bb=([a-zA-Z0-9+/=\\s]+)(?:;|$)");
    private static final Pattern DKIM_CANON_PATTERN = Pattern.compile("(?i)\\bc=([a-zA-Z0-9/-]+)");

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
        private final String evaluatedIdentity;

        public SpfEvaluationResult(String status, String spfRecord, String matchedMechanism, String evaluatedIp, String domain) {
            this(status, spfRecord, matchedMechanism, evaluatedIp, domain, "MAIL_FROM");
        }

        public SpfEvaluationResult(String status, String spfRecord, String matchedMechanism, String evaluatedIp, String domain, String evaluatedIdentity) {
            this.status = status;
            this.spfRecord = spfRecord;
            this.matchedMechanism = matchedMechanism;
            this.evaluatedIp = evaluatedIp;
            this.domain = domain;
            this.evaluatedIdentity = evaluatedIdentity != null ? evaluatedIdentity : "MAIL_FROM";
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

        public String getEvaluatedIdentity() {
            return evaluatedIdentity;
        }
    }

    public static class DkimEvaluationResult {
        private final String status;
        private final String dkimRecord;
        private final String selector;
        private final String signingDomain;
        private final String algorithm;
        private final boolean cryptographicVerified;
        private final String verificationNote;

        public DkimEvaluationResult(String status, String dkimRecord, String selector, String signingDomain, String algorithm) {
            this(status, dkimRecord, selector, signingDomain, algorithm, false, null);
        }

        public DkimEvaluationResult(String status, String dkimRecord, String selector, String signingDomain, String algorithm,
                                    boolean cryptographicVerified, String verificationNote) {
            this.status = status;
            this.dkimRecord = dkimRecord;
            this.selector = selector;
            this.signingDomain = signingDomain;
            this.algorithm = algorithm;
            this.cryptographicVerified = cryptographicVerified;
            this.verificationNote = verificationNote;
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

        public boolean isCryptographicVerified() {
            return cryptographicVerified;
        }

        public String getVerificationNote() {
            return verificationNote;
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
        private final String dataMode;

        public ActiveAuthResult(SpfEvaluationResult spfResult, DkimEvaluationResult dkimResult,
                                DmarcEvaluationResult dmarcResult, String summary, String dnsObservationJson) {
            this(spfResult, dkimResult, dmarcResult, summary, dnsObservationJson, "REAL");
        }

        public ActiveAuthResult(SpfEvaluationResult spfResult, DkimEvaluationResult dkimResult,
                                DmarcEvaluationResult dmarcResult, String summary, String dnsObservationJson,
                                String dataMode) {
            this.spfResult = spfResult;
            this.dkimResult = dkimResult;
            this.dmarcResult = dmarcResult;
            this.summary = summary;
            this.dnsObservationJson = dnsObservationJson;
            this.dataMode = dataMode != null ? dataMode : "REAL";
        }

        public String getDataMode() {
            return dataMode;
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
        return performActiveAuthentication(fromDomain, returnPathDomain, dkimHeader, hops, null, null);
    }

    public ActiveAuthResult performActiveAuthentication(String fromDomain, String returnPathDomain,
                                                       String dkimHeader, List<ReceivedHopDto> hops,
                                                       Map<String, List<String>> rawHeaders, byte[] rawBodyBytes) {
        String relayIp = extractFirstRelayIp(hops);
        String heloHost = extractFirstHeloHost(hops);
        String heloDomain = extractDomainFromHost(heloHost);

        String spfDomain;
        String spfIdentity;
        if (returnPathDomain != null && !returnPathDomain.trim().isEmpty() && !returnPathDomain.equalsIgnoreCase("mailer-daemon")) {
            spfDomain = returnPathDomain.trim();
            spfIdentity = "MAIL_FROM";
        } else if (heloDomain != null && !heloDomain.trim().isEmpty()) {
            spfDomain = heloDomain.trim();
            spfIdentity = "HELO";
        } else if (fromDomain != null && !fromDomain.trim().isEmpty()) {
            spfDomain = fromDomain.trim();
            spfIdentity = "FROM_FALLBACK";
        } else {
            spfDomain = null;
            spfIdentity = "UNKNOWN";
        }

        SpfEvaluationResult spfResult = evaluateSpf(spfDomain, relayIp, spfIdentity);

        DkimEvaluationResult dkimResult;
        if (rawHeaders != null && rawBodyBytes != null) {
            dkimResult = verifyDkimCryptographic(dkimHeader, rawHeaders, rawBodyBytes, fromDomain);
        } else {
            dkimResult = verifyDkim(dkimHeader, fromDomain);
        }

        DmarcEvaluationResult dmarcResult = evaluateDmarc(fromDomain, returnPathDomain, spfResult, dkimResult);

        String spfSummaryText = String.format("SPF=%s (Identity=%s, Domain=%s, Relay=%s)",
                spfResult.getStatus(),
                spfResult.getEvaluatedIdentity(),
                spfResult.getDomain() != null ? spfResult.getDomain() : "None",
                spfResult.getEvaluatedIp() != null ? spfResult.getEvaluatedIp() : "None");

        String dkimSummaryText;
        if ("PASS".equalsIgnoreCase(dkimResult.getStatus())) {
            dkimSummaryText = String.format("DKIM=PASS (Cryptographically Verified, Selector=%s)", dkimResult.getSelector());
        } else if ("SELECTOR_FOUND".equalsIgnoreCase(dkimResult.getStatus())) {
            dkimSummaryText = String.format("DKIM=KEY_FOUND (DNS Key Published, Selector=%s, Signature Unverified)", dkimResult.getSelector());
        } else {
            dkimSummaryText = String.format("DKIM=%s", dkimResult.getStatus());
        }

        String summary = String.format(
                "Active DNS Email Authentication Verification: %s, %s, DMARC=%s (Policy=%s). Note: DNS key publication does not represent conclusive cryptographic authentication.",
                spfSummaryText,
                dkimSummaryText,
                dmarcResult.getStatus(),
                dmarcResult.getPolicy() != null ? dmarcResult.getPolicy() : "None"
        );

        String dataMode = "REAL";
        if (fromDomain != null && (fromDomain.endsWith(".test") || fromDomain.endsWith(".local") ||
                fromDomain.endsWith(".internal") || fromDomain.endsWith(".example") ||
                fromDomain.endsWith(".invalid") || fromDomain.equals("localhost"))) {
            dataMode = "SIMULATED";
        } else if (fromDomain == null || fromDomain.isBlank()) {
            dataMode = "NOT_CHECKED";
        }

        String observationsJson = buildObservationsJson(spfResult, dkimResult, dmarcResult, dataMode);

        return new ActiveAuthResult(spfResult, dkimResult, dmarcResult, summary, observationsJson, dataMode);
    }

    public SpfEvaluationResult evaluateSpf(String domain, String relayIp) {
        return evaluateSpf(domain, relayIp, "MAIL_FROM");
    }

    public SpfEvaluationResult evaluateSpf(String domain, String relayIp, String identity) {
        return evaluateSpfInternal(domain, relayIp, identity, 0, new java.util.HashSet<>());
    }

    private SpfEvaluationResult evaluateSpfInternal(String domain, String relayIp, String identity,
                                                   int depth, java.util.Set<String> visitedDomains) {
        if (domain == null || domain.trim().isEmpty()) {
            return new SpfEvaluationResult("NONE", null, null, relayIp, domain, identity);
        }

        String normDomain = domain.trim().toLowerCase(Locale.ROOT);
        if (depth > 10) {
            return new SpfEvaluationResult("PERMERROR", null, "MAX_DNS_LOOKUPS_EXCEEDED", relayIp, normDomain, identity);
        }

        if (!visitedDomains.add(normDomain)) {
            return new SpfEvaluationResult("PERMERROR", null, "SPF_LOOP_DETECTED", relayIp, normDomain, identity);
        }

        List<String> txtRecords = dnsLookupService.lookupTxtRecords(normDomain);

        List<String> spfRecords = new ArrayList<>();
        for (String txt : txtRecords) {
            if (txt.toLowerCase(Locale.ROOT).startsWith("v=spf1")) {
                spfRecords.add(txt);
            }
        }

        if (spfRecords.isEmpty()) {
            return new SpfEvaluationResult("NONE", null, null, relayIp, normDomain, identity);
        }

        if (spfRecords.size() > 1) {
            return new SpfEvaluationResult("PERMERROR", spfRecords.get(0), "MULTIPLE_SPF_RECORDS", relayIp, normDomain, identity);
        }

        String spfRecord = spfRecords.get(0);
        if (relayIp == null || relayIp.trim().isEmpty()) {
            return new SpfEvaluationResult("SKIPPED_NO_RELAY_IP", spfRecord, null, relayIp, normDomain, identity);
        }

        String cleanIp = relayIp.trim();
        if (cleanIp.startsWith("[") && cleanIp.endsWith("]")) {
            cleanIp = cleanIp.substring(1, cleanIp.length() - 1);
        }

        return evaluateSpfRecord(spfRecord, cleanIp, normDomain, depth, identity, visitedDomains);
    }

    private SpfEvaluationResult evaluateSpfRecord(String spfRecord, String relayIp, String domain,
                                                  int depth, String identity, java.util.Set<String> visitedDomains) {
        if (depth > 10) {
            return new SpfEvaluationResult("PERMERROR", spfRecord, "MAX_DNS_LOOKUPS_EXCEEDED", relayIp, domain, identity);
        }

        String[] tokens = spfRecord.split("\\s+");
        String redirectDomain = null;

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
                return new SpfEvaluationResult(mapQualifier(qualifier), spfRecord, token, relayIp, domain, identity);
            }

            if (mechLower.startsWith("ip4:")) {
                String cidrOrIp = mechanism.substring(4).trim();
                if (matchesIpCidr(relayIp, cidrOrIp, false)) {
                    return new SpfEvaluationResult(mapQualifier(qualifier), spfRecord, token, relayIp, domain, identity);
                }
                continue;
            }

            if (mechLower.startsWith("ip6:")) {
                String cidrOrIp = mechanism.substring(4).trim();
                if (matchesIpCidr(relayIp, cidrOrIp, true)) {
                    return new SpfEvaluationResult(mapQualifier(qualifier), spfRecord, token, relayIp, domain, identity);
                }
                continue;
            }

            if (mechLower.equals("a") || mechLower.startsWith("a:") || mechLower.startsWith("a/")) {
                if (matchesAMechanism(relayIp, domain, mechanism)) {
                    return new SpfEvaluationResult(mapQualifier(qualifier), spfRecord, token, relayIp, domain, identity);
                }
                continue;
            }

            if (mechLower.equals("mx") || mechLower.startsWith("mx:") || mechLower.startsWith("mx/")) {
                if (matchesMxMechanism(relayIp, domain, mechanism)) {
                    return new SpfEvaluationResult(mapQualifier(qualifier), spfRecord, token, relayIp, domain, identity);
                }
                continue;
            }

            if (mechLower.startsWith("include:")) {
                String includedDomain = mechanism.substring(8).trim();
                SpfEvaluationResult incRes = evaluateSpfInternal(includedDomain, relayIp, identity, depth + 1, visitedDomains);
                if ("PASS".equalsIgnoreCase(incRes.getStatus())) {
                    return new SpfEvaluationResult(mapQualifier(qualifier), spfRecord, token, relayIp, domain, identity);
                }
                continue;
            }

            if (mechLower.equals("ptr") || mechLower.startsWith("ptr:")) {
                String ptrDomain = mechLower.length() > 4 && mechLower.charAt(3) == ':' ? mechanism.substring(4).trim() : domain;
                if (matchesPtrMechanism(relayIp, ptrDomain)) {
                    return new SpfEvaluationResult(mapQualifier(qualifier), spfRecord, token, relayIp, domain, identity);
                }
                continue;
            }

            if (mechLower.startsWith("exists:")) {
                String existsDomain = mechanism.substring(7).trim();
                DnsResult dnsExists = dnsLookupService.lookupDomain(existsDomain);
                if (dnsExists != null && dnsExists.getARecords() != null && !dnsExists.getARecords().isEmpty()) {
                    return new SpfEvaluationResult(mapQualifier(qualifier), spfRecord, token, relayIp, domain, identity);
                }
                continue;
            }

            if (mechLower.startsWith("redirect=")) {
                redirectDomain = mechanism.substring(9).trim();
                continue;
            }

            if (mechLower.startsWith("exp=")) {
                continue;
            }
        }

        if (redirectDomain != null && !redirectDomain.isBlank()) {
            return evaluateSpfInternal(redirectDomain, relayIp, identity, depth + 1, visitedDomains);
        }

        return new SpfEvaluationResult("NEUTRAL", spfRecord, "DEFAULT_FALLTHROUGH", relayIp, domain, identity);
    }

    private boolean matchesAMechanism(String relayIp, String defaultDomain, String mechanism) {
        String sub = mechanism.startsWith("a") ? mechanism.substring(1) : mechanism;
        String targetDomain = defaultDomain;
        String prefixStr = null;

        if (sub.startsWith(":")) {
            sub = sub.substring(1);
            int slash = sub.indexOf('/');
            if (slash >= 0) {
                targetDomain = sub.substring(0, slash).trim();
                prefixStr = sub.substring(slash + 1).trim();
            } else {
                targetDomain = sub.trim();
            }
        } else if (sub.startsWith("/")) {
            prefixStr = sub.substring(1).trim();
        }

        DnsResult dnsA = dnsLookupService.lookupDomain(targetDomain);
        if (dnsA == null || dnsA.getARecords() == null) {
            return false;
        }

        for (String aIp : dnsA.getARecords()) {
            if (prefixStr != null) {
                if (matchesIpCidr(relayIp, aIp + "/" + prefixStr, false)) {
                    return true;
                }
            } else if (aIp.equalsIgnoreCase(relayIp)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesMxMechanism(String relayIp, String defaultDomain, String mechanism) {
        String sub = mechanism.startsWith("mx") ? mechanism.substring(2) : mechanism;
        String targetDomain = defaultDomain;
        String prefixStr = null;

        if (sub.startsWith(":")) {
            sub = sub.substring(1);
            int slash = sub.indexOf('/');
            if (slash >= 0) {
                targetDomain = sub.substring(0, slash).trim();
                prefixStr = sub.substring(slash + 1).trim();
            } else {
                targetDomain = sub.trim();
            }
        } else if (sub.startsWith("/")) {
            prefixStr = sub.substring(1).trim();
        }

        DnsResult dnsMx = dnsLookupService.lookupDomain(targetDomain);
        if (dnsMx == null || dnsMx.getMxRecords() == null) {
            return false;
        }

        for (String mxHost : dnsMx.getMxRecords()) {
            DnsResult mxHostDns = dnsLookupService.lookupDomain(mxHost);
            if (mxHostDns != null && mxHostDns.getARecords() != null) {
                for (String aIp : mxHostDns.getARecords()) {
                    if (prefixStr != null) {
                        if (matchesIpCidr(relayIp, aIp + "/" + prefixStr, false)) {
                            return true;
                        }
                    } else if (aIp.equalsIgnoreCase(relayIp)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean matchesPtrMechanism(String relayIp, String ptrDomain) {
        try {
            String host = InetAddress.getByName(relayIp).getCanonicalHostName();
            if (host == null || host.equals(relayIp)) {
                return false;
            }
            String normHost = host.toLowerCase(Locale.ROOT);
            String normDomain = ptrDomain.toLowerCase(Locale.ROOT);
            if (normHost.equals(normDomain) || normHost.endsWith("." + normDomain)) {
                DnsResult fwd = dnsLookupService.lookupDomain(host);
                return fwd != null && fwd.getARecords() != null && fwd.getARecords().contains(relayIp);
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public DkimEvaluationResult verifyDkim(String dkimHeader, String fromDomain) {
        if (dkimHeader == null || dkimHeader.trim().isEmpty()) {
            return new DkimEvaluationResult("SIGNATURE_ABSENT", null, null, null, null, false, "DKIM-Signature header is absent.");
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
            return new DkimEvaluationResult("INVALID_DKIM_TAGS", null, selector, dkimDomain, algorithm, false, "DKIM header missing mandatory selector or domain tags.");
        }

        String queryHost = selector + "._domainkey." + dkimDomain;
        List<String> txtRecords = dnsLookupService.lookupTxtRecords(queryHost);

        for (String txt : txtRecords) {
            String lower = txt.toLowerCase(Locale.ROOT);
            if (lower.contains("v=dkim1") || lower.contains("p=")) {
                return new DkimEvaluationResult(
                        "SELECTOR_FOUND",
                        txt,
                        selector,
                        dkimDomain,
                        algorithm,
                        false,
                        "DKIM public key is published in DNS; cryptographic message signature was not verified offline."
                );
            }
        }

        return new DkimEvaluationResult("SELECTOR_NOT_FOUND", null, selector, dkimDomain, algorithm, false, "No matching DKIM key record found at " + queryHost);
    }

    public DkimEvaluationResult verifyDkimCryptographic(String dkimHeader, Map<String, List<String>> headers,
                                                        byte[] rawBodyBytes, String fromDomain) {
        if (dkimHeader == null || dkimHeader.trim().isEmpty()) {
            return new DkimEvaluationResult("SIGNATURE_ABSENT", null, null, null, null, false, "DKIM-Signature header is absent.");
        }

        String selector = null;
        String dkimDomain = null;
        String algorithm = "rsa-sha256";
        String signedHeadersList = null;
        String bodyHash = null;
        String rawSignature = null;
        String canonMode = "relaxed/relaxed";

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

        Matcher hdrMatcher = DKIM_HEADERS_PATTERN.matcher(dkimHeader);
        if (hdrMatcher.find()) {
            signedHeadersList = hdrMatcher.group(1).trim();
        }

        Matcher bhMatcher = DKIM_BODY_HASH_PATTERN.matcher(dkimHeader);
        if (bhMatcher.find()) {
            bodyHash = bhMatcher.group(1).trim();
        }

        Matcher sigMatcher = DKIM_SIG_PATTERN.matcher(dkimHeader);
        if (sigMatcher.find()) {
            rawSignature = sigMatcher.group(1).replaceAll("\\s+", "");
        }

        Matcher cMatcher = DKIM_CANON_PATTERN.matcher(dkimHeader);
        if (cMatcher.find()) {
            canonMode = cMatcher.group(1).trim().toLowerCase(Locale.ROOT);
        }

        if (selector == null || dkimDomain == null) {
            return new DkimEvaluationResult("INVALID_DKIM_TAGS", null, selector, dkimDomain, algorithm, false, "DKIM header missing mandatory tags.");
        }

        String queryHost = selector + "._domainkey." + dkimDomain;
        List<String> txtRecords = dnsLookupService.lookupTxtRecords(queryHost);

        String dkimRecord = null;
        for (String txt : txtRecords) {
            String lower = txt.toLowerCase(Locale.ROOT);
            if (lower.contains("v=dkim1") || lower.contains("p=")) {
                dkimRecord = txt;
                break;
            }
        }

        if (dkimRecord == null) {
            return new DkimEvaluationResult("SELECTOR_NOT_FOUND", null, selector, dkimDomain, algorithm, false, "No matching DKIM key record found at " + queryHost);
        }

        if (rawSignature == null || rawSignature.isEmpty() || bodyHash == null || signedHeadersList == null) {
            return new DkimEvaluationResult("SELECTOR_FOUND", dkimRecord, selector, dkimDomain, algorithm, false, "DKIM key published; signature tags incomplete for offline cryptographic verification.");
        }

        String pubKeyBase64 = extractPublicKeyFromDkimRecord(dkimRecord);
        if (pubKeyBase64 == null || pubKeyBase64.isEmpty()) {
            return new DkimEvaluationResult("SELECTOR_FOUND", dkimRecord, selector, dkimDomain, algorithm, false, "DKIM record present but contains empty or revoked public key.");
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(pubKeyBase64.replaceAll("\\s+", ""));
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey publicKey = kf.generatePublic(keySpec);

            if (rawBodyBytes != null) {
                MessageDigest md = MessageDigest.getInstance(algorithm.endsWith("sha1") ? "SHA-1" : "SHA-256");
                byte[] canonicalBody = canonicalizeBody(rawBodyBytes, canonMode.contains("/") ? canonMode.substring(canonMode.indexOf('/') + 1) : "relaxed");
                byte[] computedHash = md.digest(canonicalBody);
                String computedHashB64 = Base64.getEncoder().encodeToString(computedHash);
                if (!computedHashB64.equals(bodyHash)) {
                    return new DkimEvaluationResult("FAIL", dkimRecord, selector, dkimDomain, algorithm, false, "DKIM body hash mismatch; body altered in transit.");
                }
            }

            if (headers != null) {
                String headerCanon = canonMode.contains("/") ? canonMode.substring(0, canonMode.indexOf('/')) : "relaxed";
                byte[] canonicalHeaders = canonicalizeHeaders(headers, signedHeadersList, dkimHeader, headerCanon);
                Signature sig = Signature.getInstance(algorithm.endsWith("sha1") ? "SHA1withRSA" : "SHA256withRSA");
                sig.initVerify(publicKey);
                sig.update(canonicalHeaders);
                byte[] sigBytes = Base64.getDecoder().decode(rawSignature);
                boolean verified = sig.verify(sigBytes);
                if (verified) {
                    return new DkimEvaluationResult("PASS", dkimRecord, selector, dkimDomain, algorithm, true, "DKIM cryptographic signature verified successfully.");
                } else {
                    return new DkimEvaluationResult("FAIL", dkimRecord, selector, dkimDomain, algorithm, false, "DKIM cryptographic signature verification failed.");
                }
            }

            return new DkimEvaluationResult("SELECTOR_FOUND", dkimRecord, selector, dkimDomain, algorithm, false, "DKIM public key published in DNS; message headers not provided for cryptographic verification.");
        } catch (Exception ex) {
            return new DkimEvaluationResult("FAIL", dkimRecord, selector, dkimDomain, algorithm, false, "Cryptographic verification error: " + ex.getMessage());
        }
    }

    private String extractPublicKeyFromDkimRecord(String dkimRecord) {
        Pattern pPat = Pattern.compile("(?i)\\bp=([a-zA-Z0-9+/=]+)");
        Matcher m = pPat.matcher(dkimRecord);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private byte[] canonicalizeBody(byte[] body, String mode) {
        String s = new String(body, StandardCharsets.UTF_8);
        if ("simple".equalsIgnoreCase(mode)) {
            while (s.endsWith("\r\n\r\n")) {
                s = s.substring(0, s.length() - 2);
            }
            if (!s.isEmpty() && !s.endsWith("\r\n")) {
                s = s + "\r\n";
            }
            return s.getBytes(StandardCharsets.UTF_8);
        }
        String[] lines = s.split("\r?\n", -1);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.replaceAll("[ \\t]+$", "").replaceAll("[ \\t]+", " ");
            sb.append(trimmed).append("\r\n");
        }
        String result = sb.toString();
        while (result.endsWith("\r\n\r\n")) {
            result = result.substring(0, result.length() - 2);
        }
        return result.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] canonicalizeHeaders(Map<String, List<String>> headers, String signedHeadersList,
                                      String dkimHeader, String mode) {
        StringBuilder sb = new StringBuilder();
        String[] headerNames = signedHeadersList.split(":");
        boolean relaxed = !"simple".equalsIgnoreCase(mode);

        for (String hName : headerNames) {
            String normName = hName.trim();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(normName) && !entry.getValue().isEmpty()) {
                    String val = entry.getValue().get(0);
                    if (relaxed) {
                        sb.append(normName.toLowerCase(Locale.ROOT)).append(":")
                                .append(val.replaceAll("\\r?\\n[ \\t]+", " ").replaceAll("[ \\t]+", " ").trim())
                                .append("\r\n");
                    } else {
                        sb.append(entry.getKey()).append(":").append(val).append("\r\n");
                    }
                    break;
                }
            }
        }

        String dkimWithoutSig = dkimHeader.replaceAll("(?i)\\bb=[a-zA-Z0-9+/=\\s]+", "b=");
        if (relaxed) {
            sb.append("dkim-signature:")
                    .append(dkimWithoutSig.replaceAll("\\r?\\n[ \\t]+", " ").replaceAll("[ \\t]+", " ").trim());
        } else {
            sb.append("DKIM-Signature:").append(dkimWithoutSig);
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
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
        boolean dkimPass = dkimResult != null && dkimResult.isCryptographicVerified() && "PASS".equalsIgnoreCase(dkimResult.getStatus()) && dkimAligned;

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

    private boolean matchesIpCidr(String testIp, String cidrOrIp, boolean isIpv6) {
        if (testIp == null || cidrOrIp == null) {
            return false;
        }
        try {
            String cleanTest = testIp.trim().replaceAll("[\\[\\]]", "");
            String cleanTarget = cidrOrIp.trim().replaceAll("[\\[\\]]", "");
            int slashIdx = cleanTarget.indexOf('/');
            int prefix = -1;
            String targetIpStr = cleanTarget;
            if (slashIdx >= 0) {
                targetIpStr = cleanTarget.substring(0, slashIdx);
                prefix = Integer.parseInt(cleanTarget.substring(slashIdx + 1));
            }
            InetAddress testAddr = InetAddress.getByName(cleanTest);
            InetAddress targetAddr = InetAddress.getByName(targetIpStr);
            if (testAddr.getClass() != targetAddr.getClass()) {
                return false;
            }
            byte[] testBytes = testAddr.getAddress();
            byte[] targetBytes = targetAddr.getAddress();
            int maxPrefix = isIpv6 ? 128 : 32;
            if (prefix < 0) {
                prefix = maxPrefix;
            }
            if (prefix > maxPrefix) {
                return false;
            }
            int fullBytes = prefix / 8;
            for (int i = 0; i < fullBytes; i++) {
                if (testBytes[i] != targetBytes[i]) {
                    return false;
                }
            }
            int remBits = prefix % 8;
            if (remBits > 0) {
                int mask = (0xFF << (8 - remBits)) & 0xFF;
                if ((testBytes[fullBytes] & mask) != (targetBytes[fullBytes] & mask)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
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

    private String extractFirstHeloHost(List<ReceivedHopDto> hops) {
        if (hops == null || hops.isEmpty()) {
            return null;
        }
        for (ReceivedHopDto hop : hops) {
            String host = hop.getFromHost();
            if (host != null && !host.isBlank() && !host.equals("127.0.0.1") && !host.equals("localhost")) {
                return host;
            }
        }
        return null;
    }

    private String extractDomainFromHost(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }
        String clean = host.trim().replaceAll("[\\[\\]]", "");
        if (ipClassifier.isIpAddress(clean)) {
            return null;
        }
        if (clean.contains(".")) {
            return clean.toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private String buildObservationsJson(SpfEvaluationResult spf, DkimEvaluationResult dkim, DmarcEvaluationResult dmarc) {
        return buildObservationsJson(spf, dkim, dmarc, "REAL");
    }

    private String buildObservationsJson(SpfEvaluationResult spf, DkimEvaluationResult dkim, DmarcEvaluationResult dmarc, String dataMode) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"activeSpf\":{");
        sb.append("\"status\":\"").append(spf != null ? spf.getStatus() : "NONE").append("\",");
        sb.append("\"identity\":\"").append(spf != null && spf.getEvaluatedIdentity() != null ? spf.getEvaluatedIdentity() : "MAIL_FROM").append("\",");
        sb.append("\"record\":").append(spf != null && spf.getSpfRecord() != null ? "\"" + escapeJson(spf.getSpfRecord()) + "\"" : "null").append(",");
        sb.append("\"mechanism\":").append(spf != null && spf.getMatchedMechanism() != null ? "\"" + escapeJson(spf.getMatchedMechanism()) + "\"" : "null").append(",");
        sb.append("\"evaluatedIp\":").append(spf != null && spf.getEvaluatedIp() != null ? "\"" + escapeJson(spf.getEvaluatedIp()) + "\"" : "null");
        sb.append("},");

        sb.append("\"activeDkim\":{");
        sb.append("\"status\":\"").append(dkim != null ? dkim.getStatus() : "SIGNATURE_ABSENT").append("\",");
        sb.append("\"record\":").append(dkim != null && dkim.getDkimRecord() != null ? "\"" + escapeJson(dkim.getDkimRecord()) + "\"" : "null").append(",");
        sb.append("\"selector\":").append(dkim != null && dkim.getSelector() != null ? "\"" + escapeJson(dkim.getSelector()) + "\"" : "null").append(",");
        sb.append("\"signingDomain\":").append(dkim != null && dkim.getSigningDomain() != null ? "\"" + escapeJson(dkim.getSigningDomain()) + "\"" : "null").append(",");
        sb.append("\"cryptographicVerified\":").append(dkim != null && dkim.isCryptographicVerified()).append(",");
        sb.append("\"verificationNote\":").append(dkim != null && dkim.getVerificationNote() != null ? "\"" + escapeJson(dkim.getVerificationNote()) + "\"" : "null");
        sb.append("},");

        sb.append("\"activeDmarc\":{");
        sb.append("\"status\":\"").append(dmarc != null ? dmarc.getStatus() : "RECORD_ABSENT").append("\",");
        sb.append("\"record\":").append(dmarc != null && dmarc.getDmarcRecord() != null ? "\"" + escapeJson(dmarc.getDmarcRecord()) + "\"" : "null").append(",");
        sb.append("\"policy\":\"").append(dmarc != null ? dmarc.getPolicy() : "none").append("\",");
        sb.append("\"spfAligned\":").append(dmarc != null && dmarc.isSpfAligned()).append(",");
        sb.append("\"dkimAligned\":").append(dmarc != null && dmarc.isDkimAligned());
        sb.append("},");
        sb.append("\"dataMode\":\"").append(dataMode != null ? dataMode : "REAL").append("\"");

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
