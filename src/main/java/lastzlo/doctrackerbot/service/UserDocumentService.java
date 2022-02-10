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

	/*
	* the number of documents that will be displayed in one message
	* */
	@Value("${bot.limitDocumentsAtOneMessage}")
	private Long limitDocumentsAtOneMessage;

	/*
	* default value of count of synchronizations
	* without changing the count documents count
	* */
	@Value("#{new Integer('${bot.limitDocumentSyncsWithoutUpdate}')}")
	private Integer defoultCountOfSyncsWithoutUpdate;

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
			document = documentService.saveDocument(
					new Document(caseNumber));
			return addDocumentToUser(document, user);
		} else {
			return addExistsDocumentToUser(document, user);
		}
	}

	private boolean addDocumentToUser(Document document, AppUser user) {
		userDocumentsRepo.save(new UserDocument(user, document));
		return true;
	}

	private boolean addExistsDocumentToUser(Document document, AppUser user) {
		UserDocument userDocument = userDocumentsRepo.findByAppUser_IdAndDocument_Id(user.getId(), document.getId());
		if (userDocument == null) {
			document = documentService.resetDocumentCountOfSyncs(document);
			return addDocumentToUser(document, user);
		} else {
			return false;
		}
	}

	public boolean deleteDocumentFromUser(String caseNumber, AppUser user) {
		Document document = documentService.findByCaseNumber(caseNumber);
		if (document == null) return false;

		UserDocument userDocument = userDocumentsRepo.findByAppUser_IdAndDocument_Id(user.getId(), document.getId());
		if (userDocument == null) return false;
		else {
			// delete association
			userDocumentsRepo.delete(userDocument);

			// delete document
			deleteDocumentIfThereAreNoAssociatedUsers(document);

			return true;
		}
	}

	private void deleteDocumentIfThereAreNoAssociatedUsers(Document document) {
		int size = userDocumentsRepo.findAllByDocument_Id(document.getId()).size();
		if (size == 0) {
			documentService.deleteDocument(document);
		}
	}

	public String getMessageOfListUserDocuments(AppUser user) {
		String documents = userDocumentsRepo.findAllByAppUser_Id(user.getId())
				.stream()
				.map(UserDocument::getDocument)
				.map(Document::getCaseNumber)
				.collect(Collectors.joining("\n"));

		return "List of documents:\n" + documents;
	}

	public Document getDocumentByUserAndDocumentCaseNumber(AppUser user, String input) {
		Document document = documentService.findByCaseNumber(input);
		if (document == null) return null;

		UserDocument userDocument = userDocumentsRepo.findByAppUser_IdAndDocument_Id(user.getId(), document.getId());
		if (userDocument == null) return null;
		else return document;
	}

	public void synchronizeUserDocument(Document document, BotContext context) {
		List<StateRegisterDocument> stateRegisterDocuments = stateRegisterService.getDocumentsByCaseNumber(document.getCaseNumber());

		AppUser user = context.getUser();
		DocumentTrackerBot bot = context.getBot();
		int countOfDocuments = stateRegisterDocuments.size();

		if (countOfDocuments == 0) {
			String message = """
					No documents found with case number *%s*
					If I find it in the future I will notify you"""
					.formatted(document.getCaseNumber());
			bot.sendMDMessage(
					String.valueOf(user.getChatId()),
					message
			);
		} else if(countOfDocuments == document.getCountOfDocumentsFoundLastTime()) {
			notifyUserAboutActualDocuments(bot, user, document, stateRegisterDocuments);
		} else {
			List<AppUser> users = getUsersByDocument(document);
			notifyAllUsersAboutUpdateDocument(bot, users, document, stateRegisterDocuments);
			document = documentService.updateDocumentCountOfDocuments(document, countOfDocuments);
		}

		documentService.resetDocumentCountOfSyncs(document);
	}

	public void synchronizeAllUserDocuments(BotContext context) {
	}

	private void notifyUserAboutActualDocuments(
			DocumentTrackerBot bot,
			AppUser user,
			Document document,
			List<StateRegisterDocument> stateRegisterDocuments) {

		long countElementsToSkip = getCountElementsToSkip(stateRegisterDocuments);
		String documentsToString = stateRegisterDocuments.stream()
				.sorted(StateRegisterDocument::compareTo)
				.skip(countElementsToSkip)
				.map(StateRegisterService.DocumentAdapter::toMarkDown)
				.collect(Collectors.joining("\n------------\n"));

		String message = ("""
		Found %d document(s),
		with this case number: *%s*
		Last added document(s):
		%s""").formatted(
				stateRegisterDocuments.size(),
				document.getCaseNumber(),
				documentsToString
		);

		bot.sendMDMessage(
				String.valueOf(user.getChatId()),
				message
		);
	}


	private void notifyAllUsersAboutUpdateDocument(DocumentTrackerBot bot, List<AppUser> users, Document document, List<StateRegisterDocument> stateRegisterDocuments) {
		String text = getMessageWhenCountChanged(stateRegisterDocuments, document);

		log.info(String.format("Found %d users by document with case number = %s",
				users.size(), document.getCaseNumber()));

		users.forEach(user -> {
			bot.sendMDMessage(
					String.valueOf(user.getChatId()),
					text
			);
		});
	}

	public List<AppUser> getUsersByDocument(Document document) {
		return userDocumentsRepo
				.findAllByDocument_Id(document.getId())
				.stream()
				.map(UserDocument::getAppUser)
				.collect(Collectors.toList());
	}

	private String getMessageWhenCountChanged(List<StateRegisterDocument> stateRegisterDocuments, Document document) {
		List<StateRegisterDocument> newDocuments = stateRegisterDocuments.stream()
				.sorted(StateRegisterDocument::compareTo)
				.skip(document.getCountOfDocumentsFoundLastTime())
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
	}

	/*
	* returns the number of elements to skip
	* */
	private long getCountElementsToSkip(List<StateRegisterDocument> stateRegisterDocuments) {
		return (stateRegisterDocuments.size() <= limitDocumentsAtOneMessage)
					? 0l
					: (stateRegisterDocuments.size() - limitDocumentsAtOneMessage);	// skip all elements except limited quantity
	}

	public void synchroniseAllDocuments(DocumentTrackerBot bot) {
		List<Document> documents = documentService.getAll();
		log.info(String.format("Found %d documents for synchronise", documents.size()));

		documents.forEach(document -> {
			List<StateRegisterDocument> stateRegisterDocuments = stateRegisterService.getDocumentsByCaseNumber(document.getCaseNumber());

			if (stateRegisterDocuments.size() == document.getCountOfDocumentsFoundLastTime()) {
				checkDocumentCountOfSyncsByCaseNumber(bot, document);
			} else {
				List<AppUser> users = getUsersByDocument(document);
				notifyAllUsersAboutUpdateDocument(bot, users, document, stateRegisterDocuments);
				document = documentService.updateDocumentCountOfDocuments(document, stateRegisterDocuments.size());
				documentService.resetDocumentCountOfSyncs(document);
			}

		});

	}

	private void checkDocumentCountOfSyncsByCaseNumber(DocumentTrackerBot bot, Document document) {
		int actualCount = document.getCountOfSyncsByCaseNumber();
		int newCountOfSyncs = actualCount + 1;
		if (newCountOfSyncs >= defoultCountOfSyncsWithoutUpdate) {

			//delete document
			List<UserDocument> userDocuments = userDocumentsRepo.findAllByDocument_Id(document.getId());
			userDocumentsRepo.deleteAll(userDocuments);
			documentService.deleteDocument(document);

			//notify users to delete a document
			List<AppUser> users = userDocuments.stream().map(UserDocument::getAppUser).collect(Collectors.toList());
			notifyUsersToDeleteDocument(bot, users, document);
		} else {
			documentService.updateDocumentCountOfSyncs(document, newCountOfSyncs);
		}
	}

	private void notifyUsersToDeleteDocument(DocumentTrackerBot bot, List<AppUser> users, Document document) {
		String message = """
			Documents on case number *%s* are no longer tracked!
			It because there were no changes after %d synchronizations
			If you want to continue tracking, add it again with /add"""
				.formatted(
						document.getCaseNumber(),
						defoultCountOfSyncsWithoutUpdate
				);

		users.stream()
				.map(AppUser::getChatId)
				.forEach(chatId -> bot.sendMDMessage(
						String.valueOf(chatId),
						message
				));
	}

	public List<Document> getDocumentsByUser(AppUser user) {
		return userDocumentsRepo.findAllByAppUser_Id(user.getId())
				.stream()
				.map(UserDocument::getDocument)
				.collect(Collectors.toList());
	}

	public void deleteAllDocumentsFromUser(AppUser user) {
		log.info("Deleting all documents from user");
		List<UserDocument> userDocuments = userDocumentsRepo.findAllByAppUser_Id(user.getId());
		userDocumentsRepo.deleteAll(userDocuments);

		// try to delete all user documents
		userDocuments.stream()
				.map(UserDocument::getDocument)
				.forEach(this::deleteDocumentIfThereAreNoAssociatedUsers);
	}
}
