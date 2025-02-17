package prod.individual.statistic.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import prod.individual.statistic.models.HistoryClicksModel;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public interface HistoryClicksRepository extends JpaRepository<HistoryClicksModel, Long> {

    List<HistoryClicksModel> findByCampaignId(UUID campaignId);

    List<HistoryClicksModel> findByAdvertiserId(UUID advertiserId);

    int countByCampaignId(UUID campaignId);

    @Query("SELECT hc.date, COUNT(hc) FROM HistoryClicksModel hc WHERE hc.campaignId = :campaignId GROUP BY hc.date")
    List<Object[]> countDailyClicks(@Param("campaignId") UUID campaignId);

    @Query("SELECT hc.campaignId, COUNT(hc) FROM HistoryClicksModel hc WHERE hc.advertiserId = :advertiserId GROUP BY hc.campaignId")
    List<Object[]> countClicksByAdvertiser(@Param("advertiserId") UUID advertiserId);

    @Query("SELECT hc.date, hc.campaignId, COUNT(hc) FROM HistoryClicksModel hc WHERE hc.advertiserId = :advertiserId GROUP BY hc.date, hc.campaignId")
    List<Object[]> countDailyClicksByAdvertiser(@Param("advertiserId") UUID advertiserId);

    Boolean existsByClientIdAndCampaignId(UUID clientId, UUID campaignId);

    List<HistoryClicksModel> findByCampaignIdAndDate(UUID campaignId, Integer date);

    List<HistoryClicksModel> findByAdvertiserIdAndDate(UUID advertiserId, Integer date);

    @Query("SELECT hc.campaignId, COUNT(hc) FROM HistoryClicksModel hc WHERE hc.campaignId IN :campaignIds GROUP BY hc.campaignId")
    Map<UUID, Integer> countByCampaignIds(@Param("campaignIds") List<UUID> campaignIds);

}

