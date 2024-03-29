package chatcontrol.Listener;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import chatcontrol.ChatControl;
import chatcontrol.Utils.Common;
import chatcontrol.Utils.Permissions;
import chatcontrol.Utils.Writer;
import chatcontrol.Utils.Writer.TypSuboru;
import chatcontrol.Utils.Checks.ChecksUtils;

public class ChatListener implements Listener {

	@EventHandler(ignoreCancelled=true)
	public void onPlayerChat(AsyncPlayerChatEvent e) {

		if (Bukkit.getOnlinePlayers().length < ChatControl.Config.getInt("Miscellaneous.Minimum_Players_To_Enable_Plugin"))
			return;

		String finalMsg = null;

		if(!Common.hasPerm(e.getPlayer(), Permissions.Bypasses.global_perm)) {
			if (ChatControl.Config.getBoolean("Miscellaneous.Block_Chat_Until_Moved") && e.getPlayer().getLocation() == ChatControl.data.get(e.getPlayer()).loginLocation) {
				if (!e.getPlayer().hasPermission(Permissions.Bypasses.move)) {
					Common.sendMsg(e.getPlayer(), "Localization.Cannot_Chat_Until_Moved");
					e.setCancelled(true);
					return;
				}
			}

			if (ChatControl.muted && !e.getPlayer().hasPermission(Permissions.Bypasses.mute)) {
				Common.sendMsg(e.getPlayer(), "Localization.Cannot_Chat_While_Muted");
				e.setCancelled(true);
				return;
			}

			long cas = System.currentTimeMillis() / 1000L;
			if ((cas - ChatControl.data.get(e.getPlayer()).lastMessageTime) < ChatControl.Config.getLong("Chat.Message_Delay")) {
				if (!e.getPlayer().hasPermission(Permissions.Bypasses.timeChat)) {
					Common.sendRawMsg(e.getPlayer(), ChatControl.Config.getString("Localization.Time_Message").replace("%time", String.valueOf(ChatControl.Config.getLong("Chat.Message_Delay") - (cas - ChatControl.data.get(e.getPlayer()).lastMessageTime))));
					e.setCancelled(true);
					return;
				}
			}
			ChatControl.data.get(e.getPlayer()).lastMessageTime = cas;

			if (ChatControl.Config.getBoolean("Chat.Block_Duplicate_Messages")) {
				String sprava = e.getMessage().toLowerCase();
				if(ChatControl.Config.getBoolean("Chat.Strip_Unicode")) {
					sprava = Common.stripSpecialCharacters(e.getMessage());
				}
				if (ChatControl.data.get(e.getPlayer()).lastMessage.equals(sprava) || (Common.stringsAreSimilar(sprava, ChatControl.data.get(e.getPlayer()).lastMessage)
						&& ChatControl.Config.getBoolean("Chat.Block_Similar_Messages")) ) {
					if (!e.getPlayer().hasPermission(Permissions.Bypasses.dupeChat)) {
						Common.sendMsg(e.getPlayer(), "Localization.Dupe_Message");
						e.setCancelled(true);
						return;
					}
				}
				ChatControl.data.get(e.getPlayer()).lastMessage = sprava;
			}
			if(ChatControl.Config.getBoolean("Anti_Ad.Enabled") && !e.getPlayer().hasPermission(Permissions.Bypasses.ads)) {
				if (ChecksUtils.advertisingCheck(e.getPlayer(), e.getMessage().toLowerCase())) {
					Common.customAction(e.getPlayer(), "Anti_Ad.Custom_Command", e.getMessage());
					Common.messages(e.getPlayer(), e.getMessage());
					e.setCancelled(true);
				}
			}

			if (ChatControl.Config.getBoolean("Anti_Caps.Enabled") && !e.getPlayer().hasPermission(Permissions.Bypasses.caps)) {
				if(e.getMessage().length() >= ChatControl.Config.getInt("Anti_Caps.Minimum_Message_Length")) {

					int[] newMessage = Common.checkCaps(e.getMessage());
					if ((Common.percentageCaps(newMessage) >= ChatControl.Config.getInt("Anti_Caps.Total_Caps_Percentage")) || (Common.checkCapsInRow(newMessage) >= ChatControl.Config.getInt("Anti_Caps.Caps_In_A_Row"))){

						String[] parts = e.getMessage().split(" ");
						boolean capsAllowed = false;
						for (int i = 0; i < parts.length; i++) {
							boolean isOnWhitelist = false;
							for (String whitelist : ChatControl.Config.getStringList("Anti_Caps.Whitelist")) {
								if (whitelist.equalsIgnoreCase(parts[i])) {
									isOnWhitelist = true;
									capsAllowed = true;
									break;
								}
							}

							if (!isOnWhitelist) {
								if (!capsAllowed) {
									char firstChar = parts[i].charAt(0);
									parts[i] = (firstChar + parts[i].toLowerCase().substring(1));
								} else {
									parts[i] = parts[i].toLowerCase();
								}

								capsAllowed = (!parts[i].endsWith(".")) && (!parts[i].endsWith("!"));
							}
						}

						e.setMessage(StringUtils.join(parts, " "));

						if (ChatControl.Config.getBoolean("Anti_Caps.Warn_Player")) {
							Common.sendMsg(e.getPlayer(), "Localization.Caps_Message");

						}
					}
				}
			}

			if (ChatControl.Config.getBoolean("Anti_Swear.Enabled") && !e.getPlayer().hasPermission(Permissions.Bypasses.swear)) {

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

		String message = e.getMessage();
		
		if(!e.getPlayer().hasPermission(Permissions.Bypasses.replace))
			message = Common.replaceCharacters(e.getPlayer(), message);
		if(!e.getPlayer().hasPermission(Permissions.Bypasses.capitalize))
			message = Common.capitalize(message);
		if(!e.getPlayer().hasPermission(Permissions.Bypasses.insertDot))
			message = Common.insertDot(message);
		
		e.setMessage(message);
		finalMsg = message;

		if (ChatControl.Config.getBoolean("Chat.Write_To_File") && !ChatControl.Config.getStringList("Chat.Ignore_Players").contains(e.getPlayer().getName()))
			Writer.writeToFile(TypSuboru.ZAZNAM_CHATU, e.getPlayer().getName(), (finalMsg != null ? finalMsg : e.getMessage()));		
	}
}