package me.andre111.dvz;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.andre111.dvz.commands.DvZCommand;
import me.andre111.dvz.config.ConfigManager;
import me.andre111.dvz.disguise.DisguiseSystemHandler;
import me.andre111.dvz.dragon.DragonAttackListener;
import me.andre111.dvz.dragon.DragonAttackManager;
import me.andre111.dvz.dragon.DragonDeathListener;
import me.andre111.dvz.dwarf.DwarfManager;
import me.andre111.dvz.event.DVZJoinGameEvent;
import me.andre111.dvz.generator.DvZWorldProvider;
import me.andre111.dvz.listeners.Listener_Block;
import me.andre111.dvz.listeners.Listener_Entity;
import me.andre111.dvz.listeners.Listener_Game;
import me.andre111.dvz.listeners.Listener_Player;
import me.andre111.dvz.manager.BlockManager;
import me.andre111.dvz.manager.BreakManager;
import me.andre111.dvz.manager.EffectManager;
import me.andre111.dvz.manager.HighscoreManager;
import me.andre111.dvz.manager.ItemStandManager;
import me.andre111.dvz.manager.WorldManager;
import me.andre111.dvz.monster.MonsterManager;
import me.andre111.dvz.players.SpecialPlayerManager;
import me.andre111.dvz.utils.FileHandler;
import me.andre111.dvz.utils.IconMenuHandler;
import me.andre111.dvz.utils.Invulnerability;
import me.andre111.dvz.utils.Item3DHandler;
import me.andre111.dvz.utils.InventoryHandler;
import me.andre111.dvz.utils.Metrics;
import me.andre111.dvz.utils.Metrics.Graph;
import me.andre111.dvz.utils.MovementStopper;
import me.andre111.dvz.volatileCode.DvZPackets;
import me.andre111.dvz.volatileCode.DynamicClassFunctions;
import me.andre111.items.SpellItems;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

public class DvZ extends JavaPlugin {
	public static DvZ instance;
	
	private Game[] games = new Game[10];
	private GameDummy gameDummy = new GameDummy();
	public static int startedGames = 0;
	private Lobby lobby;
	
	public static ProtocolManager protocolManager;
	public static SpellItems spellItems;
	
	public static DragonAttackManager dragonAtManager;
	public static DragonDeathListener dragonDeath;
	public static DragonAttackListener attackListener;
	public static MovementStopper moveStop;
	public static Invulnerability inVul;
	public static Item3DHandler item3DHandler;
	public static ItemStandManager itemStandManager;
	public static EffectManager effectManager;
	public static SpecialPlayerManager playerManager;
	
	public static MonsterManager monsterManager;
	public static DwarfManager dwarfManager;
	
	public static Logger logger;
	public static String prefix = "[Dwarves vs Zombies] ";
	
	public PluginDescriptionFile descriptionFile;
	
	
	@Override
	public void onLoad() {
		logger = Logger.getLogger("Minecraft");

		// Get plugin description
		descriptionFile = this.getDescription();

		// Dynamic package detection
		if (!DynamicClassFunctions.setPackages()) {
			logger.log(Level.WARNING, "NMS/OBC package could not be detected, using " + DynamicClassFunctions.nmsPackage + " and " + DynamicClassFunctions.obcPackage);
		}
		DynamicClassFunctions.setClasses();
		DynamicClassFunctions.setMethods();
		DynamicClassFunctions.setFields();
	}
	
