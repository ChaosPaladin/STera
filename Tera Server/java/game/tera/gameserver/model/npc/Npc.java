package tera.gameserver.model.npc;

import java.util.Comparator;

import rlib.geom.Angles;
import rlib.geom.Coords;
import rlib.util.Rnd;
import rlib.util.array.Array;
import rlib.util.array.Arrays;
import rlib.util.pools.Foldable;
import rlib.util.pools.FoldablePool;
import rlib.util.pools.Pools;
import rlib.util.random.Random;

import tera.Config;
import tera.gameserver.IdFactory;
import tera.gameserver.manager.EventManager;
import tera.gameserver.manager.GeoManager;
import tera.gameserver.manager.ObjectEventManager;
import tera.gameserver.manager.PacketManager;
import tera.gameserver.manager.RandomManager;
import tera.gameserver.model.Character;
import tera.gameserver.model.EmotionType;
import tera.gameserver.model.Party;
import tera.gameserver.model.World;
import tera.gameserver.model.WorldRegion;
import tera.gameserver.model.ai.CharacterAI;
import tera.gameserver.model.geom.Geom;
import tera.gameserver.model.geom.NpcGeom;
import tera.gameserver.model.items.ItemInstance;
import tera.gameserver.model.npc.interaction.DialogData;
import tera.gameserver.model.npc.interaction.Link;
import tera.gameserver.model.npc.playable.NpcAppearance;
import tera.gameserver.model.npc.spawn.Spawn;
import tera.gameserver.model.playable.Player;
import tera.gameserver.model.quests.NpcIconType;
import tera.gameserver.model.quests.QuestData;
import tera.gameserver.model.quests.QuestType;
import tera.gameserver.model.regenerations.NpcRegenHp;
import tera.gameserver.model.regenerations.NpcRegenMp;
import tera.gameserver.model.regenerations.Regen;
import tera.gameserver.model.skillengine.Formulas;
import tera.gameserver.model.skillengine.Skill;
import tera.gameserver.model.skillengine.SkillGroup;
import tera.gameserver.model.skillengine.StatType;
import tera.gameserver.network.serverpackets.DeleteCharacter;
import tera.gameserver.network.serverpackets.NameColor;
import tera.gameserver.network.serverpackets.NpcInfo;
import tera.gameserver.network.serverpackets.NpcNotice;
import tera.gameserver.network.serverpackets.QuestNpcNotice;
import tera.gameserver.network.serverpackets.TargetHp;
import tera.gameserver.tables.SkillTable;
import tera.gameserver.taskmanager.RegenTaskManager;
import tera.gameserver.tasks.EmotionTask;
import tera.gameserver.tasks.TurnTask;
import tera.gameserver.templates.NpcTemplate;
import tera.gameserver.templates.SkillTemplate;
import tera.util.LocalObjects;
import tera.util.Location;

/**
 * Базовая модель нпс.
 *
 * @author Ronn
 */
public abstract class Npc extends Character implements Foldable
{
	/** таблица штрафа на экспу в зависимости от разницы в уровнях */
	public static final float[] PENALTY_EXP =
	{
		1F, // 0
		1F, // 1
		1F, // 2
		1F, // 3
		1F, // 4
		1F, // 5
		0.5F, // 6
		0.4F, // 7
		0.3F, // 8
		0.2F, // 9
		0.1F, // 10
		0F, // 12
	};

	public static final int INTERACT_RANGE = 200;

	/**
	 * public static final float[] PENALTY_EXP =
	{
		1F, // 0
		1F, // 1
		1F, // 2
		1F, // 3
		1F, // 4
		1F, // 5
		0.6F, // 6
		0.5F, // 7
		0.4F, // 8
		0.35F, // 9
		0.3F, // 10
		0.25F, // 11
		0.2F, // 12
		0.15F, // 13
		0.1F, // 14
		0.08F, // 15
		0.06F, // 16
		0.04F, // 17
		0.02F, // 18
		0.01F, // 19
		0F, // 20
	};
	 */

	/** сортировщик аггресоров по уровню агрессии */
	private static final Comparator<AggroInfo> AGGRO_COMPORATOR = new Comparator<AggroInfo>()
	{
		@Override
		public int compare(AggroInfo info, AggroInfo next)
		{
			if(info == null)
				return 1;

			if(next == null)
				return -1;

			return next.compareTo(info);
		}
	};

