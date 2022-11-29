package bar.barinade.cardman.discord.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import bar.barinade.cardman.discord.BotManager;
import bar.barinade.cardman.discord.serverconfig.data.ServerConfiguration;
import bar.barinade.cardman.discord.serverconfig.service.ClaimantService;
import bar.barinade.cardman.discord.serverconfig.service.GameKeyService;
import bar.barinade.cardman.discord.serverconfig.service.ServerConfigService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

@Component
@Scope("prototype")
public class ServerConfigCommandHandler extends CommandHandlerBase {
	
	private static final Logger m_logger = LoggerFactory.getLogger(ServerConfigCommandHandler.class);
	
	private static final String NAME_CMD_NEWKEY = "newkey";
	private static final String NAME_CMD_KEYCHAN = "keychannel";
	private static final String NAME_CMD_AUDITCHAN = "keyaudit";
	private static final String NAME_CMD_RESET = "keyreset";
	private static final String NAME_CMD_ALL = "all";
	private static final String NAME_CMD_USER = "user";
	private static final String NAME_CMD_CLAIMLIMIT = "keylimit";
	
	private static final String OPTION_NAME = "name";
	private static final String OPTION_KEY = "key";
	private static final String OPTION_PLATFORM = "platform";
	private static final String OPTION_CHANNEL = "channel";
	private static final String OPTION_COUNT = "count";
	private static final String OPTION_USER = "user";
	private static final String OPTION_STOREPAGE = "storepage";
	
	@Autowired
	private ServerConfigService configService;
	
	@Autowired
	private GameKeyService keys;
	
	@Autowired
	private ClaimantService claims;
	
	@Value("${discord.ownerid}")
	private String ownerId;
	
	@Override
	public CommandData[] getCommandsToUpsert() {
		return new CommandData[] {
				new CommandData(NAME_CMD_NEWKEY, "Add a new key to the claim system")
				.addOption(OptionType.STRING, OPTION_NAME, "Name of the item being claimed", true)
				.addOption(OptionType.STRING, OPTION_PLATFORM, "Name of the platform the key is for", true)
				.addOption(OptionType.STRING, OPTION_KEY, "The key", true)
				.addOption(OptionType.STRING, OPTION_STOREPAGE, "Link to the store page", false),
				
				new CommandData(NAME_CMD_KEYCHAN, "Set the output channel for keys.")
				.addOption(OptionType.CHANNEL, OPTION_CHANNEL, "Text channel to send messages", true),
				
				new CommandData(NAME_CMD_AUDITCHAN, "Set the output channel for claimed keys.")
				.addOption(OptionType.CHANNEL, OPTION_CHANNEL, "Text channel to send messages", true),
				
				new CommandData(NAME_CMD_RESET, "Reset claims")
					.addSubcommands(
						new SubcommandData(NAME_CMD_ALL, "Reset all claims"),
						new SubcommandData(NAME_CMD_USER, "Reset user claims")
						.addOption(OptionType.USER, OPTION_USER, "User to reset", true)
					),
				
				new CommandData(NAME_CMD_CLAIMLIMIT, "Set the claim limit for keys. 0 means unlimited.")
				.addOption(OptionType.INTEGER, OPTION_COUNT, "Claim limit", true)
		};
	}
	
	private boolean hasPermission(SlashCommandEvent event) {
		Member mmbr = event.getMember();
		if (mmbr != null
				&& !mmbr.getId().equals(ownerId)
				&& !mmbr.isOwner()
				&& !mmbr.hasPermission(Permission.ADMINISTRATOR)
				&& !mmbr.hasPermission(Permission.MANAGE_SERVER)) {
			m_logger.info("{} attempted to use config command without having permission", mmbr.getId());
			event.getHook().editOriginal("You must have Manage Server or Administrator permissions to use this command.").queue();;
			return false;
		}
		return true;
	}
	
	void cmd_keyreset(SlashCommandEvent event) {
		if (!hasPermission(event))
			return;
		
		String subcmd = event.getSubcommandName();
		if (subcmd == null || subcmd.isEmpty()) {
			event.getHook().editOriginal("You must specify a subcommand.").queue();
			return;
		}
		
		Long guildId = event.getGuild().getIdLong();
		
		if (subcmd.equals(NAME_CMD_ALL)) {
			claims.freeClaims(guildId);
			event.getHook().editOriginal("Reset all user claims for this server.").queue();
			
			try {
				ServerConfiguration config = configService.getConfig(guildId);
				JDA jda = BotManager.getJDA();
				Long chanId = config.getAuditId();
				if (chanId != null) {
					Guild guild = jda.getGuildById(guildId);
					if (guild != null) {
						MessageChannel txtchan = guild.getTextChannelById(chanId);
						if (txtchan != null) {
							txtchan.sendMessage("Reset all user claims for this server").queue();
						}
					}
				}
			} catch (Exception e) {
				m_logger.error("Error auditing claim", e);
			}
		}
		else if (subcmd.equals(NAME_CMD_USER)) {
			User u = event.getOption(OPTION_USER).getAsUser();
			if (u == null) {
				event.getHook().editOriginal("User did not exist? Did not supply user?").queue();
				return;
			}
			claims.freeClaim(guildId, u.getIdLong());
			event.getHook().editOriginal("Reset claims for user "+u.getAsMention()).queue();
			
			try {
				ServerConfiguration config = configService.getConfig(guildId);
				JDA jda = BotManager.getJDA();
				Long chanId = config.getAuditId();
				if (chanId != null) {
					Guild guild = jda.getGuildById(guildId);
					if (guild != null) {
						MessageChannel txtchan = guild.getTextChannelById(chanId);
						if (txtchan != null) {
							String words = String.format("Reset claims for user %s", u.getAsMention());
							txtchan.sendMessage(words).queue();
						}
					}
				}
			} catch (Exception e) {
				m_logger.error("Error auditing claim", e);
			}
		}
		else {
			event.getHook().editOriginal("You specified an invalid subcommand.").queue();
			m_logger.warn("{} attempted to use an invalid keyreset subcommand {}", event.getMember().getId(), subcmd);
		}
	}
	
