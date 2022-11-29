package bar.barinade.cardman.discord.serverconfig.data.pk;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import bar.barinade.cardman.discord.serverconfig.data.ServerConfiguration;

@Embeddable
public class ClaimantId implements Serializable {
	private static final long serialVersionUID = 1L;

	@Column(name = "id", nullable = false)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "guild_id", nullable = false)
	private ServerConfiguration guild;

	public ClaimantId() {}
	
	public ClaimantId(Long id, ServerConfiguration guild) {
		this.id = id;
		this.guild = guild;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ServerConfiguration getGuild() {
		return guild;
	}

	public void setGuild(ServerConfiguration guild) {
		this.guild = guild;
	}

	@Override
	public int hashCode() {
		return Objects.hash(guild, id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClaimantId other = (ClaimantId) obj;
		return Objects.equals(guild, other.guild) && Objects.equals(id, other.id);
	}
	
	
}
