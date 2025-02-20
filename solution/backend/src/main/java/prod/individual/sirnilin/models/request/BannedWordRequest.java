package prod.individual.sirnilin.models.request;

import lombok.Data;

import java.util.List;

@Data
public class BannedWordRequest {

    private List<String> words;

}
