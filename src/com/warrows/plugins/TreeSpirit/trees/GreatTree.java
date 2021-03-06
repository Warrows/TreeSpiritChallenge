package com.warrows.plugins.TreeSpirit.trees;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Stack;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.warrows.plugins.TreeSpirit.trees.components.SBlock;
import com.warrows.plugins.TreeSpirit.trees.components.TreeBody;

public class GreatTree
{

	protected Stack<ItemStack>	drops;
	private SBlock				heart;
	TreeBody<SBlock>			body;
	String						playerName;
	private byte				type;

	/**
	 * Constructeur
	 * 
	 * @param heart
	 * @param player
	 * @param type
	 */
	public GreatTree(Block heart, String player, byte type)
	{
		this.drops = new Stack<ItemStack>();
		this.heart = new SBlock(heart);
		this.body = new TreeBody<SBlock>();
		this.body.add(this.heart);
		this.playerName = player;
		this.type = type;
		heart.setType(Material.GLOWSTONE);
		TreesData.greatTreesByBlock.put(this.heart, this);
		TreesData.greatTreesByPlayerName.put(player, this);
		TreesData.newPlayersNames.remove(player);
		TreesData.hearts.add(this.heart);
	}

	/**
	 * constructeur permettant de cr�er un arbre en lisant un fichier plat.
	 * 
	 * @throws FileNotFoundException
	 */
	public GreatTree(String s, String playerConnecting)
			throws FileNotFoundException
	{
		String[] data = s.split("\n");
		String player = data[0].substring(data[0].indexOf(": ") + 2);
		boolean coop = Boolean.parseBoolean(data[1].substring(data[1]
				.indexOf(": ") + 2));
		if (coop)
		{
			GreatTree tree = TreesData.getGreatTree(player);
			if (tree == null)
			{
				TreesData.loadGreatTree(player);
				tree = TreesData.getGreatTree(player);
			}
			if (player == playerConnecting)
				tree.update();
			return;
		}
		this.drops = new Stack<ItemStack>();
		this.heart = new SBlock(data[1].substring(data[1].indexOf(": ") + 2));
		this.body = new TreeBody<SBlock>();
		String bodyStr = s.substring(s.indexOf("{"), s.indexOf("}"));
		for (String sBlock : bodyStr.split(","))
		{
			SBlock sb = new SBlock(sBlock);
			body.add(sb);
			TreesData.greatTreesByBlock.put(sb, this);
		}
		this.playerName = player;
		this.type = Byte
				.parseByte(data[2].substring(data[2].indexOf(": ") + 2));
		TreesData.greatTreesByPlayerName.put(player, this);
		TreesData.newPlayersNames.remove(player);
		TreesData.hearts.add(this.heart);
	}

	/**
	 * Ce toString est utilis� pour ecrire les arbres dans des fichiers plats et
	 * ainsi sauver les donn� huan-readable.
	 */
	public String toSave()
	{
		boolean coop = this instanceof GreatTreeCoop;
		String s = "";
		s += ("Founder: " + playerName + "\n");
		s += ("Heart: " + heart.toString() + "\n");
		s += ("Type: " + type + "\n");
		s += ("Coop: " + coop + "\n");
		s += ("Body: " + body + "\n");
		return s;
	}

	public String toString()
	{
		boolean coop = this instanceof GreatTreeCoop;
		String s = "";
		s += ("Founder: " + playerName + "\n");
		s += ("Heart: " + heart.toString() + "\n");
		s += ("Type: " + type + "\n");
		s += ("Coop: " + coop + "\n");
		s += ("BlocksNb: " + getScore() + "\n");
		return s;
	}

	public String getPlayerName()
	{
		return playerName;
	}

	public int getScore()
	{
		return body.sizeOf();
	}

	private double getDistMax()
	{
		return 2;// + Math.log(getScore());
	}

	private boolean isInBody(Block block)
	{
		for (SBlock bodyPart : body)
		{
			if (bodyPart.equals(new SBlock(block)))
				return true;
		}
		return false;
	}

