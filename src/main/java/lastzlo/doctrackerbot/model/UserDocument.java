package lastzlo.doctrackerbot.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
public class UserDocument {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", nullable = false)
	@Getter
	@Setter
	private Long id;

	@ManyToOne
	@JoinColumn
	@Getter
	@Setter
	private AppUser appUser;

	@ManyToOne
	@JoinColumn
	@Getter
	@Setter
	private Document document;

	public UserDocument() {}
	public UserDocument(AppUser user, Document document) {
		this.appUser = user;
		this.document = document;
	}
}
