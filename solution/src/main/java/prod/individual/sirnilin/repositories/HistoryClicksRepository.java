package prod.individual.sirnilin.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prod.individual.sirnilin.models.HistoryClicksModel;

@Repository
public interface HistoryClicksRepository extends JpaRepository<HistoryClicksModel, Long> {
}
