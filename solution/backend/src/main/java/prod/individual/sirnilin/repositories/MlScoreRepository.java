package prod.individual.sirnilin.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prod.individual.sirnilin.models.MlScoreModel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MlScoreRepository extends JpaRepository<MlScoreModel, Long> {
    Optional<MlScoreModel> findByAdvertiserIdAndClientId(UUID advertiserId, UUID clientId);

    List<MlScoreModel> findByAdvertiserIdInAndClientId(Collection<UUID> advertiserIds, UUID clientId);

}
