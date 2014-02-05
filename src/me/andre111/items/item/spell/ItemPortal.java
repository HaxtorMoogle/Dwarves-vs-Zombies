package me.andre111.items.item.spell;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.andre111.dvz.DvZ;
import me.andre111.dvz.Game;
import me.andre111.dvz.config.ConfigManager;
import me.andre111.items.item.ItemSpell;

public class ItemPortal extends ItemSpell {

	@Override
	public boolean cast(Player player, Location loc, Player target, Block block) {
		if(player==null) return false;

		return castIntern(player, loc);
	}

	private boolean castIntern(Player player, Location loc) {
		Game game = DvZ.instance.getPlayerGame(player.getName());

		if(game.enderPortal!=null) {
			DvZ.sendPlayerMessageFormated(player, ConfigManager.getLanguage().getString("string_portal_exists","A Portal allready exists!"));

			return false;
		} else {
			createPortal(game, player, player.getLocation());

			return true;
		}
	}

	private void createPortal(Game game, Player player, Location loc) {
		Location nloc = new Location(loc.getWorld(), loc.getBlockX()+0.5, loc.getBlockY()+10, loc.getBlockZ()+0.5);
		World w = loc.getWorld();

		createPortal(nloc);
		w.strikeLightningEffect(nloc);
		
		for(int i=0; i<10; i++) {
			ItemStack it = new ItemStack(Material.ENDER_STONE, 1);
			ItemMeta im = it.getItemMeta();
			im.setDisplayName(ConfigManager.getLanguage().getString("string_spell_disableportal","Disable Portal"));
			ArrayList<String> li = new ArrayList<String>();
			li.add(ConfigManager.getLanguage().getString("string_used_seconds","Can be used every -0- Seconds!").replace("-0-", ""+ConfigManager.getStaticConfig().getInt("spelltime_disableportal",3)));
			im.setLore(li);
			it.setItemMeta(im);

			w.dropItem(nloc, it);
		}

		player.teleport(nloc);

		player.getInventory().addItem(new ItemStack(Material.GRILLED_PORK, 64));
		player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 95000, -20));
		player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 95000, 4));


		game.enderPortal = nloc;
		game.enderActive = true;
		game.enderMan = player.getName();

		game.broadcastMessage(ConfigManager.getLanguage().getString("string_portal_create","An Enderman has created a Portal!"));
	}

	//create the portalblocks
	private void createPortal(Location loc) {
		World w = loc.getWorld();
		int x = loc.getBlockX();
		int y = loc.getBlockY()-1;
		int z = loc.getBlockZ();

		Block block = w.getBlockAt(x, y, z);
		Block block2;
		block.setType(Material.ENDER_STONE);

		for(int i=0; i<=4; i+=2) {
			block2 = block.getRelative(-1-i, 0, 0);
			block2.setType(Material.ENDER_STONE);
			for(int j=-i/2; j<=i/2; j++) {
				block2 = block.getRelative(-1-i, 0, j);
				block2.setType(Material.ENDER_STONE);
			}

			block2 = block.getRelative(1+i, 0, 0);
			block2.setType(Material.ENDER_STONE);
			for(int j=-i/2; j<=i/2; j++) {
				block2 = block.getRelative(1+i, 0, j);
				block2.setType(Material.ENDER_STONE);
			}

			block2 = block.getRelative(0, 0, -1-i);
			block2.setType(Material.ENDER_STONE);
			for(int j=-i/2; j<=i/2; j++) {
				block2 = block.getRelative(j, 0, -1-i);
				block2.setType(Material.ENDER_STONE);
			}

			block2 = block.getRelative(0, 0, 1+i);
			block2.setType(Material.ENDER_STONE);
			for(int j=-i/2; j<=i/2; j++) {
				block2 = block.getRelative(j, 0, 1+i);
				block2.setType(Material.ENDER_STONE);
			}
		}

		for(int i=-2; i<=2; i+=2) {
			for(int j=-2; j<=2; j+=2) {
				block2 = block.getRelative(i, 0, j);
				block2.setType(Material.ENDER_STONE);
			}
		}

		for(int i=-4; i<=4; i+=4) {
			for(int j=-3; j<=3; j+=3) {
				block2 = block.getRelative(i, 0, j);
				block2.setType(Material.ENDER_STONE);
			}
		}

		for(int i=-3; i<=3; i+=3) {
			for(int j=-4; j<=4; j+=4) {
				block2 = block.getRelative(i, 0, j);
				block2.setType(Material.ENDER_STONE);
			}
		}
	}
}
