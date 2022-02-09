package lastzlo.doctrackerbot.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Builder
public class StateRegisterDocument implements Comparable<StateRegisterDocument> {

	@Getter
	private String documentNumber;
	@Getter
	private String documentLink;
	@Getter
	private String vrType;
	@Getter
	private LocalDate regDate;
	@Getter
	private LocalDate lawDate;
	@Getter
	private String csType;
	@Getter
	private String caseNumber;
	@Getter
	private String courtName;
	@Getter
	private String chairmenName;

	@Override
	public int compareTo(StateRegisterDocument document) {
		Integer integer1 = Integer.valueOf(this.getDocumentNumber());
		Integer integer2 = Integer.valueOf(document.getDocumentNumber());

		return integer1.compareTo(integer2);
	}

}
