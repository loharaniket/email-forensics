package com.emailforensics.backend.config;

import com.emailforensics.backend.infrastructure.DataProvenance;
import com.emailforensics.backend.infrastructure.DnsLookupService;
import com.emailforensics.backend.infrastructure.DnsResult;
import com.emailforensics.backend.infrastructure.IpArtifact;
import com.emailforensics.backend.infrastructure.IpClassifier;
import com.emailforensics.backend.infrastructure.fixtures.DefaultDnsFixtureProvider;
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
    void shouldExposeAllFourDataModesAcrossSystem() {
        assertEquals("REAL", DataProvenance.REAL.name());
        assertEquals("SIMULATED", DataProvenance.SIMULATED.name());
        assertEquals("UNAVAILABLE", DataProvenance.UNAVAILABLE.name());
        assertEquals("NOT_CHECKED", DataProvenance.NOT_CHECKED.name());
    }
}
