package com.emailforensics.backend.infrastructure;

import com.emailforensics.backend.infrastructure.fixtures.DefaultGeoIpFixtureProvider;
import com.emailforensics.backend.infrastructure.fixtures.GeoIpFixtureProvider;
import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
    private final GeoIpFixtureProvider fixtureProvider;
    private final Environment environment;
    private final Map<String, AsnInfo> asnCache = new ConcurrentHashMap<>();

    private DatabaseReader reader;

    @Autowired
    public GeoIpService(
            IpClassifier ipClassifier,
            DnsLookupService dnsLookupService,
            @Value("${emailforensics.geoip.db-path:./GeoLite2-City.mmdb}") String dbPath,
            @Autowired(required = false) GeoIpFixtureProvider fixtureProvider,
            @Autowired(required = false) Environment environment) {
        this.ipClassifier = ipClassifier;
        this.dnsLookupService = dnsLookupService;
        this.dbPath = dbPath;
        this.fixtureProvider = fixtureProvider;
        this.environment = environment;
    }

    public GeoIpService(IpClassifier ipClassifier, String dbPath) {
        this(ipClassifier, new DnsLookupService(2000), dbPath, new DefaultGeoIpFixtureProvider(), null);
    }

    public GeoIpService(IpClassifier ipClassifier, DnsLookupService dnsLookupService, String dbPath) {
        this(ipClassifier, dnsLookupService, dbPath, new DefaultGeoIpFixtureProvider(), null);
    }

    private boolean isProduction() {
        return environment != null && environment.acceptsProfiles(Profiles.of("prod", "production"));
    }

    public void registerMockIp(String ip, IpArtifact artifact) {
        if (!isProduction() && fixtureProvider != null) {
            fixtureProvider.registerMockIp(ip, artifact);
        }
    }

    public GeoIpFixtureProvider getFixtureProvider() {
        return fixtureProvider;
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

        if (!isProduction() && fixtureProvider != null && fixtureProvider.hasMock(cleanIp)) {
            return fixtureProvider.getMockIp(cleanIp, sourceContext);
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
                    Instant.now(),
                    DataProvenance.NOT_CHECKED.name()
            );
        }

        File dbFile = resolveDbFile(this.dbPath);
        if (dbFile == null || !dbFile.exists()) {
            AsnInfo asnInfo = resolveAsn(cleanIp);
            String asnNumber = (asnInfo != null) ? asnInfo.getAsnNumber() : null;
            String organization = (asnInfo != null) ? asnInfo.getOrganization() : null;
            String country = (asnInfo != null) ? asnInfo.getCountryCode() : null;
            String status = (asnNumber != null || organization != null || country != null) ? "ENRICHED" : "DB_UNAVAILABLE";
            String mode = (asnNumber != null || organization != null || country != null) ? DataProvenance.REAL.name() : DataProvenance.UNAVAILABLE.name();

            return new IpArtifact(
                    null,
                    null,
                    cleanIp,
                    sourceContext,
                    false,
                    status,
                    asnNumber,
                    organization,
                    country,
                    null,
                    null,
                    null,
                    "MaxMind GeoLite2 (File not provided)",
                    Instant.now(),
                    mode
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

        boolean enriched = (foundInDb || asnNumber != null || country != null);
        String lookupStatus = enriched ? "ENRICHED" : "NOT_FOUND";
        String dataMode = enriched ? DataProvenance.REAL.name() : DataProvenance.UNAVAILABLE.name();

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
                Instant.now(),
                dataMode
        );
    }

    private AsnInfo resolveAsn(String cleanIp) {
        if (asnCache.containsKey(cleanIp)) {
            return asnCache.get(cleanIp);
        }

        if (!isProduction() && fixtureProvider != null) {
            AsnInfo wellKnown = fixtureProvider.checkWellKnownSubnets(cleanIp);
            if (wellKnown != null) {
                asnCache.put(cleanIp, wellKnown);
                return wellKnown;
            }
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
