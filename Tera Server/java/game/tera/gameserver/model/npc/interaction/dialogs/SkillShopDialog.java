package tera.gameserver.model.npc.interaction.dialogs;

import java.util.Comparator;

import rlib.util.array.Array;
import rlib.util.array.ArrayComparator;
import rlib.util.array.Arrays;
import rlib.util.table.IntKey;
import rlib.util.table.Table;
import tera.Config;
import tera.gameserver.manager.GameLogManager;
import tera.gameserver.manager.ObjectEventManager;
import tera.gameserver.manager.SkillLearnManager;
import tera.gameserver.model.MessageType;
import tera.gameserver.model.SkillLearn;
import tera.gameserver.model.inventory.Bank;
import tera.gameserver.model.inventory.Inventory;
import tera.gameserver.model.npc.Npc;
import tera.gameserver.model.playable.Player;
import tera.gameserver.model.skillengine.Skill;
import tera.gameserver.network.serverpackets.DialogPanel;
import tera.gameserver.network.serverpackets.DialogPanel.PanelType;
import tera.gameserver.network.serverpackets.HotKeyChanger;
import tera.gameserver.network.serverpackets.HotKeyChanger.ChangeType;
import tera.gameserver.network.serverpackets.SkillShopList;
import tera.gameserver.network.serverpackets.SystemMessage;
import tera.gameserver.tables.SkillTable;
import tera.gameserver.templates.SkillTemplate;

/**
 * Модель окна с изучением скилов.
 *
 * @author Ronn
 * @created 25.02.2012
 */
public class SkillShopDialog extends AbstractDialog
{
	/** сортировщик изучаемых склилов */
	private static final Comparator<SkillLearn> comparator = new ArrayComparator<SkillLearn>()
	{
		@Override
		protected int compareImpl(SkillLearn first, SkillLearn second)
		{
			return first.getMinLevel() - second.getMinLevel();
		}
	};

	/**
	 * Проверка на возможность изучения скила.
	 *
	 * @param player игрок, изучающий скил.
	 * @param currentSkills текущие изученные скилы игрока.
	 * @param learn изучаемый скил.
	 * @param showMessage отображать ли сообщение.
	 * @return можно ли его выучить.
	 */
	public static boolean isLearneableSkill(Player player, Table<IntKey, Skill> currentSkills, SkillLearn learn, boolean showMessage)
	{
		// если уровень не подходит, выходим
		if(learn.getMinLevel() > player.getLevel())
		{
			if(showMessage)
				player.sendMessage(MessageType.YOU_CANT_LEARN_SKILLS_AT_THIS_LEVEL);

			return false;
		}

		// если уже есть этот скил, выходим
		if(currentSkills.containsKey(learn.getUseId()))
		{
			if(showMessage)
				player.sendMessage("Этот скил уже изучен.");

			return false;
		}

		// если нету заменяемого, выходим
		if(learn.getReplaceId() != 0 && !currentSkills.containsKey(learn.getReplaceUseId()))
		{
			if(showMessage)
				player.sendMessage(MessageType.YOU_NEED_TO_LEARN_ANOTHER_SKILL_FRST_BEFORE_YOU_CAN_LEARN_THIS_SKILL);

			return false;
		}

		return true;
	}

	/**
	 * Создание нового диалога.
	 *
	 * @param npc нпс.
	 * @param player игрок.
	 * @return новый диалог.
	 */
	public static final SkillShopDialog newInstance(Npc npc, Player player, Bank bank, float resultTax)
	{
		SkillShopDialog dialog = (SkillShopDialog) DialogType.SKILL_SHOP.newInstance();

		dialog.npc = npc;
		dialog.player = player;
		dialog.bank = bank;
		dialog.resultTax = resultTax;

		return dialog;
	}

	/** доступные для изучения скилы */
	private Array<SkillLearn> learns;

	/** банк для отчислений */
	private Bank bank;

	/** итоговый налог */
	private float resultTax;

	public SkillShopDialog()
	{
		this.learns = Arrays.toArray(SkillLearn.class);
	}

	@Override
	public synchronized boolean apply()
	{
		// отправляем список скилов
		player.sendPacket(SkillShopList.getInstance(learns, player), true);

		return true;
	}

	@Override
	public void finalyze()
	{
		// очищаем список скилов
		learns.clear();

		super.finalyze();
	}

	@Override
	public DialogType getType()
	{
		return DialogType.SKILL_SHOP;
	}

	@Override
	public synchronized boolean init()
	{
		if(!super.init())
			return false;

		// получаем менеджера по изучению скилов
		SkillLearnManager learnManager = SkillLearnManager.getInstance();

		// получаем доступные для изучения скилы
		learnManager.addAvailableSkills(learns, player);

		// сортрируем
		learns.sort(comparator);

		// отображаем окно для изучения скилов
		player.sendPacket(DialogPanel.getInstance(player, PanelType.SKILL_LEARN), true);

		// отображаем список скилов
		player.sendPacket(SkillShopList.getInstance(learns, player), true);

		return true;
	}

	/**
	 * @param player изучающий скилы игрок.
	 */
	public final void setPlayer(Player player)
	{
		this.player = player;
	}

