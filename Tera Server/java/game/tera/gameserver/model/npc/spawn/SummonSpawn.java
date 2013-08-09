package tera.gameserver.model.npc.spawn;

import java.util.concurrent.ScheduledFuture;

import rlib.logging.Logger;
import rlib.logging.Loggers;
import rlib.util.SafeTask;

import tera.gameserver.manager.ExecutorManager;
import tera.gameserver.model.Character;
import tera.gameserver.model.ai.npc.ConfigAI;
import tera.gameserver.model.ai.npc.NpcAIClass;
import tera.gameserver.model.npc.Npc;
import tera.gameserver.model.npc.summons.Summon;
import tera.gameserver.templates.NpcTemplate;
import tera.util.Location;

/**
 * @author Ronn
 */
public class SummonSpawn extends SafeTask implements Spawn
{
	private static final Logger log = Loggers.getLogger(SummonSpawn.class);

	/** шаблон сумона */
	private final NpcTemplate template;
	/** место спавна */
	private final Location location;

	/** конфиг АИ суммона */
	private final ConfigAI configAI;
	/** класс АИ суммона */
	private final NpcAIClass aiClass;

	/** время жизни суммона */
	private final int lifeTime;

	/** владелец сумона */
	private volatile Character owner;

	/** отспавненный сумон */
	private volatile Summon spawned;
	/** мертвый сумон */
	private volatile Summon dead;

	/** ссылка на задачу спавна */
	private volatile ScheduledFuture<SummonSpawn> schedule;

	public SummonSpawn(NpcTemplate template, ConfigAI configAI, NpcAIClass aiClass, int lifeTime)
	{
		this.template = template;
		this.configAI = configAI;
		this.aiClass = aiClass;
		this.lifeTime = lifeTime;
		this.location = new Location();
	}

	@Override
	public void doDie(Npc npc)
	{
		if(!npc.isSummon())
			return;

		// запоминаем убитого сумона
		setDead((Summon) npc);

		// зануляем отспавненного сумона
		setSpawned(null);

		// завершение деспавна
		despawn();
	}

	@Override
	public Location getLocation()
	{
		return location;
	}

	@Override
	public int getTemplateId()
	{
		return template.getTemplateId();
	}

	@Override
	public int getTemplateType()
	{
		return template.getTemplateType();
	}

	@Override
	public void setLocation(Location location)
	{
		this.location.set(location);
	}

	@Override
	public synchronized void start()
	{
		// получаем отспавненного сумона
		Summon spawned = getSpawned();

		// если он есть, выходим
		if(spawned != null)
		{
			log.warning(this, "found duplicate spawn!");
			return;
		}

		// получаем нового владельца
		Character owner = getOwner();

		// если его нет, выходим
		if(owner == null)
			return;

		// получаем позицию спавна
		Location location = getLocation();

		Summon summon = getDead();

		if(summon != null)
		{
			// финишируем его смерть
			summon.finishDead();

			// реинициализируем
			summon.reinit();

			// запоминаем владельца
			summon.setOwner(owner);

			// спавним
			summon.spawnMe(location);
		}
		else
		{
			// создаем нового суммона
			summon = (Summon) template.newInstance();

			// запоминаем владельца
			summon.setOwner(owner);

			// запоминаем спавн
			summon.setSpawn(this);

			// создаем новое АИ
			summon.setAi(aiClass.newInstance(summon, configAI));

			// спавним
			summon.spawnMe(location);
		}

		// обнуляем мертвого сумона
		setDead(null);

		// запроминаем отспавненного
		setSpawned(summon);

		// вносим суммона
		owner.setSummon(summon);

		// получаем исполнительного менеджера
		ExecutorManager executorManager = ExecutorManager.getInstance();

		// создаем и запоминаем задачу по деспавну
		setSchedule(executorManager.scheduleGeneral(this, lifeTime));
	}

	@Override
	public synchronized void stop()
	{
		// получаем ссылку на задачу деспавна
		ScheduledFuture<SummonSpawn> schedule = getSchedule();

		// если ссылка есть
		if(schedule != null)
		{
			// отменяем задачу
			schedule.cancel(true);
			// зануляем ссылку;
			setSchedule(null);
		}

		// получаем отспавненного суммона
		Summon spawned = getSpawned();

		// если есть такой
		if(spawned != null)
			// удаляем его
			spawned.remove();
	}

	/**
	 * @return владелец сумона.
	 */
	public Character getOwner()
	{
		return owner;
	}

	/**
	 * @param owner владелец сумона.
	 */
	public void setOwner(Character owner)
	{
		this.owner = owner;
	}

	/**
	 * @param dead мертвый сумон.
	 */
	public void setDead(Summon dead)
	{
		this.dead = dead;
	}

	/**
	 * @param spawned отспавненный суммон.
	 */
	public void setSpawned(Summon spawned)
	{
		this.spawned = spawned;
	}

	/**
	 * @return отспавненный суммон.
	 */
	public Summon getSpawned()
	{
		return spawned;
	}

	public Summon getDead()
	{
		return dead;
	}

	@Override
	protected void runImpl()
	{
		despawn();
	}

	/**
	 * Деспавн суммона.
	 */
	private synchronized void despawn()
	{
		// получаем отспавненного суммона
		Summon spawned = getSpawned();

		// если есть такой
		if(spawned != null)
			// удаляем его
			spawned.remove();

		// получаем ссылку на задачу деспавна
		ScheduledFuture<SummonSpawn> schedule = getSchedule();

		// если ссылка есть
		if(schedule != null)
		{
			// отменяем задачу
			schedule.cancel(true);
			// зануляем ссылку;
			setSchedule(null);
		}
	}

	/**
	 * @returnс сылка на задачу деспавна сумона.
	 */
	public ScheduledFuture<SummonSpawn> getSchedule()
	{
		return schedule;
	}

	/**
	 * @param schedule ссылка на задачу деспавна сумона.
	 */
	public void setSchedule(ScheduledFuture<SummonSpawn> schedule)
	{
		this.schedule = schedule;
	}

	@Override
	public Location[] getRoute()
	{
		return null;
	}
}
