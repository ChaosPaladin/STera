package tera.gameserver.model.actions.dialogs;

import rlib.util.random.Random;
import tera.gameserver.model.inventory.Inventory;
import tera.gameserver.model.items.ItemInstance;
import tera.gameserver.model.playable.Player;
import tera.gameserver.network.serverpackets.DialogPanel;
import tera.gameserver.network.serverpackets.DialogPanel.PanelType;
import tera.gameserver.network.serverpackets.EnchatItemInfo;

/**
 * Модель диалога по зачарованию вещей.
 * 
 * @author Ronn
 */
public class EnchantItemDialog extends AbstractActionDialog
{
	private static final int ALKAHEST_ITEM_INDEX = 2;
	private static final int CONSUME_ITEM_INDEX = 1;
	private static final int SOURCE_ITEM_INDEX = 0;

	public static final int ITEM_COUNTER = 2;

	public static EnchantItemDialog newInstance(Player player)
	{
		EnchantItemDialog dialog = (EnchantItemDialog) ActionDialogType.ENCHANT_ITEM_DIALOG.newInstance();

		if (dialog == null)
			dialog = new EnchantItemDialog();

		dialog.actor = player;
		dialog.enemy = player;

		return dialog;
	}

	/** рандоминайзер диалога */
	private Random random;

	/** целевой затачиваемый итем */
	private ItemInstance consume;
	/** средство для заточки */
	private ItemInstance alkahest;
	/** ресурс для заточки */
	private ItemInstance source;

	@Override
	public boolean apply()
	{
		// TODO Автоматически созданная заглушка метода
		return false;
	}

	@Override
	public ActionDialogType getType()
	{
		return ActionDialogType.ENCHANT_ITEM_DIALOG;
	}

	@Override
	public synchronized boolean init()
	{
		if (super.init())
		{
			Player actor = getActor();

			actor.sendPacket(DialogPanel.getInstance(actor, PanelType.ENCHANT_ITEM), true);
			updateDialog();

			return true;
		}

		return false;
	}

	public ItemInstance getConsume()
	{
		return consume;
	}

	public void setConsume(ItemInstance consume)
	{
		this.consume = consume;
	}

	public ItemInstance getAlkahest()
	{
		return alkahest;
	}

	public void setAlkahest(ItemInstance alkahest)
	{
		this.alkahest = alkahest;
	}

	public ItemInstance getSource()
	{
		return source;
	}

	public void setSource(ItemInstance source)
	{
		this.source = source;
	}

	private void updateDialog()
	{
		System.out.println("update dialog");

		actor.sendPacket(EnchatItemInfo.getInstance(this), true);
	}

	public ItemInstance getItem(int index)
	{
		return null;
	}

	/**
	 * Ид шаблона предмета по указанному индексу ячейки.
	 * 
	 * @param index индекс ячейки.
	 * @return ид шаблона предмета.
	 */
	public int getItemId(int index)
	{
		switch (index)
		{
			case SOURCE_ITEM_INDEX:
			{
				ItemInstance source = getSource();

				if (source != null)
					return source.getItemId();

				break;
			}
			case CONSUME_ITEM_INDEX:
			{
				ItemInstance consume = getConsume();

				if (consume != null)
					return consume.getItemId();

				break;
			}
			case ALKAHEST_ITEM_INDEX:
			{
				ItemInstance alkahest = getAlkahest();

				if (alkahest != null)
					return alkahest.getItemId();

				break;
			}
		}

		return 0;
	}

	/**
	 * Уникальный ид предмета по указанному индексу ячейки.
	 * 
	 * @param index индекс ячейки.
	 * @return уникальный ид предмета.
	 */
	public int getObjectId(int index)
	{
		switch (index)
		{
			case SOURCE_ITEM_INDEX:
			{
				ItemInstance source = getSource();

				if (source != null)
					return source.getObjectId();

				break;
			}
			case CONSUME_ITEM_INDEX:
			{
				ItemInstance consume = getConsume();

				if (consume != null)
					return consume.getObjectId();

				break;
			}
			case ALKAHEST_ITEM_INDEX:
			{
				ItemInstance alkahest = getAlkahest();

				if (alkahest != null)
					return alkahest.getObjectId();

				break;
			}
		}

		return 0;
	}

