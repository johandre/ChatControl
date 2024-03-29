package chatcontrol.Listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import chatcontrol.ChatControl;
import chatcontrol.Utils.Common;
import chatcontrol.Utils.Permissions;
import chatcontrol.Utils.Writer;
import chatcontrol.Utils.Writer.TypSuboru;
import chatcontrol.Utils.Checks.ChecksUtils;

public class CommandListener implements Listener{

	@SuppressWarnings("deprecation")
	@EventHandler(ignoreCancelled=true)
	public void onPlayerCommand(PlayerCommandPreprocessEvent e){
		if(!(Bukkit.getOnlinePlayers().length >= ChatControl.Config.getInt("Miscellaneous.Minimum_Players_To_Enable_Plugin"))){
			return;
		}

		if(!Common.hasPerm(e.getPlayer(), Permissions.Bypasses.global_perm) ) {
			if(ChatControl.muted){
				if (e.getPlayer().hasPermission(Permissions.Bypasses.mute)) {
					return;
				}
				for (String msg : ChatControl.Config.getStringList("Mute.Disabled_Commands_During_Mute")){
					if(e.getMessage().startsWith("/" + msg)){
						Common.sendMsg(e.getPlayer(), "Localization.Cannot_Command_While_Muted");
						e.setCancelled(true);
						return;
					}
				}
			}

			long cas = System.currentTimeMillis() / 1000L;

			if((cas - ChatControl.data.get(e.getPlayer()).lastCommandTime) < ChatControl.Config.getLong("Commands.Command_Delay")){
				if(e.getPlayer().hasPermission(Permissions.Bypasses.timeCmd)){
					return;
				}
				for (String sprava : ChatControl.Config.getStringList("Commands.Whitelist_Time")){
					if(e.getMessage().startsWith("/" + sprava)){
						return;
					}
				}
				Common.sendRawMsg(e.getPlayer(), ChatControl.Config.getString("Localization.Command_Message").replace("%time", String.valueOf(ChatControl.Config.getLong("Commands.Command_Delay") - (cas - ChatControl.data.get(e.getPlayer()).lastCommandTime))));
				e.setCancelled(true);
				return;
			} else {
				ChatControl.data.get(e.getPlayer()).lastCommandTime = cas;
			}

			if(ChatControl.Config.getBoolean("Commands.Block_Duplicate_Commands")){
				String sprava = e.getMessage().toLowerCase();
				
				// Strip from messages like /tell <player> <msg> the player, making the check less less annoying.
				if(e.getMessage().split(" ").length > 2) {
					Player pl = Bukkit.getPlayer(e.getMessage().split(" ")[1]);
					if(pl != null && pl.isOnline()) {
						sprava = sprava.replace(pl.getName() + " ", "");
					}
				}
				
				if(ChatControl.Config.getBoolean("Commands.Strip_Unicode")) {
					sprava = Common.stripSpecialCharacters(sprava);
				}
				if(ChatControl.data.get(e.getPlayer()).lastCommand.equals(sprava) || (Common.stringsAreSimilar(sprava, ChatControl.data.get(e.getPlayer()).lastCommand)
						&& ChatControl.Config.getBoolean("Commands.Block_Similar_Messages")) ){
					if(e.getPlayer().hasPermission(Permissions.Bypasses.dupeCmd)){
						return;
					}
					for (String whitelistedMsg : ChatControl.Config.getStringList("Commands.Whitelist_Duplication")){
						if(e.getMessage().startsWith("/" + whitelistedMsg)){
							return;
						}
					}
					Common.sendMsg(e.getPlayer(), "Localization.Dupe_Command");
					e.setCancelled(true);
					return;
				}
				ChatControl.data.get(e.getPlayer()).lastCommand = sprava;
			}

			if(ChatControl.Config.getBoolean("Anti_Ad.Enabled_In_Commands") && !e.getPlayer().hasPermission(Permissions.Bypasses.ads)){
				if(ChecksUtils.advertisingCheck(e.getPlayer(), e.getMessage())){
					for(String whitelist : ChatControl.Config.getStringList("Anti_Ad.Command_Whitelist")){
						if(e.getMessage().startsWith(whitelist)){
							return;
						}
					}
					Common.customAction(e.getPlayer(), "Anti_Ad.Custom_Command", e.getMessage());
					Common.messages(e.getPlayer(), e.getMessage());
					e.setCancelled(true);
				}
			}

			if(ChatControl.Config.getBoolean("Anti_Swear.Enabled_In_Commands") && !e.getPlayer().hasPermission(Permissions.Bypasses.swear)){
				for(String ignoredCmd : ChatControl.Config.getStringList("Anti_Swear.Command_Whitelist")) {
					if(e.getMessage().startsWith(ignoredCmd)) {
						return;
					}
				}
				String finalMessage = ChecksUtils.swearCheck(e.getPlayer(), e.getMessage(), Common.prepareForSwearCheck(e.getMessage()));

				if(finalMessage != e.getMessage()) {
					if (ChatControl.Config.getBoolean("Anti_Swear.Block_Message")) {
						e.setCancelled(true);
						return;
					}
					if(ChatControl.Config.getBoolean("Anti_Swear.Replace_Word")) {
						e.setMessage(finalMessage);
					}
				}
			}
		}

		if (ChatControl.Config.getBoolean("Chat.Write_To_File") && !ChatControl.Config.getStringList("Chat.Ignore_Players").contains(e.getPlayer().getName())) {
			for(String prikaz : ChatControl.Config.getStringList("Chat.Include_Commands") ) {
				prikaz = prikaz.toLowerCase();
				if(e.getMessage().toLowerCase().startsWith(prikaz)) {
					Writer.writeToFile(TypSuboru.ZAZNAM_CHATU, "[CMD] " + e.getPlayer().getName(), e.getMessage());
				}
			}
		}
	}
}