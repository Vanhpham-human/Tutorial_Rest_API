package com.fptaptech.catalog.report;

import com.fptaptech.catalog.common.ApiErrorCode;
import com.fptaptech.catalog.common.ApiErrorResponses;
import com.fptaptech.catalog.common.ApiGroups;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Reports", description = "Báo cáo nội bộ")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(path = "/api/admin/reports", produces = MediaType.APPLICATION_JSON_VALUE)
public class ReportController {

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @ApiGroups({"admin"})
    @Operation(operationId = "dailyRevenue", summary = "Doanh thu theo ngày",
            description = "ADMIN. Tính các đơn DELIVERED hoặc RETURN_REQUESTED theo ngày giao (UTC). Đơn RETURNED không tính.")
    @ApiResponse(responseCode = "200", description = "Báo cáo doanh thu",
            content = @Content(schema = @Schema(implementation = DailyRevenueReport.class)))
    @ApiErrorResponses({
            ApiErrorCode.UNAUTHORIZED, ApiErrorCode.FORBIDDEN, ApiErrorCode.INVALID_DATE_RANGE, ApiErrorCode.VALIDATION_FAILED
    })
    @GetMapping("/daily-revenue")
    public DailyRevenueReport daily(@ParameterObject @Valid RevenueQuery query) {
        return reports.daily(query);
    }
}
