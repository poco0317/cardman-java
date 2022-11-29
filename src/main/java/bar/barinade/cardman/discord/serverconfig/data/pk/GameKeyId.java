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
public class GameKeyId implements Serializable {
	private static final long serialVersionUID = 1L;
	
	// OOPS WRONG NAME THIS IS A KEY STRING, NOT A CHANNEL
	@Column(name = "channel", nullable = false)
	private String key;
	
	@Column(name = "platform", nullable = false)
	private String platform;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "guild_id", nullable = false)
	private ServerConfiguration guild;

	public GameKeyId() {}

	public GameKeyId(String key, String platform, ServerConfiguration guild) {
		this.key = key;
		this.platform = platform;
		this.guild = guild;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public ServerConfiguration getGuild() {
		return guild;
	}

	public void setGuild(ServerConfiguration guild) {
		this.guild = guild;
	}

	@Override
	public int hashCode() {
		return Objects.hash(guild, key, platform);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GameKeyId other = (GameKeyId) obj;
		return Objects.equals(guild, other.guild) && Objects.equals(key, other.key)
				&& Objects.equals(platform, other.platform);
	}
	
}
