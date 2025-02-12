package prod.individual.sirnilin.repositories;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prod.individual.sirnilin.models.AdvertisersModel;

import java.util.UUID;

@Repository
public interface AdvertisersRepository extends JpaRepository<AdvertisersModel, Long> {
   AdvertisersModel findByAdvertiserId(@NotNull UUID advertiserId);
}
