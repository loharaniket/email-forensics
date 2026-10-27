package com.emailforensics.backend.infrastructure.fixtures;

import com.emailforensics.backend.infrastructure.GeoIpService;
import com.emailforensics.backend.infrastructure.IpArtifact;

public interface GeoIpFixtureProvider {
    boolean hasMock(String ip);
    IpArtifact getMockIp(String ip, String sourceContext);
    void registerMockIp(String ip, IpArtifact artifact);
    GeoIpService.AsnInfo checkWellKnownSubnets(String ip);
}
