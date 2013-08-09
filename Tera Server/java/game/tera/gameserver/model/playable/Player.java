package tera.gameserver.model.playable;

import rlib.geom.Angles;
import rlib.geom.Coords;
import rlib.idfactory.IdGenerator;
import rlib.idfactory.IdGenerators;
import rlib.util.Nameable;
import rlib.util.Rnd;
import rlib.util.SafeTask;
import rlib.util.Strings;
import rlib.util.array.Array;
import rlib.util.array.Arrays;
import rlib.util.table.FuncKeyValue;
import rlib.util.table.IntKey;
import rlib.util.table.Table;
import rlib.util.table.Tables;
import rlib.util.wraps.Wrap;
import rlib.util.wraps.WrapType;
import rlib.util.wraps.Wraps;

import tera.Config;
import tera.gameserver.events.global.regionwars.Region;
import tera.gameserver.events.global.regionwars.RegionState;
import tera.gameserver.manager.DataBaseManager;
import tera.gameserver.manager.ExecutorManager;
import tera.gameserver.manager.GameLogManager;
import tera.gameserver.manager.GeoManager;
import tera.gameserver.manager.GuildManager;
import tera.gameserver.manager.ObjectEventManager;
import tera.gameserver.manager.PacketManager;
import tera.gameserver.model.Account;
import tera.gameserver.model.AttackInfo;
import tera.gameserver.model.Bonfire;
import tera.gameserver.model.Character;
import tera.gameserver.model.Duel;
import tera.gameserver.model.EmotionType;
import tera.gameserver.model.FriendList;
import tera.gameserver.model.Guild;
import tera.gameserver.model.GuildIcon;
import tera.gameserver.model.GuildRank;
import tera.gameserver.model.MessageType;
import tera.gameserver.model.MoveType;
import tera.gameserver.model.Party;
import tera.gameserver.model.ReuseSkill;
import tera.gameserver.model.Route;
import tera.gameserver.model.SayType;
import tera.gameserver.model.TObject;
import tera.gameserver.model.World;
import tera.gameserver.model.actions.Action;
import tera.gameserver.model.actions.dialogs.ActionDialog;
import tera.gameserver.model.ai.PlayerAI;
import tera.gameserver.model.base.Experience;
import tera.gameserver.model.base.PlayerClass;
import tera.gameserver.model.base.PlayerGeomTable;
import tera.gameserver.model.base.Race;
import tera.gameserver.model.base.Sex;
import tera.gameserver.model.equipment.Equipment;
import tera.gameserver.model.equipment.Slot;
import tera.gameserver.model.geom.Geom;
import tera.gameserver.model.geom.PlayerGeom;
import tera.gameserver.model.inventory.Bank;
import tera.gameserver.model.inventory.Cell;
import tera.gameserver.model.inventory.Inventory;
import tera.gameserver.model.items.CrystalList;
import tera.gameserver.model.items.ItemInstance;
import tera.gameserver.model.items.ItemLocation;
import tera.gameserver.model.npc.Npc;
import tera.gameserver.model.npc.interaction.Link;
import tera.gameserver.model.npc.interaction.dialogs.Dialog;
import tera.gameserver.model.npc.summons.Summon;
import tera.gameserver.model.quests.QuestList;
import tera.gameserver.model.quests.QuestPanelState;
import tera.gameserver.model.quests.QuestState;
import tera.gameserver.model.regenerations.PlayerNegativeRegenMp;
import tera.gameserver.model.regenerations.PlayerPositiveRegenMp;
import tera.gameserver.model.regenerations.PlayerRegenHp;
import tera.gameserver.model.regenerations.Regen;
import tera.gameserver.model.resourse.ResourseInstance;
import tera.gameserver.model.skillengine.Effect;
import tera.gameserver.model.skillengine.EffectType;
import tera.gameserver.model.skillengine.Formulas;
import tera.gameserver.model.skillengine.Skill;
import tera.gameserver.model.skillengine.StatType;
import tera.gameserver.model.territory.RegionTerritory;
import tera.gameserver.model.territory.Territory;
import tera.gameserver.network.model.UserClient;
import tera.gameserver.network.serverpackets.AddExp;
import tera.gameserver.network.serverpackets.AppledCharmEffect;
import tera.gameserver.network.serverpackets.AppledEffect;
import tera.gameserver.network.serverpackets.CancelTargetHp;
import tera.gameserver.network.serverpackets.CharDead;
import tera.gameserver.network.serverpackets.CharSay;
import tera.gameserver.network.serverpackets.DeleteCharacter;
import tera.gameserver.network.serverpackets.GuildInfo;
import tera.gameserver.network.serverpackets.GuildLogs;
import tera.gameserver.network.serverpackets.GuildMembers;
import tera.gameserver.network.serverpackets.IncreaseLevel;
import tera.gameserver.network.serverpackets.ItemReuse;
import tera.gameserver.network.serverpackets.MountOff;
import tera.gameserver.network.serverpackets.NameColor;
import tera.gameserver.network.serverpackets.PlayerCurrentHp;
import tera.gameserver.network.serverpackets.PlayerCurrentMp;
import tera.gameserver.network.serverpackets.PlayerDeadWindow;
import tera.gameserver.network.serverpackets.PlayerInfo;
import tera.gameserver.network.serverpackets.PlayerMove;
import tera.gameserver.network.serverpackets.PlayerPvPOff;
import tera.gameserver.network.serverpackets.PlayerPvPOn;
import tera.gameserver.network.serverpackets.SkillListInfo;
import tera.gameserver.network.serverpackets.QuestMoveToPanel;
import tera.gameserver.network.serverpackets.ServerPacket;
import tera.gameserver.network.serverpackets.SkillReuse;
import tera.gameserver.network.serverpackets.SystemMessage;
import tera.gameserver.network.serverpackets.TargetHp;
import tera.gameserver.network.serverpackets.Tp1;
import tera.gameserver.network.serverpackets.UserInfo;
import tera.gameserver.network.serverpackets.WorldZone;
import tera.gameserver.tables.TerritoryTable;
import tera.gameserver.taskmanager.RegenTaskManager;
import tera.gameserver.tasks.BattleStanceTask;
import tera.gameserver.tasks.EmotionTask;
import tera.gameserver.tasks.ResourseCollectTask;
import tera.gameserver.templates.PlayerTemplate;
import tera.gameserver.templates.SkillTemplate;
import tera.util.ExtUtils;
import tera.util.Identified;
import tera.util.LocalObjects;

/**
 * Модель игрока в Tera-Online.
 *
 * @author Ronn
 */
public final class Player extends Playable implements Nameable, Identified
{
	/** фабрика ид кастов для скилов */
	private static final IdGenerator ID_FACTORY = IdGenerators.newSimpleIdGenerator(0, 300000);

	/** максимальное число квестов на квест трекере */
	private static final int MAXIMUM_QUEST_IN_PANEL = 7;

	/** набор лок он таргетов */
	private final Array<Character> lockOnTargets;
	/** список квестов на панели */
	private final Array<QuestState> questInPanel;

	/** таблица территорий, в которых был игрок */
	private final Table<IntKey, Territory> storedTerrs;
	/** таблица переменных игрока */
	private final Table<String, Wrap> variables;

	/** обработчик сбора ресурсов */
	private final ResourseCollectTask collectTask;
	/** обработчик боевой стойки игрока */
	private final BattleStanceTask battleStanceTask;

	/** функция по сохранению переменных игрока */
	private final FuncKeyValue<String, Wrap> saveVarFunc;

	/** клиент */
	private volatile UserClient client;
	/** аккаунт игрока */
	private volatile Account account;
	/** пати, в которой состоит игрок */
	private volatile Party party;
	/** клан, в котором состоит игрок */
	private volatile Guild guild;
	/** ранг в клане */
	private volatile GuildRank guildRank;
	/** маршрут полета */
	private volatile Route route;
	/** последний нпс с которым говорили */
	private volatile Npc lastNpc;
	/** последний диалог с нпс */
	private volatile Dialog lastDialog;
	/** последний экшен */
	private volatile Action lastAction;
	/** последний диалог акшена */
	private volatile ActionDialog lastActionDialog;
	/** скил, которым сели на маунта */
	private volatile Skill mountSkill;
	/** дуэль игрока */
	private volatile Duel duel;
	/** активный костер */
	private volatile Bonfire bonfire;
	/** последний нажатый линк */
	private volatile Link lastLink;
	/** список друзей */
	private volatile FriendList friendList;
	/** список квестов */
	private volatile QuestList questList;

	/** заметка для гильдии */
	private String guildNote;
	/** внешность игрока */
	private PlayerAppearance appearance;
	/** последний список линков */
	private Array<Link> lastLinks;

	/** настройки клиента игрока */
	private byte[] settings;
	/** настройки горячих клавиш игрока */
	private byte[] hotkey;

	/** время создания игрока */
	private long createTime;
	/** время онлаина игрока*/
	private long onlineTime;
	/** время входа в игру */
	private long onlineBeginTime;
	/** конец бана игрока */
	private long endBan;
	/** конец бана чата */
	private long endChatBan;
	/** время последней блокировки удара */
	private long lastBlock;

	/** ид фракции */
	private int fraction;
	/** уровень доступа */
	private int accessLevel;
	/** уровень усталости */
	private int stamina;
	/** счетчики убийтсв игроков */
	private int pvpCount;
	/** счетчик убийств нпс */
	private int pveCount;
	/** счетчик нанесеных ударов */
	private int attackCounter;
	/** карма игрока */
	private int karma;
	/** ид активного маунта */
	private int mountId;
	/** уровень сбора кристалов */
	private int energyLevel;
	/** уровень сбора камней */
	private int miningLevel;
	/** уровень сбора растений */
	private int plantLevel;

