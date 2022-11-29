package bar.barinade.cardman.discord.serverconfig.service;

import java.util.List;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import bar.barinade.cardman.discord.serverconfig.data.Claimant;
import bar.barinade.cardman.discord.serverconfig.data.pk.ClaimantId;
import bar.barinade.cardman.discord.serverconfig.repo.ClaimantRepo;

@Service
public class ClaimantService {
	
	private static final Logger m_logger = LoggerFactory.getLogger(ClaimantService.class);
	
	@Autowired
	private ClaimantRepo repo;
	
	@Autowired
	private ServerConfigService configs;
	
	@Transactional
	public boolean canClaim(Long guildId, Long userId) {
		List<Claimant> inGuild = repo.findByIdGuildIdAndIdId(guildId, userId);
		Long claimLimit = configs.getAllowedClaims(guildId);
		if (inGuild == null || inGuild.isEmpty() || (inGuild.get(0).getClaims() < claimLimit || claimLimit == 0)) {
			// no results means it can be claimed
			// or just have less than the amount of allowed claims
			return true;
		}
		return false;
	}
	
	@Transactional
	public Claimant get(Long guildId, Long userId) {
		List<Claimant> inGuild = repo.findByIdGuildIdAndIdId(guildId, userId);
		if (inGuild == null || inGuild.isEmpty()) {
			return null;
		}
		return inGuild.get(0);
	}
	
	@Transactional
	public Integer count(Long guildId, Long userId) {
		Claimant c = get(guildId, userId);
		if (c == null) {
			return 0;
		}
		return c.getClaims() == null ? 0 : c.getClaims();
	}
	
	@Transactional
	public void claim(Long guildId, Long userId) {
		if (canClaim(guildId, userId)) {
			Claimant claimant = get(guildId, userId);
			if (claimant == null) {
				claimant = new Claimant();
				ClaimantId id = new ClaimantId(userId, configs.getConfig(guildId));
				claimant.setId(id);
				claimant.setClaims(1);
				repo.save(claimant);
				m_logger.info("Added claimant {} to guild {}", userId, guildId);
			} else {
				int count = claimant.getClaims() == null ? 0 : claimant.getClaims();
				claimant.setClaims(count + 1);
				repo.save(claimant);
				m_logger.info("Iterated user {} guild {} claims to {}", userId, guildId, count + 1);
			}
		} else {
			m_logger.info("Could not add claimant {} to guild {}", userId, guildId);
		}
	}
	
	@Transactional
	public void freeClaims(Long guildId) {
		repo.deleteByIdGuildId(guildId);
		m_logger.info("Freed all claimants for guild {}", guildId);
	}
	
	@Transactional
	public void freeClaim(Long guildId, Long userId) {
		if (get(guildId, userId) != null) {
			repo.deleteById(new ClaimantId(userId, configs.getConfig(guildId)));
			m_logger.info("Freed claimant {} for guild {}", userId, guildId);
		}
	}

}
