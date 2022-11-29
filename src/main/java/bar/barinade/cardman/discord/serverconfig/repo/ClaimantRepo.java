package bar.barinade.cardman.discord.serverconfig.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import bar.barinade.cardman.discord.serverconfig.data.Claimant;
import bar.barinade.cardman.discord.serverconfig.data.pk.ClaimantId;

@Repository
public interface ClaimantRepo extends JpaRepository<Claimant, ClaimantId> {

	List<Claimant> findByIdGuildId(Long id);
	List<Claimant> findByIdGuildIdAndIdId(Long id, Long claimantId);
	Long deleteByIdGuildId(Long id);

}
