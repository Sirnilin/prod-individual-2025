package prod.individual.sirnilin.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TimeRequest {

    @JsonProperty("current_date")
    private int currentDate;
}
