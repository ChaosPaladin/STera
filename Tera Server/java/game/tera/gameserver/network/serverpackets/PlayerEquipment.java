package tera.gameserver.network.serverpackets;

import tera.gameserver.model.Character;
import tera.gameserver.model.equipment.Equipment;
import tera.gameserver.model.equipment.SlotType;
import tera.gameserver.model.items.ItemInstance;
import tera.gameserver.network.ServerPacketType;

/**
 * Серверный пакет, описывающий экиперовку игрока.
 * 
 * @author Ronn
 */
public class PlayerEquipment extends ServerPacket
{
	private static final ServerPacket instance = new PlayerEquipment();
	
	public static PlayerEquipment getInstance(Character owner)
	{
		PlayerEquipment packet = (PlayerEquipment) instance.newInstance();
		
		packet.objectId = owner.getObjectId();
		packet.subId = owner.getSubId();
		
		Equipment equipment = owner.getEquipment();
		
		equipment.lock();
		try
		{
			ItemInstance item = equipment.getItem(SlotType.SLOT_WEAPON);
			packet.weaponId = item == null ? 0 : item.getItemId();
			item = equipment.getItem(SlotType.SLOT_ARMOR);		
			packet.armorId = item == null ? 0 : item.getItemId();
			item = equipment.getItem(SlotType.SLOT_BOOTS);		
			packet.bootsId = item == null ? 0 : item.getItemId();
			item = equipment.getItem(SlotType.SLOT_GLOVES);		
			packet.glovesId = item == null ? 0 : item.getItemId();
			item = equipment.getItem(SlotType.SLOT_HAT);
			packet.hatId = item == null ? 0 : item.getItemId();
			item = equipment.getItem(SlotType.SLOT_MASK);		
			packet.maskId = item == null ? 0 : item.getItemId();
		}
		finally
		{
			equipment.unlock();
		}
		
		return packet;
	}
	
	/** обджект ид персонажа */
	private int objectId;
	/** саб ид персонажа */
	private int subId;
	/** ид оружия */
	private int weaponId;
	/** ид армора */
	private int armorId;
	/** ид ботинок */
	private int bootsId;
	/** ид перчей */
	private int glovesId;
	/** ид шапки */
	private int hatId;
	/** ид маски */
	private int maskId;

	@Override
	public ServerPacketType getPacketType()
	{
		return ServerPacketType.PLAYER_EQUIPMENT;
	}

	@Override
	protected final void writeImpl()
	{
		writeOpcode();
		writeInt(objectId);
		writeInt(subId);
		writeInt(weaponId);
		writeInt(armorId);
		writeInt(bootsId);
		writeInt(glovesId);		
		writeInt(hatId);
		writeInt(maskId);	
		writeInt(0);
		
		writeInt(0);//лифчик
		writeInt(0);
		
		writeInt(0);
		
		writeInt(0);
		writeInt(0);
		writeInt(0);
		writeInt(0);
		writeInt(0);//точка ствола
		writeInt(0);
		writeInt(0);
		writeInt(0);
		
		
	/*	45 FF 
		01 00 00 10 
		E8 03 00 00 
		11 27 00 00
		99 3A 00 00 
		9B 3A 00 00 
		9A 3A 00 00 
		00 00 00 00 
		00 00 00 00 
		00 00 00 00 

		B2 80 02 00//лифчик
		00 00 00 00

		00 00 00 00 

		00 00 00 00 
		00 00 00 00 
		00 00 00 00
		00 00 00 00 
		0C 00 00 00 //на сколько проточен ствол 
		00 00 00 00 
		00 00 00 00
		00 00 00 00
		
		*/
		
		
		
	}	
}









