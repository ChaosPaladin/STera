package tera.gameserver.model.ai.npc.thinkaction;

import org.w3c.dom.Node;

import rlib.util.Rnd;
import rlib.util.VarTable;
import rlib.util.array.Array;

import tera.gameserver.manager.PacketManager;
import tera.gameserver.model.Character;
import tera.gameserver.model.NpcAIState;
import tera.gameserver.model.World;
import tera.gameserver.model.WorldRegion;
import tera.gameserver.model.ai.npc.ConfigAI;
import tera.gameserver.model.ai.npc.MessagePackage;
import tera.gameserver.model.ai.npc.NpcAI;
import tera.gameserver.model.npc.Npc;
import tera.gameserver.network.serverpackets.NotifyCharacter.NotifyType;
import tera.gameserver.tables.MessagePackageTable;
import tera.util.LocalObjects;

/**
 * Базовая модель генератора действий в бою.
 *
 * @author Ronn
 */
public class DefaultBattleAction extends AbstractThinkAction
{
	/** пакет сообщений при смене цели боя */
	protected final MessagePackage switchTargetMessages;

	/** радиус ведения боя */
	protected final int battleMaxRange;
	/** радиус реакции НПС на врагов */
	protected final int reactionMaxRange;
	/** уровень критического хп */
	protected final int criticalHp;
	/** шанс перехода в ярость */
	protected final int rearRate;
	/** шанс перехода в убегание */
	protected final int runAwayRate;
	/** максимум для скольких нпс цель может быть топ агром */
	protected final int maxMostHated;