	/**
	 * Спавн объектов вокруг объекта.
	 *
	 * @param character центральный объект.
	 * @param items список объектов, которые нужно отспавнить.
	 * @param length кол-во объектов.
	 * @param radius радиус, в котором нужно отспавнить объекты.
	 */
	public static void spawnDropItems(Character character, ItemInstance[] items, int length)
	{
		if(length < 1)
			return;

		// получаем менеджера рандома
		RandomManager randManager = RandomManager.getInstance();

		// получаем рандоминайзер
		Random random = randManager.getDropItemPointRandom();

		// получаем координаты НПС.
		float x = character.getX();
		float y = character.getY();
		float z = character.getZ();

		// получаем ид континента НПС
		int continentId = character.getContinentId();

		// получаем менеджера геодаты
		GeoManager geoManager = GeoManager.getInstance();

		// перебираем итемы
		for(int i = 1; i <= length; i++)
		{
			// определяем направление
			float radians = Angles.headingToRadians(random.nextInt(0, Short.MAX_VALUE * 2));

			// оапределяем дистанцию
			int distance = random.nextInt(40, 80);

			// определяем координаты
			float newX = Coords.calcX(x, distance, radians);
			float newY = Coords.calcY(y, distance, radians);
			float newZ = geoManager.getHeight(continentId, newX, newY, z);

			// получаем итем
			ItemInstance item = items[i - 1];

			// устанавливаем ид континента
			item.setContinentId(continentId);

			// спавним итем
			item.spawnMe(newX, newY, newZ, 0);
		}
	}

	/** пул контейнеров информации об агре */
	protected final FoldablePool<AggroInfo> aggroInfoPool;

	/** аггро лист */
	protected final Array<AggroInfo> aggroList;

	/** обработчик разворота нпс */
	protected final TurnTask turnTask;

	/** спавнер */
	protected Spawn spawn;

	/** точка спавна */
	protected Location spawnLoc;

	/** таблица скилов НПС */
	protected Skill[][] skills;

	/** отсортирован ли агро лист */
	protected volatile boolean aggroSorted;

	/**
	 * @param objectId уникальный ид.
	 * @param template темплейт нпс.
	 */
	public Npc(int objectId, NpcTemplate template)
	{
		super(objectId, template);

		aggroInfoPool = Pools.newConcurrentFoldablePool(AggroInfo.class);
		aggroList = Arrays.toConcurrentArray(AggroInfo.class);

		turnTask = new TurnTask(this);

		// получаем таблицу темплейтов скилов нпс
		SkillTemplate[][] temps = template.getSkills();

		// создаем таблицу конечных скилов
		skills = new Skill[temps.length][];

		// перебираем списки темплейтов
		for(int i = 0, length = temps.length; i < length; i++)
		{
			// получаем список темплейтов группы скилов
			SkillTemplate[] list = temps[i];

			// елси их нет, пропускаем
			if(list == null)
				continue;

			// создаем экземпляры скилов по темплейтам
			skills[i] = SkillTable.create(list);

			// добалвяем
			addSkills(skills[i], false);
		}

		// получаем ормулы
		Formulas formulas = Formulas.getInstance();

		// добалвяем функции связанные с НПС
		formulas.addFuncsToNewNpc(this);

		// получаем менеджер регена
		RegenTaskManager regenManager = RegenTaskManager.getInstance();

		// добавляемся на обработку регена
		regenManager.addCharacter(this);
	}

	/**
	 * Добавление агрессии на персонажа.
	 *
	 * @param aggressor агрессор.
	 * @param aggro агр поинты.
	 * @param damage урон ли это.
	 */
	public void addAggro(Character aggressor, long aggro, boolean damage)
	{
		if(aggro < 1)
			return;

		// запоминаем у персонажа, что этот нпс на него сагрен
		aggressor.addHated(this);

		// увеличиваем силу агра
		aggro *= aggressor.calcStat(StatType.AGGRO_MOD, 1, this, null);

		// получаем аггро лист НПС
		Array<AggroInfo> aggroList = getAggroList();

		aggroList.writeLock();
		try
		{
			// получаем индекс инфо об агрессоре
			int index = aggroList.indexOf(aggressor);

			// если такого нет
			if(index < 0)
				// добавляем новый
				aggroList.add(newAggroInfo(aggressor, aggro, damage? aggro : 0));
			else
			{
				// получаем
				AggroInfo info = aggroList.get(index);

				// добавляем агр поинтов
				info.addAggro(aggro);

				// если это урон
				if(damage)
					// добавляем и очки урона
					info.addDamage(Math.min(aggro, getCurrentHp()));
			}

			// обновляем флаг отсортированности
			setAggroSorted(index == 0);
		}
		finally
		{
			aggroList.writeUnlock();
		}

		// получаем менеджер событий
		ObjectEventManager eventManager = ObjectEventManager.getInstance();

		// обновляем об изменении агрессии
		eventManager.notifyAgression(this, aggressor, aggro);
	}

