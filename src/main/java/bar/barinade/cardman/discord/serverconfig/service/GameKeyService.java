package bar.barinade.cardman.discord.serverconfig.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import bar.barinade.cardman.discord.BotManager;
import bar.barinade.cardman.discord.serverconfig.data.GameKey;
import bar.barinade.cardman.discord.serverconfig.data.ServerConfiguration;
import bar.barinade.cardman.discord.serverconfig.data.pk.GameKeyId;
import bar.barinade.cardman.discord.serverconfig.repo.GameKeyRepo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.interactions.components.Button;

@Service
public class GameKeyService {
	
	private static final Logger m_logger = LoggerFactory.getLogger(GameKeyService.class);
	
	ConcurrentHashMap<Long, MessageHistory> histories = new ConcurrentHashMap<>();
	
	@Autowired
	private ServerConfigService configService;
	
	@Autowired
	private ClaimantService claimants;
	
	@Autowired
	private GameKeyRepo keys;
	
	@Transactional
	public GameKey getKeyByButtonId(String uuid) {
		List<GameKey> found = keys.findByExtraId(uuid);
		if (found != null && found.size() > 0) {
			return found.get(0);
		}
		return null;
	}
	
	@Transactional
	public int countUnclaimed(Long guildId) {
		m_logger.info("Counting unclaimed keys in guild {}", guildId);
		List<GameKey> keylist = keys.findByIdGuildId(guildId);
		if (keylist != null) {
			keylist = keylist.stream().filter(e -> e.getClaimantUserId() == null || e.getClaimantUserId() == 0L).collect(Collectors.toList());
			return keylist.size();
		}
		return 0;
	}
	
	@Transactional
	public boolean newKey(Long guildId, String name, String platform, String key, String storelink) {
		m_logger.info("Making new key | GUILD {} | GAME {} | PLATFORM {}", guildId, name, platform);
		GameKey gameKey = new GameKey();
		gameKey.setClaimantUserId(null);
		gameKey.setExtraId(UUID.randomUUID().toString());
		gameKey.setName(name);
		gameKey.setStorelink(storelink);
		GameKeyId id = new GameKeyId(key, platform, configService.getConfig(guildId));
		if (keys.findById(id).orElse(null) == null) {
			gameKey.setId(id);
			keys.save(gameKey);
			
			m_logger.info("Made new key for guild {}. Posting message", guildId);
			postMessage(guildId, gameKey);
			
			try {
				ServerConfiguration config = configService.getConfig(guildId);
				JDA jda = BotManager.getJDA();
				Long chanId = config.getAuditId();
				if (chanId != null) {
					Guild guild = jda.getGuildById(guildId);
					if (guild != null) {
						String words = String.format("New key created - ID %s (Game: %s - Platform: %s)", gameKey.getExtraId(), name, platform);
						auditMessage(guildId, words);
					}
				}
			} catch (Exception e) {
				m_logger.error("Error auditing claim", e);
			}
			
			return true;
		} else {
			m_logger.info("Key was duplicate - denied");
			return false;
		}
	}
	
