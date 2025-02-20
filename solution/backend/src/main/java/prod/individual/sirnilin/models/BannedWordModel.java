package prod.individual.sirnilin.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "banned_words")
public class BannedWordModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String word;

    public BannedWordModel() {
    }

    public BannedWordModel(String word) {
        this.word = word;
    }
}
