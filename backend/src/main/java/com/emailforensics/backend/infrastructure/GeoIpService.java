package com.emailforensics.backend.infrastructure;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeoIpService {

    private final IpClassifier ipClassifier;
    private final DnsLookupService dnsLookupService;
    private final String dbPath;
    private final Map<String, IpArtifact> mockIpStore = new ConcurrentHashMap<>();
    private final Map<String, AsnInfo> asnCache = new ConcurrentHashMap<>();

    private DatabaseReader reader;

    @Autowired
    public GeoIpService(IpClassifier ipClassifier,
                        DnsLookupService dnsLookupService,
                        @Value("${emailforensics.geoip.db-path:./GeoLite2-City.mmdb}") String dbPath) {
        this.ipClassifier = ipClassifier;
        this.dnsLookupService = dnsLookupService;
        this.dbPath = dbPath;
    }

    public GeoIpService(IpClassifier ipClassifier, String dbPath) {
        this(ipClassifier, new DnsLookupService(2000), dbPath);
    }

    public void registerMockIp(String ip, IpArtifact artifact) {
        mockIpStore.put(ip, artifact);
    }

    @PreDestroy
    public synchronized void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignored) {
            }
            reader = null;
        }
    }

    private synchronized DatabaseReader getReader() {
        if (reader != null) {
            return reader;
        }
        File dbFile = resolveDbFile(this.dbPath);
        if (dbFile != null && dbFile.exists() && dbFile.isFile()) {
            try {
                reader = new DatabaseReader.Builder(dbFile)
                        .withCache(new CHMCache())
                        .build();
            } catch (IOException ignored) {
                reader = null;
            }
        }
        return reader;
    }

    private File resolveDbFile(String configuredPath) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            File f = new File(configuredPath);
            if (f.exists() && f.isFile()) {
                return f;
            }
            if (!configuredPath.contains("GeoLite2-City.mmdb")) {
                return null;
            }
        }
        String[] candidates = new String[]{
                "GeoLite2-City.mmdb",
                "backend/GeoLite2-City.mmdb",
                "./GeoLite2-City.mmdb",
                "../backend/GeoLite2-City.mmdb",
                System.getProperty("user.dir") + File.separator + "GeoLite2-City.mmdb",
                System.getProperty("user.dir") + File.separator + "backend" + File.separator + "GeoLite2-City.mmdb"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.isFile() && f.length() > 0) {
                return f;
            }
        }
        return null;
    }

    public IpArtifact enrichIp(String ipAddress, String sourceContext) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return null;
        }

        String cleanIp = ipAddress.trim();
        if (cleanIp.startsWith("[") && cleanIp.endsWith("]")) {
            cleanIp = cleanIp.substring(1, cleanIp.length() - 1);
        }

        if (mockIpStore.containsKey(cleanIp)) {
            IpArtifact mocked = mockIpStore.get(cleanIp);
            mocked.setSourceContext(sourceContext);
            mocked.setObservedAt(Instant.now());
            return mocked;
        }

        boolean isPrivate = ipClassifier.isPrivateOrReserved(cleanIp);
        if (isPrivate) {
            return new IpArtifact(
                    null,
                    null,
                    cleanIp,
                    sourceContext,
                    true,
                    "PRIVATE_IP_SKIPPED",
                    null,
                    "Internal / Reserved Network",
                    null,
                    null,
                    null,
                    null,
                    "IANA Reserved",
                    Instant.now()
            );
        }

        File dbFile = resolveDbFile(this.dbPath);
        if (dbFile == null || !dbFile.exists()) {
            return new IpArtifact(
                    null,
                    null,
                    cleanIp,
                    sourceContext,
                    false,
                    "DB_UNAVAILABLE",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "MaxMind GeoLite2 (File not provided)",
                    Instant.now()
            );
        }

        String country = null;
        String city = null;
        BigDecimal latitude = null;
        BigDecimal longitude = null;
        String asnNumber = null;
        String organization = null;
        boolean foundInDb = false;

        DatabaseReader currentReader = getReader();
        if (currentReader != null) {
            try {
                InetAddress addr = InetAddress.getByName(cleanIp);
                CityResponse resp = currentReader.city(addr);
                foundInDb = true;

                if (resp.getCountry() != null && resp.getCountry().getName() != null) {
                    country = resp.getCountry().getName();
                } else if (resp.getRegisteredCountry() != null && resp.getRegisteredCountry().getName() != null) {
                    country = resp.getRegisteredCountry().getName();
                }

                if (resp.getCity() != null && resp.getCity().getName() != null) {
                    city = resp.getCity().getName();
                }

                if (resp.getLocation() != null) {
                    if (resp.getLocation().getLatitude() != null) {
                        latitude = BigDecimal.valueOf(resp.getLocation().getLatitude());
                    }
                    if (resp.getLocation().getLongitude() != null) {
                        longitude = BigDecimal.valueOf(resp.getLocation().getLongitude());
                    }
                }

                if (resp.getTraits() != null) {
                    if (resp.getTraits().getAutonomousSystemNumber() != null) {
                        asnNumber = "AS" + resp.getTraits().getAutonomousSystemNumber();
                    }
                    if (resp.getTraits().getAutonomousSystemOrganization() != null) {
                        organization = resp.getTraits().getAutonomousSystemOrganization();
                    } else if (resp.getTraits().getIsp() != null) {
                        organization = resp.getTraits().getIsp();
                    }
                }
            } catch (com.maxmind.geoip2.exception.AddressNotFoundException ignored) {
            } catch (Exception ignored) {
            }
        }

        if (asnNumber == null || organization == null) {
            AsnInfo asnInfo = resolveAsn(cleanIp);
            if (asnInfo != null) {
                if (asnNumber == null && asnInfo.getAsnNumber() != null) {
                    asnNumber = asnInfo.getAsnNumber();
                }
                if (organization == null && asnInfo.getOrganization() != null) {
                    organization = asnInfo.getOrganization();
                }
                if (country == null && asnInfo.getCountryCode() != null) {
                    country = asnInfo.getCountryCode();
                }
            }
        }

        String lookupStatus = (foundInDb || asnNumber != null || country != null) ? "ENRICHED" : "NOT_FOUND";

        return new IpArtifact(
                null,
                null,
                cleanIp,
                sourceContext,
                false,
                lookupStatus,
                asnNumber,
                organization,
                country,
                city,
                latitude,
                longitude,
                "MaxMind GeoLite2",
                Instant.now()
        );
    }

    private AsnInfo resolveAsn(String cleanIp) {
        if (asnCache.containsKey(cleanIp)) {
            return asnCache.get(cleanIp);
        }

        AsnInfo wellKnown = checkWellKnownSubnets(cleanIp);
        if (wellKnown != null) {
            asnCache.put(cleanIp, wellKnown);
            return wellKnown;
        }

        if (dnsLookupService != null) {
            try {
                String[] parts = cleanIp.split("\\.");
                if (parts.length == 4) {
                    String reverseOriginDns = parts[3] + "." + parts[2] + "." + parts[1] + "." + parts[0] + ".origin.asn.cymru.com";
                    List<String> txtRecords = dnsLookupService.lookupTxtRecords(reverseOriginDns);
                    if (!txtRecords.isEmpty()) {
                        String rawTxt = txtRecords.get(0).replaceAll("^\"|\"$", "").trim();
                        String[] segments = rawTxt.split("\\|");
                        if (segments.length >= 1) {
                            String rawAsn = segments[0].trim();
                            if (!rawAsn.isEmpty()) {
                                String asnNum = rawAsn.toUpperCase().startsWith("AS") ? rawAsn.toUpperCase() : "AS" + rawAsn;
                                String countryCode = (segments.length >= 3) ? segments[2].trim() : null;
                                String orgName = null;
                                try {
                                    String asnDomain = asnNum.toLowerCase() + ".asn.cymru.com";
                                    List<String> orgRecords = dnsLookupService.lookupTxtRecords(asnDomain);
                                    if (!orgRecords.isEmpty()) {
                                        String rawOrgTxt = orgRecords.get(0).replaceAll("^\"|\"$", "").trim();
                                        String[] orgParts = rawOrgTxt.split("\\|");
                                        if (orgParts.length >= 5) {
                                            orgName = orgParts[4].trim();
                                            int comma = orgName.lastIndexOf(',');
                                            if (comma > 0) {
                                                orgName = orgName.substring(0, comma).trim();
                                            }
                                        }
                                    }
                                } catch (Exception ignored) {
                                }

                                AsnInfo result = new AsnInfo(asnNum, orgName, countryCode);
                                asnCache.put(cleanIp, result);
                                return result;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        try {
            String host = InetAddress.getByName(cleanIp).getCanonicalHostName();
            if (host != null && !host.equals(cleanIp)) {
                AsnInfo fallback = new AsnInfo(null, host, null);
                asnCache.put(cleanIp, fallback);
                return fallback;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private AsnInfo checkWellKnownSubnets(String ip) {
        if (ip.startsWith("8.8.") || ip.startsWith("8.34.") || ip.startsWith("8.35.") || ip.startsWith("64.233.") || ip.startsWith("66.102.") || ip.startsWith("66.249.") || ip.startsWith("72.14.") || ip.startsWith("74.125.") || ip.startsWith("209.85.")) {
            return new AsnInfo("AS15169", "Google LLC", "United States");
        }
        if (ip.startsWith("1.1.1.") || ip.startsWith("1.0.0.") || ip.startsWith("104.16.") || ip.startsWith("104.17.") || ip.startsWith("104.18.") || ip.startsWith("104.19.") || ip.startsWith("104.20.") || ip.startsWith("104.21.") || ip.startsWith("172.64.")) {
            return new AsnInfo("AS13335", "Cloudflare, Inc.", "United States");
        }
        if (ip.startsWith("13.64.") || ip.startsWith("13.104.") || ip.startsWith("20.190.") || ip.startsWith("40.76.") || ip.startsWith("40.112.") || ip.startsWith("52.96.") || ip.startsWith("52.112.") || ip.startsWith("52.238.")) {
            return new AsnInfo("AS8075", "Microsoft Corporation", "United States");
        }
        if (ip.startsWith("3.5.") || ip.startsWith("15.230.") || ip.startsWith("18.155.") || ip.startsWith("52.0.") || ip.startsWith("54.240.")) {
            return new AsnInfo("AS16509", "Amazon.com, Inc.", "United States");
        }
        if (ip.startsWith("140.82.")) {
            return new AsnInfo("AS36459", "GitHub, Inc.", "United States");
        }
        if (ip.startsWith("195.201.") || ip.startsWith("78.46.") || ip.startsWith("88.198.") || ip.startsWith("148.251.")) {
            return new AsnInfo("AS24940", "Hetzner Online GmbH", "Germany");
        }
        return null;
    }

    public static class AsnInfo {
        private final String asnNumber;
        private final String organization;
        private final String countryCode;

        public AsnInfo(String asnNumber, String organization, String countryCode) {
            this.asnNumber = asnNumber;
            this.organization = organization;
            this.countryCode = countryCode;
        }

        public String getAsnNumber() {
            return asnNumber;
        }

        public String getOrganization() {
            return organization;
        }

        public String getCountryCode() {
            return countryCode;
        }
    }
}
