package prod.individual.sirnilin.models.resonse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StatisticResponse {

    @JsonProperty("impressions_count")
    private Integer impressionsCount;

    @JsonProperty("clicks_count")
    private Integer clicksCount;

    private Float conversion;

    @JsonProperty("spent_impressions")
    private Float spentImpressions;

    @JsonProperty("spent_clicks")
    private Float spentClicks;

    @JsonProperty("spent_total")
    private Float spentTotal;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer date;
}
