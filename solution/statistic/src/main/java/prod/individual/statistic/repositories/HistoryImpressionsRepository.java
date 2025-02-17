package prod.individual.statistic.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import prod.individual.statistic.models.HistoryImpressionsModel;

import java.util.*;

@Repository
public interface HistoryImpressionsRepository extends JpaRepository<HistoryImpressionsModel, UUID> {

    Optional<HistoryImpressionsModel> findByClientIdAndCampaignId(UUID clientId, UUID campaignId);

    List<HistoryImpressionsModel> findByCampaignId(UUID campaignId);

    List<HistoryImpressionsModel> findByAdvertiserId(UUID advertiserId);

    int countByCampaignId(UUID campaignId);

    @Query("SELECT hi.date, COUNT(hi) FROM HistoryImpressionsModel hi WHERE hi.campaignId = :campaignId GROUP BY hi.date")
    List<Object[]> countDailyImpressions(@Param("campaignId") UUID campaignId);

    @Query("SELECT hi.campaignId, COUNT(hi) FROM HistoryImpressionsModel hi WHERE hi.advertiserId = :advertiserId GROUP BY hi.campaignId")
    List<Object[]> countImpressionsByAdvertiser(@Param("advertiserId") UUID advertiserId);

    @Query("SELECT hi.date, hi.campaignId, COUNT(hi) FROM HistoryImpressionsModel hi WHERE hi.advertiserId = :advertiserId GROUP BY hi.date, hi.campaignId")
    List<Object[]> countDailyImpressionsByAdvertiser(@Param("advertiserId") UUID advertiserId);

    Boolean existsByClientIdAndCampaignId(UUID clientId, UUID campaignId);

    List<HistoryImpressionsModel> findByCampaignIdAndDate(UUID campaignId, Integer date);

    List<HistoryImpressionsModel> findByAdvertiserIdAndDate(UUID advertiserId, Integer date);

    @Query("SELECT hi.campaignId, COUNT(hi) FROM HistoryImpressionsModel hi WHERE hi.campaignId IN :campaignIds GROUP BY hi.campaignId")
    Map<UUID, Integer> countByCampaignIds(@Param("campaignIds") List<UUID> campaignIds);

}