	public boolean hasDrop(ItemStack item)
	{
		return drops.contains(item);
	}

	public void takeDrop(ItemStack item)
	{
		drops.remove(item);
	}

	public void addDrop(ItemStack item)
	{
		drops.add(item);
	}

	public void addDrops(Collection<ItemStack> items)
	{
		drops.addAll(items);
	}

	public boolean isAdjacent(Block block)
	{
		BlockFace[] directions =
		{ BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST,
				BlockFace.UP, BlockFace.DOWN };
		for (BlockFace direction : directions)
			if (isInBody(block.getRelative(direction)))
				return true;
		return false;
	}

	public boolean isAtProximity(Block block)
	{
		for (double x = -getDistMax() + 1; x < getDistMax(); x += 1)
			for (double y = -getDistMax() + 1; y < getDistMax() + 1; y += 1)
				for (double z = -getDistMax() + 1; z < getDistMax(); z += 1)
				{
					Block b = block.getLocation()
							.add(new Location(block.getWorld(), x, y, z))
							.getBlock();
					if (isInBody(b))
						return true;
				}
		return false;
	}

	public void addToBody(Block block)
	{
		TreesData.greatTreesByBlock.put(new SBlock(block), this);
		body.add(new SBlock(block));
	}

	public void removeFromBody(Block block, int drop)
	{
		TreesData.greatTreesByBlock.remove(new SBlock(block));
		body.remove(new SBlock(block));
		switch (drop)
		{
			/* drop = 0 : drop normal
			 * drop = 1 ; pas de drop
			 * drop = 2 ; drop avec shears
			 */
			default:
				for (ItemStack item : block.getDrops())
					addDrop(item);
				block.breakNaturally();
				break;
			case 1:
				block.setType(Material.AIR);break;
			case 2:
				ItemStack item = new ItemStack(block.getType(), 1,
						block.getData());
				item = block.getWorld()
						.dropItemNaturally(block.getLocation(), item)
						.getItemStack();
				addDrop(item);
				block.setType(Material.AIR);
				break;
		}
	}

	public Block getHeart()
	{
		return heart.getBukkitBlock();
	}

	public static void checkBlock(GreatTree tree, Block origin)
	{
		for (BlockFace bf : BlockFace.values())
		{
			HashSet<SBlock> toDestroy = new HashSet<SBlock>();
			SBlock sb = new SBlock(origin.getRelative(bf));
			tree.checkBlock(sb, toDestroy);
			for (SBlock sb1 : toDestroy)
			{
				tree.removeFromBody(sb1.getBukkitBlock(), 0);
			}
		}
	}

	public TreeBody<SBlock> getBody()
	{
		return body;
	}

	private boolean checkBlock(SBlock origin, HashSet<SBlock> tested)
	{
		for (BlockFace bf : BlockFace.values())
		{
			SBlock sb = new SBlock(origin.getBukkitBlock().getRelative(bf));
			if (!tested.contains(sb) && isInBody(sb.getBukkitBlock()))
			{
				if (heart.equals(sb))
				{
					tested.clear();
					return true;
				}
				tested.add(sb);
				if (checkBlock(sb, tested))
					return true;
			}
		}
		return false;
	}

	public Location getTop()
	{
		Block destination = heart.getBukkitBlock();
		while (destination.getType() != Material.AIR
				|| destination.getRelative(BlockFace.UP).getType() != Material.AIR)
			destination = destination.getRelative(BlockFace.UP);
		Location destinationLoc = destination.getLocation();
		destinationLoc.add(0.5, 1, 0.5);
		return destinationLoc;
	}

	public boolean ownedBy(Player player)
	{
		return this.getPlayerName().equals(player.getName());
	}

	public GreatTreeCoop update()
	{
		GreatTreeCoop retour = new GreatTreeCoop(this);
		return retour;
	}

	public byte getType()
	{
		return type;
	}
}