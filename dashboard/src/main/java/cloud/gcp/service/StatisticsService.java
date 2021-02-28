package cloud.gcp.service;

import cloud.gcp.exception.NotFoundException;
import cloud.gcp.model.StatisticsRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class StatisticsService {

    private final static int maxSize = 1000;

    private SynchronizedLinkedList<StatisticsRecord> statistics;
    private StatisticsRecord curStatisticsRecord = new StatisticsRecord();
    private Map<String, Boolean> processedLettersSet = new ConcurrentHashMap<>();
    private Deque<String> processedLetters = new ConcurrentLinkedDeque<>();

    public StatisticsService() {
        this.statistics = new SynchronizedLinkedList<>();
    }

    public synchronized void incTotalLettersRead(int delta) {
        curStatisticsRecord.incTotalLettersRead(delta);
    }

    public synchronized void incTotalLettersReSent(int delta) {
        curStatisticsRecord.incTotalLettersReSent(delta);
    }

    public synchronized void setNumActiveInstances(int numActiveInstances) {
        curStatisticsRecord.setNumActiveInstances(numActiveInstances);
    }

    public synchronized void setNumUnaskedMessages(int numUnaskedMessages) {
        curStatisticsRecord.setNumUnaskedMessages(numUnaskedMessages);
    }

    public synchronized void incTotalDuplicateLetters(int delta) {
        curStatisticsRecord.incTotalDuplicateLetters(delta);
    }

    public void addId(String id) {
        if (processedLettersSet.containsKey(id)) {
            this.incTotalDuplicateLetters(1);
        }
        processedLettersSet.put(id, true);
        if (processedLetters.size() > maxSize) {
            processedLettersSet.remove(processedLetters.pollFirst());
        }
        processedLetters.add(id);
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void makeSnapshot() {
        log.info("makeSnapshot");
        this.add(curStatisticsRecord.snapshot());
    }

    private void add(StatisticsRecord statisticsRecord) {
        this.statistics.add(statisticsRecord);
    }

    public StatisticsRecord getStatistics() {
        return this.statistics.getLast().orElseThrow(NotFoundException::new);
    }

    public List<StatisticsRecord> getAllList() {
        return this.statistics.copy();
    }

    public List<StatisticsRecord> getList(int size) {
        return this.statistics.copy(size);
    }

    private String toCSV() {
        return null;
    }

}
