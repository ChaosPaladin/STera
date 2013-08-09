package tera.gameserver.events.auto;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;

import rlib.concurrent.Locks;
import rlib.util.SafeTask;
import rlib.util.Synchronized;
import rlib.util.array.Array;
import rlib.util.array.Arrays;
import rlib.util.table.IntKey;
import rlib.util.table.Table;
import rlib.util.table.Tables;
import tera.gameserver.events.Event;
import tera.gameserver.events.EventPlayer;
import tera.gameserver.events.EventState;
import tera.gameserver.events.NpcInteractEvent;
import tera.gameserver.events.Registered;
import tera.gameserver.manager.EventManager;
import tera.gameserver.manager.ExecutorManager;
import tera.gameserver.manager.ObjectEventManager;
import tera.gameserver.model.Character;
import tera.gameserver.model.TObject;
import tera.gameserver.model.World;
import tera.gameserver.model.listeners.DeleteListener;
import tera.gameserver.model.listeners.DieListener;
import tera.gameserver.model.listeners.TerritoryListener;
import tera.gameserver.model.npc.Npc;
import tera.gameserver.model.npc.interaction.Link;
import tera.gameserver.model.playable.Player;
import tera.gameserver.model.skillengine.Skill;
import tera.gameserver.model.skillengine.StatType;
import tera.gameserver.model.skillengine.funcs.Func;
import tera.gameserver.model.skillengine.funcs.stat.MathFunc;
import tera.gameserver.model.territory.Territory;
import tera.gameserver.tables.TerritoryTable;
import tera.gameserver.tables.WorldZoneTable;

/**
 * Базовая модель авто ивента.
 *
 * @author Ronn
 */
public abstract class AbstractAutoEvent extends SafeTask implements Event, Synchronized, NpcInteractEvent, Registered, TerritoryListener, DieListener, DeleteListener
{
	/** блокировщик движения игроков */
	private static final Func RUN_LOCKER = new MathFunc(StatType.RUN_SPEED, 0x50, null, null)
	{
		public float calc(Character attacker, Character attacked, Skill skill, float val)
		{
			return 0;
		}
	};

	/** синхронизатор */
	private final Lock lock;

	/** таблица участников */
	private final Table<IntKey, EventPlayer> players;

	/** ожидающие участия игроки */
	private final Array<Player> prepare;

	/** активные участники */
	private final Array<Player> activePlayers;

	/** территория ивента */
	private final Territory eventTerritory;

	/** ссылка на таск ивента */
	protected ScheduledFuture<? extends AbstractAutoEvent> schedule;

	/** статус ивента */
	protected EventState state;

	/** счетчик времени */
	protected int time;

	/** запущен ли ивент */
	protected boolean started;

	protected AbstractAutoEvent()
	{
		this.lock = Locks.newLock();
		this.prepare = Arrays.toArray(Player.class);
		this.activePlayers = Arrays.toArray(Player.class);

		// получаем таблицу территорий
		TerritoryTable territoryTable = TerritoryTable.getInstance();

		this.eventTerritory = territoryTable.getTerritory(getTerritoryId());
		this.players = Tables.newIntegerTable();
	}

	/**
	 * добавление игрока в активные.
	 */
	public final void addActivePlayer(Player player)
	{
		activePlayers.add(player);
	}

	@Override
	public void addLinks(Array<Link> links, Npc npc, Player player){}

	/**
	 * Очистить территорию от левых игроков.
	 */
	protected final void clearTerritory()
	{
		if(eventTerritory == null)
			return;

		// пролучам таблицу регионов
		WorldZoneTable worldZoneTable = WorldZoneTable.getInstance();

		// получаем список игроков на территории
		Array<TObject> objects = eventTerritory.getObjects();

		// получаем массив объектов на территории
		TObject[] objs = objects.array();

		objects.writeLock();
		try
		{
			// перебираем объекты на территории
			for(int i = 0, length = objects.size(); i < length; i++)
			{
				// получаем объект
				TObject object = objs[i];

				// если объект не игрок, пропускаем
				if(!object.isPlayer())
					continue;

				// получаем игрока
				Player player = object.getPlayer();

				// если этот игрок не является участником
				if(!players.containsKey(player.getObjectId()))
				{
					// телепортируем его на ближайший респ
					player.teleToLocation(worldZoneTable.getRespawn(player));

					// обновляем счетчики
					i--;
					length--;
				}
			}
		}
		finally
		{
			objects.writeUnlock();
		}
	}

	protected void finishedState(){}

	protected void finishingState(){}

	/**
	 * @return список аткивных игроков.
	 */
	public Array<Player> getActivePlayers()
	{
		return activePlayers;
	}

	/**
	 * @return территория ивента.
	 */
	public final Territory getEventTerritory()
	{
		return eventTerritory;
	}

	/**
	 * @return максимальный уровень для участия.
	 */
	protected int getMaxLevel()
	{
		return 0;
	}

	/**
	 * @return минимальный уровень для участия.
	 */
	protected int getMinLevel()
	{
		return 0;
	}

