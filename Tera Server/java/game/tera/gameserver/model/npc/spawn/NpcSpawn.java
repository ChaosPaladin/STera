package tera.gameserver.model.npc.spawn;

import java.util.concurrent.ScheduledFuture;

import org.w3c.dom.Node;

import rlib.geom.Coords;
import rlib.logging.Logger;
import rlib.logging.Loggers;
import rlib.util.SafeTask;
import rlib.util.VarTable;
import rlib.util.array.Array;
import rlib.util.array.Arrays;
import rlib.util.random.Random;

import tera.gameserver.manager.ExecutorManager;
import tera.gameserver.manager.RandomManager;
import tera.gameserver.model.ai.npc.ConfigAI;
import tera.gameserver.model.ai.npc.NpcAIClass;
import tera.gameserver.model.npc.Npc;
import tera.gameserver.templates.NpcTemplate;
import tera.util.Location;

/**
 * Модель спавна с новый движком АИ.
 *
 * @author Ronn
 */
public class NpcSpawn extends SafeTask implements Spawn
{
	protected static final Logger log = Loggers.getLogger(Spawn.class);

	/** темплейт нпс */
	protected NpcTemplate template;

	/** локация спавна */
	protected Location location;

	/** маршрут потрулирования */
	protected Location[] route;

	/** отспавненый нпс */
	protected Npc spawned;
	/** мертвый нпс */
	protected Npc dead;

	/** параметры для АИ */
	protected ConfigAI config;

	/** конструктор для АИ */
	protected NpcAIClass aiClass;

	/** рандоминайзер */
	protected Random random;

	/** ссылка на таск респавна */
	protected volatile ScheduledFuture<? extends Spawn> schedule;

	/** задержка в респавне */
	protected int respawnTime;
	/** рандоминайзер респавна */
	protected int randomTime;

	/** радиус спавна от точки */
	protected int minRadius;
	protected int maxRadius;

	/** остановка спавна */
	protected volatile boolean stoped;

	public NpcSpawn(NpcTemplate template, Location location, ConfigAI config, NpcAIClass aiClass)
	{
		this(null, null, template, location, 0, 0, 0, 0, config, aiClass);
	}

	public NpcSpawn(Node node, VarTable vars, NpcTemplate template, Location location, int respawnTime, int randomTime, int minRadius, int maxRadius, ConfigAI config, NpcAIClass aiClass)
	{
		this.template = template;
		this.location = location;
		this.respawnTime = respawnTime;
		this.randomTime = randomTime;
		this.minRadius = minRadius;
		this.maxRadius = maxRadius;
		this.config = config;
		this.aiClass = aiClass;

		if(node != null)
			for(Node child = node.getFirstChild(); child != null; child = child.getNextSibling())
			{
				if(child.getNodeType() != Node.ELEMENT_NODE)
					continue;

				if("route".equals(child.getNodeName()))
					parseRoute(location, child);
			}

		RandomManager manager = RandomManager.getInstance();

		this.random = manager.getNpcSpawnRandom();
	}

	@Override
	public synchronized void doDie(Npc npc)
	{
		// зануляем отспавненый нпс
		setSpawned(null);
		// запоминаем убитого
		setDead(npc);
		// запускаем отсчет респавна
		doRespawn();
	}

	/**
	 * Запуск респавна.
	 */
	public synchronized void doRespawn()
	{
		// если остановлен, не респавним
		if(isStoped())
			return;

		// если уже идет таск респавна, выходим
		if(schedule != null)
			return;

		// получаем рандомный бонус
		int randomTime = getRandomTime();

		// получаем время респа
		int respawnTime = getRespawnTime();

		// получаем исполнительного менеджера
		ExecutorManager executor = ExecutorManager.getInstance();

		// если время респа статичное
		if(randomTime == 0)
			// создаем таск со статичным временем
			schedule = executor.scheduleGeneral(this, respawnTime * 1000);
		else
			// создаем таск с рандом временем
			schedule = executor.scheduleGeneral(this, getRandom().nextInt(Math.max(0, respawnTime - randomTime), respawnTime + randomTime) * 1000);
	}

	public Random getRandom()
	{
		return random;
	}

	/**
	 * Спавн моба.
	 */
	public synchronized void doSpawn()
	{
		// если респ остановлен, выходим
		if(isStoped())
			return;

		// если уже отспавненный есть, выходим
		if(spawned != null)
			return;

		// если есть ссылка на таск респавна
		if(schedule != null)
		{
			// выключаем
			schedule.cancel(false);
			// зануляем
			schedule = null;
		}

		// получаем мертвого нпс
		Npc newNpc = getDead();

		// получаем локацию спавна
		Location location = getLocation();

		// если мертвого нпс нету
		if(newNpc == null)
		{
			// создаем нового
			newNpc = template.newInstance();
			// запоминаем спавн
			newNpc.setSpawn(this);
			// создаем АИ ему
			newNpc.setAi(aiClass.newInstance(newNpc, config));

			// точка спавна
			Location spawnLoc = null;

			// если рандоминизированная
			if(maxRadius > 0)
				// генерируем точку
				spawnLoc = Coords.randomCoords(new Location(), location.getX(), location.getY(), location.getZ(), location.getHeading() == -1? getRandom().nextInt(35000) : location.getHeading(), minRadius, maxRadius);
			else
				// иначе делаем статичную
				spawnLoc = new Location(location.getX(), location.getY(), location.getZ(), location.getHeading() == -1? getRandom().nextInt(0, 65000) : location.getHeading());

			// вносим ид континента
			spawnLoc.setContinentId(location.getContinentId());

			// спавним в мир
			newNpc.spawnMe(spawnLoc);
		}
		else
		{
			// зануляем мертвого нпс
			setDead(null);

			// переинициализиуем старого
			newNpc.reinit();

			// рассчитываем точку спавна
			Location spawnLoc = null;

			// если точка рандомная
			if(maxRadius > 0)
				// рассчитываем новую
				spawnLoc = Coords.randomCoords(newNpc.getSpawnLoc(), location.getX(), location.getY(), location.getZ(), location.getHeading() == -1? getRandom().nextInt(35000) : location.getHeading(), minRadius, maxRadius);
			else
				// берем старую
				spawnLoc = newNpc.getSpawnLoc();

			// вносим ид континента
			spawnLoc.setContinentId(location.getContinentId());

			// спавним в мир
			newNpc.spawnMe(spawnLoc);
		}

		// сохраняем отспавненное нпс
		setSpawned(newNpc);
	}

