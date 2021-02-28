package cloud.gcp.controller;

import cloud.gcp.model.StatisticsRecord;
import cloud.gcp.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class StatisticsAPI {
    private final StatisticsService statisticsService;

    StatisticsAPI(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics/all")
    List<StatisticsRecord> all() {
        return statisticsService.getAllList();
    }

    @GetMapping("/statistics")
    StatisticsRecord getCurrectStatistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("/statistics/download")
    void download() {

    }


}