	/** приконекчен ли */
	private boolean connected;
	/** находится ли игрок на ивенте */
	private boolean event;
	/** в течении каста скила был ли уже нанесен урон кому-то */
	private boolean attacking;
	/** активирован ли у игрока пвп режим */
	private boolean pvpMode;
	/** были ли изменения в настройках */
	private boolean changedSettings;
	/** были ли изменения в раскладке */
	private boolean changeHotkey;
	/** была ли изменена внешность */
	private boolean changedFace;
	/** можно ли воскресится самому */
	private boolean resurrected;

	/**
	 * @param objectId уникальный ид игрока.
	 * @param template темплейт игрока.
	 * @param accountName имя аккаунта игрока.
	 */
	public Player(int objectId, PlayerTemplate template)
	{
		super(objectId, template);

		this.saveVarFunc = new FuncKeyValue<String, Wrap>()
		{
			@Override
			public void apply(String key, Wrap value)
			{
				// получаем менеджера БД
				DataBaseManager dbManager = DataBaseManager.getInstance();

				// обновляем значение
				dbManager.updatePlayerVar(getObjectId(), key, value.toString());
			}
		};

		this.battleStanceTask = new BattleStanceTask(this);

		this.lastLinks = Arrays.toConcurrentArray(Link.class);
		this.lockOnTargets = Arrays.toConcurrentArray(Character.class);
		this.questInPanel = Arrays.toConcurrentArray(QuestState.class);

		this.collectTask = new ResourseCollectTask(this);

		this.storedTerrs = Tables.newConcurrentIntegerTable();
		this.variables = Tables.newConcurrentObjectTable();

		// получаем формулы
		Formulas formulas = Formulas.getInstance();

		// добавляем специфичные функции для игрока
		formulas.addFuncsToNewPlayer(this);

		this.resurrected = true;
	}

	@Override
	public void abortCollect()
	{
		collectTask.cancel(true);
	}

	@Override
	public void addAttackCounter()
	{
		attackCounter++;

		if(attackCounter >= Config.WORLD_PLAYER_THRESHOLD_ATTACKS)
		{
			subHeart();
			attackCounter = 0;
		}
	}

	/**
	 * @return раса игрока.
	 */
	public Race getRace()
	{
		return getTemplate().getRace();
	}

	/**
	 * Можно ли добавляться на обработку к указанному костру.
	 *
	 * @param newBonfire новый костер.
	 * @return можно ли добавлятся.
	 */
	public boolean addBonfire(Bonfire newBonfire)
	{
		// если костра сейчас нет
		if(bonfire == null)
		{
			synchronized(this)
			{
				// если его точно нет
				if(bonfire == null)
				{
					// заносим новый костер
					bonfire = newBonfire;

					// уведомляем
					sendMessage(MessageType.YOU_ARE_RECHARGING_STAMINE);

					return true;
				}
			}
		}

		return false;
	}

	@Override
	public void addDefenseCounter()
	{
		attackCounter += 3;

		lastBlock = System.currentTimeMillis();

		if(attackCounter >= Config.WORLD_PLAYER_THRESHOLD_BLOOKS)
		{
			subHeart();
			attackCounter = 0;
		}
	}

	@Override
	public void addExp(int added, TObject object, String creator)
	{
		// если выданной экспы нет, выходим
		if(added < 1)
			return;

		// получаем логера игровых событий
		GameLogManager gameLogger = GameLogManager.getInstance();

		// пишем в лог
		gameLogger.writeExpLog(getName() + " added " + added + " exp " + (object == null? " by " + creator : " by object [" + creator + "]"));

		// прибавляем к текущей экспе
		exp += added;

		// получаем значение экспы для след. лвла
		int next = Experience.getNextExperience(level);

		// если уже есть необходимое кол-во
		if(exp > next)
		{
			synchronized(this)
			{
				while(exp > next)
				{
					// отнимаем от текущего необходимое
					exp -= next;

					// увеличиваем лвл
					increaseLevel();

					// получаем значение экспы для след. лвла
					next = Experience.LEVEL[level + 1];
				}
			}
		}

		// отправляем пакет с опытом
		sendPacket(AddExp.getInstance(exp, added, next, object != null? object.getObjectId() : 0, object != null? object.getSubId() : 0), true);
	}

	/**
	 * Добавить линк в список последнего списка линков.
	 *
	 * @param link добавляемый линк.
	 */
	public void addLink(Link link)
	{
		lastLinks.add(link);
	}

	@Override
	public boolean addLockOnTarget(Character target, Skill skill)
	{
		// если уже достигнут лимит целей, либо цель не подходит, либо цель уже внесена в список, либо цель не в радиусе, то вызодим
		if(skill.getMaxTargets() <= lockOnTargets.size() || !skill.getTargetType().check(this, target) || lockOnTargets.contains(target) || !target.isInRange(this, skill.getRange()))
			return false;

		// добавляем в список
		lockOnTargets.add(target);

		// отображаем блокировку цели
		PacketManager.showLockTarget(this, target, skill);

		return true;
	}

	@Override
	public void addMe(Player player)
	{
		try
		{
			// отображаем инфу об игроке
			player.sendPacket(PlayerInfo.getInstance(this, player), true);

			// базовые дополнения
			super.addMe(player);

			// если игрок в пвп режиме
			if(isPvPMode() && !isDead())
				// отображение полоски хп
				player.sendPacket(TargetHp.getInstance(this, TargetHp.RED), true);
		}
		catch(NullPointerException e)
		{
			log.warning(this, e);
		}
	}

	@Override
	public void addPvECount()
	{
		pveCount += 1;
	}

	@Override
	public void addPvPCount()
	{
		pvpCount += 1;
	}

	@Override
	public boolean addSkill(Skill skill, boolean sendPacket)
	{
		if(super.addSkill(skill, sendPacket))
		{
			if(sendPacket)
				sendPacket(SkillListInfo.getInstance(this), true);

			return true;
		}

		return false;
	}

	@Override
	public boolean addSkill(SkillTemplate template, boolean sendPacket)
	{
		if(super.addSkill(template, sendPacket))
		{
			if(sendPacket)
				sendPacket(SkillListInfo.getInstance(this), true);

			return true;
		}

		return false;
	}

	@Override
	public boolean addSkills(Skill[] skills, boolean sendPacket)
	{
		if(super.addSkills(skills, sendPacket))
		{
			if(sendPacket)
				sendPacket(SkillListInfo.getInstance(this), true);

			return true;
		}

		return false;
	}

	@Override
	public boolean addSkills(SkillTemplate[] templates, boolean sendPacket)
	{
		if(super.addSkills(templates, sendPacket))
		{
			if(sendPacket)
				sendPacket(SkillListInfo.getInstance(this), true);

			return true;
		}

		return false;
	}

	/**
	 * Обработка добавления уровня усталости.
	 */
	public void addStamina()
	{
		setStamina(stamina + 1);
	}

	@Override
	public void addVisibleObject(TObject object)
	{
		if(object == null || object.getObjectId() == objectId || !object.isVisible())
			return;

		object.addMe(this);
	}

	@Override
	public void broadcastMove(float x, float y, float z, int heading, MoveType type, float targetX, float targetY, float targetZ, boolean selfPacket)
	{
		ServerPacket packet = getMovePacket(x, y, z, heading, type, targetX, targetY, targetZ);

		if(selfPacket)
			broadcastPacket(packet);
		else
			broadcastPacketToOthers(packet);
	}

	@Override
	public final void broadcastPacket(ServerPacket packet)
	{
		// получаем клиент игрока
		UserClient client = getClient();

		// если клиента нет, выходим
		if(client == null)
			return;

		// увеличиваем счетчик отправко
		packet.increaseSends();

		// отправляем окружюащим
		broadcastPacketToOthers(packet);

		// отправляем игроку
		client.sendPacket(packet);
	}

	@Override
	public void causingDamage(Skill skill, AttackInfo info, Character attacker)
	{
		// получаем дуэль
		Duel duel = getDuel();

		// если дуэль есть
		if(duel != null)
		{
			// синхронизируемся
			synchronized(this)
			{
				// получаем еще раз дуэль
				duel = getDuel();

				// обновляем дуэль
				if(duel != null && duel.update(skill, info, attacker, this))
					return;
			}
		}

		// если атакующий игрок не в ПвП режиме, вводим его в ПвП режим
		if(attacker.isPlayer() && isPvPMode() && !attacker.isPvPMode())
			attacker.setPvPMode(true);

		// обрабатываем удар
		super.causingDamage(skill, info, attacker);
	}

	@Override
	public boolean checkTarget(Character target)
	{
		// если цели нет либо сам на себя указываешь, цель не подходит
		if(target == null || target == this)
			return false;

		// если цель сумон
		if(target.isSummon())
			// проверяем по владелцу
			return checkTarget(target.getOwner());

		// аполучаем дуэль игрока
		Duel duel = getDuel();

		// если игрок в дуэли
		if(duel != null)
		{
			// если цель не в дуэли, то нельзя
			if(!target.isPlayer() || target.getDuel() != duel)
				return false;

			// иначе можно
			return true;
		}

		// получаем игрока цель
		Player player = target.getPlayer();

		// если цель игрок всетаки
		if(player != null)
		{
			// проверяем на мирку
			if(!isGM() && (isInPeaceTerritory() || player.isInPeaceTerritory()))
				return false;

			// проверяем на боевую зону
			if(isInBattleTerritory() != player.isInBattleTerritory())
				return false;

			// проверяем на фракцию
			if(fractionId != 0 && player.getFractionId() == fractionId)
				return false;

			// проверяем на боевую территорию
			if(isInBattleTerritory())
				return true;

			// проверяем на соппартийство
			if(party != null && party == player.getParty())
				return false;

			// проверяем на согльдийство
			if(guild != null && target.getGuild() == guild)
				return false;

			// проверяем на пвп режим
			return isPvPMode();
		}

		// пробуем получить НПС цель
		Npc npc = target.getNpc();

		// если цель всетаки нпс, то проверяем на враждебность
		if(npc != null && npc.isFriendNpc())
			return false;

		return true;
	}

	/**
	 * Очитска от прошлых ссылок.
	 */
	public void clearLinks()
	{
		lastLinks.clear();
	}