	@Override
	public void addMe(Player player)
	{
		// отправляем пакет о нпс
		player.sendPacket(NpcInfo.getInstance(this, player), true);

		// если НПС в боевой стойке
		if(isBattleStanced())
			// отображаем текущую боевую стойку
			PacketManager.showBattleStance(player, this, getEnemy());

		// получаем менеджер событий
		ObjectEventManager eventManager = ObjectEventManager.getInstance();

		// уведомляем о добавлении НПС.
		eventManager.notifyAddNpc(player, this);

		super.addMe(player);
	}

	/**
	 * Рассчет выдачи экспы.
	 *
	 * @param killer убийца нпс.
	 */
	protected void calculateRewards(Character killer)
	{
		// получаем топ дэмагера
		Character top = getMostDamager();

		// если его нет, ставим на место его убийцу
		if(top == null)
			top = killer;

		// если убийца ПК, то выходим
		//if(top.isPK())
		//	return;

		// если топ сумон
		if(top.isSummon())
			// ставим на место его игрока
			top = top.getOwner();

		// если топа нет или это не игрок, выходим
		if(top == null || !top.isPlayer())
			return;

		// получаем темплейт
		NpcTemplate template = getTemplate();

		// увеличиваем экспу за нпс на рейт сервера
		int exp = (int) (template.getExp() * Config.SERVER_RATE_EXP);

		// получаем игрок из топ дэмагера
		Player player = top.getPlayer();

		// если экспа есть
		if(exp > 0)
		{
			// получаем пати
			Party party = player.getParty();

			// если пати есть
			if(party != null)
				// даем на рассчет пати
				party.addExp(exp, player, this);
			else
			{
				//итоговая награда
				float reward = exp;

				// получаем разницу в лвлах
				int diff = Math.abs(player.getLevel() - getLevel());

				// если она превышает таблицу, зануляем награду
				if(diff >= PENALTY_EXP.length)
					reward *= 0F;
				// если разница больше 5
				else if(diff > 5)
					// применяем штраф
					reward *= PENALTY_EXP[diff];

				// если есть премиум на экспу
				if(Config.ACCOUNT_PREMIUM_EXP && player.hasPremium())
					// увеличиваем с учетом премиума
					reward *= Config.ACCOUNT_PREMIUM_EXP_RATE;

				// выдаем экспу
				player.addExp((int) reward, this, getName());
			}
		}

		// если нпс имет дроп
		if(template.isCanDrop())
		{
			// получаем локальные объекты
			LocalObjects local = LocalObjects.get();

			// получаем итоговый список дропнутых итемов
			Array<ItemInstance> items = template.getDrop(local.getNextItemList(), this, player);

			// если такие есть
			if(items != null)
			{
				// получаем группу игрока
				Party party = player.getParty();

				// получаем массив итемов
				ItemInstance[] array = items.array();

				// перебираем
				for(int i = 0, length = items.size(); i < length; i++)
				{
					ItemInstance item = array[i];

					// запоминаем с кого
					item.setDropper(this);
					// запоминаем игрока
					item.setTempOwner(player);
					// запоминаем пати
					item.setTempOwnerParty(party);
				}

				// спавним дроп
				spawnDropItems(this, array, items.size());
			}
		}
	}

	/**
	 * Проверка на возможность разговора игрока с нпс.
	 *
	 * @param player игрок, который хочет взаимодействовать с нпс.
	 * @return может ли игрок взаимодействовать.
	 */
	public boolean checkInteraction(Player player)
	{
		return isInRange(player, INTERACT_RANGE);
	}

	@Override
	public boolean checkTarget(Character target)
	{
		return true;
	}

