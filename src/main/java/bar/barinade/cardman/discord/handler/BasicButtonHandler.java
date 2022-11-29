package bar.barinade.cardman.discord.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import bar.barinade.cardman.discord.serverconfig.data.GameKey;
import bar.barinade.cardman.discord.serverconfig.service.GameKeyService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Component
@Scope("prototype")
public class BasicButtonHandler extends ListenerAdapter {
	
	private static final Logger m_logger = LoggerFactory.getLogger(BasicButtonHandler.class);

	@Autowired
	private GameKeyService keys;
	
	@Override
	public void onButtonClick(ButtonClickEvent event) {
		String buttonid = event.getComponentId();
		Member mmbr = event.getMember();
		if (buttonid != null && mmbr != null) {
			
			GameKey key = keys.getKeyByButtonId(buttonid);
			if (key != null) {
				m_logger.info("Claiming key {}", buttonid);
				
				PrivateChannel chan = mmbr.getUser().openPrivateChannel().complete();
				if (keys.claim(key, mmbr.getIdLong())) {
					event.getMessage().delete().queue();
					if (key.getStorelink() != null && !key.getStorelink().isBlank()) {
						chan.sendMessage("You claimed '"+key.getName()+"' for Platform '"+key.getId().getPlatform()+"'\n\nKEY: `"+key.getId().getKey()+"`\nLink: "+key.getStorelink()).queue();
					} else {
						chan.sendMessage("You claimed '"+key.getName()+"' for Platform '"+key.getId().getPlatform()+"'\n\nKEY: `"+key.getId().getKey()+"`").queue();
					}
				} else {
					event.deferEdit().queue();
					chan.sendMessage("You already recently claimed another key. Ask administration to reset your claims if you would like another.").queue();
				}
			} else {
				m_logger.info("Failed to find the key for a button... {}", buttonid);
				event.editMessage(event.getMessage().getContentDisplay() + "\nAn error occurred. Contact developer - ID "+buttonid).queue();
				event.editButton(null).queue();
			}
			
		}
	}
}