	/**
	 * Закрытие коннекта с клиентом.
	 */
	public void closeConnection()
	{
		if(client != null)
		{
			connected = false;
			client.close();
		}
	}

	@Override
	public void decayMe(int type)
	{
		// получаем таблицу территорий
		TerritoryTable territoryTable = TerritoryTable.getInstance();

		// обрабатываем выход
		territoryTable.onExitWorld(this);

		super.decayMe(type);
	}

	/**
	 * @param questList список квестов игрока.
	 */
	public void setQuestList(QuestList questList)
	{
		this.questList = questList;
	}

	/**
	 * @param friendList список друзей.
	 */
	public void setFriendList(FriendList friendList)
	{
		this.friendList = friendList;
	}

	@Override
	public void deleteMe()
	{
		if(isDeleted())
			return;

		// отменяем каст мкила
		abortCast(true);
		// отменяем сбор ресурса
		abortCollect();

		// зануляем нпс
		setLastNpc(null);

		// получаем последний диалог
		Dialog lastDialog = getLastDialog();

		// если есть незавершенный диалог
		if(lastDialog != null)
		{
			// закрываем
			lastDialog.close();
			// зануляем
			setLastDialog(null);
		}

		// получаем последний акшен
		Action lastAction = getLastAction();

		// если есть незавершенный акшен
		if(lastAction != null)
		{
			// отменяем
			lastAction.cancel(this);
			// зануляем
			setLastAction(null);
		}

		// получаем последний диалог акшена
		ActionDialog lastActionDialog = getLastActionDialog();

		// если есть диалог с акшена
		if(lastActionDialog != null)
		{
			// отменяем
			lastActionDialog.cancel(this);
			// зануляем
			setLastActionDialog(null);
		}

		// получаем суммона игрока
		Summon summon = getSummon();

		// если есть вызванный самон
		if(summon != null)
		{
			// удаляем его
			summon.remove();
			// зануляем
			setSummon(null);
		}

		// получаем группу игрока
		Party party = getParty();

		// если есть пати
		if(party != null)
			// удаляемся из пати
			party.removePlayer(this);

		// удаляем из мира игрока
		World.removeOldPlayer(this);

		// получаем менеджера регена
		RegenTaskManager regenManager = RegenTaskManager.getInstance();

		// удаляем из регена игрока
		regenManager.removeCharacter(this);

		// синхронизируемся
		synchronized(this)
		{
			// получаем список квестов игрока
			QuestList questList = getQuestList();

			// если есть квест лист
			if(questList != null)
			{
				// созраняем все изменения
				questList.save();
				// ложим в пул
				questList.fold();
				// зануляем
				setQuestList(null);
			}

			// сохраняем игрока в БД
			store(true);

			// получаем гильдию игрока
			Guild guild = getGuild();

			// если есть гильдия
			if(guild != null)
			{
				// выходим из гильдии
				guild.exitOutGame(this);
				// зануляем
				setGuild(null);
			}

			// получаем таблицу скилов игрока
			Table<IntKey, Skill> skills = getSkills();

			// складируем все скилы в пул из таблицы
			skills.apply(ExtUtils.FOLD_SKILL_TABLE_FUNC);

			// очищаем таблицу скилов
			skills.clear();

			// получаем таблицу переменных игрока
			Table<String, Wrap> variables = getVariables();

			// складываем в пул все обертки
			variables.apply(ExtUtils.FOLD_WRAP_TABLE_FUNC);

			// очищаем таблицу
			variables.clear();

			// получаем инвентарь игрока
			Inventory inventory = getInventory();

			// если есть инвентарь
			if(inventory != null)
			{
				// складываем в пул
				inventory.fold();
				// зануляем
				setInventory(null);
			}

			// получаем экиперовку игрока
			Equipment equipment = getEquipment();

			// если есть экиперовка
			if(equipment != null)
			{
				// складируем в пул
				equipment.fold();
				// зануляем
				setEquipment(null);
			}

			// получаем банк игрока
			Bank bank = getBank();

			// если есть банк
			if(bank != null)
			{
				// складируем в пул
				bank.fold();
				// зануляем
				setBank(null);
			}

			// получаем внешность игрока
			PlayerAppearance appearance = getAppearance();

			// если есть внешность
			if(appearance != null)
			{
				// складируем в пул
				appearance.fold();
				// зануляем
				setAppearance(null, false);
			}

			// получаем список друзей
			FriendList friendList = getFriendList();

			// если есть список друзей
			if(friendList != null)
			{
				// складируем в пул
				friendList.fold();
				// зануляем
				setFriendList(null);
			}

			// зануляем клиент
			setClient(null);
		}

		super.deleteMe();
	}

	/**
	 * @param account аккаунт игрока.
	 */
	public void setAccount(Account account)
	{
		this.account = account;
	}

	@Override
	public boolean disableItem(Skill skill, ItemInstance item)
	{
		if(super.disableItem(skill, item))
		{
			sendPacket(ItemReuse.getInstance(item.getItemId(), skill.getReuseDelay(this) / 1000), true);
			return true;
		}

		return false;
	}

	@Override
	public void doCast(float startX, float startY, float startZ, Skill skill, int state, int heading, float targetX, float targetY, float targetZ)
	{
		attacking = false;

		super.doCast(startX, startY, startZ, skill, state, heading, targetX, targetY, targetZ);
	}

	@Override
	public void doCollect(ResourseInstance resourse)
	{
		collectTask.nextTask(resourse);
	}

	@Override
	public void doDie(Character attacker)
	{
		if(isOnMount())
			getOffMount();

		// получаем группу
		Party party = getParty();

		// если группа есть
		if(party != null)
		{
			// создаем сообщение о смерти
			SystemMessage message = SystemMessage.getInstance(MessageType.PARTY_PLAYER_NAME_IS_DEAD);

			// добавляем имя игрока
			message.add("PartyPlayerName", getName());

			// отправляем
			party.sendPacket(this, message);
		}

		// если это не суицид и убийца игрок
		if(attacker != this && attacker.isPlayer())
		{
			// получаем киллера игрока
			Player killer = attacker.getPlayer();

			// отправляем сообщение клиеру, о том что он убил
			attacker.sendPacket(SystemMessage
					.getInstance(MessageType.YOU_KILLED_PLAYER)
					.addPlayer(getName()), true);

			// отправляем убитому о том, кто его убил
			sendPacket(SystemMessage
					.getInstance(MessageType.PLAYER_KILLED_YOU)
					.addPlayer(killer.getName()), true);

			// обрабатываем ПК
			//checkPK(killer);

			// если игрок не на ивенте
			if(!killer.isEvent())
				// увеличиваем ПвП счетчик
				killer.addPvPCount();

			// увеличиваем кол-во убитых игроков
			World.addKilledPlayers();
		}

		// обрабатываем удаление кристалов
		destroyCrystals(attacker);

		// если игрок ПК и это не суицид
		//if(isPK() && attacker != this)
			// дропаем итемы
		//	dropItems();

		// отправляем пакет о смерти
		broadcastPacket(CharDead.getInstance(this, true));

		// отправляем окошко с ресом
		sendPacket(PlayerDeadWindow.getInstance(), true);

		super.doDie(attacker);
	}

	@Override
	public int doFall(float startZ, float endZ)
	{
		int damage = super.doFall(startZ, endZ);

		if(damage > 0)
			sendMessage("Получено " + damage + " урона при падении с высоты.");

		return damage;
	}

	@Override
	public void doOwerturn(Character attacker)
	{
		if(isOwerturned())
			return;

		super.doOwerturn(attacker);

		float radians = Angles.headingToRadians(heading + 32500);

		float newX = Coords.calcX(x, 90, radians);
		float newY = Coords.calcY(y, 90, radians);

		// получаем менеджера геодаты
		GeoManager geoManager = GeoManager.getInstance();

		setXYZ(newX, newY, geoManager.getHeight(continentId, newX, newY, z));

		SafeTask task = new SafeTask()
		{
			@Override
			protected void runImpl()
			{
				cancelOwerturn();
			}
		};

		// получаем исполнительного менеджера
		ExecutorManager executor = ExecutorManager.getInstance();

		executor.scheduleGeneral(task, 3000);

		broadcastMove(x, y, z, heading, MoveType.STOP, x, y, z, true);
	}

	@Override
	public void effectHealHp(int heal, Character healer)
	{
		// получаем текущее состояние хп
		int add = getCurrentHp();

		// применяем хил
		super.effectHealHp(heal, healer);

		// получаем разницу со старым
		add = getCurrentHp() - add;

		// отправляем пакет с разницей
		sendPacket(PlayerCurrentHp.getInstance(this, healer, add, PlayerCurrentHp.INCREASE_PLUS), true);
	}

	@Override
	public void effectHealMp(int heal, Character healer)
	{
		// получаем текущее состояние мп
		int add = getCurrentMp();

		// применяем хил
		super.effectHealMp(heal, healer);

		// получаем разницу со старым
		add = getCurrentMp() - add;

		// отправляем пакет с разницей
		sendPacket(PlayerCurrentMp.getInstance(this, healer, add, PlayerCurrentMp.INCREASE_PLUS), true);
	}

	/**
	 * @return уровень доступа.
	 */
	public int getAccessLevel()
	{
		return accessLevel;
	}

	/**
	 * @return аккаунт игрока.
	 */
	public Account getAccount()
	{
		return account;
	}

	@Override
	public PlayerAI getAI()
	{
		if(ai == null)
			ai = new PlayerAI(this);

		return (PlayerAI) ai;
	}

	/**
	 * @return кол-во сделанных атак/блоков между изминением усталости.
	 */
	public int getAttackCounter()
	{
		return attackCounter;
	}

	@Override
	protected EmotionType[] getAutoEmotions()
	{
		return EmotionTask.PLAYER_TYPES;
	}

	/**
	 * @return базовая атака.
	 */
	public int getBaseAttack()
	{
		return (int) calcStat(StatType.ATTACK, 0, 0x20, null, null);
	}

	/**
	 * @return базовый баланс.
	 */
	public int getBaseBalance()
	{
		return (int) calcStat(StatType.BALANCE, 0, 0x20, null, null);
	}