	@Override
	public void onEnable() {
		if(DvZ.instance==null)
			DvZ.instance = this;
		
		ConfigManager.initConfig(this);
		
		//Disguisecraft check
		if (!ConfigManager.getStaticConfig().getString("disable_dcraft_check", "false").equals("true")) {
			if (!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib"))
			{
				DvZ.sendPlayerMessageFormated(Bukkit.getServer().getConsoleSender(), prefix+" "+ChatColor.RED+"ProtocolLib could not be found, disabling...");
				Bukkit.getPluginManager().disablePlugin(this);
				return;
			}
			if (!Bukkit.getPluginManager().isPluginEnabled("SpellItems"))
			{
				DvZ.sendPlayerMessageFormated(Bukkit.getServer().getConsoleSender(), prefix+" "+ChatColor.RED+"SpellItems could not be found, disabling...");
				Bukkit.getPluginManager().disablePlugin(this);
				return;
			}
		}
		DvZ.protocolManager = ProtocolLibrary.getProtocolManager();
		DvZ.spellItems = SpellItems.instance;
		
		//Disguises
		if(!DisguiseSystemHandler.init()) {
			return;
		}
		DvZPackets.setEntityID(DisguiseSystemHandler.newEntityID());
		
		Spellcontroller.plugin = this;
		IconMenuHandler.instance = new IconMenuHandler(this);
		
		SpellItems.loadFromConfiguration(ConfigManager.getItemFile());
		SpellItems.addRewardsFromConfiguration(ConfigManager.getRewardFile());
		dwarfManager = new DwarfManager();
		dwarfManager.loadDwarfes();
		monsterManager = new MonsterManager();
		monsterManager.loadMonsters();
		
		dragonAtManager = new DragonAttackManager();
		dragonAtManager.loadAttacks();
		dragonDeath = new DragonDeathListener(this);
		attackListener = new DragonAttackListener(this);
		moveStop = new MovementStopper(this);
		inVul = new Invulnerability(this);
		item3DHandler = new Item3DHandler(this);
		itemStandManager = new ItemStandManager();
		effectManager = new EffectManager();
		effectManager.loadEffects();
		playerManager = new SpecialPlayerManager();
		playerManager.loadPlayers();
		
		HighscoreManager.loadHighscore();
		BlockManager.loadConfig();
		WorldManager.reload();
		
		try {
		    Metrics metrics = new Metrics(this);
		    
		    // Plot the total amount of protections
		    Graph graph = metrics.createGraph("Games");
		    
		    graph.addPlotter(new Metrics.Plotter("Started Games") {

		    	@Override
		    	public int getValue() {
		    		int value = DvZ.startedGames;
		    		DvZ.startedGames = 0;
		    		return value;
		    	}

		    });
		    graph.addPlotter(new Metrics.Plotter("Running Games") {

		    	@Override
		    	public int getValue() {
		    		int value = 0;
		    		for(int i=0; i<games.length; i++) {
		    			if(games[i]!=null && games[i].isRunning()) {
		    				value++;
		    			}
		    		}
		    		return value;
		    	}

		    });
		    
		    metrics.start();
		    metrics.enable();
		} catch (IOException e) {
		    // Failed to submit the stats :-(
		}
		
		if(getConfig().getString("use_lobby", "true").equals("true"))
			Bukkit.getServer().createWorld(new WorldCreator(this.getConfig().getString("world_prefix", "DvZ_")+"Lobby"));
		File f = new File(Bukkit.getServer().getWorldContainer().getPath()+"/"+this.getConfig().getString("world_prefix", "DvZ_")+"Worlds/");
		if(!f.exists()) f.mkdirs();
		f = new File(Bukkit.getServer().getWorldContainer().getPath()+"/"+this.getConfig().getString("world_prefix", "DvZ_")+"Worlds/Type1/");
		if(!f.exists()) f.mkdirs();
		f = new File(Bukkit.getServer().getWorldContainer().getPath()+"/"+this.getConfig().getString("world_prefix", "DvZ_")+"Worlds/Type2/");
		if(!f.exists()) f.mkdirs();
		for (int i=0; i<10; i++) {
			File f2 = new File(Bukkit.getServer().getWorldContainer().getPath()+"/"+this.getConfig().getString("world_prefix", "DvZ_")+"Main"+i+"/");
			if(f2.exists()) FileHandler.deleteFolder(f2);
		}
		
		lobby = new Lobby(this);
		
		new Listener_Player(this);
		new Listener_Block(this);
		new Listener_Entity(this);
		new Listener_Game(this);
		new DvZWorldProvider(this);
		
		//init and reset games
		for(int i=0; i<games.length; i++) {
			int type = getConfig().getInt("game"+i, 1);
			
			if(type!=0) {
				games[i] = new Game(this, GameType.fromID(type).getFirstType());
				games[i].reset(false);
			}
		}
		//
		
		CommandExecutorDvZ command = new CommandExecutorDvZ();
		for(String st : getDescription().getCommands().keySet()) {
			getCommand(st).setExecutor(command);
		}
		
		//Run Main Game Managment
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				for(int i=0; i<games.length; i++) {
					if (games[i]!=null) {
						games[i].tick();
					}
				}
		    }
		}, 20, 20);
		
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				for(int i=0; i<games.length; i++) {
					if (games[i]!=null) {
						games[i].fastTick();
					}
				}
		    }
		}, 1, 1);
		
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				BreakManager.tick();
				//experimental resend all Itemstands every 10 seconds
				for(Player p : Bukkit.getServer().getOnlinePlayers()) {
					item3DHandler.respawnAll(p);
				}
		    }
		}, 20*10, 20*10);
		//
	}
	
	@Override
	public void onDisable() {
		//Done in game reset
		//HighscoreManager.saveHighscore();
		
		//remove all zombies
		if(item3DHandler!=null)
			item3DHandler.removeAll();
				
		DvZCommand.unregisterCommands();
				
		//remove all release inventories
		for(int i=0; i<games.length; i++) {
			if (games[i]!=null) {
				games[i].reset(false);
			}
		}
		
		if(DvZ.instance!=null)
			DvZ.instance = null;
	}
	
	public static void reloadItems() {
		SpellItems.reload();
		SpellItems.loadFromConfiguration(ConfigManager.getItemFile());
		SpellItems.addRewardsFromConfiguration(ConfigManager.getRewardFile());
	}
	
	public static void reloadRewards() {
		SpellItems.reload();
		SpellItems.loadFromConfiguration(ConfigManager.getItemFile());
		SpellItems.addRewardsFromConfiguration(ConfigManager.getRewardFile());
	}
	
	public static void log(String s) {
		logger.info(prefix+s);
	}
	
	//TODO - remove temporary workaround
	@SuppressWarnings("deprecation")
	public static void updateInventory(Player player) {
		player.updateInventory();
	}
	
	//#######################################
	//Bekomme Spiel in dem das der Spieler ist
	//#######################################
	public Game getPlayerGame(String player) {
		for(int i=0; i<games.length; i++) {
			if (games[i]!=null) {
				if (games[i].isPlayer(player))
				{
					return games[i];
				}
			}
		}
		
		return null;
	}
	
	public int getGameID(Game game) {
		for(int i=0; i<games.length; i++) {
			if (games[i]==game) {
				return i;
			}
		}
		
		return -1;
	}
	
	public Game getGame(int id) {
		return games[id];
	}

	public GameDummy getDummy() {
		return gameDummy;
	}
	
	public Lobby getLobby() {
		return lobby;
	}
	
	public int getRunningGameCount() {
		int count = 0;
		
		for(int i=0; i<games.length; i++) {
			if(games[i]!=null)
			if(games[i].isRunning()) {
				count++;
			}
		}
		
		return count;
	}
	
    public void joinGame(Player player, Game game) {
    	joinGame(player, game, false);
	}
    
    public void joinGame(Player player, boolean autojoin) {
    	for(int i=0; i<games.length; i++) {
			if(games[i]!=null) {
				joinGame(player, games[i], autojoin);
				break;
			}
    	}
    }
	
	public void joinGame(Player player, Game game, boolean autojoin) {
		if(!(getConfig().getString("autoadd_players","false").equals("true") || autojoin) && game.getPlayerState(player.getName())>1) return;
		
		game.setPlayerState(player.getName(), 1);
		if(ConfigManager.getStaticConfig().getString("use_lobby", "true").equals("true"))
			player.teleport(Bukkit.getServer().getWorld(getConfig().getString("world_prefix", "DvZ_")+"Lobby").getSpawnLocation());
		InventoryHandler.clearInv(player, false);

		DvZ.sendPlayerMessageFormated(player, ConfigManager.getLanguage().getString("string_self_added","You have been added to the game!"));
		
		//event
		DVZJoinGameEvent event = new DVZJoinGameEvent(player);
		Bukkit.getServer().getPluginManager().callEvent(event);

		//autoadd player
		if(game.getState()>1) {
			if (getConfig().getString("autoadd_players","false").equals("true") || autojoin) {
				if(!game.released) {
					game.setPlayerState(player.getName(), 2);
					DvZ.sendPlayerMessageFormated(player, ConfigManager.getLanguage().getString("string_choose","Choose your class!"));
					game.addDwarfItems(player);

					game.broadcastMessage(ConfigManager.getLanguage().getString("string_autoadd","Autoadded -0- as a Dwarf to the Game!").replace("-0-", player.getDisplayName()));
				} else {
					game.setPlayerState(player.getName(), 3);
					DvZ.sendPlayerMessageFormated(player, ConfigManager.getLanguage().getString("string_choose","Choose your class!"));
					game.addMonsterItems(player);

					game.broadcastMessage(ConfigManager.getLanguage().getString("string_autoadd_m","Autoadded -0- as a Monster to the Game!").replace("-0-", player.getDisplayName()));
				}
			}
		} else {
			if(ConfigManager.getStaticConfig().getBoolean("hscore_in_lobby", true)) {
				if(player.isValid()) {
					player.setScoreboard(HighscoreManager.createOrRefreshPlayerScore(player.getName()));
				}
			}
		}
	}
	
	public static void sendPlayerMessageFormated(Player player, String message) {
		player.sendMessage(message.split("%n"));
	}
	public static void sendPlayerMessageFormated(CommandSender sender, String message) {
		sender.sendMessage(message.split("%n"));
	}
	
	public static boolean isPathable(Block block) {
        return isPathable(block.getType());
	}
	public static boolean isPathable(Material material) {
		return
				material == Material.AIR ||
				material == Material.SAPLING ||
				material == Material.WATER ||
				material == Material.STATIONARY_WATER ||
				material == Material.POWERED_RAIL ||
				material == Material.DETECTOR_RAIL ||
				material == Material.LONG_GRASS ||
				material == Material.DEAD_BUSH ||
				material == Material.YELLOW_FLOWER ||
				material == Material.RED_ROSE ||
				material == Material.BROWN_MUSHROOM ||
				material == Material.RED_MUSHROOM ||
				material == Material.TORCH ||
				material == Material.FIRE ||
				material == Material.REDSTONE_WIRE ||
				material == Material.CROPS ||
				material == Material.SIGN_POST ||
				material == Material.LADDER ||
				material == Material.RAILS ||
				material == Material.WALL_SIGN ||
				material == Material.LEVER ||
				material == Material.STONE_PLATE ||
				material == Material.WOOD_PLATE ||
				material == Material.REDSTONE_TORCH_OFF ||
				material == Material.REDSTONE_TORCH_ON ||
				material == Material.STONE_BUTTON ||
				material == Material.SNOW ||
				material == Material.SUGAR_CANE_BLOCK ||
				material == Material.VINE ||
				material == Material.WATER_LILY ||
				material == Material.NETHER_STALK ||
				material == Material.TRIPWIRE_HOOK ||
				material == Material.TRIPWIRE ||
				material == Material.POTATO ||
				material == Material.CARROT ||
				material == Material.WOOD_BUTTON;
	}
	public final static HashSet<Byte> transparent = new HashSet<Byte>();
	static {
		transparent.add((byte)0);
		transparent.add((byte)6);
		transparent.add((byte)8);
		transparent.add((byte)9);
		transparent.add((byte)27);
		transparent.add((byte)28);
		transparent.add((byte)31);
		transparent.add((byte)32);
		transparent.add((byte)37);
		transparent.add((byte)38);
		transparent.add((byte)39);
		transparent.add((byte)40);
		transparent.add((byte)50);
		transparent.add((byte)51);
		transparent.add((byte)55);
		transparent.add((byte)59);
		transparent.add((byte)63);
		transparent.add((byte)65);
		transparent.add((byte)66);
		transparent.add((byte)68);
		transparent.add((byte)69);
		transparent.add((byte)70);
		transparent.add((byte)71);
		transparent.add((byte)75);
		transparent.add((byte)76);
		transparent.add((byte)77);
		transparent.add((byte)78);
		transparent.add((byte)83);
		transparent.add((byte)106);
		transparent.add((byte)111);
		transparent.add((byte)115);
		transparent.add((byte)131);
		transparent.add((byte)132);
		transparent.add((byte)141);
		transparent.add((byte)142);
		transparent.add((byte)143);
	}
	
	public static int scheduleRepeatingTask(final Runnable task, int delay, int interval) {
		return Bukkit.getScheduler().scheduleSyncRepeatingTask(DvZ.instance, task, delay, interval);
	}

	public static void cancelTask(int taskId) {
		Bukkit.getScheduler().cancelTask(taskId);
	}
}
