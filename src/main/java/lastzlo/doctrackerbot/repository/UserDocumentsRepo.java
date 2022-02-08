package lastzlo.doctrackerbot.repository;

import lastzlo.doctrackerbot.model.UserDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDocumentsRepo extends JpaRepository<UserDocument, Long> {


	List<UserDocument> findAllByAppUser_Id(Long userId);

	List<UserDocument> findAllByDocument_Id(Long documentId);

	UserDocument findByAppUser_IdAndDocument_Id(Long userId, Long documentId);
//
//	List<UserDocument> findAllByDocument_Id(Long id);

}