	/**
	 * @return базовая защита.
	 */
	public int getBaseDefense()
	{
		return (int) calcStat(StatType.DEFENSE, 0, 0x20, null, null);
	}

	/**
	 * @return базовая сила.
	 */
	public int getBaseImpact()
	{
		return (int) calcStat(StatType.IMPACT, 0, 0x20, null, null);
	}

	/**
	 * @return ид класса.
	 */
	@Override
	public int getClassId()
	{
		return getTemplate().getClassId();
	}

	/**
	 * @return клиент игрока.
	 */
	public UserClient getClient()
	{
		return client;
	}

	/**
	 * Определние цвета ника цели игрока.
	 *
	 * @param target цель.
	 * @return нужный цвет ника.
	 */
	public int getColor(Player target)
	{
		if(fractionId != 0)
			return fractionId != target.getFractionId()? NameColor.COLOR_RED_PVP : NameColor.COLOR_NORMAL;
		else if(duel != null && duel == target.duel)
			return NameColor.COLOR_RED_PVP;
		else if(party != null && party == target.party)
			return NameColor.COLOR_BLUE;
		else if(guild != null && guild == target.getGuild())
			return NameColor.COLOR_GREEN;
		//else if(target.isPK())
		//	return NameColor.COLOR_ORANGE;

		return isPvPMode() || target.isPvPMode()? NameColor.COLOR_RED_PVP : NameColor.COLOR_NORMAL;
	}

	/**
	 * @return текущий цвет своего ника.
	 */
	public int getColor()
	{
		//if(isPK())
		//	return NameColor.COLOR_ORANGE;
		//else
		if(isPvPMode())
			return NameColor.COLOR_RED;
		else
			return NameColor.COLOR_NORMAL;
	}

	/**
	 * @return дата создания игрока.
	 */
	public long getCreateTime()
	{
		return createTime;
	}

	@Override
	public Duel getDuel()
	{
		return duel;
	}

	/**
	 * @return дата окончания бана.
	 */
	public final long getEndBan()
	{
		return endBan;
	}

	/**
	 * @return дата окончания бана чата.
	 */
	public final long getEndChatBan()
	{
		return endChatBan;
	}

	/**
	 * @return уровень сбора кристалов.
	 */
	public final int getEnergyLevel()
	{
		return energyLevel;
	}

	/**
	 * @return кол-во опыта у игрока.
	 */
	public long getExp()
	{
		return exp;
	}

	/**
	 * @return внешность игрока.
	 */
	public PlayerAppearance getAppearance()
	{
		return appearance;
	}

	/**
	 * @return фракция игрока.
	 */
	public final int getFraction()
	{
		return fraction;
	}

	/**
	 * @return список друзей.
	 */
	public FriendList getFriendList()
	{
		if(friendList == null)
			synchronized(this)
			{
				if(friendList == null)
					friendList = FriendList.getInstance(this);
			}

		return friendList;
	}

	@Override
	public Guild getGuild()
	{
		return guild;
	}

	/**
	 * @return ид клана.
	 */
	public int getGuildId()
	{
		return guild == null ? 0 : guild.getId();
	}

	/**
	 * @return название клана.
	 */
	public String getGuildName()
	{
		return guild == null ? null : guild.getName();
	}

	/**
	 * @return титул клана.
	 */
	public String getGuildTitle()
	{
		return guild == null? null : guild.getTitle();
	}

	/**
	 * @return заметка для гильдии о игроке.
	 */
	public String getGuildNote()
	{
		return guildNote;
	}

	/**
	 * @return ранг в клане.
	 */
	public final GuildRank getGuildRank()
	{
		return guildRank;
	}

	/**
	 * @return ид ранга в клане.
	 */
	public final int getGuildRankId()
	{
		return guildRank == null? 0 : guildRank.getIndex();
	}

	/**
	 * @return название иконки гильдии.
	 */
	public String getGuildIconName()
	{
		if(guild == null)
			return null;

		GuildIcon icon = guild.getIcon();

		return icon == null? null : icon.getName();
	}

	/**
	 * @return настроки горячих клавишь.
	 */
	public byte[] getHotkey()
	{
		return hotkey;
	}

	@Override
	public int getKarma()
	{
		return karma;
	}

	/**
	 * @return последни акшен.
	 */
	public Action getLastAction()
	{
		return lastAction;
	}

	/**
	 * @return последний акшен диалог.
	 */
	public ActionDialog getLastActionDialog()
	{
		return lastActionDialog;
	}

	/**
	 * @return время последней блокировки.
	 */
	public final long getLastBlock()
	{
		return lastBlock;
	}

	/**
	 * @return последний диалог.
	 */
	public Dialog getLastDialog()
	{
		return lastDialog;
	}

	/**
	 * @return последняя нажатая ссылка.
	 */
	public Link getLastLink()
	{
		return lastLink;
	}

	/**
	 * @return последний нпс с которым взаимодействовал игрок.
	 */
	public Npc getLastNpc()
	{
		return lastNpc;
	}

	@Override
	public int getLevel()
	{
		return level;
	}

	/**
	 * Получение линка по индексу из списка последних линков.
	 *
	 * @param index индекс линка.
	 * @return линк.
	 */
	public Link getLink(int index)
	{
		lastLinks.writeLock();
		try
		{
			if(index >= lastLinks.size() || index < 0)
				return null;

			return lastLinks.get(index);
		}
		finally
		{
			lastLinks.writeUnlock();
		}
	}

	@Override
	public Array<Character> getLockOnTargets()
	{
		return lockOnTargets;
	}

	/**
	 * @return максимальный уровень усталости.
	 */
	public int getMaxStamina()
	{
		return (int) calcStat(StatType.BASE_HEART, 120, this, null);
	}

	/**
	 * @return уровень сбора камней.
	 */
	public final int getMiningLevel()
	{
		return miningLevel;
	}

	/**
	 * @return минимальный уровень усталости.
	 */
	public int getMinStamina()
	{
		return (int) (calcStat(StatType.BASE_HEART, 120, this, null) / 100 * calcStat(StatType.MIN_HEART_PERCENT, 1, this, null));
	}

	/**
	 * @return ид активного маунта.
	 */
	public final int getMountId()
	{
		return mountId;
	}

	/**
	 * @return скил активного маунта.
	 */
	public final Skill getMountSkill()
	{
		return mountSkill;
	}

	@Override
	public ServerPacket getMovePacket(float x, float y, float z, int heading, MoveType type, float targetX, float targetY, float targetZ)
	{
		return PlayerMove.getInstance(this, type, x, y, z, heading, targetX, targetY, targetZ);
	}

	@Override
	public void getOffMount()
	{
		// получаем скил, которым сели на маунта
		Skill skill = getMountSkill();

		// если скила нет, выходим
		if(skill == null)
			return;

		// получаем темплейт скила
		SkillTemplate template = skill.getTemplate();

		// удаляем пассивные бонусы
		template.removePassiveFuncs(this);

		// зануляем ид маунта
		setMountId(0);

		// отправляем пакет слезания с маунта
		broadcastPacket(MountOff.getInstance(this, skill.getIconId()));

		// зануляем скил маунта
		setMountSkill(null);

		// обновляем статы
		updateInfo();
	}

	/**
	 * @return время последнего входа в игру.
	 */
	public long getOnlineBeginTime()
	{
		return onlineBeginTime;
	}

	/**
	 * @return сколько уже времени онлаин.
	 */
	public long getOnlineTime()
	{
		return onlineTime + (System.currentTimeMillis() - onlineBeginTime);
	}

	@Override
	public int getOwerturnId()
	{
		return 0x080F6C72; //0x080F6C72;  //0x080F6C72 1010802 80F6C72
	}

	@Override
	public Party getParty()
	{
		return party;
	}

	/**
	 * @return уровень сбора растений.
	 */
	public final int getPlantLevel()
	{
		return plantLevel;
	}

	@Override
	public Player getPlayer()
	{
		return this;
	}

	/**
	 * @return класс игрока.
	 */
	public PlayerClass getPlayerClass()
	{
		return getTemplate().getPlayerClass();
	}

	/**
	 * @return кол-во убитых нпс.
	 */
	public final int getPveCount()
	{
		return pveCount;
	}

	/**
	 * @return кол-во убитых игроков.
	 */
	public final int getPvpCount()
	{
		return pvpCount;
	}

	/**
	 * @return список квестов.
	 */
	public QuestList getQuestList()
	{
		if(questList == null)
			synchronized(this)
			{
				if(questList == null)
					questList = QuestList.newInstance(this);
			}

		return questList;
	}

	/**
	 * @return ид расы игрока.
	 */
	public int getRaceId()
	{
		return getTemplate().getRaceId();
	}

	/**
	 * @return маршрут полета.
	 */
	public Route getRoute()
	{
		return route;
	}

	/**
	 * @return настройки клиента.
	 */
	public byte[] getSettings()
	{
		return settings;
	}

	/**
	 * @return пол игрока.
	 */
	public Sex getSex()
	{
		return getTemplate().getSex();
	}

	/**
	 * @return пол игрока.
	 */
	public int getSexId()
	{
		return getTemplate().getSex().ordinal();
	}

	/**
	 * @return уровень усталости игрока.
	 */
	public int getStamina()
	{
		return stamina;
	}

	@Override
	public int getSubId()
	{
		return Config.SERVER_PLAYER_SUB_ID;
	}

	@Override
	public PlayerTemplate getTemplate()
	{
		return (PlayerTemplate) template;
	}

	@Override
	public String getTitle()
	{
		return title;
	}

	/**
	 * Получение значения переменной.
	 *
	 * @param name название переменной.
	 * @return значение переменной.
	 */
	public final Wrap getVar(String name)
	{
		return variables.get(name);
	}

