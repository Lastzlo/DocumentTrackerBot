package lastzlo.doctrackerbot.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Objects;
import java.util.Set;

@Entity
public class Document {

	@Getter
	@Setter
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "document_id", nullable = false)
	private Long id;

	@Getter
	@Setter
	@Column(unique=true)
	private String caseNumber;

	@Getter
	@Setter
	private Integer countOfDocumentsFoundLastTime = 0;

	@Getter
	@Setter
	private Integer countOfSyncsByCaseNumber = 0;

	@Getter
	@Setter
	@OneToMany(mappedBy = "document")
	private Set<UserDocument> userDocuments;

	public Document() {
	}

	public Document(String caseNumber) {
		this.caseNumber = caseNumber;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Document document = (Document) o;
		return id.equals(document.id) && caseNumber.equals(document.caseNumber);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, caseNumber);
	}
}
