package prod.individual.sirnilin.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import prod.individual.sirnilin.models.HistoryClicksModel;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistoryClicksRepository extends JpaRepository<HistoryClicksModel, Long> {

    List<HistoryClicksModel> findByCampaignId(UUID campaignId);

    List<HistoryClicksModel> findByAdvertiserId(UUID advertiserId);

    long countByCampaignId(UUID campaignId);

    @Query("SELECT hc.date, COUNT(hc) FROM HistoryClicksModel hc WHERE hc.campaignId = :campaignId GROUP BY hc.date")
    List<Object[]> countDailyClicks(@Param("campaignId") UUID campaignId);

    @Query("SELECT hc.campaignId, COUNT(hc) FROM HistoryClicksModel hc WHERE hc.advertiserId = :advertiserId GROUP BY hc.campaignId")
    List<Object[]> countClicksByAdvertiser(@Param("advertiserId") UUID advertiserId);

    @Query("SELECT hc.date, hc.campaignId, COUNT(hc) FROM HistoryClicksModel hc WHERE hc.advertiserId = :advertiserId GROUP BY hc.date, hc.campaignId")
    List<Object[]> countDailyClicksByAdvertiser(@Param("advertiserId") UUID advertiserId);
}

