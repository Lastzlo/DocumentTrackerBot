package lastzlo.doctrackerbot.service;

import lastzlo.doctrackerbot.bot.BotContext;
import lastzlo.doctrackerbot.bot.DocumentTrackerBot;
import lastzlo.doctrackerbot.model.AppUser;
import lastzlo.doctrackerbot.model.Document;
import lastzlo.doctrackerbot.model.StateRegisterDocument;
import lastzlo.doctrackerbot.model.UserDocument;
import lastzlo.doctrackerbot.repository.UserDocumentsRepo;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
public class UserDocumentService {

	@Value("${bot.limitDocumentsAtOneMessage}")
	private Long limitDocumentsAtOneMessage;

	private final UserDocumentsRepo userDocumentsRepo;
	private final DocumentService documentService;
	private final StateRegisterService stateRegisterService;

	@Autowired
	public UserDocumentService(UserDocumentsRepo userDocumentsRepo, DocumentService documentService, StateRegisterService stateRegisterService) {
		this.userDocumentsRepo = userDocumentsRepo;
		this.documentService = documentService;
		this.stateRegisterService = stateRegisterService;
	}

	public boolean addDocumentToUser(String caseNumber, AppUser user) {
		Document document = documentService.findByCaseNumber(caseNumber);

		if(document == null) {
			document = documentService.saveDocument(new Document(caseNumber));
			return addDocumentToUser(document, user);
		} else {
			return addExistsDocumentToUser(document, user);
		}
	}

	public boolean addDocumentToUser(Document document, AppUser user) {
		userDocumentsRepo.save(new UserDocument(user, document));
		return true;
	}

	public boolean addExistsDocumentToUser(Document document, AppUser user) {
		UserDocument userDocument = userDocumentsRepo.findByAppUser_IdAndDocument_Id(user.getId(), document.getId());
		if (userDocument == null) {
			return addDocumentToUser(document, user);
		} else {
			return false;
		}
	}

	public boolean deleteDocumentFromUser(String caseNumber, AppUser user) {
		Document document = documentService.findByCaseNumber(caseNumber);

		if (document == null) return false;
		else return deleteExistsDocumentFromUser(document, user);
	}

	public boolean deleteExistsDocumentFromUser(Document document, AppUser user) {
		UserDocument userDocument = userDocumentsRepo.findByAppUser_IdAndDocument_Id(user.getId(), document.getId());

		if (userDocument == null) {
			return false;
		} else {
			userDocumentsRepo.delete(userDocument);

			if (hasUsersWithDocument(document.getId())) documentService.deleteDocument(document);

			return true;
		}

	}

//	public boolean deleteDocumentFromUser(Document document, AppUser user) {
//		UserDocument userDocument = userDocumentsRepo.findByAppUser_IdAndDocument_Id(user.getId(), document.getId());
//
//		if (userDocument == null) {
//			return false;
//		} else {
//			userDocumentsRepo.delete(userDocument);
//			return true;
//		}
//	}

	public boolean hasUsersWithDocument(Long documentId) {
		int size = userDocumentsRepo.findAllByDocument_Id(documentId).size();
		return size != 0;
	}

//	public List<AppUser> getUsersByDocument(Document document) {
//		List<UserDocument> userDocumentList = userDocumentsRepo.findAllByDocument_Id(document.getId());
//
//		return userDocumentList.stream()
//				.map(userDocument -> userDocument.getAppUser())
//				.collect(Collectors.toList());
//
//
//	}

	public String getDocumentsByUserInOneString(AppUser user) {
		return userDocumentsRepo.findAllByAppUser_Id(user.getId())
				.stream()
				.map(UserDocument::getDocument)
				.map(Document::getCaseNumber)
				.collect(Collectors.joining("\n"));
	}

	public Document getDocumentByUserAndDocumnetCaseNumber(AppUser user, String input) {
		Document document = documentService.findByCaseNumber(input);
		if (document == null) return null;

		UserDocument userDocument = userDocumentsRepo.findByAppUser_IdAndDocument_Id(user.getId(), document.getId());
		if (userDocument == null) return null;
		else return document;
	}

	public void synchronizeDocument(Document document, BotContext context) {
		List<StateRegisterDocument> stateRegisterDocuments = stateRegisterService.getDocumentsByCaseNumber(document.getCaseNumber());

		AppUser user = context.getUser();
		DocumentTrackerBot bot = context.getBot();

		if (stateRegisterDocuments.size() == 0) {
			String text = "Not found documents with this case number: " + document.getCaseNumber();
			bot.sendMarkDownMessage(
					String.valueOf(user.getChatId()),
					text
			);
		} else {

			if(stateRegisterDocuments.size() == document.getLastCount()) {
				notifyUserAboutActualDocuments(document, context, stateRegisterDocuments);
			} else {
				notifyAllUsersAboutUpdateDocument(stateRegisterDocuments, document, bot);
			}
		}
	}

