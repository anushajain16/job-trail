package com.example.anusha.job_trail.analytics;

import com.example.anusha.job_trail.analytics.dto.ConversionResponse;
import com.example.anusha.job_trail.analytics.dto.FunnelResponse;
import com.example.anusha.job_trail.analytics.dto.ResumePerformanceResponse;
import com.example.anusha.job_trail.analytics.dto.TimeInStageResponse;
import com.example.anusha.job_trail.auth.security.AuthenticatedUser;
import com.example.anusha.job_trail.auth.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/funnel")
    public FunnelResponse funnel(@CurrentUser AuthenticatedUser currentUser) {
        return analyticsService.funnel(currentUser.id());
    }

    @GetMapping("/conversion")
    public ConversionResponse conversion(@CurrentUser AuthenticatedUser currentUser) {
        return analyticsService.conversion(currentUser.id());
    }

    @GetMapping("/time-in-stage")
    public TimeInStageResponse timeInStage(@CurrentUser AuthenticatedUser currentUser) {
        return analyticsService.timeInStage(currentUser.id());
    }

    @GetMapping("/resume-performance")
    public ResumePerformanceResponse resumePerformance(@CurrentUser AuthenticatedUser currentUser) {
        return analyticsService.resumePerformance(currentUser.id());
    }
}