	/**
	 * Полная очистка аггр листа.
	 */
	public void clearAggroList()
	{
		// получаем аггро лист
		Array<AggroInfo> aggroList = getAggroList();

		// получаем пул
		FoldablePool<AggroInfo> pool = getAggroInfoPool();

		aggroList.writeLock();
		try
		{
			AggroInfo[] array = aggroList.array();

			// ложим пул аггро инфы
			for(int i = 0, length = aggroList.size(); i < length; i++)
			{
				// получаем инфу об агре
				AggroInfo info = array[i];

				// получаем агрессора
				Character aggressor = info.getAggressor();

				// удаляем себя из нпс, у которого есть агр на этого персонажа
				aggressor.removeHate(this);

				// складываем в пул инфо
				pool.put(info);
			}

			// очищаем список
			aggroList.clear();
		}
		finally
		{
			aggroList.writeUnlock();
		}

		// ставим флаг отсортированности
		setAggroSorted(true);
	}

	@Override
	public void decayMe(int type)
	{
		super.decayMe(type);

		// очищаем агро лист
		clearAggroList();
	}

	@Override
	public void deleteMe()
	{
		// получаем АИ
		CharacterAI ai = getAI();

		// останавливаем АИ
		if(ai != null)
			ai.stopAITask();

		super.deleteMe();
	}

	/**
	 * Увеличение счетчика убийств.
	 *
	 * @param attacker убийца.
	 */
	protected void addCounter(Character attacker)
	{
		World.addKilledNpc();

		// если есть атакующий персонаж
		if(attacker != null)
		{
			// увеличиваем ему счетчик убийств
			attacker.addPvECount();

			// если он ПК и игрок
			//if(attacker.isPK() && attacker.isPlayer())
			//{
				// получаем игрока
			//	Player player = attacker.getPlayer();

				// отмываем карму
			//	player.clearKarma(this);
			//}
		}
	}

	@Override
	public void doDie(Character attacker)
	{
		addCounter(attacker);

		synchronized(this)
		{
			if(isSpawned())
				calculateRewards(attacker);

			super.doDie(attacker);

			deleteMe(DeleteCharacter.DEAD);
		}

		Spawn spawn = getSpawn();

		if(spawn != null)
			spawn.doDie(this);
	}

	@Override
	public void doOwerturn(Character attacker)
	{
		// если уже опрокинут, выходим
		if(isOwerturned())
			return;

		// базавая обработка опрокидывания
		super.doOwerturn(attacker);

		// расчитываем направление
		float radians = Angles.degreeToRadians(Angles.headingToDegree(heading) + 180);

		// получаем шаблон НПС
		NpcTemplate template = getTemplate();

		// получаем дистанцию опрокидывания
		int distance = template.getOwerturnDist();

		// расчитываем точку опрокидывания
		float newX = Coords.calcX(x, distance, radians);
		float newY = Coords.calcY(y, distance, radians);

		// получаем менеджера геодаты
		GeoManager geoManager = GeoManager.getInstance();

		float newZ = geoManager.getHeight(getContinentId(), newX, newY, getZ());

		// применяем новое положение
		setXYZ(newX, newY, newZ);

		// запускаем таймер опрокидывания
		owerturnTask.nextOwerturn(template.getOwerturnTime());
	}

	@Override
	public void finalyze(){}

	/**
	 * Получить кол-во агра на персонажа.
	 *
	 * @param aggressor агрессор.
	 * @return кол-во агра.
	 */
	public long getAggro(Character aggressor)
	{
		// получаем аггро лист НПС
		Array<AggroInfo> aggroList = getAggroList();

		aggroList.writeLock();
		try
		{
			// получаем индекс инфо об агрессоре
			int index = aggroList.indexOf(aggressor);

			// если такого нет
			if(index < 0)
				// возвращаем -1
				return -1;

			// получаем контейнер
			AggroInfo info = aggroList.get(index);

			// возвращаем кол-во агра
			return info.getAggro();
		}
		finally
		{
			aggroList.writeUnlock();
		}
	}

	/**
     * @return пул контейнеров информации об агре.
     */
    protected FoldablePool<AggroInfo> getAggroInfoPool()
	{
		return aggroInfoPool;
	}

	/**
	 * @return список агрессоров.
	 */
	public final Array<AggroInfo> getAggroList()
	{
		return aggroList;
	}

	/**
	 * @return радиус агра нпс.
	 */
	public final int getAggroRange()
	{
		return getTemplate().getAggro();
	}

	@Override
	public final CharacterAI getAI()
	{
		return (CharacterAI) ai;
	}

	@Override
	public final int getAttack(Character attacked, Skill skill)
	{
		return (int) calcStat(StatType.ATTACK, getTemplate().getAttack(), attacked, skill);
	}

