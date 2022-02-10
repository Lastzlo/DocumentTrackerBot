package lastzlo.doctrackerbot.service;

import lastzlo.doctrackerbot.model.StateRegisterDocument;
import lastzlo.doctrackerbot.repository.StateRegisterRepository;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
public class StateRegisterService {

	@Value("${stateRegister.reviewUrl}")
	private String reviewUrl;

	private final StateRegisterRepository repository;

	@Autowired
	public StateRegisterService(StateRegisterRepository repository) {
		this.repository = repository;
	}

	private String getHtmlPageByCaseNumber(String caseNumber) {
		String htmlPage = repository.loadPageByCaseNumber(caseNumber);
		log.info("HTML page by case number " + caseNumber + " was loaded");
		return htmlPage;
	}


	public List<StateRegisterDocument> getDocumentsByCaseNumber(String caseNumber) {
		String htmlPage = getHtmlPageByCaseNumber(caseNumber);
		List<StateRegisterDocument> documents = Jsoup.parse(htmlPage)
				.getElementsByAttributeValueContaining("class", "CaseNumber tr")
				.stream()
				.filter(el -> el.html().equals(caseNumber))
				.map(Element::parent)
				.map(el -> DocumentAdapter.fromElement(el, reviewUrl))
				.sorted(StateRegisterDocument::compareTo)
				.collect(Collectors.toList());

		log.info(String.format("Parsed %d documents with case number = %s",
				documents.size(), caseNumber));

		return documents;
	}

	public static class DocumentAdapter {

		private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

		private static StateRegisterDocument fromElement(Element el, String stateRegisterURL) {
			String documentNumber =
					el.getElementsByAttributeValueContaining("class", "doc_text2")
							.get(0)
							.html();

			String documentLink = stateRegisterURL + documentNumber;

			String vrType =
					el.getElementsByAttributeValueContaining("class", "VRType tr")
							.get(0)
							.html();

			String regDateRaw =
					el.getElementsByAttributeValueContaining("class", "RegDate tr")
							.get(0)
							.html();
			LocalDate regDate = LocalDate.parse(regDateRaw, formatter);

			String lawDateRaw =
					el.getElementsByAttributeValueContaining("class", "LawDate tr")
							.get(0)
							.html();
			LocalDate lawDate = LocalDate.parse(lawDateRaw, formatter);

			String csType =
					el.getElementsByAttributeValueContaining("class", "CSType tr")
							.get(0)
							.html();

			String caseNumber =
					el.getElementsByAttributeValueContaining("class", "CaseNumber tr")
							.get(0)
							.html();

			String courtName =
					el.getElementsByAttributeValueContaining("class", "CourtName tr")
							.get(0)
							.html();

			String chairmenName =
					el.getElementsByAttributeValueContaining("class", "ChairmenName tr")
							.get(0)
							.html();

			return StateRegisterDocument.builder()
					.documentNumber(documentNumber)
					.documentLink(documentLink)
					.vrType(vrType)
					.regDate(regDate)
					.lawDate(lawDate)
					.csType(csType)
					.caseNumber(caseNumber)
					.courtName(courtName)
					.chairmenName(chairmenName)
					.build();
		}

		public static String toMarkDown(StateRegisterDocument document) {
			return ("""
				№ рішення: [%s](%s)
				Форма судового рішення:
				*%s*
				Дата ухвалення рішення:
				*%s*
				Дата набрання законної сили:
				*%s*
				Форма судочинства: %s
				№ судової справи: *%s*
				Назва суду:
				*%s*
				Суддя: *%s*"""
			)
					.formatted(
							document.getDocumentNumber(),
							document.getDocumentLink(),
							document.getVrType(),
							document.getRegDate().format(formatter),
							document.getLawDate().format(formatter),
							document.getCsType(),
							document.getCaseNumber(),
							document.getCourtName(),
							document.getChairmenName());


		}

	}

}
