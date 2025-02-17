package prod.individual.sirnilin.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prod.individual.sirnilin.models.TimeModel;

@Repository
public interface TimeRepository extends JpaRepository<TimeModel, Long> {
}
