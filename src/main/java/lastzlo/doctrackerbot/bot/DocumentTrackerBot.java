package lastzlo.doctrackerbot.bot;

import lastzlo.doctrackerbot.model.AppUser;
import lastzlo.doctrackerbot.service.AppUserService;
import lastzlo.doctrackerbot.service.UserDocumentService;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

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

	@Scheduled(cron = "${bot.synchroniseCron}", zone = "${bot.timezone}")
	private void synchroniseAllDocuments() {
		log.info("Synchronization of all documents started");
		userDocumentService.synchroniseAllDocuments(this);
		log.info("Synchronization of all documents finished");
	}

	public void sendMDMessage(String chatId, String text) {
		SendMessage message = new SendMessage();
		message.enableMarkdown(true);
		message.setChatId(chatId);
		message.setText(text);

		try {
			execute(message);
		} catch (TelegramApiException e) {
			TelegramApiRequestException requestException = (TelegramApiRequestException) e;
			if (requestException.getErrorCode() == 403) {
				log.warn("Bot was blocked by the user with chatId = " + chatId);
				stopBotForUserWhoBlockedIt(chatId);
			} else {
				e.printStackTrace();
			}
		}
	}

	public void sendMessage(String chatId, String text) {
		SendMessage message = SendMessage.builder()
				.chatId(chatId)
				.text(text)
				.build();
		try {
			execute(message);
		} catch (TelegramApiException e) {
			TelegramApiRequestException requestException = (TelegramApiRequestException) e;
			if (requestException.getErrorCode() == 403) {
				log.warn("Bot was blocked by the user with chatId = " + chatId);
				stopBotForUserWhoBlockedIt(chatId);
			} else {
				e.printStackTrace();
			}
		}
	}

	private void stopBotForUserWhoBlockedIt(String chatId) {
		AppUser user = userService.findByChatId(Long.valueOf(chatId));
		user.setState(BotState.STOPPED);
		userService.saveUser(user);
		userDocumentService.deleteAllDocumentsFromUser(user);
		log.warn("Bot stopped for user with chatId = " + chatId);
	}

}