	void cmd_keylimit(SlashCommandEvent event) {
		if (!hasPermission(event))
			return;
		
		Long guildId = event.getGuild().getIdLong();
		Long count = event.getOption(OPTION_COUNT).getAsLong();
		
		if (count == null || count < 0L) {
			event.getHook().editOriginal("You must set the claim limit to at least 0.").queue();
			return;
		}
		
		configService.setAllowedClaims(guildId, count);
		event.getHook().editOriginal("Set the claim limit to "+count).queue();
		
		try {
			ServerConfiguration config = configService.getConfig(guildId);
			JDA jda = BotManager.getJDA();
			Long chanId = config.getAuditId();
			if (chanId != null) {
				Guild guild = jda.getGuildById(guildId);
				if (guild != null) {
					MessageChannel txtchan = guild.getTextChannelById(chanId);
					if (txtchan != null) {
						String words = String.format("Updated key claim limit to %d", count);
						txtchan.sendMessage(words).queue();
					}
				}
			}
		} catch (Exception e) {
			m_logger.error("Error auditing claim", e);
		}
	}
	
	void cmd_newkey(SlashCommandEvent event) {
		if (!hasPermission(event))
			return;
		Long guildId = event.getGuild().getIdLong();
		Long outputId = configService.getOutputChannel(guildId);
		String gameName = event.getOption(OPTION_NAME).getAsString();
		String platformName = event.getOption(OPTION_PLATFORM).getAsString();
		String key = event.getOption(OPTION_KEY).getAsString();
		String storepage = null;
		if (event.getOption(OPTION_STOREPAGE) != null) {
			storepage = event.getOption(OPTION_STOREPAGE).getAsString();
		}
		
		boolean madekey = keys.newKey(guildId, gameName, platformName, key, storepage);
		if (madekey) {
			if (outputId == null || outputId == 0L) {
				event.getHook().editOriginal("Made a new key:\nGame '"+gameName+"'\nPlatform '"+platformName+"'\nRemember to set the key output channel!").queue();
			} else {
				event.getHook().editOriginal("Made a new key:\nGame '"+gameName+"'\nPlatform '"+platformName+"'").queue();
			}
		} else {
			event.getHook().editOriginal("Could not make this key. It may already exist.").queue();
		}
		
	}
	
	void cmd_keychannel(SlashCommandEvent event) {
		if (!hasPermission(event))
			return;
		
		Long guildId = event.getGuild().getIdLong();
		final ChannelType chantype = event.getOption(OPTION_CHANNEL).getChannelType();
		if (!chantype.equals(ChannelType.TEXT)) {
			event.getHook().editOriginal("You must specify a Text Channel. Your channel was of type '"+chantype.toString()+"'").queue();
			return;
		}
		final MessageChannel channel = event.getOption(OPTION_CHANNEL).getAsMessageChannel();
		
		Long outputId = configService.getOutputChannel(guildId);
		configService.setOutputChannel(guildId, channel.getIdLong());
		event.getHook().editOriginal("Key output channel set to '"+channel.getName()+"'").queue();
		
		if (outputId == null || outputId == 0L) {
			keys.postMessages(guildId);
		}
	}
	
	void cmd_keyaudit(SlashCommandEvent event) {
		if (!hasPermission(event))
			return;
		
		Long guildId = event.getGuild().getIdLong();
		final ChannelType chantype = event.getOption(OPTION_CHANNEL).getChannelType();
		if (!chantype.equals(ChannelType.TEXT)) {
			event.getHook().editOriginal("You must specify a Text Channel. Your channel was of type '"+chantype.toString()+"'").queue();
			return;
		}
		final MessageChannel channel = event.getOption(OPTION_CHANNEL).getAsMessageChannel();
		
		configService.setAuditChannel(guildId, channel.getIdLong());
		event.getHook().editOriginal("Key claim auditing channel set to '"+channel.getName()+"'").queue();
	}

}
