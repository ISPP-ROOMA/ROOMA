package com.example.demo.Incident;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IncidentScheduler {

    private final IncidentService incidentService;

    public IncidentScheduler(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void autoCloseResolvedIncidents() {
        incidentService.autoCloseResolvedIncidents();
    }
}