	/**
	 * Кол-во необходимых предметов для заточки в ячейку по указанному индексу.
	 * 
	 * @param index индекс ячейки.
	 * @return кол-во вставляемых предметов.
	 */
	public int getNeedItemCount(int index)
	{
		switch (index)
		{
			case SOURCE_ITEM_INDEX:
				return 1;
			case CONSUME_ITEM_INDEX:
				return 1;
			case ALKAHEST_ITEM_INDEX:
			{
				ItemInstance source = getSource();

				if (source != null)
					return source.getExtractable();

				break;
			}
		}

		return 0;
	}

	/**
	 * Является ли ячейка с указанным индексом ,ячейкой для затачиваемого предмета.
	 * 
	 * @param index индекс ячейки.
	 * @return является ли ячейка с указанным индексом, ячейкой для затачиваемого предмета.
	 */
	public boolean isEnchantItem(int index)
	{
		switch (index)
		{
			case SOURCE_ITEM_INDEX:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Добавление предмета в диалог.
	 * 
	 * @param index индекс ячейки.
	 * @param objectId уникальный ид предмета.
	 * @param itemId ид шаблона предмета.
	 */
	public void addItem(int index, int objectId, int itemId)
	{
		Player actor = getActor();

		switch (index)
		{
			case SOURCE_ITEM_INDEX:
			{
				if (getConsume() != null || getAlkahest() != null)
				{
					actor.sendMessage("Нужно очистить используемый предмет и alkshest.");
					return;
				}

				ItemInstance source = findItem(objectId, itemId);

				if (source == null)
				{
					actor.sendMessage("Не найден такой предмет.");
					return;
				}

				if (source.getExtractable() < 1)
				{
					actor.sayMessage("Этот предмет нельзя зачаровывать.");
					return;
				}

				setSource(source);
				updateDialog();
				break;
			}
			case CONSUME_ITEM_INDEX:
			{
				ItemInstance source = getSource();

				if (source == null)
				{
					actor.sendMessage("Не указан предмет для зачарования.");
					return;
				}

				if (getAlkahest() != null)
				{
					actor.sendMessage("Нужно очистить alkshest.");
					return;
				}

				ItemInstance consume = findItem(objectId, itemId);

				if (consume == null)
				{
					actor.sendMessage("Не найден такой предмет.");
					return;
				}

				if (source.getExtractable() != consume.getExtractable())
				{
					actor.sendMessage("Этот предмет не подходит по уровню зачарования.");
					return;
				}

				setConsume(consume);
				updateDialog();
				break;
			}
			case ALKAHEST_ITEM_INDEX:
			{
				ItemInstance source = getSource();

				if (source == null || getConsume() == null)
				{
					actor.sendMessage("Заполните остальные ячейки.");
					return;
				}

				ItemInstance alkahest = findItem(objectId, itemId);

				if (alkahest == null)
				{
					actor.sendMessage("Не найден такой предмет.");
					return;
				}

				if (alkahest.getItemCount() < source.getExtractable())
				{
					actor.sendMessage("Недостаточное кол-во.");
					return;
				}

				setAlkahest(alkahest);
				updateDialog();
			}
		}
	}

	/**
	 * Поиск предмета.
	 * 
	 * @param objectId уникальный ид предмета.
	 * @param itemId ид шаблона прдемета.
	 * @return предмет в инвенторе.
	 */
	public ItemInstance findItem(int objectId, int itemId)
	{
		Player actor = getActor();

		if (actor == null)
			return null;

		Inventory inventory = actor.getInventory();

		if (objectId != 0)
			return inventory.getItemForObjectId(objectId);

		return inventory.getItemForItemId(itemId);
	}
}
