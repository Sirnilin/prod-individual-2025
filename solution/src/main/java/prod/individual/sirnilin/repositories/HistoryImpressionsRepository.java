package prod.individual.sirnilin.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prod.individual.sirnilin.models.HistoryImpressionsModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HistoryImpressionsRepository extends JpaRepository<HistoryImpressionsModel, UUID> {
    Optional<HistoryImpressionsModel> findByClientIdAndCampaignId(UUID clientId, UUID campaignId);

    List<HistoryImpressionsModel> findByCampaignId(UUID campaignId);

    List<HistoryImpressionsModel> findByAdvertiserId(UUID advertiserId);
}
