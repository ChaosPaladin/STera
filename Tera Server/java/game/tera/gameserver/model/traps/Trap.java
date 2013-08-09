package tera.gameserver.model.traps;

import java.util.concurrent.ScheduledFuture;

import rlib.geom.Coords;
import rlib.util.array.Array;
import rlib.util.pools.Foldable;
import rlib.util.pools.FoldablePool;
import rlib.util.pools.Pools;
import tera.Config;
import tera.gameserver.IdFactory;
import tera.gameserver.manager.ExecutorManager;
import tera.gameserver.model.Character;
import tera.gameserver.model.TObject;
import tera.gameserver.model.World;
import tera.gameserver.model.playable.Player;
import tera.gameserver.model.skillengine.Skill;
import tera.gameserver.network.serverpackets.CharObjectDelete;
import tera.gameserver.network.serverpackets.TrapInfo;
import tera.util.LocalObjects;

/**
 * Модель ловушки в тере.
 *
 * @author Ronn
 */
public class Trap extends TObject implements Foldable, Runnable
{
	private static final FoldablePool<Trap> pool = Pools.newConcurrentFoldablePool(Trap.class);

	/**
	 * Создание новой ловукши.
	 *
	 * @param owner создатель и владелец.
	 * @param skill скил, который создает.
	 * @param lifeTime время жизни.
	 * @param radius ражиус активации.
	 * @return новая ловука.
	 */
	public static Trap newInstance(Character owner, Skill skill, int lifeTime, int radius)
	{
		// получаем ловушку из пула
		Trap trap = pool.take();

		// если ее нету
		if(trap == null)
		{
			// получаем фабрику ид
			IdFactory idFactory = IdFactory.getInstance();

			// создаем новую
			trap = new Trap(idFactory.getNextTrapId());
		}

		// устанавливаем континент
		trap.setContinentId(owner.getContinentId());

		// спавним ловушку
		trap.spawnMe(owner, skill, lifeTime, radius);

		return trap;
	}

	/** владелец ловушки*/
	protected Character owner;

	/** атакующий скил */
	protected Skill skill;

	/** время жизни */
	protected ScheduledFuture<? extends Runnable> lifeTask;

	/** радиус активации */
	protected int radius;

	public Trap(int objectId)
	{
		super(objectId);
	}

	/**
	 * Обработка активации ловушки.
	 *
	 * @param object объект, который изменил свое положение.
	 */
	public boolean activate(TObject object)
	{
		// если объект не персонаж
		if(!object.isCharacter())
			return false;

		// получаем владельца ловушки
		Character owner = getOwner();

		// получаем цель персонажа
		Character target = object.getCharacter();

		// если цель является владельцом либо не врагом, выходим
		if(owner == null || owner == target || !owner.checkTarget(target))
			return false;

		// определяем дистанцию
		float dist = target.getGeomDistance(x, y);

		// если дистанция больше радиуса, выходим
		if(dist > radius)
			return false;

		// отменяем тайм жизни
		if(lifeTask != null)
		{
			lifeTask.cancel(false);
			lifeTask = null;
		}

		// получаем исполнительного менеджера
		ExecutorManager executor = ExecutorManager.getInstance();

		// активируем ловушку с задержкой 100 мс.
		executor.scheduleGeneral(this, 100);

		return true;
	}

	@Override
	public void addMe(Player player)
	{
		// добавляем отображения себя
		player.sendPacket(TrapInfo.getInstance(this), true);
	}

	@Override
	public synchronized void deleteMe()
	{
		if(deleted)
			return;

		// удаляем с мира
		super.deleteMe();

		// ложим в пул
		fold();
	}

	@Override
	public void finalyze()
	{
		this.owner = null;
		this.skill = null;
		this.lifeTask = null;
	}

	/**
	 * Складировать.
	 */
	public void fold()
	{
		pool.put(this);
	}

	/**
	 * @return владелец ловушки.
	 */
	public Character getOwner()
	{
		return owner;
	}

	/**
	 * @return атакующий скил.
	 */
	public Skill getSkill()
	{
		return skill;
	}

	@Override
	public int getSubId()
	{
		return Config.SERVER_TRAP_SUB_ID;
	}

	/**
	 * @return ид темплейта ловушки.
	 */
	public int getTemplateId()
	{
		return skill != null? skill.getIconId() : 0;
	}

	@Override
	public Trap getTrap()
	{
		return this;
	}

	@Override
	public boolean isTrap()
	{
		return true;
	}

	@Override
	public void reinit()
	{
		// получаем фабрику ид
		IdFactory idFactory = IdFactory.getInstance();

		this.objectId = idFactory.getNextTrapId();
	}

	@Override
	public void removeMe(Player player, int type)
	{
		// удаляем себя
		player.sendPacket(CharObjectDelete.getInstance(this), true);
	}

	@Override
	public void run()
	{
		// активирующий скил
		Skill skill = getSkill();

		// если это детонация, то юзаем скил
		if(skill != null && lifeTask == null)
			skill.useSkill(owner, x, y, z);

		// удаляем с мира
		deleteMe();
	}

	/**
	 * Спавн в мир ловушку.
	 *
	 * @param owner владелец.
	 * @param skill скил ловушки.
	 * @param lifeTime время жизни.
	 * @param radius радиус активации.
	 */
	public void spawnMe(Character owner, Skill skill, int lifeTime, int radius)
	{
		// запоминаем владельца
		this.owner = owner;
		// запоминаем скил
		this.skill = skill;
		// запоминаем ширину ловушки
		this.radius = radius;

		// спавним в мире
		spawnMe(Coords.calcX(owner.getX(), 20, owner.getHeading()), Coords.calcY(owner.getY(), 20, owner.getHeading()), owner.getZ(), 0);

		LocalObjects local = LocalObjects.get();

		// получаем набор персонажей в радиусе ловушки
		Array<Character> chars = World.getAround(Character.class, local.getNextCharList(), owner, radius);

		// получаем исполнительного менеджера
		ExecutorManager executor = ExecutorManager.getInstance();

		// если таких нет, ставим на ожидание цели
		if(chars.isEmpty())
			this.lifeTask = executor.scheduleGeneral(this, lifeTime * 1000);
		// если есть
		else
		{
			// массив целей
			Character[] array = chars.array();

			// перебираем
			for(int i = 0, length = chars.size(); i < length; i++)
			{
				Character target = array[i];

				// если персонаж враг
				if(owner.checkTarget(target))
				{
					// активируем ловушку с задержкой 100 мс.
					executor.scheduleGeneral(this, 100);
					return;
				}
			}

			// если врага нет в радиусе, ставим на ожидание
			this.lifeTask = executor.scheduleGeneral(this, lifeTime * 1000);
		}
	}
}
