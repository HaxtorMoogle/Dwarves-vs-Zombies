package me.andre111.dvz.listeners;

import me.andre111.dvz.DvZ;
import me.andre111.dvz.Game;
import me.andre111.dvz.Spellcontroller;
import me.andre111.dvz.dwarf.CustomDwarf;
import me.andre111.dvz.monster.CustomMonster;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTeleportEvent;

public class Listener_Entity implements Listener {
	private DvZ plugin;

	public Listener_Entity(DvZ plugin){
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		if (event.getEntityType()==EntityType.PLAYER) {
			Player player = (Player)event.getEntity();
			
			Game game = plugin.getPlayerGame(player.getName());
			if (game!=null) {
				//Monster Droppen nix
				if (game.isMonster(player.getName())) {
					event.getDrops().clear();
				}
			}
		}
	}
	
	@EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled()) return;
		
		if (event.getCause() == DamageCause.FALL && event.getEntity() instanceof Player && Spellcontroller.jumpingNormal.contains((Player)event.getEntity())) {
			event.setCancelled(true);
			Spellcontroller.jumpingNormal.remove((Player)event.getEntity());
			if(Spellcontroller.jumping.contains((Player)event.getEntity())) {
				Spellcontroller.jumping.remove((Player)event.getEntity());
				Spellcontroller.spellIronGolemLand((Player)event.getEntity());
			}
		}
		//disabled monster damage
		if (event.isCancelled()) return;
		if(event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			Game game = plugin.getPlayerGame(player.getName());
			if(game!=null) {
				if(game.isMonster(player.getName()) || game.isDwarf(player.getName(), true)) {
					String damage = "";
					
					if(event.getCause() == DamageCause.CONTACT) {
						damage = "contact";
					} else if(event.getCause() == DamageCause.DROWNING) {
						damage = "drown";
					} else if(event.getCause() == DamageCause.ENTITY_EXPLOSION || event.getCause() == DamageCause.BLOCK_EXPLOSION) {
						damage = "explosion";
					} else if(event.getCause() == DamageCause.FALL) {
						damage = "fall";
					} else if(event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.FIRE_TICK) {
						damage = "fire";
					} else if(event.getCause() == DamageCause.LAVA) {
						damage = "lava";
					} else if(event.getCause() == DamageCause.POISON) {
						damage = "poison";
					} else if(event.getCause() == DamageCause.STARVATION) {
						damage = "starve";
					} else if(event.getCause() == DamageCause.WITHER) {
						damage = "wither";
					}
					
					if(!damage.equals("")) {
						if(game.isMonster(player.getName())) {
							int pid = game.getPlayerState(player.getName()) - Game.monsterMin;
							CustomMonster cm = DvZ.monsterManager.getMonster(pid);
							
							if(cm.isDamageDisabled(damage)) {
								event.setCancelled(true);
							}
						} else if(game.isDwarf(player.getName(), true)) {
							int pid = game.getPlayerState(player.getName()) - Game.dwarfMin;
							CustomDwarf cd = DvZ.dwarfManager.getDwarf(pid);
							
							if(cd != null && cd.isDamageDisabled(damage)) {
								event.setCancelled(true);
							}
						}
					}
				}
				
				//graceperiode
				if(game.isGraceTime() && game.isDwarf(player.getName(), true)) {
					event.setCancelled(true);
				}
			}
		}
    }
	
	@EventHandler(priority=EventPriority.LOWEST)
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {
		if (event.isCancelled()) return;

		if(event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
			//disable friendly fire
			Game game = plugin.getPlayerGame(((Player)event.getEntity()).getName());
			if (game!=null) {
				if (!plugin.getConfig().getString("friendly_fire","true").equals("true")) {
					String player = ((Player)event.getEntity()).getName();
					String damager = ((Player)event.getDamager()).getName();
					
					if((game.isDwarf(player, false) && game.isDwarf(damager, false)) ||
					   (game.isMonster(player) && game.isMonster(damager))) {
						event.setCancelled(true);
						return;
					}
				}
			}
			//spawn no pvp
			Player p = ((Player)event.getEntity());
			if (plugin.getConfig().getInt("spawn_nopvp",0)>0 && p.getLocation().distanceSquared(p.getWorld().getSpawnLocation())<=plugin.getConfig().getInt("spawn_nopvp",0)*plugin.getConfig().getInt("spawn_nopvp",0)) {
				event.setCancelled(true);
			}
			if (plugin.getConfig().getInt("spawn_nopvp",0)>0 && p.getLocation().getWorld()==Bukkit.getServer().getWorld(plugin.getConfig().getString("world_prefix", "DvZ_")+"Lobby")) {
				event.setCancelled(true);
			}
		}
		//bows, snowballs and more
		if(event.getEntity() instanceof Player && event.getDamager() instanceof Projectile) {
			if(((Projectile)event.getDamager()).getShooter() instanceof Player) {
				//disable friendly fire
				Game game = plugin.getPlayerGame(((Player)event.getEntity()).getName());
				if (game!=null) {
					if (!plugin.getConfig().getString("friendly_fire","true").equals("true")) {
						String player = ((Player)event.getEntity()).getName();
						String damager = ((Player)((Projectile)event.getDamager()).getShooter()).getName();
						if((game.isDwarf(player, false) && game.isDwarf(damager, false)) ||
								   (game.isMonster(player) && game.isMonster(damager))) {
							event.setCancelled(true);
							return;
						}
					}
				}
				//spawn no pvp
				Player p = ((Player)event.getEntity());
				if (plugin.getConfig().getInt("spawn_nopvp",0)>0 && p.getLocation().distanceSquared(p.getWorld().getSpawnLocation())<=plugin.getConfig().getInt("spawn_nopvp",0)*plugin.getConfig().getInt("spawn_nopvp",0)) {
					event.setCancelled(true);
				}
				if (plugin.getConfig().getInt("spawn_nopvp",0)>0 && p.getLocation().getWorld()==Bukkit.getServer().getWorld(plugin.getConfig().getString("world_prefix", "DvZ_")+"Lobby")) {
					event.setCancelled(true);
				}
			}
		}
		
		if(event.isCancelled()) return;
		
		//IronGolem more damage/buffed monsters
		//irongolem deaktiviert, da custom monster existieren
		if(event.getDamager() instanceof Player) {
			Player dgm = (Player) event.getDamager();
			Game game = plugin.getPlayerGame(dgm.getName());
			if (game!=null) {
				if(game.isPlayer(dgm.getName())) {
					if(/*game.getPlayerState(dgm.getName())==35 ||*/ game.isBuffed(dgm.getName())) {
						event.setDamage(event.getDamage()*5);
					}
					//custom dwarf
					if(game.isDwarf(dgm.getName(), false)) {
						int id = game.getPlayerState(dgm.getName()) - Game.dwarfMin;
						event.setDamage(event.getDamage()*DvZ.dwarfManager.getDwarf(id).getDamageBuff());
					}
					//custom monster
					if(game.isMonster(dgm.getName())) {
						int id = game.getPlayerState(dgm.getName()) - Game.monsterMin;
						event.setDamage(event.getDamage()*DvZ.monsterManager.getMonster(id).getDamageBuff());
					}
					
					//Dwarf kill effects
					if(game.isDwarf(dgm.getName(), false)) {
						event.setDamage(event.getDamage()*DvZ.effectManager.getDwarfKillMultiplier(game, dgm.getName()));
					}
				}
			}
		}
		
		if((event.getDamager() instanceof Snowball)) {
			//Hurting Snowballs
			if (event.getDamager().getFallDistance() == Spellcontroller.identifier) {
				event.setDamage((double) Spellcontroller.sdamage);
			}
		}
    }
	
	//Assasin kills
	@EventHandler(priority = EventPriority.HIGH)
	public void onEntityDamage2(EntityDamageEvent e) {
		if(e.isCancelled()) return;
	    if(!(e instanceof EntityDamageByEntityEvent))
	        return;

	    EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) e;
	    if(!(event.getDamager() instanceof Player))
	        return;
	    if(!(event.getEntity() instanceof Player))
	    	return;
	    
	    Player player = (Player) event.getEntity();
	    Player damager = (Player) event.getDamager();
	    
	    //is kill?
	    if(e.getDamage()<player.getHealth())
	    	return;
	    
	    Game game = plugin.getPlayerGame(damager.getName());
	    if (game!=null) {
	    	if(game.isDwarf(player.getName(), true) && game.getPlayerState(damager.getName())==Game.assasinState) {
	    		game.resetCustomCooldown(damager.getName(), "assassin_time");
	    	}
	    }
	}
	
	//Spielerteleports zur Lobby
	@EventHandler
	public void onEntityTeleport(EntityTeleportEvent event) {
		if(event.getEntityType()==EntityType.PLAYER) {
			World w = event.getTo().getWorld();
			World fw = event.getFrom().getWorld();
			World lobby = Bukkit.getServer().getWorld(plugin.getConfig().getString("world_prefix", "DvZ_")+"Lobby");
			World main = Bukkit.getServer().getWorld(plugin.getConfig().getString("world_prefix", "DvZ_")+"Main");
			
			if(w==lobby && fw!=main) {
				//TODO - disable to toquestion wich game to join
				//plugin.getGame(0).addPlayer(((Player)event.getEntity()).getName());
				//DvZ.sendPlayerMessageFormated(((Player)event.getEntity()), plugin.getLanguage().getString("string_self_added", "You have been added to the game!"));
			}
		}
	}
	
	//Enderdragon - disable portals
	@EventHandler(priority = EventPriority.NORMAL)
	public void onEntityCreatePortalEvent(EntityCreatePortalEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof EnderDragon)
		{ 
			if (entity.getWorld().getEnvironment() != Environment.THE_END) event.setCancelled(true); 
		}
	}
}
