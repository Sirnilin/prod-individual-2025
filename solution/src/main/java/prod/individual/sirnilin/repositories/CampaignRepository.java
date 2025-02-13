package prod.individual.sirnilin.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prod.individual.sirnilin.models.CampaignModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<CampaignModel, Long> {
    Optional<CampaignModel> findByCampaignId(UUID campaignId);

    List<CampaignModel> findByAdvertiserId(UUID advertiserId);
}
