package bar.barinade.cardman.discord.serverconfig.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import bar.barinade.cardman.discord.serverconfig.data.GameKey;
import bar.barinade.cardman.discord.serverconfig.data.pk.GameKeyId;

@Repository
public interface GameKeyRepo extends JpaRepository<GameKey, GameKeyId> {
	
	List<GameKey> findByIdGuildId(Long id);
	Long deleteByIdGuildId(Long id);
	List<GameKey> findByExtraId(String uuid);

}
