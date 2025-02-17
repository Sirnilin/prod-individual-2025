package prod.individual.sirnilin.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import prod.individual.sirnilin.models.CampaignModel;
import prod.individual.sirnilin.models.Gender;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<CampaignModel, Long> {
    Optional<CampaignModel> findByCampaignId(UUID campaignId);

    List<CampaignModel> findByAdvertiserId(UUID advertiserId);

    @Query("SELECT c FROM CampaignModel c " +
            "WHERE c.startDate <= :currentDate AND c.endDate >= :currentDate " +
            "AND (c.targeting.gender IS NULL OR c.targeting.gender = :clientGender OR c.targeting.gender = 'ALL') " +
            "AND (c.targeting.ageFrom IS NULL OR :clientAge >= c.targeting.ageFrom) " +
            "AND (c.targeting.ageTo IS NULL OR :clientAge <= c.targeting.ageTo) " +
            "AND (c.targeting.location IS NULL OR c.targeting.location = :clientLocation)")
    List<CampaignModel> findMatchingCampaigns(@Param("currentDate") Integer currentDate,
                                              @Param("clientGender") Gender clientGender,
                                              @Param("clientAge") int clientAge,
                                              @Param("clientLocation") String clientLocation);

    List<CampaignModel> findByCampaignIdIn(List<UUID> campaignIds);

    @Modifying
    @Transactional
    @Query("UPDATE CampaignModel c SET c.countImpressions = c.countImpressions + 1 WHERE c.campaignId = :campaignId")
    void incrementImpressions(@Param("campaignId") UUID campaignId);

    @Modifying
    @Transactional
    @Query("UPDATE CampaignModel c SET c.countClicks = c.countClicks + 1 WHERE c.campaignId = :campaignId")
    void incrementClicks(@Param("campaignId") UUID campaignId);
}
