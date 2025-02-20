package prod.individual.sirnilin.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import prod.individual.sirnilin.models.BannedWordModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BannedWordRepository extends JpaRepository<BannedWordModel, UUID> {

    List<BannedWordModel> findAllByWord(String word);

    Optional<BannedWordModel> findByWordIgnoreCase(String word);
}
