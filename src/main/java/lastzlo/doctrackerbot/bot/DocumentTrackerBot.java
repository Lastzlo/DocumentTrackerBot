package lastzlo.doctrackerbot.bot;

import lastzlo.doctrackerbot.model.AppUser;
import lastzlo.doctrackerbot.service.AppUserService;
import lastzlo.doctrackerbot.service.UserDocumentService;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Log4j2
@Component
public class DocumentTrackerBot extends TelegramLongPollingBot {

	@Value("${bot.botName}")
	private String botUsername;

	@Value("${bot.botToken}")
	private String botToken;

	@Getter
	private final AppUserService userService;

	@Getter
	private final UserDocumentService userDocumentService;


	@Autowired
	public DocumentTrackerBot(AppUserService userService, UserDocumentService userDocumentService) {
		this.userService = userService;
		this.userDocumentService = userDocumentService;
	}

	@Override
	public String getBotUsername() {
		return botUsername;
	}

	@Override
	public String getBotToken() {
		return botToken;
	}

	@Override
	public void onUpdateReceived(Update update) {
		if (update.hasMessage() && update.getMessage().hasText()) {
			handleIncomingMessage(update.getMessage());
		}
	}

	private void handleIncomingMessage(Message message) {
		final String text = message.getText();
		final long chatId = message.getChatId();

		BotState state;

		AppUser user = userService.findByChatId(chatId);
		if (user == null) {
			state = BotState.getInitialState();

			user = userService.saveUser(
					new AppUser(chatId, BotState.getInitialState())
			);
			log.info("New user registered: " + chatId);
		} else {
			state = user.getState();
			log.info("Update received for user: " + user.getChatId() + " in state: " + state);
		}

		BotContext context = BotContext.of(this, user, text);
		state = state.nextState(context);

		user.setState(state);
		userService.saveUser(user);
	}



	public void sendMarkDownMessage(String chatId, String text) {
		SendMessage message = new SendMessage();
		message.enableMarkdown(true);
		message.setChatId(chatId);
		message.setText(text);

		try {
			execute(message);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}

}
