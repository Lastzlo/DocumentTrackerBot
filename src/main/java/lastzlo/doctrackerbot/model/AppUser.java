package lastzlo.doctrackerbot.model;

import lastzlo.doctrackerbot.bot.BotState;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Objects;
import java.util.Set;

@Entity
public class AppUser {

	@Getter
	@Setter
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "appuser_id", nullable = false)
	private Long id;

	@Getter
	@Setter
	@Column(unique=true)
	private Long chatId;

	@Getter
	@Setter
	@Enumerated(EnumType.ORDINAL)
	private BotState state;

	public AppUser() {
	}

	@Getter
	@Setter
	@OneToMany(mappedBy = "appUser")
	private Set<UserDocument> userDocuments;

	public AppUser(Long chatId, BotState state) {
		this.chatId = chatId;
		this.state = state;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AppUser appUser = (AppUser) o;
		return id.equals(appUser.id) && chatId.equals(appUser.chatId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, chatId);
	}
}