	/**
	 * @param name название переменной.
	 * @param def значение при отсутствии.
	 * @return значение переменной.
	 */
	public final int getVar(String name, int def)
	{
		// получаем таблицу переменных
		Table<String, Wrap> variables = getVariables();

		synchronized(variables)
		{
			// получаем значение переменной
			Wrap wrap = variables.get(name);

			// если ее нет, возвращаем дефолт
			if(wrap == null)
				return def;

			// если это обертка, возвращаем обернутое значение
			if(wrap.getWrapType() == WrapType.INTEGER)
				return wrap.getInt();
		}

		return def;
	}

	/**
	 * @return таблица переменных.
	 */
	public final Table<String, Wrap> getVariables()
	{
		return variables;
	}

	/**
	 * @return находится ли игрок в дуэли.
	 */
	public boolean hasDuel()
	{
		return duel != null;
	}

	@Override
	public boolean hasGuild()
	{
		return guild != null;
	}

	/**
	 * @return если сохраненные настройки клавишь.
	 */
	public boolean hasHotKey()
	{
		return hotkey != null;
	}

	/**
	 * @return есть ли последний акшен.
	 */
	public boolean hasLastAction()
	{
		return lastAction != null;
	}

	/**
	 * @return есть ли диалог с последнего акшена.
	 */
	public boolean hasLastActionDialog()
	{
		return lastActionDialog != null;
	}

	/**
	 * @return есть ли коннект.
	 */
	public boolean hasNetConnection()
	{
		return client != null;
	}

	@Override
	public boolean hasParty()
	{
		return party != null;
	}

	@Override
	public boolean hasPremium()
	{
		Account account = getAccount();

		if(account == null)
			return false;

		long time = account.getEndPay();

		return System.currentTimeMillis() < time;
	}

	/**
	 * @return есть ли сохраненные настройки клиента.
	 */
	public boolean hasSettings()
	{
		return settings != null;
	}

	/**
	 * Увеличение уровня игрока.
	 */
	public void increaseLevel()
	{
		if(level + 1 > Config.WORLD_PLAYER_MAX_LEVEL)
			return;

		level += 1;

		setCurrentHp(getMaxHp());
		setCurrentMp(getMaxMp());

		broadcastPacket(IncreaseLevel.getInstance(this));

		// получаем менеджера событий
		ObjectEventManager eventManager = ObjectEventManager.getInstance();

		// уведомляем о изменении уровня
		eventManager.notifyChangedLevel(this);
	}

	@Override
	public boolean isAttacking()
	{
		return attacking;
	}

	/**
	 * @return the changedFace
	 */
	public final boolean isChangedFace()
	{
		return changedFace;
	}

	/**
	 * @return the changedSettings
	 */
	public final boolean isChangedSettings()
	{
		return changedSettings;
	}

	/**
	 * @return the changeHotkey
	 */
	public final boolean isChangeHotkey()
	{
		return changeHotkey;
	}

	@Override
	public boolean isCollecting()
	{
		return collectTask.isRunning();
	}

	/**
	 * @return подключен ли к клиенту.
	 */
	public boolean isConnected()
	{
		return connected;
	}

	/**
	 * @return находится ли игрок на ивенте.
	 */
	public final boolean isEvent()
	{
		return event;
	}

	@Override
	public boolean isGM()
	{
		return accessLevel > 100;
	}

	@Override
	public boolean isOnMount()
	{
		return mountId != 0;
	}

	@Override
	public final boolean isPlayer()
	{
		return true;
	}

	@Override
	public boolean isPvPMode()
	{
		return pvpMode;
	}

	/**
	 * @return можно ли самому вскресится.
	 */
	public final boolean isResurrected()
	{
		return resurrected;
	}

	/**
	 * Был ли игрок в этой территории.
	 *
	 * @param territory проверяемая территория.
	 * @return был ли в ней уже игрок.
	 */
	public boolean isWhetherIn(Territory territory)
	{
		return storedTerrs.containsKey(territory.getId());
	}

	public void loadVariables()
	{
		//TODO
	}

	@Override
	protected Geom newGeomCharacter()
	{
		return new PlayerGeom(this, PlayerGeomTable.getHeight(getRaceId(), getSexId()), PlayerGeomTable.getRadius(getRaceId(), getSexId()));
	}

	@Override
	protected Regen newRegenHp()
	{
		return new PlayerRegenHp(this);
	}

	@Override
	protected Regen newRegenMp()
	{
		if(getClassId() == 2 || getClassId() == 3)
			return new PlayerNegativeRegenMp(this);
		else
			return new PlayerPositiveRegenMp(this);
	}

	@Override
	public int nextCastId()
	{
		return ID_FACTORY.getNextId();
	}

	/**
	 * Удаление из обработки костра.
	 *
	 * @param oldBonfire удаляющий костер.
	 */
	public void removeBonfire(Bonfire oldBonfire)
	{
		// если это текущий костер
		if(bonfire == oldBonfire)
		{
			synchronized(this)
			{
				// если это точно текущий костер
				if(bonfire == oldBonfire)
				{
					// удаляем его
					bonfire = null;
					// уведомляем
					sendMessage(MessageType.YOU_ARE_NO_LONGER_RECHARGING_STAMINA);
				}
			}
		}
	}

	@Override
	public void removeMe(Player player, int type)
	{
		// получаем дуэль
		Duel duel = getDuel();

		// если дуэль есть
		if(duel != null)
		{
			// синхронизируемся
			synchronized(this)
			{
				// получаем еще раз дуэль
				duel = getDuel();

				// обновляем дуэль
				if(duel != null && player.getDuel() == duel)
					duel.cancel(false, true);
			}
		}

		player.sendPacket(DeleteCharacter.getInstance(this, type), true);
	}

	@Override
	public void removeSkill(int skillId, boolean sendPacket)
	{
		super.removeSkill(skillId, sendPacket);

		if(sendPacket)
			sendPacket(SkillListInfo.getInstance(this), true);
	}

	@Override
	public void removeSkill(Skill skill, boolean sendPacket)
	{
		super.removeSkill(skill, sendPacket);

		if(sendPacket)
			sendPacket(SkillListInfo.getInstance(this), true);
	}

	@Override
	public void removeSkill(SkillTemplate template, boolean sendPacket)
	{
		super.removeSkill(template, sendPacket);

		if(sendPacket)
			sendPacket(SkillListInfo.getInstance(this), true);
	}

	@Override
	public void removeSkills(SkillTemplate[] templates, boolean sendPacket)
	{
		super.removeSkills(templates, sendPacket);

		if(sendPacket)
			sendPacket(SkillListInfo.getInstance(this), true);
	}

	@Override
	public void removeVisibleObject(TObject object, int type)
	{
		if(object == null || object.getObjectId() == objectId)
			return;

		object.removeMe(this, type);
	}

	/**
	 * Сохранение переменных в БД.
	 */
	public void saveVars()
	{
		// получаем таблицу переменных
		Table<String, Wrap> variables = getVariables();

		synchronized(variables)
		{
			variables.apply(saveVarFunc);
		}
	}

	/**
	 * Отправка пакетов эффектов.
	 */
	public void sendEffects()
	{
		if(effectList == null || effectList.size() < 1)
			return;

		effectList.lock();
		try
		{
			Array<Effect> effects = effectList.getEffects();

			Effect[] array = effects.array();

			for(int i = 0, length = effects.size(); i < length; i++)
			{
				Effect effect = array[i];

				if(effect == null || effect.isEnded())
					continue;

				if(effect.getEffectType() == EffectType.CHARM_BUFF)
				{
					sendPacket(AppledCharmEffect.getInstance(this, effect.getEffectId(), effect.getTimeEnd() * 1000), true);
					continue;
				}

				sendPacket(AppledEffect.getInstance(effect.getEffector(), effect.getEffected(), effect.getEffectId(), effect.getTimeEnd() * 1000), true);
			}
		}
		finally
		{
			effectList.unlock();
		}
	}

	@Override
	public void sendMessage(MessageType type)
	{
		sendPacket(SystemMessage.getInstance(type), true);
	}

	@Override
	public void sendMessage(String message)
	{
		sendPacket(CharSay.getInstance(Strings.EMPTY, message, SayType.SYSTEM_CHAT, 0, 0), true);
	}

	@Override
	public void sendPacket(ServerPacket packet, boolean increaseSends)
	{
		// если пакета нет, выходим
		if(packet == null)
			return;

		// получаем клиент игрока
		UserClient client = getClient();

		// если клиента нет, выходим
		if(client == null)
			return;

		// добавляем в очередь на отправку
		client.sendPacket(packet, increaseSends);
	}

	/**
	 * Отсылка итемов находяхчихся в откате.
	 */
	public void sendReuseItems()
	{
		Table<IntKey, ReuseSkill> reuses = getReuseSkills();

		for(ReuseSkill reuse : reuses)
			if(reuse.isItemReuse())
				sendPacket(ItemReuse.getInstance(reuse.getItemId(), (int) Math.max(0, (reuse.getEndTime() - System.currentTimeMillis()) / 1000)), true);
	}

	/**
	 * Отсылка скилов находяхчихся в откате.
	 */
	public void sendReuseSkills()
	{
		Table<IntKey, ReuseSkill> reuses = getReuseSkills();

		for(ReuseSkill reuse : reuses)
			if(!reuse.isItemReuse())
				sendPacket(SkillReuse.getInstance(reuse.getSkillId(), (int) Math.max(0, reuse.getEndTime() - System.currentTimeMillis())), true);
	}

	/**
	 * Установка уровня прав игрока.
	 *
	 * @param level новый уровень прав.
	 */
	public void setAccessLevel(int level)
	{
		accessLevel = level;
	}

	/**
	 * @param attackCounter кол-во сделанных атак между изминениями усталости.
	 */
	public void setAttackCounter(int attackCounter)
	{
		this.attackCounter = attackCounter;
	}

	@Override
	public void setAttacking(boolean attacking)
	{
		this.attacking = attacking;
	}

	/**
	 * @param changedFace the changedFace to set
	 */
	public final void setChangedFace(boolean changedFace)
	{
		this.changedFace = changedFace;
	}