	@Override
	protected EmotionType[] getAutoEmotions()
	{
		return EmotionTask.MONSTER_TYPES;
	}

	@Override
	public final int getBalance(Character attacker, Skill skill)
	{
		return (int) calcStat(StatType.BALANCE, getTemplate().getBalance(), attacker, skill);
	}

	@Override
	public final int getDefense(Character attacker, Skill skill)
	{
		return (int) calcStat(StatType.DEFENSE, getTemplate().getDefense(), attacker, skill);
	}

	/**
	 * @return конечное направление.
	 */
	public final int getEndHeading()
	{
		return turnTask.getEndHeading();
	}

	/**
	 * @return базовый получаемый опыт с нпс.
	 */
	public final int getExp()
	{
		return getTemplate().getExp();
	}

	/**
	 * @return название фракции нпс.
	 */
	public final String getFraction()
	{
		return getTemplate().getFactionId();
	}

	/**
	 * @return радиус фракции нпс.
	 */
	public final int getFractionRange()
	{
		return getTemplate().getFactionRange();
	}

	@Override
	public final int getImpact(Character attacked, Skill skill)
	{
		return (int) calcStat(StatType.IMPACT, getTemplate().getImpact(), attacked, skill);
	}

	@Override
	public int getLevel()
	{
		return getTemplate().getLevel();
	}

	/**
	 * @param player игрок, запрашивающий диалог.
	 * @return набор ссылок.
	 */
	public final Array<Link> getLinks(Player player)
	{
		// получаем темплейт нпс
		NpcTemplate template = getTemplate();

		// получаем локальные объекты
		LocalObjects local = LocalObjects.get();

		// получаем список ссылок
		Array<Link> links = local.getNextLinkList();

		// получаем менеджера ивентов
		EventManager eventManager = EventManager.getInstance();

		// добавляем ссылки с ивентов
		eventManager.addLinks(links, this, player);

		// получаем диалог нпс
		DialogData dialog = template.getDialog();

		// если диалог есть
		if(dialog != null)
			// добавляем его ссылки
			dialog.addLinks(links, this, player);

		// получаем данные по квестам
		QuestData quests = template.getQuests();

		// добавляем ссылки из квестов
		quests.addLinks(links, this, player);

		// возвращаем итоговый список ссылок
		return links;
	}

	/**
	 * @return лидер этого миниона.
	 */
	public MinionLeader getMinionLeader()
	{
		return null;
	}

	/**
	 * @return персонаж, который само много надэмажил.
	 */
	public Character getMostDamager()
	{
		// получаем аггро лист
		Array<AggroInfo> aggroList = getAggroList();

		// если он пуст, выходим
		if(aggroList.isEmpty())
			return null;

		// получаем топ демагера
		Character top = null;

		aggroList.readLock();
		try
		{
			// получаем все инфо об агрессорах
			AggroInfo[] array = aggroList.array();

			// счетчик топ урона
			long damage = -1;

			// перебираем инфо о агрессорах
			for(int i = 0, length = aggroList.size(); i < length; i++)
			{
				// получаем инфо о агрессоре
				AggroInfo info = array[i];

				// если инфо нет, пропускаем
				if(info == null)
					continue;

				// если урона больше топ значения
				if(info.getDamage() > damage)
				{
					// запоминаем агрессора
					top = info.getAggressor();

					// запоминаем топ уровень урона
					damage = info.getDamage();
				}
			}
		}
		finally
		{
			aggroList.readUnlock();
		}

		return top;
	}

	/**
	 * @return приоритетная цель нпс.
	 */
	public Character getMostHated()
	{
		// получаем аггро лист НПС
		Array<AggroInfo> aggroList = getAggroList();

		// если он пуст, то агрессора нет
		if(aggroList.isEmpty())
			return null;

		// если аггро лист не отсортирован
		if(!isAggroSorted())
		{
			// сортируем список агрессоров
			aggroList.sort(AGGRO_COMPORATOR);

			// ставим флаг отсортированности
			setAggroSorted(true);
		}

		// получаем главного агрессора
		AggroInfo top = aggroList.first();

		return top != null ? top.getAggressor() : null;
	}

	@Override
	public final String getName()
	{
		return getTemplate().getName();
	}

	@Override
	public Npc getNpc()
	{
		return this;
	}

	/**
	 * @return тип НПС.
	 */
	public final NpcType getNpcType()
	{
		return getTemplate().getNpcType();
	}