	@Transactional
	public boolean claim(GameKey key, Long userId) {
		Long guildId = key.getId().getGuild().getId();
		if (!claimants.canClaim(guildId, userId)) {
			m_logger.info("Can't claim key {} for user {}", key.getExtraId(), userId);
			try {
				ServerConfiguration config = configService.getConfig(guildId);
				JDA jda = BotManager.getJDA();
				Long chanId = config.getAuditId();
				if (chanId != null) {
					Guild guild = jda.getGuildById(guildId);
					if (guild != null) {
						Member member = guild.retrieveMemberById(userId).complete();
						if (member != null) {
							String words = String.format("User %s (%s) failed to claim key ID %s (Game: %s - Platform: %s) - Out of claim attempts", member.getAsMention(), userId, key.getExtraId(), key.getName(), key.getId().getPlatform());
							auditMessage(guildId, words);
						}
					}
				}
			} catch (Exception e) {
				m_logger.error("Error auditing claim", e);
			}
			return false;
		} else {
			claimants.claim(guildId, userId);
			key.setClaimantUserId(userId);
			m_logger.info("KEY CLAIMED - UUID {} - USER {}", key.getExtraId(), userId);
			keys.save(key);
			
			try {
				ServerConfiguration config = configService.getConfig(guildId);
				JDA jda = BotManager.getJDA();
				Long chanId = config.getAuditId();
				if (chanId != null) {
					Guild guild = jda.getGuildById(guildId);
					if (guild != null) {
						Member member = guild.retrieveMemberById(userId).complete();
						if (member != null) {
							String words = String.format("User %s (%s) successfully claimed key ID %s (Game: %s - Platform: %s)", member.getAsMention(), userId, key.getExtraId(), key.getName(), key.getId().getPlatform());
							auditMessage(guildId, words);
						}
					}
				}
			} catch (Exception e) {
				m_logger.error("Error auditing claim", e);
			}
			
			return true;
		}
	}
	
