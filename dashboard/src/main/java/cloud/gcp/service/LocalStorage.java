package cloud.gcp.service;

import cloud.gcp.model.StatisticsRecord;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class LocalStorage {

    public static Resource getResource(List<StatisticsRecord> records) {
        try {
            CSVMapper mapper = new CSVMapper();
            Path file = mapper.convertToCSV(records).toPath();
            Resource resource = new UrlResource(file.toUri());
            if(resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found");
            }
        } catch (IOException ex) {
            throw new RuntimeException("Cannot create CSV file", ex);
        }
    }
}
