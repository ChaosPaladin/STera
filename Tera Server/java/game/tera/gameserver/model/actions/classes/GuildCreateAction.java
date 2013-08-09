package tera.gameserver.model.actions.classes;

import rlib.util.array.Array;
import tera.Config;
import tera.gameserver.manager.ExecutorManager;
import tera.gameserver.model.Party;
import tera.gameserver.model.actions.ActionType;
import tera.gameserver.model.inventory.Inventory;
import tera.gameserver.model.npc.interaction.dialogs.Dialog;
import tera.gameserver.model.playable.Player;
import tera.gameserver.network.serverpackets.ActionDoned;
import tera.gameserver.network.serverpackets.ActionInvite;

/**
 * Модель запуска акшена дуэли.
 *
 * @author Ronn
 */
public class GuildCreateAction extends AbstractAction<String>
{
	@Override
	public synchronized void assent(Player player)
	{
		// получаем инициатора
		Player actor = getActor();
		// получаем название гильдии
		String guildName = getTarget();

		// получаем тип акшена
		ActionType type = getType();

		super.assent(player);

		if(!test(actor, guildName))
			return;

		// отправляем пакет
		actor.sendPacket(ActionDoned.getInstance(actor.getObjectId(), actor.getSubId(), actor.getObjectId(), actor.getSubId(), type.ordinal(), objectId), true);

		// получаем группу игрока
		Party party = player.getParty();

		// если группы нет
		if(party == null)
		{
			// сообщаем об этом и выходим
			player.sendMessage("Вы должны состоять в группе.");
			return;
		}

		// получаем список соппартийцев
		Array<Player> members = party.getMembers();

		// получаем диалог
		Dialog dialog = actor.getLastDialog();

		// если есть
		if(dialog != null)
			// закрываем
			dialog.close();

		// запускаем новый акшен
		JoinGuildAction.newInstance(actor, guildName, members, 3000).invite();
	}

	@Override
	public synchronized void cancel(Player player)
	{
		// получаем инициатора
		Player actor = getActor();
		// получаем тип акшена
		ActionType type = getType();

		// если есть инициатор
		if(actor != null)
			// отправляем ему пакет
			actor.sendPacket(ActionDoned.getInstance(actor.getObjectId(), actor.getSubId(), actor.getObjectId(), actor.getSubId(), type.ordinal(), objectId), true);

		super.cancel(player);
	}

	@Override
	public ActionType getType()
	{
		return ActionType.CREATE_GUILD;
	}

	@Override
	public void init(Player actor, String name)
	{
		this.actor = actor;
		this.target = name;
	}

	@Override
	public synchronized void invite()
	{
		// получаем инициатора
		Player actor = getActor();

		// получаем название гильдии
		String target = getTarget();

		// если его нет либо отсутствует название гильдии, выходим
		if(actor == null || target == null)
			return;

		// получаем тип акшена
		ActionType type = getType();

		// запоминаем акшен
		actor.setLastAction(this);
		// отправляем пакет акшена
		actor.sendPacket(ActionInvite.getInstance(actor.getName(), target, type.ordinal(), objectId), true);

		// получаем исполнительного менеджера
		ExecutorManager executor = ExecutorManager.getInstance();

		// запускаем таск
		setSchedule(executor.scheduleGeneral(this, 30000));
	}

	@Override
	public boolean test(Player actor, String target)
	{
		if(target == null || actor == null)
			return false;

		// проверяем название гильдии
		if(target == null || !Config.checkName(target))
		{
			actor.sendMessage("Неправильное название гильдии.");
			return false;
		}

		// проверяем уровень игрока
		if(actor.getLevel() < 8)
		{
			actor.sendMessage("У вас недостаточный уровень.");
			return false;
		}

		// проверяем наличие гильдии у игрока
		if(actor.hasGuild())
		{
			actor.sendMessage("Вы уже состоите в гильдии.");
			return false;
		}

		// получаем группу игрока
		Party party = actor.getParty();

		// если группы нет
		if(party == null)
		{
			// сообщаем об этом и выходим
			actor.sendMessage("Вы должны состоять в группе.");
			return false;
		}

		// получаем список соппартийцев
		Array<Player> members = party.getMembers();

		members.readLock();
		try
		{
			Player[] array = members.array();

			// перебираем
			for(int i = 0, length = members.size(); i < length; i++)
				// если у кого-то уже есть гильдия
				if(array[i].hasGuild())
				{
					// сообщаем и выходим
					actor.sendMessage("В вашей группе уже есть человек, состоящий в гильдии.");
					return false;
				}
		}
		finally
		{
			members.readUnlock();
		}

		// получаем инвентарь игрока
		Inventory inventory = actor.getInventory();

		// если у него не хватает денег
		if(inventory.getMoney() < 3000)
		{
			// сообщаем и выходим
			actor.sendMessage("У вас недостаточно денег.");
			return false;
		}

		return true;
	}
}
