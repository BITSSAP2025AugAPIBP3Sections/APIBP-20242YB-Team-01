package com.ebaazee.analytics_service.graphql;

import com.ebaazee.analytics_service.dto.ReportResponse;
import com.ebaazee.analytics_service.service.ReportService;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
public class ReportResolver {

    private static final Logger log = LoggerFactory.getLogger(ReportResolver.class);

    private final ReportService reportService;

    public ReportResolver(ReportService reportService) {
        this.reportService = reportService;
    }

    @QueryMapping(name = "downloadProductReport")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ReportResponse downloadProductReport() {
        log.debug("GraphQL Query downloadProductReport called");
        try {
            byte[] bytes = reportService.generateProductReport();
            log.info("Successfully generated product report ({} bytes)", bytes.length);
            String b64 = Base64.getEncoder().encodeToString(bytes);
            return new ReportResponse("Product_Report.xlsx", b64);
        } catch (Exception e) {
            log.error("Failed to generate product report", e);
            return new ReportResponse(null, null);
        }
    }
}
