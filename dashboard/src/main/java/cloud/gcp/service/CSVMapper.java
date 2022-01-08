package cloud.gcp.service;

import cloud.gcp.model.StatisticsRecord;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class CSVMapper {

    public File convertToCSV(List<StatisticsRecord> records) throws IOException {
        File csvOutputFile = File.createTempFile("stat-", ".csv");
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            pw.println(this.convertToCSV(StatisticsRecord.getFields()));
            records.stream()
                    .map(r -> r.asLine())
                    .map(this::convertToCSV)
                    .forEach(pw::println);
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
        }
        return csvOutputFile;
    }

    private String convertToCSV(String[] data) {
        return Stream.of(data)
                .map(this::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }

    private String escapeSpecialCharacters(String data) {
        String escaped= data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escaped = "\"" + data + "\"";
        }
        return escaped;
    }
}
