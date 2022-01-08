package cloud.gcp.controller;

import cloud.gcp.model.StatisticsRecord;
import cloud.gcp.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@RestController
@Slf4j
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
    ResponseEntity<Resource> download(HttpServletRequest request) {
        Resource resource = statisticsService.toCSV();

        // determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }

        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }


}
