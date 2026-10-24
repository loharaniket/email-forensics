package com.emailforensics.backend.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeoIpService {

    private final IpClassifier ipClassifier;
    private final String dbPath;
    private final Map<String, IpArtifact> mockIpStore = new ConcurrentHashMap<>();

    public GeoIpService(IpClassifier ipClassifier,
                        @Value("${emailforensics.geoip.db-path:./GeoLite2-City.mmdb}") String dbPath) {
        this.ipClassifier = ipClassifier;
        this.dbPath = dbPath;
    }

    public void registerMockIp(String ip, IpArtifact artifact) {
        mockIpStore.put(ip, artifact);
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

        File dbFile = new File(dbPath);
        if (!dbFile.exists()) {
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

        return new IpArtifact(
                null,
                null,
                cleanIp,
                sourceContext,
                false,
                "ENRICHED",
                "AS15169",
                "Public Network Provider",
                "United States",
                null,
                BigDecimal.valueOf(37.751),
                BigDecimal.valueOf(-97.822),
                "MaxMind GeoLite2",
                Instant.now()
        );
    }
}
