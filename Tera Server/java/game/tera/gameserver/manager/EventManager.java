package tera.gameserver.manager;

import rlib.logging.Logger;
import rlib.logging.Loggers;
import rlib.util.SafeTask;
import rlib.util.array.Array;
import rlib.util.array.Arrays;
import rlib.util.random.Random;
import rlib.util.random.Randoms;
import rlib.util.table.Table;
import rlib.util.table.Tables;
import tera.Config;
import tera.gameserver.events.Event;
import tera.gameserver.events.EventType;
import tera.gameserver.events.NpcInteractEvent;
import tera.gameserver.events.Registered;
import tera.gameserver.model.npc.Npc;
import tera.gameserver.model.npc.interaction.Link;
import tera.gameserver.model.playable.Player;

/**
 * Менеджер ивентов.
 *
 * @author Ronn
 * @created 04.03.2012
 */
public final class EventManager
{
	private static final Logger log = Loggers.getLogger(EventManager.class);

	private static EventManager instance;

	public static EventManager getInstance()
	{
		if(instance == null)
			instance = new EventManager();

		return instance;
	}

	/** рандоминайзер */
	private final Random rand;

	/** обработчик запусков ивентов */
	private final SafeTask NEXT_EVENT = new SafeTask()
	{
		@Override
		protected void runImpl()
		{
			// получаем случайный автоматический ивент
			Event event = autoable.get(rand.nextInt(0, autoable.size() - 1));

			// если ивент есть или уже запущен или этот неудалось запустить
			if(event == null || runAutoEvent != null || !event.start())
			{
				// получаем исполнительный менеджер
				ExecutorManager executor = ExecutorManager.getInstance();

				// запускаем новый таймер
				executor.scheduleGeneral(this, rand.nextInt(Config.EVENT_MIN_TIMEOUT, Config.EVENT_MAX_TIMEOUT) * 60 * 1000);
			}
		}
	};

	/** таблица всех ивентов */
	private final Table<String, Event> nameEvents;
	/** таблица всех ивентов с регистрацией */
	private final Table<String, Registered> registeredEvents;

	/** список всех доступных ивентов */
	private final Array<Event> events;
	/** список автозапускаемых ивентов */
	private final Array<Event> autoable;
	/** ивенты работающие с нпс */
	private final Array<NpcInteractEvent> npcInteractEvents;

	/** запущенный авто ивент */
	private volatile Event runAutoEvent;

	private EventManager()
	{
		rand = Randoms.newRealRandom();
		nameEvents = Tables.newObjectTable();
		registeredEvents = Tables.newObjectTable();
		events = Arrays.toArray(Event.class);
		autoable = Arrays.toArray(Event.class);
		npcInteractEvents = Arrays.toArray(NpcInteractEvent.class);

		// перебираем все доступные ивенты
		for(EventType type : EventType.values())
		{
			// получаем инстанс ивента
			Event example = type.get();

			// если загрузить его не цдалось, пропускаем
			if(example == null || !example.onLoad())
				continue;

			// добавляем в список всех ивентов доступных
			events.add(example);

			// если ивент автоматический
			if(example.isAuto())
				// добавляем в список автоматических
				autoable.add(example);

			// добавляем в таблицу по имени ивент
			nameEvents.put(example.getName(), example);

			// если ивент с регистрацией
			if(example instanceof Registered)
				// вносим в соответствующую таблицу
				registeredEvents.put(example.getName(), (Registered) example);

			// если ивент связан с НПС
			if(example instanceof NpcInteractEvent)
				// добавляем в нужный список
				npcInteractEvents.add((NpcInteractEvent) example);
		}

		// сообщаем о кол-ве ивентов
		log.info("loaded " + events.size() + " events.");

		// если есть авто ивенты
		if(!autoable.isEmpty())
		{
			// получаем исполнительный менеджер
			ExecutorManager executor = ExecutorManager.getInstance();

			// рассчитываем через сколько он начнетсмя
			int time = rand.nextInt(Config.EVENT_MIN_TIMEOUT, Config.EVENT_MAX_TIMEOUT) * 60 * 1000;

			// запускаем таймер
			executor.scheduleGeneral(NEXT_EVENT, time);

			// уведомляем
			log.info("the nearest event in " + (time / 1000 / 60) + " minutes.");
		}
	}

	/**
	 * Получение ссылок для диалога с НПС.
	 *
	 * @param links контейнер ссылок.
	 * @param npc нпс с которым говорит игрок.
	 * @param player игрок.
	 */
	public void addLinks(Array<Link> links, Npc npc, Player player)
	{
		// получаем массив ивентов
		NpcInteractEvent[] array = npcInteractEvents.array();

		// перебираем ивенты
		for(int i = 0, length = npcInteractEvents.size(); i < length; i++)
			array[i].addLinks(links, npc, player);
	}

	/**
	 * @param event завершенный ивент.
	 */
	public void finish(Event event)
	{
		if(event == null)
			return;

		// если ивент автоматический
		if(event.isAuto())
		{
			// зануляем ссылку на запущенный автоматический ивент
			setRunAutoEvent(null);

			// получаем исполнительный менеджер
			ExecutorManager executor = ExecutorManager.getInstance();

			// запускаем таймер на запуск нового ивента
			executor.scheduleGeneral(NEXT_EVENT, rand.nextInt(Config.EVENT_MIN_TIMEOUT, Config.EVENT_MAX_TIMEOUT) * 60 * 1000);
		}
	}

	/**
	 * Регистрация игрока на ивент.
	 *
	 * @param eventName название ивента.
	 * @param player игрок.
	 */
	public void registerPlayer(String eventName, Player player)
	{
		// получаем ивент по названи.
		Registered event = registeredEvents.get(eventName);

		// если такой нашли
		if(event != null)
			// пробуем зарегистрировать
			event.registerPlayer(player);
	}

	/**
	 * @param runAutoEvent работающий авто ивент.
	 */
	public void setRunAutoEvent(Event runAutoEvent)
	{
		this.runAutoEvent = runAutoEvent;
	}

	/**
	 * Оповещение о старте ивента.
	 *
	 * @param event стартовавший ивент.
	 */
	public void start(Event event)
	{
		// если ивент автоматический
		if(event.isAuto())
			setRunAutoEvent(event);
	}

	/**
	 * Запуск ивента с нужным названием.
	 *
	 * @param eventName название ивента.
	 */
	public void startEvent(String eventName)
	{
		// получаем ивент по названию
		Event event = nameEvents.get(eventName);

		// если этого ивента нету либо он автоматический и сейчас запущен аналогичный, выходим
		if(event == null || event.isAuto() && runAutoEvent != null)
			return;

		// запускаем
		event.start();
	}

	/**
	 * Остановка ивента с нужным названием.
	 *
	 * @param eventName название ивента.
	 */
	public void stopEvent(String eventName)
	{
		// получаем ивент по названи.
		Event event = nameEvents.get(eventName);

		// если не нашли, выходим
		if(event == null)
			return;

		// останавливаем
		event.stop();
	}

	/**
	 * Отмена регистрации игрока на ивент.
	 *
	 * @param eventName название ивента.
	 * @param player игрок.
	 */
	public void unregisterPlayer(String eventName, Player player)
	{
		// получаем ивент по названию
		Registered event = registeredEvents.get(eventName);

		// если такой ивент нашли
		if(event != null)
			// отрегистрируем игрока
			event.unregisterPlayer(player);
	}
}