	@Override
	public int getOwerturnId()
	{
		return 0x482DEB16;
	}

	/**
	 * @return случайный скил из указанной группы.
	 */
	public Skill getRandomSkill(SkillGroup group)
	{
		// получсаем список скилов этой группы
		Skill[] list = skills[group.ordinal()];

		// если его нет, возвращаем нуль либо возвращаем случайный скил
		return list == null || list.length < 1? null : list[Rnd.nextInt(0, list.length - 1)];
	}

	/**
	 * Получить первый доступный скил указанной группы.
	 *
	 * @param group группа скилов.
	 * @return первый доступный в не откате скил.
	 */
	public Skill getFirstEnabledSkill(SkillGroup group)
	{
		// получсаем список скилов этой группы
		Skill[] array = skills[group.ordinal()];

		// если есть такие
		if(array.length > 0)
			for(Skill skill : array)
				if(!isSkillDisabled(skill))
					return skill;

		return null;
	}

	/**
	 * @return спанер нпс.
	 */
	public final Spawn getSpawn()
	{
		return spawn;
	}

	/**
	 * @return точка спавна.
	 */
	public final Location getSpawnLoc()
	{
		return spawnLoc;
	}

	@Override
	public final int getSubId()
	{
		return Config.SERVER_NPC_SUB_ID;
	}

	@Override
	public final NpcTemplate getTemplate()
	{
		return (NpcTemplate) template;
	}

	/**
	 * @return есть ли у нпс диалог.
	 */
	public final boolean hasDialog()
	{
		return getTemplate().getDialog() != null;
	}

	/**
	 * @return аггресивный ли нпс.
	 */
	public final boolean isAggressive()
	{
		return getTemplate().getAggro() > 0;
	}

	/**
	 * @return отсортирован ли аггро лист.
	 */
	public final boolean isAggroSorted()
	{
		return aggroSorted;
	}

	/**
	 * @return является ли НПС дружелюбным.
	 */
	public boolean isFriendNpc()
	{
		return false;
	}

	/**
	 * @return является ли НПС гвардом.
	 */
	public boolean isGuard()
	{
		return false;
	}

	/**
	 * @return является ли НПС минионом.
	 */
	public boolean isMinion()
	{
		return false;
	}

	/**
	 * @return является ли НПС лидером минионов.
	 */
	public boolean isMinionLeader()
	{
		return false;
	}

	/**
	 * @return является ли НПС монстром.
	 */
	public boolean isMonster()
	{
		return false;
	}

	@Override
	public final boolean isNpc()
	{
		return true;
	}

	/**
	 * @return является ли НПС РБ.
	 */
	public boolean isRaidBoss()
	{
		return false;
	}

	/**
	 * @return находится ли нпс в процессе разворота.
	 */
	public boolean isTurner()
	{
		return turnTask.isTurner();
	}

	/**
     * @param aggressor агрессор.
     * @param aggro уровень агрессии.
     * @param damage нанесенный урон.
     * @return новый контейнер информации об агре.
     */
    protected AggroInfo newAggroInfo(Character aggressor, long aggro, long damage)
    {
    	AggroInfo info = aggroInfoPool.take();

    	if(info == null)
    		info = new AggroInfo();

    	info.setAggressor(aggressor);
    	info.setAggro(aggro);
    	info.setDamage(damage);

    	return info;
    }

	@Override
    protected Geom newGeomCharacter()
    {
    	NpcTemplate template = getTemplate();

    	return new NpcGeom(this, template.getGeomHeight(), template.getGeomRadius());
    }

	@Override
    protected Regen newRegenHp()
    {
    	return new NpcRegenHp(this);
    }

	@Override
    protected Regen newRegenMp()
    {
    	return new NpcRegenMp(this);
    }

	/**
	 * Развернуть нпс до указанного направления.
	 *
	 * @param newHeading новое направление.
	 */
	public void nextTurn(int newHeading)
	{
		turnTask.nextTurn(newHeading);
	}

	@Override
	public void reinit()
	{
		// получаем фабрику ИД
		IdFactory idFactory = IdFactory.getInstance();

		// получаем новый уникальный ид для НПС
		objectId = idFactory.getNextNpcId();
	}

