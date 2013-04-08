package com.warrows.plugins.TreeSpirit.trees.blocks;

import org.bukkit.Location;
import org.bukkit.Material;

public class Heart extends TreeBlock implements Solid
{
	private static final long serialVersionUID = -5242640187336269964L;

	public Heart(Location loc)
	{
		super(loc);
		loc.getBlock().setType(Material.GLOWSTONE);
	}

	@Override
	public void addChild(Popable child)
	{
		// TODO Auto-generated method stub
		
	}
}
