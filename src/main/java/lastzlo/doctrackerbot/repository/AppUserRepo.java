package lastzlo.doctrackerbot.repository;

import lastzlo.doctrackerbot.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserRepo extends JpaRepository<AppUser, Long> {

	AppUser findByChatId(Long chatId);

}
