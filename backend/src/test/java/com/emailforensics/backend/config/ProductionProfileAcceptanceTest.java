package com.emailforensics.backend.config;

import com.emailforensics.backend.infrastructure.DataProvenance;
import com.emailforensics.backend.infrastructure.DnsLookupService;
import com.emailforensics.backend.infrastructure.DnsResult;
import com.emailforensics.backend.infrastructure.GeoIpService;
import com.emailforensics.backend.infrastructure.IpArtifact;
import com.emailforensics.backend.infrastructure.IpClassifier;
import com.emailforensics.backend.infrastructure.fixtures.DefaultDnsFixtureProvider;
import com.emailforensics.backend.infrastructure.fixtures.DefaultGeoIpFixtureProvider;
import com.emailforensics.backend.visual.OcrService;
import com.emailforensics.backend.visual.fixtures.DefaultOcrFixtureProvider;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProductionProfileAcceptanceTest {

    @Test
    void shouldRejectStartupWhenJwtSecretIsMissingInProductionProfile() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        prodEnv.setProperty("spring.datasource.password", "aValidSecureCustomPassword#992");
        prodEnv.setProperty("app.jwt.secret", "");

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                prodEnv,
                "aValidSecureCustomPassword#992",
                ""
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateConfiguration);
        assertNotNull(ex.getMessage());
    }

    @Test
    void shouldRejectStartupWhenDefaultCommittedJwtSecretIsUsedInProductionProfile() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        prodEnv.setProperty("spring.datasource.password", "aValidSecureCustomPassword#992");
        prodEnv.setProperty("app.jwt.secret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                prodEnv,
                "aValidSecureCustomPassword#992",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateConfiguration);
        assertNotNull(ex.getMessage());
    }

    @Test
    void shouldRejectStartupWhenDbPasswordIsMissingInProductionProfile() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        prodEnv.setProperty("spring.datasource.password", "");
        prodEnv.setProperty("app.jwt.secret", "aVeryStrongAndSecureProductionSecretKeyWithSufficientLength123");

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                prodEnv,
                "",
                "aVeryStrongAndSecureProductionSecretKeyWithSufficientLength123"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateConfiguration);
        assertNotNull(ex.getMessage());
    }

    @Test
    void shouldRejectStartupWhenDefaultCommittedDbPasswordIsUsedInProductionProfile() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        prodEnv.setProperty("spring.datasource.password", "aniket@12345");
        prodEnv.setProperty("app.jwt.secret", "aVeryStrongAndSecureProductionSecretKeyWithSufficientLength123");

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                prodEnv,
                "aniket@12345",
                "aVeryStrongAndSecureProductionSecretKeyWithSufficientLength123"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateConfiguration);
        assertNotNull(ex.getMessage());
    }

    @Test
    void shouldAcceptStartupWhenValidSecretsProvidedInProductionProfile() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        prodEnv.setProperty("spring.datasource.password", "SuperCustomUniqueProductionDbPassword987!");
        prodEnv.setProperty("app.jwt.secret", "SuperCustomUniqueProductionJwtSigningSecretKeyAtLeast32Chars!");

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                prodEnv,
                "SuperCustomUniqueProductionDbPassword987!",
                "SuperCustomUniqueProductionJwtSigningSecretKeyAtLeast32Chars!"
        );

        validator.validateConfiguration();
    }

    @Test
    void productionProfileMustNeverReturnSimulatedDnsRecords() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        DefaultDnsFixtureProvider fixtureProvider = new DefaultDnsFixtureProvider();
        DnsLookupService dnsService = new DnsLookupService(1000, fixtureProvider, prodEnv);

        DnsResult bankResult = dnsService.lookupDomain("secure-bank.test");
        assertNotNull(bankResult);
        assertNotEquals(DataProvenance.SIMULATED.name(), bankResult.getDataMode());

        DnsResult phishResult = dnsService.lookupDomain("phish-sender.test");
        assertNotNull(phishResult);
        assertNotEquals(DataProvenance.SIMULATED.name(), phishResult.getDataMode());

        List<String> txtRecords = dnsService.lookupTxtRecords("phish-sender.test");
        assertNotNull(txtRecords);
    }

    @Test
    void productionProfileMustNeverReturnFabricatedAsnOrMockIpData() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        IpClassifier ipClassifier = new IpClassifier();
        DefaultDnsFixtureProvider dnsFixture = new DefaultDnsFixtureProvider();
        DnsLookupService dnsService = new DnsLookupService(1000, dnsFixture, prodEnv);
        DefaultGeoIpFixtureProvider geoFixture = new DefaultGeoIpFixtureProvider();

        IpArtifact mockArtifact = new IpArtifact(
                null, null, "198.51.100.99", "TEST", false, "ENRICHED",
                "AS99999", "Fabricated Corp", "FakeCountry", "FakeCity",
                null, null, "MockSource", null, DataProvenance.SIMULATED.name()
        );
        geoFixture.registerMockIp("198.51.100.99", mockArtifact);

        GeoIpService geoIpService = new GeoIpService(
                ipClassifier,
                dnsService,
                "nonexistent-geolite.mmdb",
                geoFixture,
                prodEnv
        );

        IpArtifact enriched = geoIpService.enrichIp("198.51.100.99", "TEST_HOP");
        assertNotNull(enriched);
        assertNotEquals(DataProvenance.SIMULATED.name(), enriched.getDataMode());
        assertNotEquals("Fabricated Corp", enriched.getOrganization());
    }

    @Test
    void productionProfileMustNeverReturnMockOcrData() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        DefaultOcrFixtureProvider ocrFixture = new DefaultOcrFixtureProvider();
        String testHash = "d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2";
        ocrFixture.registerMockOcrText(testHash, "SECRET FABRICATED CREDENTIALS");

        OcrService ocrService = new OcrService("./nonexistent_tessdata", ocrFixture, prodEnv);

        byte[] fakeImage = new byte[]{1, 2, 3, 4, 5};
        OcrService.OcrResult result = ocrService.processImage(fakeImage, testHash);

        assertNotNull(result);
        assertNotEquals(DataProvenance.SIMULATED.name(), result.getDataMode());
        assertNull(result.getOcrText());
    }

    @Test
    void shouldExposeAllFourDataModesAcrossSystem() {
        assertEquals("REAL", DataProvenance.REAL.name());
        assertEquals("SIMULATED", DataProvenance.SIMULATED.name());
        assertEquals("UNAVAILABLE", DataProvenance.UNAVAILABLE.name());
        assertEquals("NOT_CHECKED", DataProvenance.NOT_CHECKED.name());
    }
}
