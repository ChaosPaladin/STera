package tera.gameserver.scripts.items;

import rlib.util.table.IntKey;
import rlib.util.table.Table;
import rlib.util.table.Tables;
import tera.gameserver.manager.ObjectEventManager;
import tera.gameserver.model.MessageType;
import tera.gameserver.model.inventory.Inventory;
import tera.gameserver.model.items.ItemInstance;
import tera.gameserver.model.playable.Player;
import tera.gameserver.network.serverpackets.SystemMessage;
import tera.gameserver.tables.SkillTable;
import tera.gameserver.templates.SkillTemplate;

/**
 * Модель итемов, изучающие скилы.
 *
 * @author Ronn
 */
public class SkillLearnItem extends AbstractItemExecutor
{
	/** таблица изучаемых скилов */
	private final Table<IntKey, SkillTemplate[]> skillTable;

	public SkillLearnItem(int[] itemIds, int access)
	{
		super(itemIds, access);

		this.skillTable = Tables.newIntegerTable();

		try
		{
			skillTableInit();
		}
		catch(Exception e)
		{
			log.warning(this, e);
		}
	}

	@Override
	public void execution(ItemInstance item, Player player)
	{
		// получаем темплейт изучаемого скила
		SkillTemplate[] template = skillTable.get(item.getItemId());

		// если темплейтов нет, ыходим
		if(template == null || template.length < 1)
			return;

		// получаем первый темплейт в массиве
		SkillTemplate first = template[0];

		// если скил такой уже есть, выходим
		if(player.getSkill(first.getId()) != null)
			return;

		// получаем инвентарь игрока
		Inventory inventory = player.getInventory();

		// если такой итем удалился из него
		if(inventory != null && inventory.removeItem(item.getItemId(), 1L))
		{
			// изучаем скил
			player.addSkills(template, true);

			// отпровляем сообщение об изучении
			player.sendPacket(SystemMessage.getInstance(MessageType.YOUVE_LEARNED_SKILL_NAME).addSkillName(template[0].getName()), true);

			// отправляем пакет о использовании рецепта
			player.sendPacket(SystemMessage.getInstance(MessageType.ITEM_USE).addItem(item.getItemId(), 1), true);

			// получаем менеджера событий
			ObjectEventManager eventManager = ObjectEventManager.getInstance();

			// обновляем инвентарь
			eventManager.notifyInventoryChanged(player);
		}
	}

	/**
	 * Инициализация таблицы скилов.
	 */
	private void skillTableInit()
	{
		// получаем таблицу скилов
		SkillTable table = SkillTable.getInstance();

		// книга для изучение первого маунта
		skillTable.put(20, table.getSkills(-15, 67219975));
	}
}
