package tera.gameserver.model.actions.classes;

import tera.gameserver.model.actions.ActionType;
import tera.gameserver.model.actions.dialogs.EnchantItemDialog;
import tera.gameserver.model.playable.Player;
import tera.gameserver.network.serverpackets.AppledAction;

/**
 * Модель действия по открытию диалога зачоравания вещей.
 *
 * @author Ronn
 */
public final class EnchantItemAction extends AbstractAction<Void>
{
	@Override
	public void assent(Player player)
	{
		// получам инициатора
		Player actor = getActor();

		super.assent(player);

		if(!test(actor, target))
			return;

		// создаем диалог заточки
		/*EnchantItemDialog dialog = EnchantItemDialog.newInstance(actor);

		// если не удалось инициализировать
		if(!dialog.init())
			// закрываем
			dialog.cancel(actor);*/
	}

	@Override
	public synchronized void cancel(Player player)
	{
		// получаем инициатора
		Player actor = getActor();

		// если актора нет, выходим
		if(actor == null)
		{
			log.warning(this, new Exception("not found actor"));
			return;
		}

		// зануляем акшен
		actor.setLastAction(null);

		super.cancel(player);
	}

	@Override
	public ActionType getType()
	{
		return ActionType.ENCHANT_ITEM;
	}

	@Override
	public void init(Player actor, String name)
	{
		this.actor = actor;
	}

	@Override
	public synchronized void invite()
	{
		// получаем инициатора
		Player actor = getActor();

		// если кого-то из них нету, выходим
		if(actor == null || actor.isOnMount() || actor.isFlyingPegas() || actor.hasLastActionDialog())
			return;

		// запоминаем у игрока акшен
		actor.setLastAction(this);

		ActionType type = getType();

		// отправляем соответсвующие пакеты
		actor.sendPacket(AppledAction.newInstance(actor, null, type.ordinal(), objectId), true);

		assent(actor);
	}

	@Override
	public boolean test(Player actor, Void target)
	{
		// если кого-то нет, выходим
		if(actor == null || actor.isOnMount() || actor.isFlyingPegas() || actor.hasLastActionDialog())
			return false;

		return true;
	}
}
