package com.ebaazee.analytics_service.graphql;

import com.ebaazee.analytics_service.dto.ReportResponse;
import com.ebaazee.analytics_service.service.ReportService;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.Base64;

@Controller
public class ReportResolver {

    private final ReportService reportService;

    public ReportResolver(ReportService reportService) {
        this.reportService = reportService;
    }

    @QueryMapping(name = "downloadProductReport")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ReportResponse downloadProductReport() {
        try {
            byte[] bytes = reportService.generateProductReport();
            String b64 = Base64.getEncoder().encodeToString(bytes);
            return new ReportResponse("Product_Report.xlsx", b64);
        } catch (Exception e) {
            return new ReportResponse(null, null);
        }
    }
}
