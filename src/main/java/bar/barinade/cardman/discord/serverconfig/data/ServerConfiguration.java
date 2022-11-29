package bar.barinade.cardman.discord.serverconfig.data;


import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "server_configs")
public class ServerConfiguration {
	
	@Id
	@Column(name = "guild_id")
	private Long id;
	
	@Column(name = "channel_id", nullable = true)
	private Long channelId;
	
	@Column(name = "audit_id", nullable = true)
	private Long auditId;
	
	@Column(name = "allowed_claims", nullable = true)
	private Long allowedClaims;
	
	@OneToMany(mappedBy = "id.guild")
	private Set<GameKey> gameKeys;
	
	@OneToMany(mappedBy = "id.guild")
	private Set<Claimant> claimants;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getChannelId() {
		return channelId;
	}

	public void setChannelId(Long channelId) {
		this.channelId = channelId;
	}

	public Set<GameKey> getGameKeys() {
		return gameKeys;
	}

	public void setGameKeys(Set<GameKey> keys) {
		this.gameKeys = keys;
	}
	
	public Long getAuditId() {
		return auditId;
	}

	public void setAuditId(Long auditId) {
		this.auditId = auditId;
	}

	public Set<Claimant> getClaimants() {
		return claimants;
	}

	public void setClaimants(Set<Claimant> claimants) {
		this.claimants = claimants;
	}

	@Override
	public int hashCode() {
		return Objects.hash(channelId, id, gameKeys);
	}

	public Long getAllowedClaims() {
		return allowedClaims;
	}

	public void setAllowedClaims(Long allowedClaims) {
		this.allowedClaims = allowedClaims;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServerConfiguration other = (ServerConfiguration) obj;
		return Objects.equals(channelId, other.channelId) && Objects.equals(id, other.id)
				&& Objects.equals(gameKeys, other.gameKeys);
	}

}
