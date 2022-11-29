package bar.barinade.cardman.discord.serverconfig.data;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import bar.barinade.cardman.discord.serverconfig.data.pk.GameKeyId;

@Entity
@Table(name = "game_keys")
public class GameKey {

	@EmbeddedId
	private GameKeyId id;
	
	@Column(name = "name", nullable = false)
	private String name;
	
	@Column(name = "claimant_user_id", nullable = true)
	private Long claimantUserId;
	
	@Column(name = "extra_id", nullable = false)
	private String extraId;
	
	@Column(name = "storelink", nullable = true)
	private String storelink;

	public String getStorelink() {
		return storelink;
	}

	public void setStorelink(String storelink) {
		this.storelink = storelink;
	}

	public String getExtraId() {
		return extraId;
	}

	public void setExtraId(String extraId) {
		this.extraId = extraId;
	}

	public Long getClaimantUserId() {
		return claimantUserId;
	}

	public void setClaimantUserId(Long claimantUserId) {
		this.claimantUserId = claimantUserId;
	}

	public GameKeyId getId() {
		return id;
	}

	public void setId(GameKeyId id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