	/**
	 * Изучить скилл указанного ид.
	 *
	 * @param skillId ид скила.
	 * @return изучен ли скил.
	 */
	public synchronized boolean studySkill(int skillId)
	{
		// получаем игрока
		Player player = getPlayer();

		// получаем НПС
		Npc npc = getNpc();

		// если игрока или нпс нет, выходим
		if(player == null || npc == null)
			return false;

		//player.sendMessage("study skill " + (skillId + 67108864));

		// если игрок внезоны действия нпс
		if(!npc.isInRange(player, 200))
		{
			close();
			return false;
		}

		// искомый скил
		SkillLearn learn = null;

		// получаем список изучаемых скилов
		Array<SkillLearn> learns = getLearns();

		// массив всех доступных
		SkillLearn[] array = learns.array();

		// перебираем
		for(int i = 0, length = learns.size(); i < length; i++)
		{
			// изучаемый скил
			SkillLearn temp = array[i];

			// если с нужным ид
			if(temp.getId() == skillId)
			{
				// сохраняем и выходим
				learn = temp;
				break;
			}
		}

		// если нужного не нашли, выходим
		if(learn == null)
			return false;

		// текущий набор скилов игрока
		Table<IntKey, Skill> skills = player.getSkills();

		// если скил нельзя выучить, выходим
		if(!isLearneableSkill(player, skills, learn, true))
			return false;

		// получаем инвентарь игрока
		Inventory inventory = player.getInventory();

		// получаем банк для отчислений
		Bank bank = getBank();

		// пробуем изучить скил
		if(!learn(bank, inventory, player, learn, resultTax))
			return false;

		// удаляем из списка изученный скил
		learns.slowRemove(learn);

		return true;
	}

	/**
	 * @return список изучаемых скилов.
	 */
	public Array<SkillLearn> getLearns()
	{
		return learns;
	}

	/**
	 * @return банк для отчислений.
	 */
	public Bank getBank()
	{
		return bank;
	}

	/**
	 * Процесс изучения скила.
	 *
	 * @param bank банк для отчислений.
	 * @param inventory инвентарь игрока.
	 * @param player изучающий скил игрок.
	 * @param learn изучаемый скил.
	 * @param tax налог.
	 * @return успешно ли изучен.
	 */
	private boolean learn(Bank bank, Inventory inventory, Player player, SkillLearn learn, float tax)
	{
		long price = (long) (learn.getPrice() * tax);

		lock(bank);
		try
		{
			inventory.lock();
			try
			{
				// если нету денег, выходим
				if(inventory.getMoney() < price)
				{
					player.sendMessage(MessageType.YOU_DONT_HAVE_ENOUGH_GOLD_TO_LEARN_THAT_SKILL);
					return false;
				}

				// если не изучался
				if(!learn(player, learn))
					return false;

				// забираем деньги
				inventory.subMoney(price);

				// выдаем ему отчисления
				if(bank != null)
					bank.addMoney(price - learn.getPrice());

				// получаем логера игровых событий
				GameLogManager gameLogger = GameLogManager.getInstance();

				// записываем событие о покупке скила
				gameLogger.writeItemLog(player.getName() + " buy skill for " + price + " gold");

				// получаем менеджера событий
				ObjectEventManager eventManager = ObjectEventManager.getInstance();

				// уведомляем инвентарь
				eventManager.notifyInventoryChanged(player);

				return true;
			}
			finally
			{
				inventory.unlock();
			}
		}
		finally
		{
			unlock(bank);
		}
	}

	/**
	 * роцесс изучение игроком скила.
	 *
	 * @param player изучающий скил игрок.
	 * @param learn изчаемый скил.
	 * @return успешно ли изучен.
	 */
	private boolean learn(Player player, SkillLearn learn)
	{
		// получаем таблицу скилов
		SkillTable skillTable = SkillTable.getInstance();

		// получаем будущий скил
		SkillTemplate[] skill = skillTable.getSkills(learn.getClassId(), learn.getUseId());

		// если в базе его нет, выходим
		if(skill == null || skill.length < 1)
			return false;

		// если стоит конфиг на изучение только реализованных и скил не реализован, выходим
		if(Config.WORLD_LEARN_ONLY_IMPLEMENTED_SKILLS && !skill[0].isImplemented())
			return false;

		// получаем менеджера событий
		ObjectEventManager eventManager = ObjectEventManager.getInstance();

		// если скил заменяемый
		if(learn.getReplaceId() > 0)
		{
			// получаем удаляемые скилы
			SkillTemplate[] removed = skillTable.getSkills(learn.getClassId(), learn.getReplaceUseId());

			// если такие есть
			if(removed != null)
			{
				// удаляем
				player.removeSkills(removed, false);

				// отсылаем пакет для замены на панельки
				player.sendPacket(HotKeyChanger.getInstance(ChangeType.REPLACE, removed[0].getId() - 67108864, skill[0].getId() - 67108864), true);
			}
		}

		// добавляем новые скилы
		player.addSkills(skill, true);

		// отпровляем сообщение об изучении
		player.sendPacket(SystemMessage.getInstance(MessageType.YOUVE_LEARNED_SKILL_NAME).addSkillName(skill[0].getName()), true);

		// уведомляем о изучении скила
		eventManager.notifySkillLearned(player, learn);

		return true;
	}

	/**
	 * @param bank блокируемый банк.
	 */
	public void lock(Bank bank)
	{
		if(bank != null)
			bank.lock();
	}

	/**
	 * @param bank разблокируемый банк.
	 */
	public void unlock(Bank bank)
	{
		if(bank != null)
			bank.unlock();
	}
}