	private void notifyAllUsersAboutUpdateDocument(List<StateRegisterDocument> stateRegisterDocuments, Document document, DocumentTrackerBot bot) {

		String text = getMessageWhenCountChanged(stateRegisterDocuments, document);

		List<AppUser> appUsers = getUsersByDocument(document);
		log.info(String.format("Found %d users by document with case number = %s",
				appUsers.size(), document.getCaseNumber()));

		appUsers.forEach(user -> {
			bot.sendMarkDownMessage(
					String.valueOf(user.getChatId()),
					text
			);
		});

		//update document
		document.setLastCount(stateRegisterDocuments.size());
		documentService.saveDocument(document);
	}

	public List<AppUser> getUsersByDocument(Document document) {
		return userDocumentsRepo
				.findAllByDocument_Id(document.getId())
				.stream()
				.map(UserDocument::getAppUser)
				.collect(Collectors.toList());
	}

	private void notifyUserAboutActualDocuments(Document document, BotContext context, List<StateRegisterDocument> stateRegisterDocuments) {
//		StringBuilder builder = new StringBuilder().append("Found ")
//				.append(stateRegisterDocuments.size())
//				.append(" document(s)\n")
//				.append("with this case number: ")
//				.append(document.getCaseNumber())
//				.append("\nLast added document(s):\n");

		long countElementsToSkip = getCountElementsToSkip(stateRegisterDocuments);
		String documentsToString = stateRegisterDocuments.stream()
				.sorted(StateRegisterDocument::compareTo)
				.skip(countElementsToSkip)
				.map(StateRegisterService.DocumentAdapter::toMarkDown)
				.collect(Collectors.joining("\n------------\n"));

//		String message = builder
//				.append("------------\n")
//				.append(documentsToString)
//				.toString();

		String message = ("""
		Found %d document(s),
		with this case number: *%s*
		Last added document(s):
		%s""").formatted(
				stateRegisterDocuments.size(),
				document.getCaseNumber(),
				documentsToString
		);

		context.getBot().sendMarkDownMessage(
				String.valueOf(context.getUser().getChatId()),
				message
		);
	}


//	private String handleStateRegisterDocuments(List<StateRegisterDocument> stateRegisterDocuments, Document document) {
//		if (stateRegisterDocuments.size() == document.getLastCount()) {
//			return handleWhenCountNotChanged(stateRegisterDocuments);
//		} else {
//			return handleWhenCountChanged(stateRegisterDocuments, document);
//		}
//	}

	private String getMessageWhenCountChanged(List<StateRegisterDocument> stateRegisterDocuments, Document document) {
		List<StateRegisterDocument> newDocuments = stateRegisterDocuments.stream()
				.sorted(StateRegisterDocument::compareTo)
				.skip(document.getLastCount())
				.collect(Collectors.toList());

		long skipElements = getCountElementsToSkip(newDocuments);
		String documentsToString = newDocuments.stream()
				.skip(skipElements)
				.map(StateRegisterService.DocumentAdapter::toMarkDown)
				.collect(Collectors.joining("\n------------\n"));

		return  ("""
		Found *%d* *NEW* document(s),
		with this case number: *%s*
		Last added document(s):
		%s""").formatted(
			newDocuments.size(),
			document.getCaseNumber(),
			documentsToString
		);

//		StringBuilder builder = new StringBuilder()
//				.append("Found ")
//				.append(newDocuments.size())
//				.append(" **NEW** document(s)\n")
//				.append("with this case number: **")
//				.append(document.getCaseNumber())
//				.append("**\nLast added document(s):\n");

//		long skipElements = getCountElementsToSkip(newDocuments);
//		String documentsToString = newDocuments.stream()
//				.skip(skipElements)
//				.map(StateRegisterService.DocumentAdapter::toMarkDown)
//				.collect(Collectors.joining("\n------------\n"));

//		return builder
//				.append("------------\n")
//				.append(documentsToString)
//				.toString();
	}

	private long getCountElementsToSkip(List<StateRegisterDocument> stateRegisterDocuments) {
		long skipElements;
		if (stateRegisterDocuments.size() <= limitDocumentsAtOneMessage) {
			skipElements = 0l;
		} else {
			skipElements = stateRegisterDocuments.size() - limitDocumentsAtOneMessage;
		}
		return skipElements;
	}

//	private String handleWhenCountNotChanged(List<StateRegisterDocument> stateRegisterDocuments) {
//		StringBuilder builder = new StringBuilder().append("Not found new documents, last");
//
//		if (stateRegisterDocuments.size() == 1) {
//			builder.append(" document:\n");
//		} else {
//			builder.append(" documents:\n");
//		}
//
//		long skipElements = getCountElementsToSkip(stateRegisterDocuments);
//
//		String documentsToString = stateRegisterDocuments.stream()
//				.sorted(StateRegisterDocument::compareTo)
//				.skip(skipElements)
//				.map(StateRegisterService.DocumentAdapter::toMarkDown)
//				.collect(Collectors.joining("\n------------\n"));
//		String message = builder.append(documentsToString).toString();
//		return message;
//	}


}
