package prod.individual.sirnilin.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prod.individual.sirnilin.models.HistoryClicksModel;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistoryClicksRepository extends JpaRepository<HistoryClicksModel, Long> {
    List<HistoryClicksModel> findByCampaignId(UUID campaignId);

    List<HistoryClicksModel> findByAdvertiserId(UUID advertiserId);
}
