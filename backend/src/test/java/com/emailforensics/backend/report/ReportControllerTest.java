package com.emailforensics.backend.report;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getEvidenceReport_notFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/00000000-0000-0000-0000-000000000000/report"))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadEvidenceReportJson_notFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/00000000-0000-0000-0000-000000000000/report/json"))
                .andExpect(status().isNotFound());
    }
}