	/**
	 * @param changedSettings the changedSettings to set
	 */
	public final void setChangedSettings(boolean changedSettings)
	{
		this.changedSettings = changedSettings;
	}

	/**
	 * @param changeHotkey the changeHotkey to set
	 */
	public final void setChangeHotkey(boolean changeHotkey)
	{
		this.changeHotkey = changeHotkey;
	}

	/**
	 * @param client клиент, который управляет игроком.
	 */
	public void setClient(UserClient client)
	{
		this.client = client;
		this.account = client != null? client.getAccount() : null;
		this.connected = client != null && client.isConnected();
	}

	/**
	 * @param connected подключен ли клиент.
	 */
	public void setConnected(boolean connected)
	{
		this.connected = connected;
	}

	/**
	 * @param createTime дата создания игрока.
	 */
	public void setCreateTime(final long createTime)
	{
		this.createTime = createTime;
	}

	/**
	 * @param duel текущий дуэль.
	 */
	public void setDuel(Duel duel)
	{
		this.duel = duel;
	}

	/**
	 * @param endBan дата окончания бана.
	 */
	public final void setEndBan(long endBan)
	{
		this.endBan = endBan;
	}

	/**
	 * @param endChatBan дата окончания бана чата.
	 */
	public final void setEndChatBan(long endChatBan)
	{
		this.endChatBan = endChatBan;
	}

	/**
	 * @param energyLevel уровень сбора кристалов.
	 */
	public final void setEnergyLevel(int energyLevel)
	{
		this.energyLevel = Math.min(energyLevel, 300);
	}

	/**
	 * @param event находится ли игрок на ивенте.
	 */
	public final void setEvent(boolean event)
	{
		this.event = event;
	}

	/**
	 * @param val кол-во опыта.
	 */
	public void setExp(int exp)
	{
		this.exp = Math.max(exp, 0);
	}

	/**
	 * @param appearance внешность игрока.
	 * @param isNew является ли это новой внешностью.
	 */
	public void setAppearance(PlayerAppearance appearance, boolean isNew)
	{
		this.appearance = appearance;

		if(isNew)
			setChangedFace(true);
	}

	/**
	 * @param fraction фракция игрока.
	 */
	public final void setFraction(int fraction)
	{
		this.fraction = fraction;
	}

	/**
	 * @param guild гильдия.
	 */
	public void setGuild(Guild guild)
	{
		this.guild = guild;
	}

	/**
	 * @param id ид гильдии.
	 */
	public void setGuildId(int id)
	{
		// получаем менеджера гильдии
		GuildManager guildManager = GuildManager.getInstance();

		// получаем искомую гильдию
		guild = guildManager.getGuild(id);
	}

	/**
	 * @param guildNote заметка для гильдии о игроке.
	 */
	public void setGuildNote(String guildNote)
	{
		if(guildNote.isEmpty())
			guildNote = Strings.EMPTY;

		this.guildNote = guildNote;
	}

	/**
	 * @param clanRank ранг клана.
	 */
	public final void setGuildRank(GuildRank guildRank)
	{
		this.guildRank = guildRank;
	}

	/**
	 * @param hotkey настроки горячих клавишь.
	 * @param isNew является ли это новым хначением.
	 */
	public void setHotkey(byte[] hotkey, boolean isNew)
	{
		this.hotkey = hotkey;

		if(isNew)
			changeHotkey = true;
	}

	@Override
	public void setKarma(int karma)
	{
		this.karma = Math.max(karma, 0);
	}

	/**
	 * @param lastAction последний акшен.
	 */
	public void setLastAction(Action lastAction)
	{
		this.lastAction = lastAction;
	}

	/**
	 * @param lastActionDialog последний акшен диалог.
	 */
	public void setLastActionDialog(ActionDialog lastActionDialog)
	{
		this.lastActionDialog = lastActionDialog;
	}

	/**
	 * @param lastDialog последний диалог.
	 */
	public void setLastDialog(Dialog lastDialog)
	{
		this.lastDialog = lastDialog;
	}

	/**
	 * @param lastLink последний линк.
	 */
	public void setLastLink(Link lastLink)
	{
		this.lastLink = lastLink;
	}

	/**
	 * @param lastNpc последний нпс, с которым было взаимодействие.
	 */
	public void setLastNpc(Npc lastNpc)
	{
		this.lastNpc = lastNpc;
	}

	/**
	 * Установка уровня игрока.
	 *
	 * @param newLevel новый уровень игрока.
	 * @return приминилось ли изминение.
	 */
	public boolean setLevel(int newLevel)
	{
		if(newLevel > Config.WORLD_PLAYER_MAX_LEVEL)
			level = Config.WORLD_PLAYER_MAX_LEVEL;
		else if(newLevel < 1)
			level = 1;
		else
			level = newLevel;

		return level == newLevel;
	}

	/**
	 * @param miningLevel уровень сбора кристалов.
	 */
	public final void setMiningLevel(int miningLevel)
	{
		this.miningLevel = Math.min(miningLevel, 300);
	}

	/**
	 * @param mountId ид активного маунта.
	 */
	public final void setMountId(int mountId)
	{
		this.mountId = mountId;
	}

	/**
	 * @param mountSkill скил активного маунта.
	 */
	public final void setMountSkill(Skill mountSkill)
	{
		this.mountSkill = mountSkill;
	}

	/**
	 * @param onlineBeginTime время начала текущей сессии.
	 */
	public void setOnlineBeginTime(long onlineBeginTime)
	{
		this.onlineBeginTime = onlineBeginTime;
	}

	/**
	 * @param time общее время онлайна.
	 */
	public void setOnlineTime(final long time)
	{
		onlineTime = time;
		setOnlineBeginTime(System.currentTimeMillis());
	}

	/**
	 * @param party группа игрока.
	 */
	public void setParty(Party party)
	{
		this.party = party;
	}

	/**
	 * @param plantLevel уровень сбора растений.
	 */
	public final void setPlantLevel(int plantLevel)
	{
		this.plantLevel = Math.min(plantLevel, 300);
	}

	/**
	 * @param pveCount кол-во убитых нпс.
	 */
	public final void setPvECount(int pveCount)
	{
		this.pveCount = pveCount;
	}

	/**
	 * @param pvpCount кол-во убитых игроков.
	 */
	public final void setPvPCount(int pvpCount)
	{
		this.pvpCount = pvpCount;
	}

	@Override
	public void setPvPMode(boolean pvpMode)
	{
		// применяем флаг
		this.pvpMode = pvpMode;

		// если игрок уже в мире
		if(isSpawned())
		{
			// получаем локальные объекты
			LocalObjects local = LocalObjects.get();

			// список видимых игроков
			Array<Player> players = World.getAround(Player.class, local.getNextPlayerList(), this);

			// массив игроков
			Player[] array = players.array();

			ServerPacket hp = null;
			ServerPacket pvp = null;

			if(pvpMode)
			{
				hp = TargetHp.getInstance(this, TargetHp.RED);
				pvp = PlayerPvPOn.getInstance(this);
			}
			else
			{
				hp = CancelTargetHp.getInstance(this);
				pvp = PlayerPvPOff.getInstance(this);
			}

			hp.increaseSends();
			pvp.increaseSends();

			for(int i = 0, length = players.size(); i < length; i++)
			{
				Player player = array[i];

				if(player == null)
					continue;

				player.sendPacket(pvp, true);
				player.sendPacket(hp, true);
			}

			sendPacket(pvp, true);
			sendPacket(NameColor.getInstance(getColor(), this), true);

			updateColor(players);

			hp.complete();
			pvp.complete();
		}

		// если активирован режим ПвП и игрок ПК
		//if(!isPkMode() && isPvPMode() && isPK())
			// активируем и режим ПК
		//	setPkMode(true);
	}

	/**
	 * Обновление цветов ников.
	 */
	public void updateColor(Array<Player> players)
	{
		// получаем массив игроков
		Player[] array = players.array();

		// перебираем их
		for(int i = 0, length = players.size(); i < length; i++)
		{
			// получаем игрока
			Player player = array[i];

			// обновляем ему свой цвет ника
			player.updateColor(this);

			// обновляем себе его цвет ника
			updateColor(player);
		}
	}

	/**
	 * Обновление цветов ников.
	 */
	public void updateColor()
	{
		// получаем локальные объекты
		LocalObjects local = LocalObjects.get();

		// список видимых игроков
		Array<Player> players = World.getAround(Player.class, local.getNextPlayerList(), this);

		// обновляем цвет ников
		updateColor(players);
	}

	/**
	 * @param resurrected можно ли самому вскресится.
	 */
	public final void setResurrected(boolean resurrected)
	{
		this.resurrected = resurrected;
	}

	/**
	 * @param route маршрут палета.
	 */
	public void setRoute(Route route)
	{
		this.route = route;
	}

	/**
	 * @param settings настройки клиента.
	 * @param isNew является ли это новым хначением.
	 */
	public void setSettings(byte[] settings, boolean isNew)
	{
		this.settings = settings;

		if(isNew)
			changedSettings = true;
	}

	/**
	 * @param stamina уровень усталости игрока.
	 */
	public void setStamina(int stamina)
	{
		// получаем максимально допустимую стамину
		int maxStamina = getMaxStamina();
		// получаем минимальную стамину
		int minStamina = getMinStamina();

		// ограничиваем по максимуму
		if(stamina > maxStamina)
			stamina = maxStamina;

		// ограничиваем по минимуму
		if(stamina < minStamina)
			stamina = minStamina;

		// запоминаем новое значение стамины
		this.stamina = stamina;

		// получаем текущее значение хп
		int current = getCurrentHp();
		// получаем новое максимальное значение хп
		int max = getMaxHp();

		// флаг необходимости обновить юзер инфо
		boolean updateUserInfo = false;

		// если текущее хп больше нового максимума
		if(current > max)
		{
			// срезаем
			setCurrentHp(max);
			// ставим флаг обновления юзер инфо
			updateUserInfo = true;
		}

		// получаем текущее состояние мп
		current = getCurrentMp();

		// получаем новое макс. мп
		max = getMaxMp();

		// если текущее мп больше нового максимума
		if(current > max)
		{
			// срзеаем
			setCurrentMp(max);
			// ставим флаг обновления юзер инфо
			updateUserInfo = true;
		}

		// если надо обновить юзер инфо
		if(updateUserInfo)
			// обновляем
			updateInfo();

		// получаем менеджера событий
		ObjectEventManager eventManager = ObjectEventManager.getInstance();

		// уведомляем всех заинтересованных в изменении стамины
		eventManager.notifyStaminaChanged(this);
	}