	public DefaultBattleAction(Node node)
	{
		super(node);

		try
		{
			// парсим параметры
			VarTable vars = VarTable.newInstance(node, "set", "name", "val");

			this.battleMaxRange = vars.getInteger("battleMaxRange", ConfigAI.DEFAULT_BATTLE_MAX_RANGE);
			this.reactionMaxRange = vars.getInteger("reactionMaxRange", ConfigAI.DEFAULT_REACTION_MAX_RANGE);

			this.criticalHp = vars.getInteger("criticalHp", ConfigAI.DEFAULT_CRITICAL_HP);
			this.rearRate = vars.getInteger("rearRate", ConfigAI.DEFAULT_REAR_RATE);
			this.runAwayRate = vars.getInteger("runAwayRate", ConfigAI.DEFAULT_RUN_AWAY_RATE);
			this.maxMostHated = vars.getInteger("maxMostHated", ConfigAI.DEFAULT_MAX_MOST_HATED);

			// получаем таблицу сообщений
			MessagePackageTable messageTable = MessagePackageTable.getInstance();

			this.switchTargetMessages = messageTable.getPackage(vars.getString("switchTargetMessages", ConfigAI.DEFAULT_SWITCH_TARGET_MESSAGES));
		}
		catch(Exception e)
		{
			log.warning(this, e);
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * @return радиус ведения боя.
	 */
	protected final int getBattleMaxRange()
	{
		return battleMaxRange;
	}

	/**
	 * @return % хп, который является критическим.
	 */
	protected final int getCriticalHp()
	{
		return criticalHp;
	}

	/**
	 * @return максимальное число топ агров для цели.
	 */
	protected final int getMaxMostHated()
	{
		return maxMostHated;
	}

	/**
	 * @return радиус реакции НПС на врагов.
	 */
	protected final int getReactionMaxRange()
	{
		return reactionMaxRange;
	}

	/**
	 * @return шанс входа в состояние ярости.
	 */
	protected final int getRearRate()
	{
		return rearRate;
	}

	/**
	 * @return шанс входа в состояние убегания.
	 */
	protected final int getRunAwayRate()
	{
		return runAwayRate;
	}

	@Override
	public <A extends Npc> void think(NpcAI<A> ai, A actor, LocalObjects local, ConfigAI config, long currentTime)
	{
		// если нпс мертв
		if(actor.isDead())
		{
			// очищаем задания
			ai.clearTaskList();
			// очищаем агр лист
			actor.clearAggroList();
			// переводим в режим ожидания
			ai.setNewState(NpcAIState.WAIT);
			// выходим
			return;
		}

		// если нпс щас что-то делает, выходим
		if(actor.isTurner() || actor.isCastingNow())
			return;

		// если НПС был оглушен/опрокинут
		if(actor.isStuned() || actor.isOwerturned())
		{
			// если есть задачи
			if(ai.isWaitingTask())
				// отменяем их
				ai.clearTaskList();

			// выходим
			return;
		}

		// если актор вышел за пределы максимального радиуса атаки
		if(!actor.isInRangeZ(actor.getSpawnLoc(), getReactionMaxRange()))
		{
			// очищаем задания
			ai.clearTaskList();
			// очищаем агр лист
			actor.clearAggroList();
			// переводим в режим возврпщения домой
			ai.setNewState(NpcAIState.RETURN_TO_HOME);
			// отправляем иконку думания
			PacketManager.showNotifyIcon(actor, NotifyType.NOTICE_THINK);
			// обновляем время отправки иконки
			ai.setLastNotifyIcon(currentTime);
			// выходим
			return;
		}

		// получаем топовый агр
		Character mostHated = actor.getMostHated();

		// если такого нету, но актор агрессивный
		if(mostHated == null && actor.isAggressive())
		{
			// получаем текущий регион НПС
			WorldRegion region = actor.getCurrentRegion();

			// если регион есть
			if(region != null)
			{
				// получаем список персонажей
				Array<Character> charList = local.getNextCharList();

				// собираем сведения о целях вокруг
				World.getAround(Character.class, charList, actor, actor.getAggroRange());

				// получаем массив целей
				Character[] array = charList.array();

				// перебираем потенциальные цели
				for(int i = 0, length = charList.size(); i < length; i++)
				{
					// получаем потенциальную цель
					Character target = array[i];

					// если на цель возможна агрессия и цель в зоне агра
					if(ai.checkAggression(target))
						// добавляем в агр лист
						actor.addAggro(target, 1, false);
				}
			}
		}

		// еще раз пробуем получить главного агр персонажа
		mostHated = actor.getMostHated();

		// если главная цель отсутствует, выходим
		if(mostHated == null)
		{
			// очищаем задания
			ai.clearTaskList();
			// очищаем агр лист
			actor.clearAggroList();
			// переводим в режим возврпщения домой
			ai.setNewState(NpcAIState.RETURN_TO_HOME);
			// отправляем иконку думания
			PacketManager.showNotifyIcon(actor, NotifyType.NOTICE_THINK);
			// обновляем время отправки иконки
			ai.setLastNotifyIcon(currentTime);
			// выходим
			return;
		}

		// получаем текущую цель АИ
		Character target = ai.getTarget();

		// если цель выходит за радиус боевых действий
		if(mostHated.isDead() || !mostHated.isInRange(actor.getSpawnLoc(), getBattleMaxRange()))
		{
			// удаляем его с агр листа
			actor.removeAggro(mostHated);
			// очищаем задания
			ai.clearTaskList();
			// выходим
			return;
		}

		// если у НПС критический уровень хп
		if(actor.getCurrentHpPercent() < getCriticalHp())
		{
			// расчитываем шанс
			int rate = Rnd.nextInt(0, 100000);

			// если он выпал на ярость
			if(rate <= getRearRate())
			{
				// очищаем задания
				ai.clearTaskList();
				// переводим в режим ярости
				ai.setNewState(NpcAIState.IN_RAGE);
				// отправляем иконку ярости
				PacketManager.showNotifyIcon(actor, NotifyType.READ_REAR);
				// обновляем время отправки иконки
				ai.setLastNotifyIcon(currentTime);
				// выходим
				return;
			}

			// если он выпал на сбегание
			if(rate <= getRunAwayRate())
			{
				// очищаем задания
				ai.clearTaskList();
				// переводим в режим убегания
				ai.setNewState(NpcAIState.IN_RUN_AWAY);
				// выходим
				return;
			}
		}

		// если топ агр не является текущей целью АИ
		if(mostHated != target)
			// обновляем текущую цель АИ
			ai.setTarget(mostHated);

		// если есть ожидающие задания
		if(ai.isWaitingTask())
			// выполняем и если это небыло последнее задание
			if(ai.doTask(actor, currentTime, local))
				// выходим
				return;

		// если нпс щас что-то делает, выходим
		if(actor.isTurner() || actor.isCastingNow() || actor.isStuned() || actor.isOwerturned() || actor.isMoving())
			return;

		// если давно не оторбажали иконку думания
		if(currentTime - ai.getLastNotifyIcon() > 15000)
		{
			// отправляем иконку думания
			PacketManager.showNotifyIcon(actor, NotifyType.YELLOW_QUESTION);
			// обновляем время отправки иконки
			ai.setLastNotifyIcon(currentTime);
		}

		// создаем новое задание
		ai.getCurrentFactory().addNewTask(ai, actor, local, config, currentTime);

		// если есть ожидающие задания
		if(ai.isWaitingTask())
			// выполняем
			ai.doTask(actor, currentTime, local);
	}
}
