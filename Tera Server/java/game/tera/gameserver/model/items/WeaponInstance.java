package tera.gameserver.model.items;

import tera.gameserver.templates.ItemTemplate;
import tera.gameserver.templates.WeaponTemplate;

/**
 * Модель оружия.
 * 
 * @author Ronn
 */
public final class WeaponInstance extends GearedInstance
{
	/**
	 * @param objectId уник ид оружия.
	 * @param template темплейт оружия.
	 */
	public WeaponInstance(int objectId, ItemTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public boolean checkCrystal(CrystalInstance crystal)
	{
		if(crystals == null || crystal.getType() != CrystalType.WEAPON)
			return false;
		
		if(crystal.getItemLevel() > template.getItemLevel())
			return false;
		
		return crystals.hasEmptySlot();
	}
	
	@Override
	public WeaponTemplate getTemplate()
	{
		return (WeaponTemplate) template;
	}
	
	@Override
	public WeaponInstance getWeapon()
	{
		return this;
	}
	
	@Override
	public boolean isWeapon()
	{
		return true;
	}
}