	/**
	 * Установка нового значения переменной.
	 *
	 * @param name название переменной.
	 * @param val значеие переменной.
	 */
	public void setVar(String name, int val)
	{
		// получаем переменную
		Table<String, Wrap> variables = getVariables();

		synchronized(variables)
		{
			Wrap wrap = variables.get(name);

			// если переменной небыло
			if(wrap == null)
			{
				// вносим в таблицу
				variables.put(name, Wraps.newIntegerWrap(val, true));

				// получаем менеджера БД
				DataBaseManager dbManager = DataBaseManager.getInstance();

				// вносим в БД
				dbManager.insertPlayerVar(objectId, name, String.valueOf(val));
			}
			else if(wrap.getWrapType() == WrapType.INTEGER)
				wrap.setInt(val);
			else
				variables.put(name, Wraps.newIntegerWrap(val, true));
		}
	}

	/**
	 * Установка кастомного параметра игроку.
	 *
	 * @param name название параметра.
	 * @param value значение параметра.
	 */
	public void setVar(String name, String value)
	{
		//TODO userVariables.put(name, value);
	}

	@Override
	public void setXYZ(float x, float y, float z)
	{
		super.setXYZ(x, y, z);

		updateTerritories();
	}

	@Override
	public void spawnMe()
	{
		super.spawnMe();

		// получаем таблицу территорий
		TerritoryTable territoryTable = TerritoryTable.getInstance();

		// обрабатываем вход
		territoryTable.onEnterWorld(this);

		updateGuild();

		emotionTask.start();
	}

	@Override
	public boolean startBattleStance(Character enemy)
	{
		if(isBattleStanced())
		{
			battleStanceTask.update();
			return false;
		}

		if(Config.DEVELOPER_MAIN_DEBUG)
			sendMessage("start battle stance.");

		battleStanceTask.now();
		battleStanced = true;

		sendMessage(MessageType.BATTLE_STANCE_ON);

		updateInfo();

		return true;
	}

	@Override
	public void stopBattleStance()
	{
		if(Config.DEVELOPER_MAIN_DEBUG)
			sendMessage("stop battle stance.");

		battleStanceTask.stop();
		battleStanced = false;

		sendMessage(MessageType.BATTLE_STANCE_OFF);

		updateInfo();
	}

	/**
	 * Сохранение игрока в БД.
	 */
	public synchronized void store(boolean deleted)
	{
		if(isDeleted())
			return;

		// получаем менеджера БД
		DataBaseManager dbManager = DataBaseManager.getInstance();

		// обновляем игрока в БД
		dbManager.fullStore(this);
	}

	/**
	 * Добавление территорий в число побывавшихся.
	 *
	 * @param territory добавляемая территория.
	 * @param inDB вносить ли в БД.
	 */
	public void storeTerritory(Territory territory, boolean inDB)
	{
		// вносим в таблицу
		storedTerrs.put(territory.getId(), territory);

		// если надо внести в БД
		if(inDB)
		{
			// получаем менеджера БД
			DataBaseManager dbManager = DataBaseManager.getInstance();

			// вносим в БД
			dbManager.storeTerritory(this, territory);
		}
	}

	/**
	 * Обработка уменьшения уровня усталости.
	 */
	public void subHeart()
	{
		setStamina(stamina - 1);
	}

	@Override
	public void teleToLocation(int continentId, float x, float y, float z, int heading)
	{
		// убираем из мира
		decayMe(DeleteCharacter.DISAPPEARS);

		int current = getContinentId();

		// телепортируем
		super.teleToLocation(continentId, x, y, z, heading);

		// если изменился континент, обновляем
		if(current != continentId)
		{
			// получаем менеджера БД
			DataBaseManager dbManager = DataBaseManager.getInstance();

			// обновляем текущий континент игрока в БД
			dbManager.updatePlayerContinentId(this);
		}

		// отправляем пакет телепорта
		broadcastPacket(Tp1.getInstance(this));

		// получаем ид новой зоны
		int zoneId = World.getRegion(this).getZoneId(this);

		if(zoneId < 1)
			zoneId = getContinentId() + 1;

		// обновляем ид зоны
		setZoneId(zoneId);

		// получаем менеджера событий
		ObjectEventManager eventManager = ObjectEventManager.getInstance();

		// обновляем ид зоны
		eventManager.notifyChangedZoneId(this);

		// отправялм пакет с зоной
		sendPacket(WorldZone.getInstance(this), true);
	}

	@Override
	public String toString()
	{
		return getName();
	}

	/**
	 * Удаление кастомного параметра.
	 *
	 * @param name название параметра.
	 */
	public void unsetVar(String name)
	{
		if(name == null)
			return;
	}

	/**
	 * Обновление цветов ника.
	 *
	 * @param target целевой игрок.
	 */
	public void updateColor(Player target)
	{
		sendPacket(NameColor.getInstance(getColor(target), target), true);
		target.sendPacket(NameColor.getInstance(target.getColor(this), this), true);
	}

	@Override
	public void updateCoords()
	{
		if(party != null)
			party.updateCoords(this);
	}

	@Override
	public void updateEffects()
	{
		if(effectList == null || effectList.size() < 1)
			return;

		Array<Effect> effects = effectList.getEffects();

		Effect[] array = effects.array();

		for(int i = 0, length = effects.size(); i < length; i++)
		{
			Effect effect = array[i];

			if(effect == null || effect.isEnded())
				continue;

			broadcastPacket(AppledEffect.getInstance(effect.getEffector(), effect.getEffected(), effect));
		}
	}

	/**
	 * Обновление гильдии.
	 */
	public void updateGuild()
	{
		sendPacket(GuildInfo.getInstance(this), true);

		if(guild == null)
			return;

		sendPacket(GuildMembers.getInstance(this), true);
		sendPacket(GuildLogs.getInstance(this), true);
	}

	@Override
	public void updateHp()
	{
		// отправляем текущее состояние хп
		sendPacket(PlayerCurrentHp.getInstance(this, null, 0, PlayerCurrentHp.INCREASE), true);

		// пати игрока
		Party party = getParty();

		// если пати есть
		if(party != null)
			// обновляем свое состояние в пати
			party.updateStat(this);

		// дуэль игрока
		Duel duel = getDuel();

		// если дуэль есть
		if(duel != null)
		{
			// получаем соперника по дуэли
			Player enemy = duel.getEnemy(this);

			// если соперник есть
			if(enemy != null)
				// отправляем состояние хп
				enemy.sendPacket(TargetHp.getInstance(this, TargetHp.RED), true);
		}

		// если активирован пвп режим
		if(pvpMode)
			// отправляем всем свое хп
			broadcastPacketToOthers(TargetHp.getInstance(this, TargetHp.RED));
	}

	@Override
	public void updateInfo()
	{
		sendPacket(UserInfo.getInstance(this), true);
	}

	@Override
	public void updateMp()
	{
		sendPacket(PlayerCurrentMp.getInstance(this, null, 0, PlayerCurrentMp.INCREASE), true);

		if(party != null)
			party.updateStat(this);
	}

	/**
	 * Обновление отображения для окружающих.
	 */
	public void updateOtherInfo()
	{
		// получаем локальные объекты
		LocalObjects local = LocalObjects.get();

		// получаем окружающих персонажей
		Array<Player> players = World.getAround(Player.class, local.getNextPlayerList(), this);

		// массив игроков
		Player[] array = players.array();

		for(int i = 0, length = players.size(); i < length; i++)
		{
			// окружающий игрок.
			Player target = array[i];

			// удаляем у нас игрока
			target.removeMe(this, DeleteCharacter.DISAPPEARS);

			// удаляем себя у игрока
			removeMe(target, DeleteCharacter.DISAPPEARS);

			// показываем игрока себе
			target.addMe(this);

			// показываем себя игроку
			addMe(target);
		}
	}

	/**
	 * обновление квеста на панели.
	 *
	 * @param quest обновляемый квест.
	 * @param panel новое запраиваемое состояние.
	 */
	public void updateQuestInPanel(QuestState quest, QuestPanelState panelState)
	{
		// получаем менеджера БД
		DataBaseManager dbManager = DataBaseManager.getInstance();

		questInPanel.writeLock();
		try
		{
			switch(panelState)
			{
				case REMOVED:
				{
					// получаем номер в списке квестов на панели
					int index = questInPanel.indexOf(quest);

					// если его там нет, выходим
					if(index < 0)
						return;

					// удаляем из списка
					questInPanel.fastRemove(index);

					// вносим новое состояние
					quest.setPanelState(QuestPanelState.REMOVED);

					// обновляем в БД
					dbManager.updateQuest(quest);

					break;
				}
				case ADDED:
				{
					// если этот квест уже есть в списке, выходим
					if(questInPanel.contains(quest))
						return;

					// если список переполнен
					if(questInPanel.size() >= MAXIMUM_QUEST_IN_PANEL)
					{
						// сообщаем и выходим
						sendMessage(MessageType.QUEST_TRACKER_DISPLAYS_UP_TO_7_QUESTS);
						return;
					}

					// добавляем в список
					questInPanel.add(quest);

					// обновляем состояние на панели
					quest.setPanelState(QuestPanelState.ADDED);

					// отправляем пакет с добавлением
					sendPacket(QuestMoveToPanel.getInstance(quest), true);

					// обновляем в БД квест
					dbManager.updateQuest(quest);

					break;
				}
				// взятие нового квеста
				case ACCEPTED:
				{
					// если еще есть место на панели
					if(questInPanel.size() >= MAXIMUM_QUEST_IN_PANEL)
						// ставим состояние удаленности с панели
						quest.setPanelState(QuestPanelState.REMOVED);
					else
					{
						// ставим состояние нахождения на панели
						quest.setPanelState(QuestPanelState.ADDED);

						// если нету еще
						if(!questInPanel.contains(quest))
							// добавляем в список
							questInPanel.add(quest);

						// отправляем пакет с добавлением
						sendPacket(QuestMoveToPanel.getInstance(quest), true);
					}

					// обнолвяем в БД
					dbManager.updateQuest(quest);

					break;
				}
				case UPDATE:
				{
					// если этот квест уже есть в списке, выходим
					if(questInPanel.contains(quest))
						return;

					// если список переполнен
					if(questInPanel.size() >= MAXIMUM_QUEST_IN_PANEL)
						return;

					// добавляем в список
					questInPanel.add(quest);

					// обновляем состояние на панели
					quest.setPanelState(QuestPanelState.ADDED);

					// отправляем пакет с добавлением
					sendPacket(QuestMoveToPanel.getInstance(quest), true);

					// обновляем в БД квест
					dbManager.updateQuest(quest);

					break;
				}
				case NONE: break;
				default:
					log.warning(this, new Exception("incorrect panel state."));
			}
		}
		finally
		{
			questInPanel.writeUnlock();
		}
	}

