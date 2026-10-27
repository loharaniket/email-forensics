package com.emailforensics.backend.infrastructure.fixtures;

import com.emailforensics.backend.infrastructure.DataProvenance;
import com.emailforensics.backend.infrastructure.GeoIpService;
import com.emailforensics.backend.infrastructure.IpArtifact;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultGeoIpFixtureProvider implements GeoIpFixtureProvider {

    private final Map<String, IpArtifact> mockIpStore = new ConcurrentHashMap<>();

    @Override
    public boolean hasMock(String ip) {
        if (ip == null) {
            return false;
        }
        return mockIpStore.containsKey(ip.trim());
    }

    @Override
    public IpArtifact getMockIp(String ip, String sourceContext) {
        if (ip == null) {
            return null;
        }
        IpArtifact template = mockIpStore.get(ip.trim());
        if (template == null) {
            return null;
        }
        return new IpArtifact(
                template.getId(),
                template.getAnalysisRun(),
                template.getIpAddress(),
                sourceContext,
                template.getIsPrivate(),
                template.getLookupStatus(),
                template.getAsnNumber(),
                template.getOrganization(),
                template.getCountry(),
                template.getCity(),
                template.getLatitude(),
                template.getLongitude(),
                template.getDataSource(),
                Instant.now(),
                DataProvenance.SIMULATED.name()
        );
    }

    @Override
    public void registerMockIp(String ip, IpArtifact artifact) {
        if (ip != null && artifact != null) {
            artifact.setDataMode(DataProvenance.SIMULATED.name());
            mockIpStore.put(ip.trim(), artifact);
        }
    }

    @Override
    public GeoIpService.AsnInfo checkWellKnownSubnets(String ip) {
        if (ip == null) {
            return null;
        }
        if (ip.startsWith("8.8.") || ip.startsWith("8.34.") || ip.startsWith("8.35.") ||
                ip.startsWith("64.233.") || ip.startsWith("66.102.") || ip.startsWith("66.249.") ||
                ip.startsWith("72.14.") || ip.startsWith("74.125.") || ip.startsWith("209.85.")) {
            return new GeoIpService.AsnInfo("AS15169", "Google LLC", "United States");
        }
        if (ip.startsWith("1.1.1.") || ip.startsWith("1.0.0.") || ip.startsWith("104.16.") ||
                ip.startsWith("104.17.") || ip.startsWith("104.18.") || ip.startsWith("104.19.") ||
                ip.startsWith("104.20.") || ip.startsWith("104.21.") || ip.startsWith("172.64.")) {
            return new GeoIpService.AsnInfo("AS13335", "Cloudflare, Inc.", "United States");
        }
        if (ip.startsWith("13.64.") || ip.startsWith("13.104.") || ip.startsWith("20.190.") ||
                ip.startsWith("40.76.") || ip.startsWith("40.112.") || ip.startsWith("52.96.") ||
                ip.startsWith("52.112.") || ip.startsWith("52.238.")) {
            return new GeoIpService.AsnInfo("AS8075", "Microsoft Corporation", "United States");
        }
        if (ip.startsWith("3.5.") || ip.startsWith("15.230.") || ip.startsWith("18.155.") ||
                ip.startsWith("52.0.") || ip.startsWith("54.240.")) {
            return new GeoIpService.AsnInfo("AS16509", "Amazon.com, Inc.", "United States");
        }
        if (ip.startsWith("140.82.")) {
            return new GeoIpService.AsnInfo("AS36459", "GitHub, Inc.", "United States");
        }
        if (ip.startsWith("195.201.") || ip.startsWith("78.46.") || ip.startsWith("88.198.") ||
                ip.startsWith("148.251.")) {
            return new GeoIpService.AsnInfo("AS24940", "Hetzner Online GmbH", "Germany");
        }
        return null;
    }
}
