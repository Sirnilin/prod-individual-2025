package prod.individual.sirnilin.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prod.individual.sirnilin.models.CampaignModel;

@Repository
public interface CampaignRepository extends JpaRepository<CampaignModel, Long> {
}