	/**
	 * Обновление панели квестов.
	 */
	public void updateQuestPanel()
	{
		/*Array<QuestState> quests = questList.getActiveQuests();

		quests.readLock();
		try
		{
			QuestState[] array = quests.array();

			for(int i = 0, length = quests.size(); i < length; i++)
			{
				QuestState quest = array[i];

				if(quest.getPanelState() == QuestPanelState.ADDED && !questInPanel.contains(quest))
					questInPanel.add(quest);
			}
		}
		finally
		{
			quests.readUnlock();
		}*/
	}

	@Override
	public void updateReuse(Skill skill, int reuseDelay)
	{
		sendPacket(SkillReuse.getInstance(skill.getReuseId(), reuseDelay), true);
	}

	@Override
	public void updateStamina()
	{
		updateInfo();

		if(party != null)
			party.updateStat(this);
	}

	/**
	 * Обработка уничтожения кристалов.
	 */
	public void destroyCrystals(Character killer)
	{
		// если это суицид, либо смерть в боевой зоне, либо смерть от ПК, то не разрушаем кристалы
		if(killer == this || isInBattleTerritory())
			return;

		// получаем экиперовку игрока
		Equipment equipment = getEquipment();

		// если она есть
		if(equipment != null)
		{
			equipment.lock();
			try
			{
				// получаем все слоты
				Slot[] slots = equipment.getSlots();

				// были ли удалены кристалы
				boolean changed = false;

				// перебираем слоты
				for(int i = 0, length = slots.length; i < length; i++)
				{
					ItemInstance item  = slots[i].getItem();

					// пустые слоты пропускаем
					if(item == null)
						continue;

					// получаем список кристалов в итеме
					CrystalList crystals = item.getCrystals();

					// если в нем кристалов нет, пропускаем
					if(crystals == null || crystals.isEmpty())
						continue;

					//если уже был удален кристал
					if(changed)
						// просто обрабатываем удаление новых
						crystals.destruction(this);
					else
						// пробуем удалить кристалы
						changed = crystals.destruction(this);
				}

				// если был удале кристал
				if(changed)
				{
					// получаем менеджера событий
					ObjectEventManager eventManager = ObjectEventManager.getInstance();

					// обновляем инвентарь
					eventManager.notifyInventoryChanged(this);
				}
			}
			finally
			{
				equipment.unlock();
			}
		}
	}

	/**
	 * Обработка ПК.
	 *
	 * @param player убийца игрока.
	 */
//	public void checkPK(Player player)
//	{
//		if(!player.isPvPMode() || player.isEvent() || isPvPMode() || player.getLevel() - getLevel() < 5)
//			return;
//
//		boolean updateColor = !player.isPK();
//
//		// получаем территории убийцы
//		Array<Territory> territories = player.getTerritories();
//
//		territories.readLock();
//		try
//		{
//			// получаем массив территорий
//			Territory[] array = territories.array();
//
//			for(int i = 0, length = territories.size(); i < length; i++)
//			{
//				// получаем территорию
//				Territory territory = array[i];
//
//				// получаем тип территории
//				switch(territory.getType())
//				{
//					// если это боевая, то выходим
//					case BATTLE_TERRITORY: return;
//					// если это территория региона
//					case REGION_TERRITORY:
//					{
//						// получаем территорию региона
//						RegionTerritory regionTerritory = (RegionTerritory) territory;
//
//						// получаем управляющий регион
//						Region region = regionTerritory.getRegion();
//
//						// если сейчас идет осада, выходим
//						if(region.getState() == RegionState.PREPARE_END_WAR)
//							return;
//
//						break;
//					}
//					default: continue;
//				}
//			}
//		}
//		finally
//		{
//			territories.readUnlock();
//		}
//
//		// рассчитываем карму
//		int karma = 200 * (player.getLevel() - getLevel());
//
//		synchronized(player)
//		{
//			player.setKarma(player.getKarma() + karma);
//		}
//
//		// сообщаем об получении кармы
//		player.sendMessage("Вы получили " + karma + " кармы, итоговая составляет: " + player.getKarma());
//
//		// если только вошел в ПК
//		if(updateColor)
//		{
//			// обновляем цвета ников
//			player.updateColor();
//			// обновляем себе свой цвет ника
//			player.sendPacket(NameColor.getInstance(player.getColor(), player), true);
//		}
//	}

	/**
	 * Обработка дропа итемов.
	 */
	public void dropItems()
	{
		// получаем локальные объекты
		LocalObjects local = LocalObjects.get();

		// получаем список итемов
		Array<ItemInstance> itemList = local.getNextItemList();

		// опредкляем шанс выпадения
		int chance = 5;

		// список донат итемов
		int[] donat = Config.WORLD_DONATE_ITEMS;

		// получаем экиперовку игрока
		Equipment equipment = getEquipment();

		// выпало ли что-то из эквипа
		boolean equipDrop = false;

		// если экиперовка есть
		if(equipment != null)
		{
			equipment.lock();
			try
			{
				// получаем все слоты
				Slot[] slots = equipment.getSlots();

				// перебираем слоты
				for(int i = 0, length = slots.length; i < length; i++)
				{
					// получаем слот
					Slot slot = slots[i];

					// получаем итем в слоте
					ItemInstance item = slot.getItem();

					// если итема нет, либо шанс не выпал, пропускаем
					if(item == null || Arrays.contains(donat, item.getItemId()) || !Rnd.chance(chance))
						continue;

					// удаляем итем из слота
					slot.setItem(null);

					// ложим в список
					itemList.add(item);

					// помечаем что эквип дропнулся
					equipDrop = true;
				}
			}
			finally
			{
				equipment.unlock();
			}
		}

		// получаем инвентарь игрока
		Inventory inventory = getInventory();

		// флаг дропнутости итема из инвенторя
		boolean inventoryDrop = false;

		// если инвентарь есть
		if(inventory != null)
		{
			inventory.lock();
			try
			{
				// получаем все ячейки
				Cell[] cells = inventory.getCells();

				// перебираем ячейки
				for(int i = 0, length = cells.length; i < length; i++)
				{
					Cell cell = cells[i];

					// получаем итем в ячейке
					ItemInstance item = cell.getItem();

					// если итема нет, либо шанс не выпал, пропускаем
					if(item == null || item.isStackable() || Arrays.contains(donat, item.getItemId()) || !Rnd.chance(chance))
						continue;

					// удаляем итем из ячейки
					cell.setItem(null);

					// ложим в список
					itemList.add(item);

					inventoryDrop = true;
				}
			}
			finally
			{
				inventory.unlock();
			}
		}

		// если есть дропнутые итемы
		if(!itemList.isEmpty())
		{
			// получаем менеджера БД
			DataBaseManager dbManager = DataBaseManager.getInstance();

			// получаем массив итемов
			ItemInstance[] array = itemList.array();

			// перебираем итемы
			for(int i = 0, length = itemList.size(); i < length; i++)
			{
				ItemInstance item = array[i];

				// зануляем владельца
				item.setOwnerId(0);

				// обновляем положение
				item.setLocation(ItemLocation.NONE);

				// указываем кто дропнул
				item.setDropper(this);

				// обновляем в БД
				dbManager.updateLocationItem(item);
			}

			Npc.spawnDropItems(this, array, itemList.size());

			// получаем менеджера событий
			ObjectEventManager eventManager = ObjectEventManager.getInstance();

			if(equipDrop)
				eventManager.notifyEquipmentChanged(this);
			else if(inventoryDrop)
				eventManager.notifyInventoryChanged(this);
		}
	}

	/**
	 * Очитска кармы при убийстве НПС.
	 *
	 * @param npc убитый НПС.
	 */
//	public void clearKarma(Npc npc)
//	{
//		if(npc.getExp() < 20)
//			return;
//
//		// рассчитываем допустимую разницу по уравням
//		int clear = Math.max(Math.min(getLevel() - npc.getLevel(), 5), 0);
//
//		// рассчитываем кол-во очищеной кармы
//		clear = Math.min((5 - clear) * 50 * npc.getKarmaMod(), getKarma());
//
//		// если есть очищенная карма
//		if(clear > 0)
//		{
//			// очищаем
//			setKarma(getKarma() - clear);
//
//			// сообщаем об очищении
//			sendMessage("Вы очистили " + clear + " кармы, у вас осталось " + getKarma() + ".");
//		}
//
//		// если очистились от ПК
//		if(!isPK())
//		{
//			// обновляем цвета ников
//			updateColor();
//
//			// обновляем себе свой цвет ника
//			sendPacket(NameColor.getInstance(getColor(), this), true);
//		}
//	}

	@Override
	public boolean isRangeClass()
	{
		return getPlayerClass().isRange();
	}
}