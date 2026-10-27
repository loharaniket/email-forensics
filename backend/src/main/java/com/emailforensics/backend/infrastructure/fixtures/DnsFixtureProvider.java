package com.emailforensics.backend.infrastructure.fixtures;

import com.emailforensics.backend.infrastructure.DnsResult;
import java.util.List;

public interface DnsFixtureProvider {
    boolean supports(String domain);
    DnsResult getSimulatedDnsResult(String domain);
    boolean supportsTxt(String domain);
    List<String> getSimulatedTxtRecords(String domain);
}