	/**
	 * Удалить персонажа с аггр листа.
	 *
	 * @param agressor удаляемый персонаж.
	 */
	public void removeAggro(Character agressor)
	{
		// получаем аггро лист НПС
		Array<AggroInfo> aggroList = getAggroList();

		aggroList.writeLock();
		try
		{
			// ищем контейнер агрессора
			int index = aggroList.indexOf(agressor);

			// если нашли
			if(index >= 0)
			{
				// ссылка на удаляемое аггро инфо
				AggroInfo aggroInfo = aggroList.get(index);

				// получаем текущее кол-во агрессии
				long aggro = aggroInfo.getAggro();

				// удаляем НПС из хейт листа его
				agressor.removeHate(this);

				// складываем в пул
				aggroInfoPool.put(aggroInfo);

				// удаляем из списка
				aggroList.fastRemove(index);

				// обновляем флаг отсортированности
				setAggroSorted(index != 0);

				// получаем менеджер событий
				ObjectEventManager eventManager = ObjectEventManager.getInstance();

				// уведомляем об изменении агрессии
				eventManager.notifyAgression(this, agressor, -aggro);
			}
		}
		finally
		{
			aggroList.writeUnlock();
		}
	}

	@Override
	public void removeMe(Player player, int type)
	{
		player.sendPacket(DeleteCharacter.getInstance(this, type), true);
	}

	/**
	 * @param aggroSorted отсортирован ли список агрессоров.
	 */
	public final void setAggroSorted(boolean aggroSorted)
	{
		this.aggroSorted = aggroSorted;
	}

	/**
	 * @param spawn спавнер нпс.
	 */
	public final void setSpawn(Spawn spawn)
	{
		this.spawn = spawn;
	}

	/**
	 * @param spawnLoc точка спавна.
	 */
	public final void setSpawnLoc(Location spawnLoc)
	{
		this.spawnLoc = spawnLoc;
	}

	@Override
	public void spawnMe()
	{
		super.spawnMe();

		World.addSpawnedNpc();
	}

	@Override
	public void spawnMe(Location loc)
	{
		setSpawnLoc(loc);

		setCurrentHp(getMaxHp());
		setCurrentMp(getMaxMp());

		super.spawnMe(loc);

		// получаем текущий регион
		WorldRegion region = getCurrentRegion();

		// если он есть и он активный
		if(region != null && region.isActive())
		{
			// запускаем АИ
			getAI().startAITask();

			// запускаем обработку авто эмоций
			emotionTask.start();
		}
	}

	@Override
	public boolean startBattleStance(Character enemy)
	{
		// если надо обновить боевую стойку
		if(enemy != null && enemy != getEnemy() || enemy == null && isBattleStanced())
			PacketManager.showBattleStance(this, enemy);

		// ставим флаг нахождения в боевой стойке
		setBattleStanced(enemy != null);

		// вносим цель
		setEnemy(enemy);

		return true;
	}

    @Override
	public void stopBattleStance()
	{
		setBattleStanced(false);
		broadcastPacketToOthers(NpcNotice.getInstance(this, 0, 0));
	}

    /**
	 * Уменьшени агрессии.
	 *
	 * @param aggressor агрессор.
	 * @param aggro агр поинты.
	 */
	public void subAggro(Character aggressor, long aggro)
	{
		// получаем аггро лист
		Array<AggroInfo> aggroList = getAggroList();

		aggroList.writeLock();
		try
		{
			// ищем информацию об агре
			int index = aggroList.indexOf(aggressor);

			// если нашли
			if(index > -1)
			{
				// получаем контейнер
				AggroInfo info = aggroList.get(index);

				// уменьшаем агрессию
				info.subAggro(aggro);

				// если агрессия отсутствует
				if(info.getAggro() < 1)
				{
					// удаляем контейнер из списка
					aggroList.fastRemove(index);

					// удаляемся из списка сагренных НПС
					aggressor.removeHate(this);

					// складываем в пул
					aggroInfoPool.put(info);
				}

				// обновляем флаг отсортированности
				setAggroSorted(index != 0);

				// получаем менеджер событий
				ObjectEventManager eventManager = ObjectEventManager.getInstance();

				// уведомляем об изменении агрессии
				eventManager.notifyAgression(this, aggressor, aggro * -1);
			}
		}
		finally
		{
			aggroList.writeUnlock();
		}
	}

    @Override
	public void teleToLocation(int continentId, float x, float y, float z, int heading)
	{
		decayMe(DeleteCharacter.DISAPPEARS);

		super.teleToLocation(continentId, x, y, z, heading);

		spawnMe(getSpawnLoc());
	}

