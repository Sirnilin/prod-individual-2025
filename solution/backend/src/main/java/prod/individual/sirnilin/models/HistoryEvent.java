package prod.individual.sirnilin.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HistoryEvent {
    private UUID clientId;
    private UUID campaignId;
    private UUID advertiserId;
    private Integer currentDate;
    private Float cost;
}