	/**
	 * @return таблица участников.
	 */
	public final Table<IntKey, EventPlayer> getPlayers()
	{
		return players;
	}

	/**
	 * @return список зарегестрированных.
	 */
	public final Array<Player> getPrepare()
	{
		return prepare;
	}

	/**
	 * @return время на регистрацию игроков.
	 */
	protected int getRegisterTime()
	{
		return 0;
	}

	/**
	 * @return стадия ивента.
	 */
	protected final EventState getState()
	{
		return state;
	}

	/**
	 * @return ид ивент территории.
	 */
	protected int getTerritoryId()
	{
		return 0;
	}

	@Override
	public boolean isAuto()
	{
		return true;
	}

	/**
	 * @return нужно ли при текущей стадии смотреть на убийства.
	 */
	protected boolean isCheckDieState()
	{
		return false;
	}

	/**
	 * @return нужно ли при текущей стадии ивента проверять зону.
	 */
	protected boolean isCheckTerritoryState()
	{
		return false;
	}

	/**
	 * @return запущен ли ивент.
	 */
	public final boolean isStarted()
	{
		return started;
	}

	@Override
	public final void lock()
	{
		lock.lock();
	}

	/**
	 * Блокер движения.
	 */
	protected void lockMove(Player player)
	{
		RUN_LOCKER.addFuncTo(player);
	}

	/**
	 * обработка удаление из мира указанного игрока.
	 *
	 * @param player удаляемый игрок.
	 */
	protected void onDelete(Player player){}

	@Override
	public void onDelete(TObject object)
	{
		// если удаляемый объект не игрок, выходим
		if(!object.isPlayer())
			return;

		// получаем игрока
		Player player = object.getPlayer();

		// если игрок не на ивенте, выходим
		if(!player.isEvent())
			return;

		onDelete(player);
	}

	@Override
	public void onDie(Character killer, Character killed)
	{
		// если убитый не игрок или не та стадия ивента, то выходим
		if(!isCheckDieState() || !killed.isPlayer())
			return;

		// получаем игрока
		Player player = killed.getPlayer();

		// если игрок не является участником ивента, выходим
		if(!player.isEvent() || !players.containsKey(killed.getObjectId()))
			return;

		onDie(player, killer);
	}

	/**
	 * Обработка убийства игрока.
	 *
	 * @param killed убитый игрок.
	 * @param killer убийка игрока.
	 */
	protected void onDie(Player killed, Character killer){}

	/**
	 * Обработка входа левого игрока в ивент зону.
	 *
	 * @param player вошедший игрок.
	 */
	protected void onEnter(Player player){}

	@Override
	public void onEnter(Territory territory, TObject object)
	{
		// если это не нужная территория, либо не та стадия ивента, либо это не игрок, либо это не участник, то выходим
		if(territory != eventTerritory || !isCheckTerritoryState() || !object.isPlayer() || players.containsKey(object.getObjectId()))
			return;

		// обрабатываем вход игрока
		onEnter(object.getPlayer());
	}

	/**
	 * Обработка выхода левого игрока из ивент зоны.
	 *
	 * @param player вышедший игрок.
	 */
	protected void onExit(Player player){}

	@Override
	public void onExit(Territory territory, TObject object)
	{
		// если это не нужная территория, либо не та стадия ивента, либо это не игрок, либо это не участник, то выходим
		if(territory != eventTerritory || !isCheckTerritoryState() || !object.isPlayer() || !players.containsKey(object.getObjectId()))
			return;

		// обрабатываем выход с ивент зоны
		onExit(object.getPlayer());
	}

	@Override
	public boolean onLoad()
	{
		return true;
	}

	@Override
	public boolean onReload()
	{
		return true;
	}

	@Override
	public boolean onSave()
	{
		return true;
	}

	protected void prepareBattleState(){}

	protected void prepareEndState(){}

	protected void prepareStartState(){}

	@Override
	public boolean registerPlayer(Player player)
	{
		lock();
		try
		{
			// если ивент не запущен
			if(!isStarted())
			{
				player.sendMessage("Ивент не запущен.");
				return false;
			}

			// если идет стадия не регистрации
			if(getState() != EventState.REGISTER)
			{
				player.sendMessage("Время регистрации вышло.");
				return false;
			}

			// если игрок не подходит по уровню
			if(player.getLevel() > getMaxLevel() || player.getLevel() < getMinLevel())
			{
				player.sendMessage("Вы не подходите по уровню.");
				return false;
			}

			// получаем список зарегестрированных
			Array<Player> prepare = getPrepare();

			// если игрок уже зарегестрирован
			if(prepare.contains(player))
			{
				player.sendMessage("Вы уже зарегестрированы.");
				return false;
			}

			// если игрок мертв
			if(player.isDead())
			{
				player.sendMessage("Вы мертвы.");
				return false;
			}

			// если игрок в дуэли
			if(player.hasDuel())
			{
				player.sendMessage("Вы находитесь в дуэли.");
				return false;
			}

			// добавляем в зарегестрированные
			prepare.add(player);

			// ставим флаг что на ивенте
			player.setEvent(true);
			// уведомляем
			player.sendMessage("Вы зарегестрированы.");

			return true;
		}
		finally
		{
			unlock();
		}
	}