    @Override
	public String toString()
	{
		return "NpcInstance  id = " + getTemplateId() + ", type = " + getTemplateType();
	}

    @Override
	public void updateHp()
	{
		// создаем пакет для отображения хп
		TargetHp packet = TargetHp.getInstance(this, TargetHp.RED);

		// получаем агро лист
		Array<AggroInfo> aggroList = getAggroList();

		aggroList.readLock();
		try
		{
			// получаем список агрессоров
			AggroInfo[] array = aggroList.array();

			// перебираем их
			for(int i = 0, length = aggroList.size(); i < length; i++)
			{
				Character aggressor = array[i].getAggressor();

				// подсчитываем кол-во отправок
				if(aggressor != null && (aggressor.isPlayer() || aggressor.isSummon()))
					packet.increaseSends();
			}

			// опять перебираем
			for(int i = 0, length = aggroList.size(); i < length; i++)
			{
				Character aggressor = array[i].getAggressor();

				// отправляем пакет
				if(aggressor != null)
				{
					if(aggressor.isPlayer())
						aggressor.sendPacket(packet, false);
					else if(aggressor.isSummon() && aggressor.getOwner() != null)
						aggressor.getOwner().sendPacket(packet, false);
				}

			}
		}
		finally
		{
			aggroList.readUnlock();
		}
	}

	/**
	 * @param player игрок.
	 */
	public void updateQuestInteresting(Player player, boolean delete)
	{
		// если игрока нет, выходим
		if(player == null)
		{
			log.warning(this, new Exception("not found player"));
			return;
		}

		// получаем все связанные квесты с нпс
		QuestData quests = getTemplate().getQuests();

		// получаем первый доступные квест
		QuestType type = quests.hasQuests(this, player);

		if(type == null && delete)
			player.sendPacket(QuestNpcNotice.getInstance(this, NpcIconType.NONE), true);
		else if(type != null)
		{
			// отображаем иконку квеста
			switch(type)
			{
				case STORY_QUEST: player.sendPacket(QuestNpcNotice.getInstance(this, NpcIconType.RED_NOTICE), true); break;
				case LEVEL_UP_QUEST:
				case ZONE_QUEST: player.sendPacket(QuestNpcNotice.getInstance(this, NpcIconType.YELLOW_NOTICE), true); break;
				case GUILD_QUEST: player.sendPacket(QuestNpcNotice.getInstance(this, NpcIconType.BLUE_NOTICE), true); break;
				case DEALY_QUEST: player.sendPacket(QuestNpcNotice.getInstance(this, NpcIconType.GREEN_NOTICE), true); break;
			}
		}
	}

	/**
	 * Является ли дружественным для указанного игрока.
	 *
	 * @param player игрок.
	 * @return является ли дружественным.
	 */
	public boolean isFriend(Player player)
	{
		return isFriendNpc();
	}

	/**
	 * @return модификатор отмытия кармы.
	 */
	public int getKarmaMod()
	{
		return 1;
	}

	@Override
	public boolean isOwerturnImmunity()
	{
		return getTemplate().isOwerturnImmunity();
	}

	/**
	 * Будет ли цель спереди после разворота НПС.
	 *
	 * @param target проверяемая цель.
	 * @return будет ли она спереди.
	 */
	public boolean isInTurnFront(Character target)
	{
		if(target == null)
			return false;

		float dx = target.getX() - getX();
		float dy = target.getY() - getY();

		int head = (int) (Math.atan2(-dy, -dx) * HEADINGS_IN_PI + 32768);

		head = turnTask.getEndHeading() - head;

		if(head < 0)
			head = head + 1 + Integer.MAX_VALUE & 0xFFFF;
		else if(head > 0xFFFF)
			head &= 0xFFFF;

		return head != -1 && head <= 8192 || head >= 57344;
	}

	/**
	 * @return внешность НПС.
	 */
	public NpcAppearance getAppearance()
	{
		return null;
	}

	/**
	 * @return цвет имени НПС.
	 */
	public int getNameColor()
	{
		return NameColor.COLOR_NORMAL;
	}

	/**
	 * Завершение отображения смерти.
	 */
	public void finishDead(){}

	@Override
	public boolean isBroadcastEndSkillForCollision()
	{
		return true;
	}

	/**
	 * @return маршрут патрулирования.
	 */
	public Location[] getRoute()
	{
		return getSpawn().getRoute();
	}
}