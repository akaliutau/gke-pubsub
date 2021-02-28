package cloud.gcp.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class StatisticsRecord implements Serializable {
    private Timestamp timestamp;
    private int totalLettersRead;
    private int totalDuplicateLetters;
    private int totalLettersReSent;
    private int numUnaskedMessages;
    private int numActiveInstances;

    public void incTotalLettersRead(int delta) {
        this.totalLettersRead += delta;
    }

    public void incTotalDuplicateLetters(int delta) {
        this.totalDuplicateLetters += delta;
    }

    public void incTotalLettersReSent(int delta) {
        this.totalLettersReSent += delta;
    }

    public StatisticsRecord snapshot() {
        return new StatisticsRecord(
                new Timestamp(System.currentTimeMillis()),
                this.totalLettersRead,
                this.totalDuplicateLetters,
                this.totalLettersReSent,
                this.numUnaskedMessages,
                this.numActiveInstances);
    }

}
