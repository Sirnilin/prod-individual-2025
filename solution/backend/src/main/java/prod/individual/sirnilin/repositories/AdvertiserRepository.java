package prod.individual.sirnilin.repositories;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prod.individual.sirnilin.models.AdvertiserModel;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdvertiserRepository extends JpaRepository<AdvertiserModel, Long> {
   Optional<AdvertiserModel> findByAdvertiserId(@NotNull UUID advertiserId);
}
