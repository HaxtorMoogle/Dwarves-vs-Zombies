package me.andre111.dvz.manager;

import java.util.List;
import java.util.Map;

import me.andre111.dvz.DvZ;
import me.andre111.dvz.Game;
import me.andre111.dvz.config.ConfigManager;
import me.andre111.dvz.utils.PlayerHandler;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EffectManager {
	private int[][] effectMonsterDay;
	private int[][] effectMonsterNight;
	private int[][] effectMonsterMidNight;
	private int[][] effectDwarfAbove;
	private int[][] effectDwarfBelow;
	
	private boolean killEffectEnabled;
	private int killEffectTime;
	private boolean killEffectParticles;
	
	public void playerEffects(Game game) {
		addMonsterEffects(game);
		addDwarfEffects(game);
	}
	
	private void addMonsterEffects(Game game) {
		int id = DvZ.instance.getGameID(game);
		World w =  Bukkit.getServer().getWorld(DvZ.instance.getConfig().getString("world_prefix", "DvZ_")+"Main"+id+"");
		if(w==null) return;
		
		long time = w.getTime();
		int[][] effects = effectMonsterDay;
		//Day
		if(time>=0 && time<=12000) {
			effects = effectMonsterDay;
		//Night
		} else {
			//midnight
			if(time>=18000-500 && time<=18000+500) {
				effects = effectMonsterMidNight;
			} else {
				effects = effectMonsterNight;
			}
		}
		
		for(int i=0; i<effects.length; i++) {
			int effect = effects[i][0];
			int level = effects[i][1];
			
			if(effect!=-1) {
				addMonsterEffect(game, effect, level);
			}
		}
	}
	
	private void addMonsterEffect(Game game, int id, int level) {
		for(Map.Entry<String, Integer> e : game.playerstate.entrySet()){
			String playern = e.getKey();
			
			if(game.isMonster(playern)) {
				Player player = Bukkit.getServer().getPlayerExact(playern);
				
				if(player!=null) {
					if(!PlayerHandler.hasHigherPotionEffect(player, id, level)) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.getById(id), 3*20, level), true);
					}
				}
			}
		}
	}
	
	private void addDwarfEffects(Game game) {
		for(Map.Entry<String, Integer> e : game.playerstate.entrySet()){
			String playern = e.getKey();
			
			if(game.isDwarf(playern, true)) {
				Player player = Bukkit.getServer().getPlayerExact(playern);
				if(player!=null) {
					int light = player.getLocation().getBlock().getLightLevel();

					//above
					for(int i=light-1; i>=0; i--) {
						int id = effectDwarfAbove[i][0];
						int level = effectDwarfAbove[i][1];
						if(id!=-1)
						if(!PlayerHandler.hasHigherPotionEffect(player, id, level)) {
							player.addPotionEffect(new PotionEffect(PotionEffectType.getById(id), 3*20, level), true);
						}
					}
					//below
					for(int i=light+1; i<16; i++) {
						int id = effectDwarfBelow[i][0];
						int level = effectDwarfBelow[i][1];
						if(id!=-1)
						if(!PlayerHandler.hasHigherPotionEffect(player, id, level)) {
							player.addPotionEffect(new PotionEffect(PotionEffectType.getById(id), 3*20, level), true);
						}
					}
				}
			}
		}
	}
	
	public void killEffects(Game game) {
		if(!killEffectParticles) return;
		
		for(Map.Entry<String, Integer> e : game.playerstate.entrySet()){
			String playern = e.getKey();
			Player player = Bukkit.getServer().getPlayerExact(playern);
			
			if(player!=null) {
				if(game.isDwarf(playern, true)) {
					if(game.getCustomCooldown(playern, "effects_kill")>=0) {
						spawnParticle(game, player);
					}
				}
			}
		}
	}
	
	public void dwarfKilledMonster(Game game, Player dwarf) {
		if(killEffectEnabled) {
			//Strenght is broken with Disguisecraft
			//dwarf.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, killEffectTime*20, 20), true);
			
			game.setCustomCooldown(dwarf.getName(), "effects_kill", killEffectTime);
		}
	}
	
	public double getDwarfKillMultiplier(Game game, String dwarf) {
		if(game.getCustomCooldown(dwarf, "effects_kill")>0) {
			return 20;
		}
		
		return 1;
	}
	
	private void spawnParticle(Game game, Player player) {
		player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
	}
	//not working
	/*
		PacketContainer packet = DvZ.protocolManager.createPacket(0x3F);
		
		packet.getStrings().
			write(0, "magicCrit");
		packet.getFloat().
			write(0, (float) player.getLocation().getX()).
			write(1, (float) player.getLocation().getY()).
			write(2, (float) player.getLocation().getZ()).
			write(3, 0F).
			write(4, 0F).
			write(5, 0F).
			write(6, 0F);
		packet.getIntegers().
			write(0, 1);
		
		sendToAll(game, packet);
	}
	
	private void sendToAll(Game game, PacketContainer packet) {
		System.out.println("Sparticles!");
		try {
			for(Map.Entry<String, Integer> e : game.playerstate.entrySet()){
				String playern = e.getKey();
				Player player = Bukkit.getServer().getPlayerExact(playern);
				
				if(player!=null) {
					DvZ.protocolManager.sendServerPacket(player, packet);
				}
			}
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}*/
	
	public void loadEffects() {
		//monster
		List<String> monsterDayEffects = ConfigManager.getMonsterFile().getStringList("effects.day");
		List<String> monsterNightEffects = ConfigManager.getMonsterFile().getStringList("effects.night");
		List<String> monsterMidNightEffects = ConfigManager.getMonsterFile().getStringList("effects.midnight");

		//Day
		effectMonsterDay = new int[monsterDayEffects.size()][2];
		for(int i=0; i<monsterDayEffects.size(); i++) {
			String effect = monsterDayEffects.get(i);
			while(effect.startsWith(" ")) effect = effect.substring(1);
			while(effect.endsWith(" ")) effect = effect.substring(0, effect.length()-1);

			int eid = -1;
			int elevel = 0;

			String[] effectPart = effect.split(" ");
			if(effectPart.length>0) eid = Integer.parseInt(effectPart[0]);
			if(effectPart.length>1) elevel = Integer.parseInt(effectPart[1]);

			effectMonsterDay[i][0] = eid;
			effectMonsterDay[i][1] = elevel;
		}
		//Night
		effectMonsterNight = new int[monsterNightEffects.size()][2];
		for(int i=0; i<monsterNightEffects.size(); i++) {
			String effect = monsterNightEffects.get(i);
			while(effect.startsWith(" ")) effect = effect.substring(1);
			while(effect.endsWith(" ")) effect = effect.substring(0, effect.length()-1);

			int eid = -1;
			int elevel = 0;

			String[] effectPart = effect.split(" ");
			if(effectPart.length>0) eid = Integer.parseInt(effectPart[0]);
			if(effectPart.length>1) elevel = Integer.parseInt(effectPart[1]);

			effectMonsterNight[i][0] = eid;
			effectMonsterNight[i][1] = elevel;
		}
		//Night
		effectMonsterMidNight = new int[monsterMidNightEffects.size()][2];
		for(int i=0; i<monsterMidNightEffects.size(); i++) {
			String effect = monsterMidNightEffects.get(i);
			while(effect.startsWith(" ")) effect = effect.substring(1);
			while(effect.endsWith(" ")) effect = effect.substring(0, effect.length()-1);

			int eid = -1;
			int elevel = 0;

			String[] effectPart = effect.split(" ");
			if(effectPart.length>0) eid = Integer.parseInt(effectPart[0]);
			if(effectPart.length>1) elevel = Integer.parseInt(effectPart[1]);

			effectMonsterMidNight[i][0] = eid;
			effectMonsterMidNight[i][1] = elevel;
		}

		//dwarves
		effectDwarfAbove = new int[16][2];
		effectDwarfBelow = new int[16][2];
		for(int k=0; k<16; k++) {
			//above
			String dwarfEffects = ConfigManager.getClassFile().getString("effects.lightlevel.above."+k, "");

			effectDwarfAbove[k][0] = -1;
			effectDwarfAbove[k][1] = 0;

			if(!dwarfEffects.equals("")) {
				while(dwarfEffects.startsWith(" ")) dwarfEffects = dwarfEffects.substring(1);
				while(dwarfEffects.endsWith(" ")) dwarfEffects = dwarfEffects.substring(0, dwarfEffects.length()-1);

				int eid = -1;
				int elevel = 0;

				String[] effectPart = dwarfEffects.split(" ");
				if(effectPart.length>0) eid = Integer.parseInt(effectPart[0]);
				if(effectPart.length>1) elevel = Integer.parseInt(effectPart[1]);

				effectDwarfAbove[k][0] = eid;
				effectDwarfAbove[k][1] = elevel;
			}

			//below
			String dwarfEffects2 = ConfigManager.getClassFile().getString("effects.lightlevel.below."+k, "");

			effectDwarfBelow[k][0] = -1;
			effectDwarfBelow[k][1] = 0;

			if(!dwarfEffects2.equals("")) {
				while(dwarfEffects2.startsWith(" ")) dwarfEffects2 = dwarfEffects2.substring(1);
				while(dwarfEffects2.endsWith(" ")) dwarfEffects2 = dwarfEffects2.substring(0, dwarfEffects2.length()-1);

				int eid2 = -1;
				int elevel2 = 0;

				String[] effectPart2 = dwarfEffects2.split(" ");
				if(effectPart2.length>0) eid2 = Integer.parseInt(effectPart2[0]);
				if(effectPart2.length>1) elevel2 = Integer.parseInt(effectPart2[1]);

				effectDwarfBelow[k][0] = eid2;
				effectDwarfBelow[k][1] = elevel2;
			}
		}
		
		//kill effects
		killEffectEnabled = ConfigManager.getClassFile().getString("effects.kill.enabled", "true").equals("true");
		killEffectTime = ConfigManager.getClassFile().getInt("effects.kill.duration", 3);
		killEffectParticles = ConfigManager.getClassFile().getString("effects.kill.particles", "true").equals("true");
	}
}