	@Transactional
	public void clearMessages(Long guildId, boolean repost) {
		m_logger.info("Resetting messages for guild {} | repost? {}", guildId, repost);
		ServerConfiguration config = configService.getConfig(guildId);
		JDA jda = BotManager.getJDA();
		
		Long chanId = config.getChannelId();
		if (chanId == null) {
			m_logger.info("Channel was null for guild {}", guildId);
			return;
		}
		
		Guild guild = jda.getGuildById(guildId);
		if (guild == null) {
			m_logger.info("Guild {} could not be retrieved", guildId);
			return;
		}
		
		MessageChannel txtchan = guild.getTextChannelById(chanId);
		if (txtchan == null) {
			m_logger.info("Text channel {} could not be retrieved for guild {}", chanId, guildId);
			return;
		}
		
		histories.remove(guildId);
		
		Callable<Void> whenDone = null;
		if (repost) {
			whenDone = new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					postMessages(guildId);
					return null;
				}};
		}
		Consumer<Throwable> whenFailed = new Consumer<Throwable>() {
			@Override
			public void accept(Throwable t) {
				auditMessage(guildId, "Failed to finish clearing and reposting messages due to exception (report to developer): "+t.getMessage());
			}
		};
		recursivelyGetChannelHistoryAndDeleteOwnPosts(txtchan, whenDone, whenFailed);
		
		m_logger.info("Reset messages for guild {} | repost? {}", guildId, repost);
	}
	
	@Transactional
	public void postMessages(Long guildId) {
		m_logger.info("Posting all messages for guild {}", guildId);
		ServerConfiguration config = configService.getConfig(guildId);
		JDA jda = BotManager.getJDA();
		
		Long chanId = config.getChannelId();
		if (chanId == null) {
			m_logger.info("Channel was null for guild {}", guildId);
			return;
		}
		
		Guild guild = jda.getGuildById(guildId);
		if (guild == null) {
			m_logger.info("Guild {} could not be retrieved", guildId);
			return;
		}
		
		MessageChannel txtchan = guild.getTextChannelById(chanId);
		if (txtchan == null) {
			m_logger.info("Text channel {} could not be retrieved for guild {}", chanId, guildId);
			return;
		}
		
		List<GameKey> allKeys = keys.findByIdGuildId(guildId);
		allKeys = allKeys.stream().filter(key -> {
			return key.getClaimantUserId() == null;
		}).sorted(new Comparator<GameKey>() {
			@Override
			public int compare(GameKey o1, GameKey o2) {
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		}).collect(Collectors.toList());
		
		final int sz = allKeys.size();
		for (int i = 0; i < sz; i++) {
			final GameKey key = allKeys.get(i);
			String words = String.format("Game: %s\nPlatform: %s", key.getName(), key.getId().getPlatform());
			String storepage = key.getStorelink();
			if (storepage != null && !storepage.isBlank()) {
				words += "\nStore Page: "+storepage;
			}
			
			if (i == sz - 1) {
				txtchan.sendMessage(words).setActionRow(Button.primary(key.getExtraId().toString(), "Claim Key")).queue(m -> {
					auditMessage(guildId, "Finished reposting "+sz+" keys.");
				});
			} else {
				txtchan.sendMessage(words).setActionRow(Button.primary(key.getExtraId().toString(), "Claim Key")).queue();
			}
		}
		
		m_logger.info("Finished posting all messages for guild {}", guildId);
	}
	
	@Transactional
	public void postMessage(Long guildId, GameKey key) {
		m_logger.info("Posting single message for guild {}", guildId);
		ServerConfiguration config = configService.getConfig(guildId);
		JDA jda = BotManager.getJDA();
		
		Long chanId = config.getChannelId();
		if (chanId == null) {
			m_logger.info("Channel was null for guild {}", guildId);
			return;
		}
		
		Guild guild = jda.getGuildById(guildId);
		if (guild == null) {
			m_logger.info("Guild {} could not be retrieved", guildId);
			return;
		}
		
		MessageChannel txtchan = guild.getTextChannelById(chanId);
		if (txtchan == null) {
			m_logger.info("Text channel {} could not be retrieved for guild {}", chanId, guildId);
			return;
		}
		
		String words = String.format("Game: %s\nPlatform: %s", key.getName(), key.getId().getPlatform());
		String storepage = key.getStorelink();
		if (storepage != null && !storepage.isBlank()) {
			words += "\nStore Page: "+storepage;
		}
		txtchan.sendMessage(words).setActionRow(Button.primary(key.getExtraId().toString(), "Claim Key")).queue();
		
		m_logger.info("Posted single message for guild {}", guildId);
	}
	
	private void recursivelyGetChannelHistoryAndDeleteOwnPosts(MessageChannel c, Callable<Void> whenDone, Consumer<Throwable> whenFailed) {
		MessageHistory history = gethistory(c);
		
		JDA jda = c.getJDA();
		history.retrievePast(100).queue(messagelist -> {
			if (messagelist == null || messagelist.size() == 0) {
				// delete the saved messages
				List<Message> filtered = history.getRetrievedHistory().stream().filter(m -> {
					return m.getAuthor().getIdLong() == jda.getSelfUser().getIdLong();
				}).collect(Collectors.toList());
				
				m_logger.info("Beginning to purge {} messages in channel {}", filtered.size(), c.getId());
				
				CompletableFuture.allOf(c.purgeMessages(filtered).toArray(new CompletableFuture[0])).exceptionally(e -> {
					m_logger.error("Error purging in channel "+c.getId(), e);
					if (whenFailed != null) {
						whenFailed.accept(e);
					}
					return null;
				}).whenComplete((result, ex) -> {
					m_logger.info("Finished purging in channel {}", c.getId());
					if (whenDone != null) {
						try {
							whenDone.call();
						} catch (Exception e) {
							m_logger.error("Failed to call whenDone in recursive channel purge: "+e.getMessage(), e);
							if (whenFailed != null) {
								whenFailed.accept(e);
							}
						}
					}
				});
			} else {
				// repeat
				recursivelyGetChannelHistoryAndDeleteOwnPosts(c, whenDone, whenFailed);
			}
		});
	}
	
	
	private MessageHistory gethistory(MessageChannel c) {
		long id = c.getIdLong();
		if (!histories.containsKey(id)) {
			histories.put(id, c.getHistory());
		}
		return histories.get(id);
	}
	
	private void auditMessage(Long guildId, String msg) {
		try {
			ServerConfiguration config = configService.getConfig(guildId);
			JDA jda = BotManager.getJDA();
			Long chanId = config.getAuditId();
			if (chanId != null) {
				Guild guild = jda.getGuildById(guildId);
				if (guild != null) {
					MessageChannel txtchan = guild.getTextChannelById(chanId);
					if (txtchan != null) {
						txtchan.sendMessage(msg).queue();
					}
				}
			}
		} catch (Exception e) {
			m_logger.error("Error auditing claim", e);
		}
	}
}
