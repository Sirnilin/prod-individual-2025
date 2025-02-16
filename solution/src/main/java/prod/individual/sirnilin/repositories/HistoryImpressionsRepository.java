package prod.individual.sirnilin.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    long countByCampaignId(UUID campaignId);

    @Query("SELECT hi.date, COUNT(hi) FROM HistoryImpressionsModel hi WHERE hi.campaignId = :campaignId GROUP BY hi.date")
    List<Object[]> countDailyImpressions(@Param("campaignId") UUID campaignId);

    @Query("SELECT hi.campaignId, COUNT(hi) FROM HistoryImpressionsModel hi WHERE hi.advertiserId = :advertiserId GROUP BY hi.campaignId")
    List<Object[]> countImpressionsByAdvertiser(@Param("advertiserId") UUID advertiserId);

    @Query("SELECT hi.date, hi.campaignId, COUNT(hi) FROM HistoryImpressionsModel hi WHERE hi.advertiserId = :advertiserId GROUP BY hi.date, hi.campaignId")
    List<Object[]> countDailyImpressionsByAdvertiser(@Param("advertiserId") UUID advertiserId);
}

