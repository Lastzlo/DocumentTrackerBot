package lastzlo.doctrackerbot.service;

import lastzlo.doctrackerbot.model.AppUser;
import lastzlo.doctrackerbot.model.Document;
import lastzlo.doctrackerbot.model.UserDocument;
import lastzlo.doctrackerbot.repository.UserDocumentsRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class UserDocumentService {

	private final UserDocumentsRepo userDocumentsRepo;
	private final DocumentService documentService;

	@Autowired
	public UserDocumentService(UserDocumentsRepo userDocumentsRepo, DocumentService documentService) {
		this.userDocumentsRepo = userDocumentsRepo;
		this.documentService = documentService;
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

			if (hasUsersWithDocument(document)) documentService.deleteDocument(document);

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

	public boolean hasUsersWithDocument(Document document) {
		int size = userDocumentsRepo.findAllByDocument_Id(document.getId()).size();
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
}
