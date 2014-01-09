package chatcontrol.Listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import chatcontrol.ChatControl;
import chatcontrol.Storage;
import chatcontrol.Utils.Common;

public class PlayerListener implements Listener{
	
	@EventHandler
	public void onPreLogin(AsyncPlayerPreLoginEvent e){
		long cas = System.currentTimeMillis() / 1000L;
		if(ChatControl.lastLoginTime.containsKey(e.getAddress()) && ChatControl.lastLoginTime.get(e.getAddress()) > ChatControl.Config.getLong("Anti_Bot.Rejoin_Time")){
			if((ChatControl.Config.getLong("Anti_Bot.Rejoin_Time") - (cas - ChatControl.lastLoginTime.get(e.getAddress()))) <= 0){
				return;
			}
			String msg = ChatControl.Config.getString("Localization.Prefix").replace("&", "�") + "\n\n" + ChatControl.Config.getString("Localization.Rejoin_Message").replace("&", "�").replace("%time", String.valueOf(ChatControl.Config.getLong("Anti_Bot.Rejoin_Time") - (cas - ChatControl.lastLoginTime.get(e.getAddress()))));
			msg.split("\n");
			e.disallow(Result.KICK_OTHER, msg);
		}
	}

	@EventHandler
	public void onJoin(final PlayerJoinEvent e){
		if(!ChatControl.data.containsKey(e.getPlayer())){
			ChatControl.data.put(e.getPlayer(), new Storage());
		}
		long cas = System.currentTimeMillis() / 1000L;
		if(!e.getPlayer().isOp() && !e.getPlayer().hasPermission("chatcontrol.bypass.rejoin") && !e.getPlayer().hasPermission("chatcontrol.admin")){
			ChatControl.lastLoginTime.put(e.getPlayer().getAddress().getAddress(), cas);
		}
		if(e.getPlayer().getName().equalsIgnoreCase("kangarko")){
			e.getPlayer().sendMessage("�b================================================");
			e.getPlayer().sendMessage("�3Na serveri je nainstalovany ChatControl v." + ChatControl.description.getVersion() /*+ (ChatControl.isOutdated ? ". Latest: " + ChatControl.newVersion : "")*/);
			e.getPlayer().sendMessage("�b================================================");
		}
		ChatControl.data.get(e.getPlayer()).loginLocation = e.getPlayer().getLocation();
		//ChatControl.loginLocation.put(e.getPlayer(), e.getPlayer().getLocation());
		if(ChatControl.muted && ChatControl.Config.getBoolean("Mute.Disable.Join_Messages")){
			e.setJoinMessage(null);
			return;
		}
		/*
		if(ChatControl.Config.getBoolean("Miscellaneous.Check_For_Updates") && ChatControl.isOutdated){
			for(Player pl : Bukkit.getOnlinePlayers()){
				if(pl.isOp() || pl.hasPermission("chatcontrol.update") || pl.hasPermission("chatcontrol.admin")){
					String sprava = ChatControl.Config.getString("Localization.Update_Needed").replace("&", "�").replace("%current", plugin.getDescription().getVersion()).replace("%new", ChatControl.newVersion);
					sprava.split("\n");
					pl.sendMessage(sprava);
				}
			}
		}
		 */
		switch (ChatControl.Config.getString("Messages.Common.Join_Message")) {
		case "default":
			break;
		case "none":
			e.setJoinMessage(null);
			break;
		default:
			e.setJoinMessage(ChatControl.Config.getString("Messages.Common.Join_Message").replace("&", "�").replace("%player", e.getPlayer().getName()).replace("%prefix", Common.prefix()));
			break;
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e){
		if(ChatControl.muted){
			if(ChatControl.Config.getBoolean("Mute.Disable.Quit_Messages")){
				e.setQuitMessage(null);
				return;
			}
		}
		switch (ChatControl.Config.getString("Messages.Common.Quit_Message")) {
		case "default":
			break;
		case "none":
			e.setQuitMessage(null);
			break;
		default:
			e.setQuitMessage(ChatControl.Config.getString("Messages.Common.Quit_Message").replace("&", "�").replace("%player", e.getPlayer().getName()).replace("%prefix", Common.prefix()));
			break;
		}
	}

	@EventHandler
	public void onKick(PlayerKickEvent e){
		if(ChatControl.muted){
			if(ChatControl.Config.getBoolean("Mute.Disable.Kick_Messages")){
				e.setLeaveMessage(null);
				return;
			}
		}
		switch (ChatControl.Config.getString("Messages.Common.Kick_Message")) {
		case "default":
			break;
		case "none":
			e.setLeaveMessage(null);
			break;
		default:
			e.setLeaveMessage(ChatControl.Config.getString("Messages.Common.Kick_Message").replace("&", "�").replace("%player", e.getPlayer().getName()).replace("%prefix", Common.prefix()));
			break;
		}
	}	

	@EventHandler
	public void onDeath(PlayerDeathEvent e){
		if(ChatControl.muted){
			if(ChatControl.Config.getBoolean("Mute.Disable.Death_Messages")){
				e.setDeathMessage(null);
				return;
			}
		}
	}

	@EventHandler(ignoreCancelled=true)
	public void onSignChange(SignChangeEvent e){
		if(ChatControl.Config.getBoolean("Signs.Duplication_Check")){
			if (e.getPlayer().hasPermission("chatcontrol.bypass.dupe") || e.getPlayer().hasPermission("chatcontrol.admin") || e.getPlayer().isOp()) {
				return;
			}
			if(/*ChatControl.lastSignText.containsKey(e.getPlayer()) && ChatControl.lastSignText.get(e.getPlayer()).equals*/ChatControl.data.get(e.getPlayer()).lastSignText == (e.getLine(0) + e.getLine(1) + e.getLine(2) + e.getLine(3))){
				Common.sendMsg(e.getPlayer(), "Localization.Dupe_Sign");
				e.setCancelled(true);
				return;
			}
			//ChatControl.lastSignText.put(e.getPlayer(), e.getLine(0) + e.getLine(1) + e.getLine(2) + e.getLine(3));
			ChatControl.data.get(e.getPlayer()).lastSignText = e.getLine(0) + e.getLine(1) + e.getLine(2) + e.getLine(3);
		}

		if (ChatControl.Config.getBoolean("Signs.Advertising_Check")) {
			if (Bukkit.getOnlinePlayers().length < ChatControl.Config.getInt("Miscellaneous.Minimum_Players_To_Enable_Plugin")) {
				return;
			}
			String msg = e.getLine(0) + e.getLine(1) + e.getLine(2) + e.getLine(3);
			if (Common.msgIsAd(e.getPlayer(), msg.toLowerCase())) {
				if ((e.getPlayer().hasPermission("chatcontrol.bypass.ad")) || (e.getPlayer().hasPermission("chatcontrol.admin"))) {
					return;
				}
				Common.customAction(e.getPlayer(), "Anti_Ad.Custom_Command", msg);
				Common.messages(e.getPlayer(), "�7[SIGN]�f " + msg);
				if(ChatControl.Config.getBoolean("Signs.Rewrite_Lines")){
					String[] cenzura = ChatControl.Config.getString("Signs.Rewrite_Text").replace("&", "�").split(":");
					try {
						e.setLine(0, cenzura[0]);
						e.setLine(1, cenzura[1]);
						e.setLine(2, cenzura[2]);
						e.setLine(3, cenzura[3]);
					} catch (Exception ex){}
				} else {
					e.setCancelled(true);
				}
			}
		}
	}
}