package lastzlo.doctrackerbot.service;

import lastzlo.doctrackerbot.model.AppUser;
import lastzlo.doctrackerbot.repository.AppUserRepo;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Log4j2
@Service
public class AppUserService {

	private final AppUserRepo appUserRepo;

	@Autowired
	public AppUserService(AppUserRepo appUserRepo) {
		this.appUserRepo = appUserRepo;
	}

	public AppUser findByChatId(Long chatId) {
		return appUserRepo.findByChatId(chatId);
	}

	public AppUser saveUser(AppUser user) {
		return appUserRepo.save(user);
	}

}
