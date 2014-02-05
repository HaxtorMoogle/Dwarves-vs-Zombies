package me.andre111.dvz.commands;

import java.util.Map;

import me.andre111.dvz.DvZ;
import me.andre111.dvz.Game;
import me.andre111.dvz.config.ConfigManager;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InfoCommand extends DvZCommand {
	//get some information about the game
	public InfoCommand(String name) {
		super(name);
	}

	@Override
	public boolean handle(int gameID, CommandSender sender, String[] args) {
		if(!sender.hasPermission("dvz.info")) {
			DvZ.sendPlayerMessageFormated(sender, "You don't have the Permission to do that!");
			return false;
		}
		
		Game game = getGameFromID(gameID, sender);
		
		if(game!=null) {
			int gameS = game.getState();
			if(gameS==1) {
				if(!game.getStarting()) {
					DvZ.sendPlayerMessageFormated(sender, ConfigManager.getLanguage().getString("string_game_notrunning","No Game running!"));
				} else {
					DvZ.sendPlayerMessageFormated(sender, ConfigManager.getLanguage().getString("string_game_start","Game starting in -0- Seconds!").replace("-0-", ""+game.getStartTime()));
				}
			} else {
				int dwarf = 0;
				int dwarfoff = 0;
				int mons = 0;
				int monsoff = 0;
				
				for(Map.Entry<String, Integer> e : game.playerstate.entrySet()){
					boolean online = false;
					Player player = Bukkit.getServer().getPlayerExact(e.getKey());
					if (player!=null) online = true;
					
					if (game.isDwarf(e.getKey(), true)) {
						if (online) dwarf++; else dwarfoff++;
					}
					if (game.isMonster(e.getKey())) {
						if (online) mons++; else monsoff++;
					}
				}
				
				
				int hours = 0;
				int minutes = 0;
				int seconds = game.getDauer();
				hours = (int)Math.floor(seconds/60.0/60.0);
				minutes = (int)Math.floor((seconds-(hours*60*60))/60.0);
				seconds = seconds-(minutes*60)-(hours*60*60);
				
				DvZ.sendPlayerMessageFormated(sender, ConfigManager.getLanguage().getString("string_game_running","Game running for -0- Hours -1- Minutes -2- Seconds!").replace("-0-", ""+hours).replace("-1-", ""+minutes).replace("-2-", ""+seconds));
				DvZ.sendPlayerMessageFormated(sender, ConfigManager.getLanguage().getString("string_game_count","-0- (-1- Offline) Dwarves and -2-(-3- Offline) Monsters!").replace("-0-", ""+dwarf).replace("-1-", ""+dwarfoff).replace("-2-", ""+mons).replace("-3-", ""+monsoff));
			}
		}
		return true;
	}
}
