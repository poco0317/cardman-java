package bar.barinade.cardman.discord.serverconfig.service;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import bar.barinade.cardman.discord.serverconfig.data.ServerConfiguration;
import bar.barinade.cardman.discord.serverconfig.repo.ServerConfigurationRepo;
@Service
public class ServerConfigService {
	
	private static final Logger m_logger = LoggerFactory.getLogger(ServerConfigService.class);

	@Autowired
	private ServerConfigurationRepo configRepo;
	
	@Transactional
	public ServerConfiguration getConfig(Long guildId) {
		ServerConfiguration config = configRepo.findById(guildId).orElse(null);
		if (config == null) {
			config = new ServerConfiguration();
			config.setId(guildId);
			config = configRepo.saveAndFlush(config);
		}
		return config;
	}
	
	@Transactional
	public void setOutputChannel(Long guildId, Long channelId) {
		ServerConfiguration config = getConfig(guildId);
		config.setChannelId(channelId);
		configRepo.saveAndFlush(config);
		m_logger.debug("Guild {} set output channel to {}", guildId, channelId);
	}
	
	@Transactional
	public Long getOutputChannel(Long guildId) {
		return getConfig(guildId).getChannelId();
	}
	
	@Transactional
	public void setAuditChannel(Long guildId, Long channelId) {
		ServerConfiguration config = getConfig(guildId);
		config.setAuditId(channelId);
		configRepo.saveAndFlush(config);
		m_logger.debug("Guild {} set claim audit channel to {}", guildId, channelId);
	}
	
	@Transactional
	public Long getAuditChannel(Long guildId) {
		return getConfig(guildId).getAuditId();
	}
	
	@Transactional
	public void setAllowedClaims(Long guildId, Long count) {
		getConfig(guildId).setAllowedClaims(count);
	}
	
	@Transactional
	public Long getAllowedClaims(Long guildId) {
		Long l = getConfig(guildId).getAllowedClaims(); 
		if (l == null) {
			return 0L;
		}
		return l;
	}
	
}
