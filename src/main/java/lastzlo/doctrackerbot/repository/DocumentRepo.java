package lastzlo.doctrackerbot.repository;

import lastzlo.doctrackerbot.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepo extends JpaRepository<Document, Long> {
	Document findByCaseNumber(String caseNumber);


}