	/**
	 * @return класс аи используемого в спавне.
	 */
	public NpcAIClass getAiClass()
	{
		return aiClass;
	}

	/**
	 * @return конфиг спавневого АИ.
	 */
	public final ConfigAI getConfig()
	{
		return config;
	}

	/**
	 * @return мертвый нпс.
	 */
	public final  Npc getDead()
	{
		return dead;
	}

	@Override
	public Location getLocation()
	{
		return location;
	}

	/**
	 * @return максимальный радиус спавна от точки.
	 */
	public final int getMaxRadius()
	{
		return maxRadius;
	}

	/**
	 * @return минимальный радиус спавна от точки.
	 */
	public final int getMinRadius()
	{
		return minRadius;
	}

	/**
	 * @return рандомный респавн.
	 */
	public final int getRandomTime()
	{
		return randomTime;
	}

	/**
	 * @return время респавна.
	 */
	public final int getRespawnTime()
	{
		return respawnTime;
	}

	@Override
	public Location[] getRoute()
	{
		return route;
	}

	/**
	 * @return заспавненный нпс.
	 */
	public final Npc getSpawned()
	{
		return spawned;
	}

	/**
	 * @return темплейт спавневого нпс.
	 */
	public final NpcTemplate getTemplate()
	{
		return template;
	}

	@Override
	public final int getTemplateId()
	{
		return template.getTemplateId();
	}

	@Override
	public final int getTemplateType()
	{
		return template.getTemplateType();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(location.getX());
		result = prime * result + Float.floatToIntBits(location.getY());
		result = prime * result + Float.floatToIntBits(location.getZ());
		return result;
	}

	/**
	 * @return является ли спавн РБ.
	 */
	public final boolean isRaid()
	{
		return template.isRaid();
	}

	/**
	 * @return остановлен ли спавн.
	 */
	public final boolean isStoped()
	{
		return stoped;
	}

	protected void parseRoute(Location location, Node node)
	{
		Array<Location> points = Arrays.toArray(Location.class);

		VarTable vars = VarTable.newInstance();

		for(Node child = node.getFirstChild(); child != null; child = child.getNextSibling())
		{
			if(child.getNodeType() != Node.ELEMENT_NODE)
				continue;

			if("point".equals(child.getNodeName()))
			{
				vars.parse(child);

				points.add(new Location(vars.getFloat("x"), vars.getFloat("y"), vars.getFloat("z"), 0, location.getContinentId()));
			}
		}

		points.trimToSize();

		setRoute(points.array());
	}

	@Override
	protected void runImpl()
	{
		doSpawn();
	}

	/**
	 * @param dead мертвый нпс.
	 */
	public void setDead(Npc dead)
	{
		if(dead != null)
			dead.finalyze();

		this.dead = dead;
	}

	@Override
	public void setLocation(Location location)
	{
		this.location.set(location);
	}

	/**
	 * @param maxRadius максимальный радиус от точки.
	 */
	public final void setMaxRadius(int maxRadius)
	{
		this.maxRadius = maxRadius;
	}

	/**
	 * @param minRadius минимальный радиус от точки.
	 */
	public final void setMinRadius(int minRadius)
	{
		this.minRadius = minRadius;
	}

	/**
	 * @param config параметры АИ.
	 */
	public final void setOptions(ConfigAI config)
	{
		this.config = config;
	}

	/**
	 * @param respawnTime время респавна.
	 */
	public final void setRespawnTime(int respawnTime)
	{
		this.respawnTime = respawnTime;
	}

	public void setRoute(Location[] route)
	{
		this.route = route;
	}

	/**
	 * @param spawned отспавненный нпс.
	 */
	public final void setSpawned(Npc spawned)
	{
		this.spawned = spawned;
	}

	/**
	 * @param stoped остановлен ли респавн.
	 */
	public final void setStoped(boolean stoped)
	{
		this.stoped = stoped;
	}

	/**
	 * @param template темплейт респавневого нпс.
	 */
	public final void setTemplate(NpcTemplate template)
	{
		this.template = template;
	}


	@Override
	public void start()
	{
		// убираем флаг остановки респа
		setStoped(false);

		// респавним
		doSpawn();
	}

	@Override
	public synchronized void stop()
	{
		if(isStoped())
			return;

		// ставим флаг остановки
		setStoped(true);

		// получаем отспавненого нпс
		Npc spawned = getSpawned();

		// если такой имеется
		if(spawned != null)
		{
			// удаляем его из мира
			spawned.deleteMe();
			// запоминаем как мертвого
			setDead(spawned);
			// зануляем отспавненого
			setSpawned(null);
		}

		// если есть таск респавна
		if(schedule != null)
		{
			// обрываем его
			schedule.cancel(false);
			// зануляем
			schedule = null;
		}
	}

	@Override
	public String toString()
	{
		return "NpcSpawn  template = " + template + ", location = " + location + ", aiClass = " + aiClass;
	}
}