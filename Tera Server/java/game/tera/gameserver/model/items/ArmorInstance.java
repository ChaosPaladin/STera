package tera.gameserver.model.items;

import tera.gameserver.templates.ArmorTemplate;
import tera.gameserver.templates.ItemTemplate;

/**
 * Модель брони.
 * 
 * @author Ronn
 */
public final class ArmorInstance extends  GearedInstance
{
	/**
	 * @param objectId уникальный ид.
	 * @param template темплейт итема.
	 */
	public ArmorInstance(int objectId, ItemTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public boolean checkCrystal(CrystalInstance crystal)
	{
		if(crystals == null || crystal.getType() != CrystalType.ARMOR)
			return false;
		
		if(crystal.getItemLevel() > template.getItemLevel())
			return false;
		
		return crystals.hasEmptySlot();
	}

	@Override
	public ArmorInstance getArmor()
	{
		return this;
	}

	/**
	 * @return armorKind материал брони.
	 */
	public ArmorKind getArmorKind()
	{
		return getTemplate().getArmorKind();
	}
	
	@Override
	public ArmorTemplate getTemplate()
	{
		return (ArmorTemplate) template;
	}
	
	@Override
	public boolean isArmor()
	{
		return true;
	}
}
