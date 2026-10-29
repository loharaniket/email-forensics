package com.emailforensics.backend.config;

import com.emailforensics.backend.infrastructure.fixtures.DefaultDnsFixtureProvider;
import com.emailforensics.backend.infrastructure.fixtures.DefaultGeoIpFixtureProvider;
import com.emailforensics.backend.infrastructure.fixtures.DnsFixtureProvider;
import com.emailforensics.backend.infrastructure.fixtures.GeoIpFixtureProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"test", "dev"})
@ConditionalOnProperty(name = "emailforensics.fixtures.mock-providers-enabled", havingValue = "true", matchIfMissing = true)
public class MockFixtureConfiguration {

    @Bean
    public DnsFixtureProvider dnsFixtureProvider() {
        return new DefaultDnsFixtureProvider();
    }

    @Bean
    public GeoIpFixtureProvider geoIpFixtureProvider() {
        return new DefaultGeoIpFixtureProvider();
    }
}

