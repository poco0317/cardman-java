package bar.barinade.cardman.discord.serverconfig.data;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import bar.barinade.cardman.discord.serverconfig.data.pk.ClaimantId;

@Entity
@Table(name = "claimants")
public class Claimant {

	@EmbeddedId
	private ClaimantId id;
	
	@Column(name = "claims", nullable = true)
	private Integer claims;

	public ClaimantId getId() {
		return id;
	}

	public void setId(ClaimantId id) {
		this.id = id;
	}

	public Integer getClaims() {
		return claims;
	}

	public void setClaims(Integer claims) {
		this.claims = claims;
	}
	
}
