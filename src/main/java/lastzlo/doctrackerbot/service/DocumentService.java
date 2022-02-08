package lastzlo.doctrackerbot.service;

import lastzlo.doctrackerbot.model.Document;
import lastzlo.doctrackerbot.repository.DocumentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {

	private final DocumentRepo documentRepo;

	@Autowired
	public DocumentService(DocumentRepo documentRepo) {
		this.documentRepo = documentRepo;
	}

	public Document saveDocument(Document doc) {
		return documentRepo.save(doc);
	}

	public Document findByCaseNumber(String caseNumber) {
		return documentRepo.findByCaseNumber(caseNumber);
	}

//	public boolean addDocumentToUser(String caseNumber, AppUser user) {
//		Document documentFromDb = documentRepo.findByCaseNumber(caseNumber);
//		if (documentFromDb == null) {
//			documentFromDb = documentRepo.save(
//					new Document(caseNumber)
//			);
//
//			return userDocumentService.addDocumentToUser(documentFromDb, user);
//		} else {
//			return userDocumentService.addExistsDocumentToUser(documentFromDb, user);
//		}
//	}
//
//	public boolean deleteDocumentFromUser(String input, AppUser user) {
//		Document documentFromDb = documentRepo.findByCaseNumber(input);
//
//		if (documentFromDb == null) return false;
//
//		boolean isDeleteSuccess = userDocumentService.deleteDocumentFromUser(documentFromDb, user);
//		if (! isDeleteSuccess) return false;
//
//		boolean hasUsersWithDocument = userDocumentService.hasUsersWithDocument(documentFromDb);
//		if (! hasUsersWithDocument) documentRepo.delete(documentFromDb);
//
//		return true;
//
//
////		if (documentFromDb == null) {
////			return false;
////		} else {
////			boolean isDeleteSuccess = userDocumentService.deleteDocumentFromUser(documentFromDb, user);
////
////			if (isDeleteSuccess) {
////				boolean hasUsersWithDocument = userDocumentService.hasUsersWithDocument(documentFromDb);
////
////				if (! hasUsersWithDocument) {
////					documentRepo.delete(documentFromDb);
////				}
////
////				return true;
////			} else {
////				return false;
////			}
////
////		}
//
//
//	}

	public void deleteDocument(Document document) {
		documentRepo.delete(document);
	}
}
