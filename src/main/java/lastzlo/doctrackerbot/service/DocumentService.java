package lastzlo.doctrackerbot.service;

import lastzlo.doctrackerbot.model.Document;
import lastzlo.doctrackerbot.repository.DocumentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

	public void deleteDocument(Document document) {
		documentRepo.delete(document);
	}

	public List<Document> getAll() {
		return documentRepo.findAll();
	}
}