	/**
	 * Отправка анонса о регистрации.
	 */
	protected void registerState()
	{
		World.sendAnnounce("До старта ивента осталось " + time + " минут(а)");
		World.sendAnnounce("Зарегистрировано " + prepare.size() + " участника(ов)");
		World.sendAnnounce("Регистрация у НПС [Mystel]");

		time--;

		if(time == 0)
			setState(EventState.PREPARE_START);
	}

	/**
	 * Удаление игрока из активных.
	 */
	public final void removeActivePlayer(Object player)
	{
		activePlayers.fastRemove(player);
	}

	/**
	 * удаление из таблицы участников игрока.
	 *
	 * @param objectId ид игрока.
	 * @return удаляемый игрок.
	 */
	public final EventPlayer removeEventPlayer(int objectId)
	{
		return players.remove(objectId);
	}

	@Override
	protected void runImpl()
	{
		lock();
		try
		{
			switch(getState())
			{
				case REGISTER: registerState(); break;
				case PREPARE_START: prepareStartState(); break;
				case PREPARE_BATLE: prepareBattleState(); break;
				case RUNNING: runningState(); break;
				case PREPARE_END: prepareEndState(); break;
				case FINISHING: finishingState(); break;
				case FINISHED: finishedState();
			}
		}
		finally
		{
			unlock();
		}
	}

	protected void runningState(){}

	/**
	 * @param запущен ли ивент.
	 */
	public final void setStarted(boolean started)
	{
		this.started = started;
	}

	/**
	 * @param state стадия ивента.
	 */
	protected final void setState(EventState state)
	{
		this.state = state;
	}

	@Override
	public boolean start()
	{
		lock();
		try
		{
			// если запущен, выходим
			if(isStarted())
				return false;

			// добавляемся на прослушку территории ивента
			if(eventTerritory != null)
				eventTerritory.addListener(this);

			// получаем менеджера событий
			ObjectEventManager objectEventManager = ObjectEventManager.getInstance();

			// добавляемся на прослушку удаления объектов
			objectEventManager.addDeleteListener(this);
			// добавляемся на прослушку смертей
			objectEventManager.addDieListener(this);

			// обновляем время регистрации
			time = getRegisterTime();

			// получаем ивент менеджер
			EventManager eventManager = EventManager.getInstance();

			// уведомляе о запуске
			eventManager.start(this);

			World.sendAnnounce("Запущен автоматический ивент \"" + getName() + "\"");

			// ставим флаг запущенности
			setStarted(true);
			// ставим стадию регистрации
			setState(EventState.REGISTER);

			// получаем исполнительный менеджер
			ExecutorManager executor = ExecutorManager.getInstance();

			// запускаем задание регистрации
			schedule = executor.scheduleGeneralAtFixedRate(this, 60000, 60000);

			return true;
		}
		finally
		{
			unlock();
		}
	}

	@Override
	public boolean stop()
	{
		lock();
		try
		{
			// если не запущен, выходим
			if(!isStarted())
				return false;

			// очищаем таблицу участников
			players.clear();
			activePlayers.clear();

			// удаляемся с прослушки территории
			if(eventTerritory != null)
				eventTerritory.removeListener(this);

			// получаем менеджера событий
			ObjectEventManager objectEventManager = ObjectEventManager.getInstance();

			// удаляемся с прослушки удаляемых объектов
			objectEventManager.removeDeleteListener(this);
			// удаляемся с прослушки смертей
			objectEventManager.removeDieListener(this);

			World.sendAnnounce("Ивент \"" + getName() + "\" завершен.");

			// ставим флаг выключенности
			setStarted(false);
			// ставим стадию финиша
			setState(EventState.FINISHED);

			return true;
		}
		finally
		{
			unlock();
		}
	}

	@Override
	public final void unlock()
	{
		lock.unlock();
	}

	/**
	 * Разблокер движения.
	 */
	protected void unlockMove(Player player)
	{
		RUN_LOCKER.removeFuncTo(player);
	}

	@Override
	public boolean unregisterPlayer(Player player)
	{
		lock();
		try
		{
			// если ивент не запущен
			if(!isStarted())
			{
				player.sendMessage("Ивент не запущен.");
				return false;
			}

			// если не стадия регистрации
			if(getState() != EventState.REGISTER)
			{
				player.sendMessage("Время регистрации вышло.");
				return false;
			}

			// получаем список зарегестрированных
			Array<Player> prepare = getPrepare();

			// если игрока нет
			if(!prepare.contains(player))
			{
				player.sendMessage("Вы не зарегестрированы.");
				return false;
			}

			// удаляем из списка зарегестрированных
			prepare.fastRemove(player);

			// убераем флаг ивента
			player.setEvent(false);
			// отправляем сообщение
			player.sendMessage("Вы отрегестрированы.");

			return false;
		}
		finally
		{
			unlock();
		}
	}
}
