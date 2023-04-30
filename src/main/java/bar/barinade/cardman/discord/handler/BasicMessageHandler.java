package bar.barinade.cardman.discord.handler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import bar.barinade.cardman.discord.serverconfig.service.ClaimantService;
import bar.barinade.cardman.discord.serverconfig.service.GameKeyService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Component
@Scope("prototype")
public class BasicMessageHandler extends ListenerAdapter {
	
	private static final Logger m_logger = LoggerFactory.getLogger(BasicMessageHandler.class);
	
	private static final String CMD_REFRESH = "cm!refresh";
	private static final String CMD_RESETCLAIMS = "cm!resetclaims";
	private static final String CMD_COUNT = "cm!count";
	
	
	@Value("${discord.ownerid}")
	private String ownerId;
	
	@Autowired
	private GameKeyService keys;
	
	@Autowired
	private ClaimantService claims;
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		
		// must be in a server
		if (event.getGuild() == null) {
			return;
		}
		
		String msg = event.getMessage().getContentDisplay();
		Long guildId = event.getGuild().getIdLong();
		
		if (msg.startsWith(CMD_REFRESH) && hasPermission(event)) {
			event.getChannel().sendMessage("Resetting sent messages in output channel.").queue();
			keys.clearMessages(guildId, true);
		}
		
		if (msg.startsWith(CMD_RESETCLAIMS) && hasPermission(event)) {
			Mentions mentions = event.getMessage().getMentions();
			List<User> userMentions = mentions.getUsers();
			if (userMentions != null && !userMentions.isEmpty()) {
				User u = userMentions.get(0);
				claims.freeClaim(guildId, u.getIdLong());
				event.getChannel().sendMessage("Reset claims for user "+u.getId() + " | " +u.getName()).queue();
			} else {
				claims.freeClaims(guildId);
				event.getChannel().sendMessage("Resetting all user claims for this server.").queue();
			}
		}
		
		if (msg.startsWith(CMD_COUNT) && hasPermission(event)) {
			int count = keys.countUnclaimed(guildId);
			event.getChannel().sendMessage("There are "+count+" unclaimed keys in this server.").queue();
		}
	}
	
	private boolean hasPermission(MessageReceivedEvent event) {
		Member mmbr = event.getMember();
		if (mmbr != null
				&& !mmbr.getId().equals(ownerId)
				&& !mmbr.isOwner()
				&& !mmbr.hasPermission(Permission.ADMINISTRATOR)
				&& !mmbr.hasPermission(Permission.MANAGE_SERVER)) {
			m_logger.info("{} attempted to use config command without having permission", mmbr.getId());
			// event.getChannel().sendMessage("You must have Manage Server or Administrator permissions to use this command.").queue();
			return false;
		}
		return true;
	}
}
