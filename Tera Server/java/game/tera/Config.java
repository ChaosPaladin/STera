package tera;

import java.io.File;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rlib.geoengine.GeoConfig;
import rlib.logging.Loggers;
import rlib.util.Files;
import rlib.util.Strings;
import rlib.util.Util;
import rlib.util.VarTable;
import rlib.util.array.Arrays;
import tera.gameserver.document.DocumentConfig;

import com.jolbox.bonecp.BoneCPConfig;

/**
 * Конфиг сервера
 * 
 * @author Ronn
 * @created 11.03.2012
 */
public final class Config
{
	/** ---------------------------- Настройки аккаунтов ---------------------------------- */

	/** минимальный уровень прав для входа на сервер */
	public static int ACCOUNT_MIN_ACCESS_LEVEL;

	/** рейт базовых рейтов при проплаченном аккаунте */
	public static float ACCOUNT_PREMIUM_EXP_RATE;
	/** рейт базовых рейтов при проплаченном аккаунте */
	public static float ACCOUNT_PREMIUM_MONEY_RATE;
	/** рейт базовых рейтов при проплаченном аккаунте */
	public static float ACCOUNT_PREMIUM_DROP_RATE;
	/** рейт базовых рейтов при проплаченном аккаунте */
	public static float ACCOUNT_PREMIUM_QUEST_RATE;

	/** включено ли автосоздание аккаунтов при входе в игру */
	public static boolean ACCOUNT_AUTO_CREATE;
	/** могут ли входить только проплаченные аккаунты */
	public static boolean ACCOUNT_ONLY_PAID;
	/** имеют ли проплаченные аккаунты бонуск экспы */
	public static boolean ACCOUNT_PREMIUM_EXP;
	/** имеют ли проплаченные аккаунты бонус денег */
	public static boolean ACCOUNT_PREMIUM_MONEY;
	/** имеют ли проплаченные аккаунты бонус дропа */
	public static boolean ACCOUNT_PREMIUM_DROP;
	/** имеют ли проплаченные аккаунты бонус дропа */
	public static boolean ACCOUNT_PREMIUM_QUEST;

	/** ---------------------------- Настройки сервера ---------------------------------- */

	/** допустимые ники на сервере */
	public static String SERVER_NAME_TEMPLATE;
	/** версия сборки сервера */
	public static String SERVER_VERSION;
	/** адресс вывода в фаил онлайна сервера */
	public static String SERVER_ONLINE_FILE;

	/** саб ид игрока */
	public static int SERVER_PLAYER_SUB_ID;
	/** саб ид нпс */
	public static int SERVER_NPC_SUB_ID;
	/** саб ид итема */
	public static int SERVER_ITEM_SUB_ID;
	/** саб ид выстрела */
	public static int SERVER_SHOT_SUB_ID;
	/** саб ид объекта */
	public static int SERVER_OBJECT_SUB_ID;
	/** саб ид ловушек */
	public static int SERVER_TRAP_SUB_ID;
	/** саб ид ловушек */
	public static int SERVER_RESOURSE_SUB_ID;
	/** порт сервера */
	public static int SERVER_PORT;

	/** множитель получаемого опыта */
	public static float SERVER_RATE_EXP;
	/** множитель получаемого опыта в группе */
	public static float SERVER_PARTY_RATE_EXP;
	/** множитель дропнутых денег */
	public static float SERVER_RATE_MONEY;
	/** множитель выпадающего дропа */
	public static float SERVER_RATE_DROP_ITEM;
	/** множитель награды за квесты */
	public static float SERVER_RATE_QUEST_REWARD;
	/** % бонуса к онлайну сервера */
	public static float SERVER_ONLINE_FAKE;

	/** использовать опкоды снифера */
	public static boolean SERVER_USE_SNIFFER_OPCODE;
	/** использовать ли реальный рандоминайзер для дропа */
	public static boolean SERVER_DROP_REAL_RANDOM;
	/** использовать ли реальный рандоминайзер для критов */
	public static boolean SERVER_CRIT_REAL_RANDOM;
	/** использовать ли реальный рандоминайзер для эффектов */
	public static boolean SERVER_EFFECT_REAL_RANDOM;
	/** использовать ли реальный рандоминайзер для функций */
	public static boolean SERVER_FUNC_REAL_RANDOM;
	/** использовать ли реальный рандоминайзер для дэмага */
	public static boolean SERVER_DAMAGE_REAL_RANDOM;
	/** использовать ли реальный рандоминайзер для опрокидывания */
	public static boolean SERVER_OWERTURN_REAL_RANDOM;

	/** ---------------------------- Настройки гео движка ------------------------------------- */

	/** размер гео карты по Х */
	public static int GEO_ENGINE_OFFSET_X;
	/** размер гео карты по Y */
	public static int GEO_ENGINE_OFFSET_Y;
	/** размер гео квадрата */
	public static int GEO_ENGINE_QUARD_SIZE;
	/** высота гео квадрата */
	public static int GEO_ENGINE_QUARD_HEIGHT;

	/** ---------------------------- Настройки игрового мира ---------------------------------- */

	/** время жизни выпавшего итема */
	public static int WORLD_LIFE_TIME_DROP_ITEM;
	/** время блокировки выпавшего итема на поднятие чужаком */
	public static int WORLD_BLOCK_TIME_DROP_ITEM;
	/** минимальный уровень доступа для входа в мир */
	public static int WORLD_MIN_ACCESS_LEVEL;
	/** через сколько успешных атак уменьшать кол-во сердечек */
	public static int WORLD_PLAYER_THRESHOLD_ATTACKS;
	/** через сколько успешных отражений атак уменьшать кол-во сердечек */
	public static int WORLD_PLAYER_THRESHOLD_BLOOKS;
	/** время нахождения в боевой стойке */
	public static int WORLD_PLAYER_TIME_BATTLE_STANCE;
	/** максимальная разница уровня игрока-моб для выпадения дропа */
	public static int WORLD_MAX_DIFF_LEVEL_ON_DROP;
	/** ширина региона в мире */
	public static int WORLD_WIDTH_REGION;
	/** высота региона в мире */
	public static int WORLD_HEIGHT_REGION;
	/** максимальный онлайн */
	public static int WORLD_MAXIMUM_ONLINE;
	/** размер банков игроков */
	public static int WORLD_BANK_MAX_SIZE;
	/** размер банков гильдии */
	public static int WORLD_GUILD_BANK_MAX_SIZE;
	/** максимально доступный уровень игрока */
	public static int WORLD_PLAYER_MAX_LEVEL;
	/** шанс удаления кристала при смерти */
	public static int WORLD_CHANCE_DELETE_CRYSTAL;
	/** максимальный уровень сбора */
	public static int WORLD_MAX_COLLECT_LEVEL;
	/** кол-во континентов в мире */
	public static int WORLD_CONTINENT_COUNT;
	/** максимальная дистанция для работы трейда */
	public static int WORLD_TRADE_MAX_RANGE;
	/** максимальная дистанция для работы дуэли */
	public static int WORLD_DUEL_MAX_RANGE;
	/** максимальная дистанция для приглашения в ГИ */
	public static int WORLD_GUILD_INVITE_MAX_RANGE;
	/** максимально допустимая рассинхронизация скилов */
	public static int WORLD_MAX_SKILL_DESYNC;
	/** шанс заточки предмета */
	public static int WORLD_ENCHANT_ITEM_CHANCE;

	/** глобальный модификатор отката мили скилов */
	public static float WORLD_SHORT_SKILL_REUSE_MOD;
	/** глобальный модификатор отката дальних скилов */
	public static float WORLD_RANGE_SKILL_REUSE_MOD;
	/** глобальный модификатор отката остальных скилов */
	public static float WORLD_OTHER_SKILL_REUSE_MOD;

	/** список донат итемов */
	public static int[] WORLD_DONATE_ITEMS;

	/** модификатор цен вмагазине */
	public static float WORLD_SHOP_PRICE_MOD;

	/** изучать ли скилы автоматически */
	public static boolean WORLD_AUTO_LEARN_SKILLS;
	/** доступно ли ПК на сервере */
	public static boolean WORLD_PK_AVAILABLE;
	/** изучать только реализованные скилы */
	public static boolean WORLD_LEARN_ONLY_IMPLEMENTED_SKILLS;

	/** ---------------------------- Настройки базы данных ---------------------------------- */

	/** экземпляр конфига для БД */
	public static final BoneCPConfig DATA_BASE_CONFIG = new BoneCPConfig();

	/** класс драйвера базы данных */
	public static String DATA_BASE_DRIVER;
	/** адресс базы данных */
	public static String DATA_BASE_URL;
	/** логин для доступа к базе данных */
	public static String DATA_BASE_LOGIN;
	/** пароль для доступа к базе данных */
	public static String DATA_BASE_PASSWORD;

	/** максимальное кол-во коннектов к базе в пуле */
	public static int DATA_BASE_MAX_CONNECTIONS;
	/** максимальное кол-во создаваемых statements */
	public static int DATA_BASE_MAX_STATEMENTS;

	/** чистить ли БД при старте сервера */
	public static boolean DATA_BASE_CLEANING_START;

	/** ---------------------------- Настройки потоков ---------------------------------- */

	/** размер пула основных потоков */
	public static int THREAD_POOL_SIZE_GENERAL;
	/** размер пула потоков движения */
	public static int THREAD_POOL_SIZE_MOVE;
	/** размер пула потоков АИ */
	;
	public static int THREAD_POOL_SIZE_AI;
	/** размер пула потоков применения скилов */
	;
	public static int THREAD_POOL_SIZE_SKILL_USE;
	/** размер пула потоков каста скилов */
	;
	public static int THREAD_POOL_SIZE_SKILL_CAST;
	/** размер пула потоков передвежения скилов */
	;
	public static int THREAD_POOL_SIZE_SKILL_MOVE;
	/** размер пула потоков, исполняющий асинхронно клиентские пакеты */
	public static int THREAD_POOL_PACKET_RUNNER;

	/** ---------------------------- Настройки для разработки ---------------------------------- */

	/** установки принудительной скорости атаки */
	public static int DEVELOPER_FORCE_ATTACK_SPEED;

	/** вывод дебаг инфы о пересылке клиент пакетов */
	public static boolean DEVELOPER_DEBUG_CLIENT_PACKETS;
	/** вывод дебаг инфы о пересылке серверных пакетов */
	public static boolean DEVELOPER_DEBUG_SERVER_PACKETS;
	/** вывод общей дебаг инфы */
	public static boolean DEVELOPER_MAIN_DEBUG;
	/** вывод дебага связанного с расчетом таргетов скилов */
	public static boolean DEVELOPER_DEBUG_TARGET_TYPE;
	/** вывод дебага связанного с кастом скила */
	public static boolean DEVELOPER_DEBUG_CASTING_SKILL;
	/** вывод дебага связанного с перемещением игрока */
	public static boolean DEVELOPER_DEBUG_MOVING_PLAYER;
	/** вывод дебага связанного с перемещением нпс */
	public static boolean DEVELOPER_DEBUG_MOVING_NPC;
	/** активировать ли логирование перемещения игроков */
	public static boolean DEVELOPER_GEO_LOGING;

	/** ---------------------------- Настройки АИ ---------------------------------- */

	/** максимальный радиус действия моба */
	public static int AI_MAX_ACTIVE_RANGE;
	/** максимальный радиус реагирования моба */
	public static int AI_MAX_REACTION_RANGE;
	/** минимальный ренж случайного брождения от точки респавна */
	public static int AI_MIN_RANDOM_WALK_RANGE;
	/** максимальный ренж случайного брождения от точки спавна */
	public static int AI_MAX_RANDOM_WALK_RANGE;
	/** время между брождением */
	public static int AI_MIN_RANDOM_WALK_DELAY;
	/** время между брождением */
	public static int AI_MAX_RANDOM_WALK_DELAY;
	/** промежуток между выполнением АИ тасков */
	public static int AI_TASK_DELAY;

	/** шансы использовани скилов */
	public static int AI_ATTACK_RATE;
	public static int AI_BUFF_RATE;
	public static int AI_DEBUFF_RATE;
	public static int AI_DEFENSE_RATE;
	public static int AI_JUMP_RATE;
	public static int AI_ULTIMATE_RATE;
	public static int AI_SIDE_RATE;
	public static int AI_SPRINT_RATE;

	/** ---------------------------- Настройки асинхронной сети ---------------------------------- */
	/**
	 * /** кол-во потоков в асинхронной сети
	 */
	public static int NETWORK_GROUP_SIZE;
	/** приоритет потоков асинхронной сети */
	public static int NETWORK_THREAD_PRIORITY;
	/** размер буффера клиентских пакетов */
	public static int NETWORK_READ_BUFFER_SIZE;
	/** размер буфера серверных пакетов */
	public static int NETWORK_WRITE_BUFFER_SIZE;
	/** максимальное число разрезаемых пакетов */
	public static int NETWORK_MAXIMUM_PACKET_CUT;

	/** отображать ли эксепшены читаемой сети */
	public static boolean NETWORK_VISIBLE_READ_EXCEPTION;
	/** отображать ли эксепшены записываемой сети */
	public static boolean NETWORK_VISIBLE_WRITE_EXCEPTION;

	/** --------------------------------------- Настройки ивентов ----------------------------------------- */

	/** минимальный интервал между авто ивентами */
	public static int EVENT_MIN_TIMEOUT;
	/** максимальный интервал между авто ивентами */
	public static int EVENT_MAX_TIMEOUT;

	/** время регистрации на ТвТ */
	public static int EVENT_TVT_REGISTER_TIME;
	/** время боя на ТвТ */
	public static int EVENT_TVT_BATTLE_TIME;
	/** минимальное кол-в игроков на ТвТ */
	public static int EVENT_TVT_MIN_PLAYERS;
	/** максимальное кол-во игроков на ТвТ */
	public static int EVENT_TVT_MAX_PLAYERS;
	/** минимальный уровень для ТвТ */
	public static int EVENT_TVT_MIN_LEVEL;
	/** максимальный уровень для ТвТ */
	public static int EVENT_TVT_MAX_LEVEL;

	/** время регистрации на ЛХ */
	public static int EVENT_LH_REGISTER_TIME;
	/** время боя на ЛХ */
	public static int EVENT_LH_BATTLE_TIME;
	/** минимальное кол-в игроков на ЛХ */
	public static int EVENT_LH_MIN_PLAYERS;
	/** максимальное кол-во игроков на ЛХ */
	public static int EVENT_LH_MAX_PLAYERS;
	/** минимальный уровень для ЛХ */
	public static int EVENT_LH_MIN_LEVEL;
	/** максимальный уровень для ЛХ */
	public static int EVENT_LH_MAX_LEVEL;

	/** время регистрации на ТМТ */
	public static int EVENT_TMT_REGISTER_TIME;
	/** время боя на ТМТ */
	public static int EVENT_TMT_BATTLE_TIME;
	/** минимальное кол-в команд на ТМТ */
	public static int EVENT_TMT_MIN_TEAMS;
	/** максимальное кол-во команд на ТМТ */
	public static int EVENT_TMT_MAX_TEAMS;
	/** минимальное кол-в игроков в команде на ТМТ */
	public static int EVENT_TMT_MIN_TEAM_SIZE;
	/** максимальное кол-во игроков в команде на ТМТ */
	public static int EVENT_TMT_MAX_TEAM_SIZE;
	/** минимальный уровень для ТМТ */
	public static int EVENT_TMT_MIN_LEVEL;
	/** максимальный уровень для ТМТ */
	public static int EVENT_TMT_MAX_LEVEL;

	/** время регистрации на твМ */
	public static int EVENT_EB_REGISTER_TIME;
	/** время боя на твМ */
	public static int EVENT_EB_BATTLE_TIME;
	/** минимальное кол-в игроков на твМ */
	public static int EVENT_EB_MIN_PLAYERS;
	/** максимальное кол-во игроков на ТвтвМТ */
	public static int EVENT_EB_MAX_PLAYERS;
	/** минимальный уровень для твМ */
	public static int EVENT_EB_MIN_LEVEL;
	/** максимальный уровень для твМ */
	public static int EVENT_EB_MAX_LEVEL;

	/** сколько денег за 1 очко славы */
	public static int EVENT_HERO_POINT_TO_GOLD;

	/** ---------------------------- Настройки дистанционного управления ---------------------------------- */

	/** логин для подключения уонтрола */
	public static String DIST_CONTROL_LOGIN;
	/** пароль для подключения уонтрола */
	public static String DIST_CONTROL_PASSWORD;

	/** порт сервера дистанционного управления */
	public static int DIST_CONTROL_PORT;
	/** интервал в цикле проверке состояния клиента */
	public static int DIST_CONTROL_CLIENT_INTERVAL;

	/** включено ли дистанционное управление */
	public static boolean DIST_CONTROL_ENABLED;

	/** ---------------------------- Настройки ивентов ---------------------------------- */

	public static String EVENT_TEAM_VS_TEAM_NAME;

	public static int EVENT_TEAM_VS_TEAM_MIN_COUNT_PLAYERS;
	public static int EVENT_TEAM_VS_TEAM_MAX_COUNT_PLAYERS;

	/** ---------------------------- Остальное ---------------------------------- */

	public static String SERVER_DIR;

	/** конфигурация для гео движка */
	public static GeoConfig GEO_CONFIG = new GeoConfig()
	{
		@Override
		public int getOffsetX()
		{
			return GEO_ENGINE_OFFSET_X;
		}

		@Override
		public int getOffsetY()
		{
			return GEO_ENGINE_OFFSET_Y;
		}

		@Override
		public int getQuardHeight()
		{
			return GEO_ENGINE_QUARD_HEIGHT;
		}

		@Override
		public int getQuardSize()
		{
			return GEO_ENGINE_QUARD_SIZE;
		}

		@Override
		public int getSplit()
		{
			return 0;
		}
	};

	private static Pattern namePattern;

	/**
	 * Проверка корректности имения чего-либо.
	 * 
	 * @param name проверяемое имя.
	 * @return проходит ли проверку для сервера.
	 */
	public static final boolean checkName(String name)
	{
		Matcher metcher = namePattern.matcher(name);

		return metcher.matches();
	}

	/**
	 * инициализация конфига
	 */
	public static void init()
	{
		// получаем расположение сборки сервера
		SERVER_DIR = Util.getRootPath();

		SERVER_DIR = ".";

		// получаем версию ядра
		SERVER_VERSION = "rev. " + (new File(SERVER_DIR + "/libs/stera.jar").lastModified() / 1000 / 60 % 1000000 - 600000);

		// создаем табицу статов
		VarTable vars = VarTable.newInstance();

		Loggers.info("Config", "dir : " + SERVER_DIR + ", " + SERVER_VERSION);

		// получаем все файлы конфига
		File[] files = Files.getFiles(new File(SERVER_DIR + "/config"));

		// парсим весь конфиг
		parseFiles(files, vars);

		ACCOUNT_MIN_ACCESS_LEVEL = vars.getInteger("ACCOUNT_MIN_ACCESS_LEVEL");

		ACCOUNT_PREMIUM_EXP_RATE = vars.getFloat("ACCOUNT_PREMIUM_EXP_RATE");
		ACCOUNT_PREMIUM_MONEY_RATE = vars.getFloat("ACCOUNT_PREMIUM_MONEY_RATE");
		ACCOUNT_PREMIUM_DROP_RATE = vars.getFloat("ACCOUNT_PREMIUM_DROP_RATE");
		ACCOUNT_PREMIUM_QUEST_RATE = vars.getFloat("ACCOUNT_PREMIUM_QUEST_RATE");

		ACCOUNT_AUTO_CREATE = vars.getBoolean("ACCOUNT_AUTO_CREATE");
		ACCOUNT_ONLY_PAID = vars.getBoolean("ACCOUNT_ONLY_PAID");
		ACCOUNT_PREMIUM_EXP = vars.getBoolean("ACCOUNT_PREMIUM_EXP");
		ACCOUNT_PREMIUM_MONEY = vars.getBoolean("ACCOUNT_PREMIUM_MONEY");
		ACCOUNT_PREMIUM_DROP = vars.getBoolean("ACCOUNT_PREMIUM_DROP");
		ACCOUNT_PREMIUM_QUEST = vars.getBoolean("ACCOUNT_PREMIUM_QUEST");

		SERVER_NAME_TEMPLATE = vars.getString("SERVER_NAME_TEMPLATE");
		SERVER_ONLINE_FILE = vars.getString("SERVER_ONLINE_FILE", Strings.EMPTY);

		SERVER_PLAYER_SUB_ID = vars.getInteger("SERVER_PLAYER_SUB_ID");
		SERVER_NPC_SUB_ID = vars.getInteger("SERVER_NPC_SUB_ID");
		SERVER_ITEM_SUB_ID = vars.getInteger("SERVER_ITEM_SUB_ID");
		SERVER_SHOT_SUB_ID = vars.getInteger("SERVER_SHOT_SUB_ID");
		SERVER_OBJECT_SUB_ID = vars.getInteger("SERVER_OBJECT_SUB_ID");
		SERVER_TRAP_SUB_ID = vars.getInteger("SERVER_TRAP_SUB_ID");
		SERVER_RESOURSE_SUB_ID = vars.getInteger("SERVER_RESOURSE_SUB_ID");
		SERVER_PORT = vars.getInteger("SERVER_PORT");

		GEO_ENGINE_OFFSET_X = vars.getInteger("GEO_ENGINE_OFFSET_X");
		GEO_ENGINE_OFFSET_Y = vars.getInteger("GEO_ENGINE_OFFSET_Y");
		GEO_ENGINE_QUARD_SIZE = vars.getInteger("GEO_ENGINE_QUARD_SIZE");
		GEO_ENGINE_QUARD_HEIGHT = vars.getInteger("GEO_ENGINE_QUARD_HEIGHT");

		SERVER_RATE_EXP = vars.getFloat("SERVER_RATE_EXP");
		SERVER_PARTY_RATE_EXP = vars.getFloat("SERVER_PARTY_RATE_EXP");
		SERVER_RATE_MONEY = vars.getFloat("SERVER_RATE_MONEY");
		SERVER_RATE_DROP_ITEM = vars.getFloat("SERVER_RATE_DROP_ITEM");
		SERVER_RATE_QUEST_REWARD = vars.getFloat("SERVER_RATE_QUEST_REWARD");
		SERVER_ONLINE_FAKE = vars.getFloat("SERVER_ONLINE_FAKE");

		SERVER_USE_SNIFFER_OPCODE = vars.getBoolean("SERVER_USE_SNIFFER_OPCODE");
		SERVER_DROP_REAL_RANDOM = vars.getBoolean("SERVER_DROP_REAL_RANDOM");
		SERVER_CRIT_REAL_RANDOM = vars.getBoolean("SERVER_CRIT_REAL_RANDOM");
		SERVER_EFFECT_REAL_RANDOM = vars.getBoolean("SERVER_EFFECT_REAL_RANDOM");
		SERVER_FUNC_REAL_RANDOM = vars.getBoolean("SERVER_FUNC_REAL_RANDOM");
		SERVER_DAMAGE_REAL_RANDOM = vars.getBoolean("SERVER_DAMAGE_REAL_RANDOM");
		SERVER_OWERTURN_REAL_RANDOM = vars.getBoolean("SERVER_OWERTURN_REAL_RANDOM");

		WORLD_LIFE_TIME_DROP_ITEM = vars.getInteger("WORLD_LIFE_TIME_DROP_ITEM");
		WORLD_BLOCK_TIME_DROP_ITEM = vars.getInteger("WORLD_BLOCK_TIME_DROP_ITEM");
		WORLD_MIN_ACCESS_LEVEL = vars.getInteger("WORLD_MIN_ACCESS_LEVEL");
		WORLD_PLAYER_THRESHOLD_ATTACKS = vars.getInteger("WORLD_PLAYER_THRESHOLD_ATTACKS");
		WORLD_PLAYER_THRESHOLD_BLOOKS = vars.getInteger("WORLD_PLAYER_THRESHOLD_BLOOKS");
		WORLD_PLAYER_TIME_BATTLE_STANCE = vars.getInteger("WORLD_PLAYER_TIME_BATTLE_STANCE") * 1000;
		WORLD_MAX_DIFF_LEVEL_ON_DROP = vars.getInteger("WORLD_MAX_DIFF_LEVEL_ON_DROP");
		WORLD_WIDTH_REGION = vars.getInteger("WORLD_WIDTH_REGION");
		WORLD_HEIGHT_REGION = vars.getInteger("WORLD_HEIGHT_REGION");
		WORLD_MAXIMUM_ONLINE = vars.getInteger("WORLD_MAXIMUM_ONLINE");
		WORLD_BANK_MAX_SIZE = vars.getInteger("WORLD_BANK_MAX_SIZE");
		WORLD_GUILD_BANK_MAX_SIZE = vars.getInteger("WORLD_GUILD_BANK_MAX_SIZE");
		WORLD_MAX_COLLECT_LEVEL = vars.getInteger("WORLD_MAX_COLLECT_LEVEL");
		WORLD_CONTINENT_COUNT = vars.getInteger("WORLD_CONTINENT_COUNT");
		WORLD_TRADE_MAX_RANGE = vars.getInteger("WORLD_TRADE_MAX_RANGE");
		WORLD_DUEL_MAX_RANGE = vars.getInteger("WORLD_DUEL_MAX_RANGE");
		WORLD_GUILD_INVITE_MAX_RANGE = vars.getInteger("WORLD_GUILD_INVITE_MAX_RANGE");
		WORLD_MAX_SKILL_DESYNC = vars.getInteger("WORLD_MAX_SKILL_DESYNC") * vars.getInteger("WORLD_MAX_SKILL_DESYNC");
		WORLD_ENCHANT_ITEM_CHANCE = vars.getInteger("WORLD_ENCHANT_ITEM_CHANCE");
		WORLD_PLAYER_MAX_LEVEL = vars.getInteger("WORLD_PLAYER_MAX_LEVEL");
		WORLD_CHANCE_DELETE_CRYSTAL = vars.getInteger("WORLD_CHANCE_DELETE_CRYSTAL", 13);

		WORLD_SHORT_SKILL_REUSE_MOD = vars.getFloat("WORLD_SHORT_SKILL_REUSE_MOD");
		WORLD_RANGE_SKILL_REUSE_MOD = vars.getFloat("WORLD_RANGE_SKILL_REUSE_MOD");
		WORLD_OTHER_SKILL_REUSE_MOD = vars.getFloat("WORLD_OTHER_SKILL_REUSE_MOD");

		WORLD_DONATE_ITEMS = vars.getIntegerArray("WORLD_DONATE_ITEMS", ",", Arrays.toIntegerArray());

		WORLD_SHOP_PRICE_MOD = vars.getFloat("WORLD_SHOP_PRICE_MOD");

		WORLD_AUTO_LEARN_SKILLS = vars.getBoolean("WORLD_AUTO_LEARN_SKILLS");
		WORLD_PK_AVAILABLE = vars.getBoolean("WORLD_PK_AVAILABLE");
		WORLD_LEARN_ONLY_IMPLEMENTED_SKILLS = vars.getBoolean("WORLD_LEARN_ONLY_IMPLEMENTED_SKILLS");

		DATA_BASE_DRIVER = vars.getString("DATA_BASE_DRIVER");
		DATA_BASE_URL = vars.getString("DATA_BASE_URL");
		DATA_BASE_LOGIN = vars.getString("DATA_BASE_LOGIN");
		DATA_BASE_PASSWORD = vars.getString("DATA_BASE_PASSWORD");

		DATA_BASE_MAX_CONNECTIONS = vars.getInteger("DATA_BASE_MAX_CONNECTIONS");
		DATA_BASE_MAX_STATEMENTS = vars.getInteger("DATA_BASE_MAX_STATEMENTS");
		DATA_BASE_CLEANING_START = vars.getBoolean("DATA_BASE_CLEANING_START");

		THREAD_POOL_SIZE_GENERAL = vars.getInteger("THREAD_POOL_SIZE_GENERAL");
		THREAD_POOL_SIZE_MOVE = vars.getInteger("THREAD_POOL_SIZE_MOVE");
		THREAD_POOL_SIZE_AI = vars.getInteger("THREAD_POOL_SIZE_AI");
		THREAD_POOL_PACKET_RUNNER = vars.getInteger("THREAD_POOL_PACKET_RUNNER");
		THREAD_POOL_SIZE_SKILL_USE = vars.getInteger("THREAD_POOL_SIZE_SKILL_USE");
		THREAD_POOL_SIZE_SKILL_CAST = vars.getInteger("THREAD_POOL_SIZE_SKILL_CAST");
		THREAD_POOL_SIZE_SKILL_MOVE = vars.getInteger("THREAD_POOL_SIZE_SKILL_MOVE");

		DEVELOPER_FORCE_ATTACK_SPEED = vars.getInteger("DEVELOPER_FORCE_ATTACK_SPEED");
		DEVELOPER_DEBUG_CLIENT_PACKETS = vars.getBoolean("DEVELOPER_DEBUG_CLIENT_PACKETS");
		DEVELOPER_DEBUG_SERVER_PACKETS = vars.getBoolean("DEVELOPER_DEBUG_SERVER_PACKETS");
		DEVELOPER_MAIN_DEBUG = vars.getBoolean("DEVELOPER_MAIN_DEBUG");
		DEVELOPER_DEBUG_TARGET_TYPE = vars.getBoolean("DEVELOPER_DEBUG_TARGET_TYPE");
		DEVELOPER_DEBUG_CASTING_SKILL = vars.getBoolean("DEVELOPER_DEBUG_CASTING_SKILL");
		DEVELOPER_DEBUG_MOVING_PLAYER = vars.getBoolean("DEVELOPER_DEBUG_MOVING_PLAYER");
		DEVELOPER_DEBUG_MOVING_NPC = vars.getBoolean("DEVELOPER_DEBUG_MOVING_NPC");
		DEVELOPER_GEO_LOGING = vars.getBoolean("DEVELOPER_GEO_LOGING");

		EVENT_MIN_TIMEOUT = vars.getInteger("EVENT_MIN_TIMEOUT");
		EVENT_MAX_TIMEOUT = vars.getInteger("EVENT_MAX_TIMEOUT");
		EVENT_TVT_REGISTER_TIME = vars.getInteger("EVENT_TVT_REGISTER_TIME");
		EVENT_TVT_BATTLE_TIME = vars.getInteger("EVENT_TVT_BATTLE_TIME");
		EVENT_TVT_MIN_PLAYERS = vars.getInteger("EVENT_TVT_MIN_PLAYERS");
		EVENT_TVT_MAX_PLAYERS = vars.getInteger("EVENT_TVT_MAX_PLAYERS");
		EVENT_TVT_MIN_LEVEL = vars.getInteger("EVENT_TVT_MIN_LEVEL");
		EVENT_TVT_MAX_LEVEL = vars.getInteger("EVENT_TVT_MAX_LEVEL");
		EVENT_LH_REGISTER_TIME = vars.getInteger("EVENT_LH_REGISTER_TIME");
		EVENT_LH_BATTLE_TIME = vars.getInteger("EVENT_LH_BATTLE_TIME");
		EVENT_LH_MIN_PLAYERS = vars.getInteger("EVENT_LH_MIN_PLAYERS");
		EVENT_LH_MAX_PLAYERS = vars.getInteger("EVENT_LH_MAX_PLAYERS");
		EVENT_LH_MIN_LEVEL = vars.getInteger("EVENT_LH_MIN_LEVEL");
		EVENT_LH_MAX_LEVEL = vars.getInteger("EVENT_LH_MAX_LEVEL");
		EVENT_TMT_REGISTER_TIME = vars.getInteger("EVENT_TMT_REGISTER_TIME");
		EVENT_TMT_BATTLE_TIME = vars.getInteger("EVENT_TMT_BATTLE_TIME");
		EVENT_TMT_MIN_TEAMS = vars.getInteger("EVENT_TMT_MIN_TEAMS");
		EVENT_TMT_MAX_TEAMS = vars.getInteger("EVENT_TMT_MAX_TEAMS");
		EVENT_TMT_MIN_TEAM_SIZE = vars.getInteger("EVENT_TMT_MIN_TEAM_SIZE");
		EVENT_TMT_MAX_TEAM_SIZE = vars.getInteger("EVENT_TMT_MAX_TEAM_SIZE");
		EVENT_TMT_MIN_LEVEL = vars.getInteger("EVENT_TMT_MIN_LEVEL");
		EVENT_TMT_MAX_LEVEL = vars.getInteger("EVENT_TMT_MAX_LEVEL");
		EVENT_EB_REGISTER_TIME = vars.getInteger("EVENT_EB_REGISTER_TIME");
		EVENT_EB_BATTLE_TIME = vars.getInteger("EVENT_EB_BATTLE_TIME");
		EVENT_EB_MIN_PLAYERS = vars.getInteger("EVENT_EB_MIN_PLAYERS");
		EVENT_EB_MAX_PLAYERS = vars.getInteger("EVENT_EB_MAX_PLAYERS");
		EVENT_EB_MIN_LEVEL = vars.getInteger("EVENT_EB_MIN_LEVEL");
		EVENT_EB_MAX_LEVEL = vars.getInteger("EVENT_EB_MAX_LEVEL");
		EVENT_HERO_POINT_TO_GOLD = vars.getInteger("EVENT_HERO_POINT_TO_GOLD");

		AI_MAX_ACTIVE_RANGE = vars.getInteger("AI_MAX_ACTIVE_RANGE");
		AI_MAX_REACTION_RANGE = vars.getInteger("AI_MAX_REACTION_RANGE");
		AI_MIN_RANDOM_WALK_RANGE = vars.getInteger("AI_MIN_RANDOM_WALK_RANGE");
		AI_MAX_RANDOM_WALK_RANGE = vars.getInteger("AI_MAX_RANDOM_WALK_RANGE");
		AI_MIN_RANDOM_WALK_DELAY = vars.getInteger("AI_MIN_RANDOM_WALK_DELAY");
		AI_MAX_RANDOM_WALK_DELAY = vars.getInteger("AI_MAX_RANDOM_WALK_DELAY");
		AI_TASK_DELAY = vars.getInteger("AI_TASK_DELAY");

		AI_ATTACK_RATE = vars.getInteger("AI_ATTACK_RATE");
		AI_BUFF_RATE = vars.getInteger("AI_BUFF_RATE");
		AI_DEBUFF_RATE = vars.getInteger("AI_DEBUFF_RATE");
		AI_DEFENSE_RATE = vars.getInteger("AI_DEFENSE_RATE");
		AI_JUMP_RATE = vars.getInteger("AI_JUMP_RATE");
		AI_ULTIMATE_RATE = vars.getInteger("AI_ULTIMATE_RATE");
		AI_SIDE_RATE = vars.getInteger("AI_SIDE_RATE");
		AI_SPRINT_RATE = vars.getInteger("AI_SPRINT_RATE", 50);

		NETWORK_GROUP_SIZE = vars.getInteger("NETWORK_GROUP_SIZE");
		NETWORK_READ_BUFFER_SIZE = vars.getInteger("NETWORK_READ_BUFFER_SIZE");
		NETWORK_THREAD_PRIORITY = vars.getInteger("NETWORK_THREAD_PRIORITY");
		NETWORK_WRITE_BUFFER_SIZE = vars.getInteger("NETWORK_WRITE_BUFFER_SIZE");
		NETWORK_MAXIMUM_PACKET_CUT = vars.getInteger("NETWORK_MAXIMUM_PACKET_CUT");

		DIST_CONTROL_LOGIN = vars.getString("DIST_CONTROL_LOGIN");
		DIST_CONTROL_PASSWORD = vars.getString("DIST_CONTROL_PASSWORD");

		DIST_CONTROL_PORT = vars.getInteger("DIST_CONTROL_PORT");
		DIST_CONTROL_CLIENT_INTERVAL = vars.getInteger("DIST_CONTROL_CLIENT_INTERVAL");

		DIST_CONTROL_ENABLED = vars.getBoolean("DIST_CONTROL_ENABLED");

		DATA_BASE_CONFIG.setJdbcUrl(DATA_BASE_URL);
		DATA_BASE_CONFIG.setUsername(DATA_BASE_LOGIN);
		DATA_BASE_CONFIG.setPassword(DATA_BASE_PASSWORD);
		DATA_BASE_CONFIG.setAcquireRetryAttempts(0);
		DATA_BASE_CONFIG.setAcquireIncrement(5);
		DATA_BASE_CONFIG.setReleaseHelperThreads(0);
		DATA_BASE_CONFIG.setMinConnectionsPerPartition(2);
		DATA_BASE_CONFIG.setMaxConnectionsPerPartition(DATA_BASE_MAX_CONNECTIONS);
		DATA_BASE_CONFIG.setStatementsCacheSize(DATA_BASE_MAX_STATEMENTS);

		// настройки для мускул драйвера
		Properties properties = new Properties();

		// ставим принудительное использование UTF-8
		properties.setProperty("useUnicode", "true");
		properties.setProperty("characterEncoding", "UTF-8");

		// добавляем в конфиг мускул драйвера
		DATA_BASE_CONFIG.setDriverProperties(properties);

		namePattern = Pattern.compile(SERVER_NAME_TEMPLATE);

		Loggers.info("Config", "initialized.");
	}

	/**
	 * Чтение параметров с файлов
	 * 
	 * @param files
	 */
	private static void parseFiles(File[] files, VarTable vars)
	{
		// пробегаемся по всем файлам в папке конфига
		for (File file : files)
		{
			// если фаил скрытый, пропускаем
			if (file.isHidden())
				continue;

			// если папка дефолт, пропускаем
			if (file.isDirectory() && !file.getName().contains("defaults"))
			{
				parseFiles(file.listFiles(), vars);
				continue;
			}

			// если фаил хмл, парсим
			if (file.getName().endsWith(".xml"))
				vars.set(new DocumentConfig(file).parse());
		}
	}

	/**
	 * Перезагрузка конфига.
	 */
	public static void reload()
	{
		// создаем новую табицу статов
		VarTable vars = VarTable.newInstance();

		// получаем все файлы конфига
		File[] files = Files.getFiles(new File(SERVER_DIR + "/config"));

		// парсим весь конфиг
		parseFiles(files, vars);

		ACCOUNT_MIN_ACCESS_LEVEL = vars.getInteger("ACCOUNT_MIN_ACCESS_LEVEL");

		ACCOUNT_PREMIUM_EXP_RATE = vars.getFloat("ACCOUNT_PREMIUM_EXP_RATE");
		ACCOUNT_PREMIUM_MONEY_RATE = vars.getFloat("ACCOUNT_PREMIUM_MONEY_RATE");
		ACCOUNT_PREMIUM_DROP_RATE = vars.getFloat("ACCOUNT_PREMIUM_DROP_RATE");
		ACCOUNT_PREMIUM_QUEST_RATE = vars.getFloat("ACCOUNT_PREMIUM_QUEST_RATE");

		ACCOUNT_AUTO_CREATE = vars.getBoolean("ACCOUNT_AUTO_CREATE");
		ACCOUNT_ONLY_PAID = vars.getBoolean("ACCOUNT_ONLY_PAID");
		ACCOUNT_PREMIUM_EXP = vars.getBoolean("ACCOUNT_PREMIUM_EXP");
		ACCOUNT_PREMIUM_MONEY = vars.getBoolean("ACCOUNT_PREMIUM_MONEY");
		ACCOUNT_PREMIUM_DROP = vars.getBoolean("ACCOUNT_PREMIUM_DROP");
		ACCOUNT_PREMIUM_QUEST = vars.getBoolean("ACCOUNT_PREMIUM_QUEST");

		SERVER_NAME_TEMPLATE = vars.getString("SERVER_NAME_TEMPLATE");
		SERVER_ONLINE_FILE = vars.getString("SERVER_ONLINE_FILE", Strings.EMPTY);

		SERVER_PLAYER_SUB_ID = vars.getInteger("SERVER_PLAYER_SUB_ID");
		SERVER_NPC_SUB_ID = vars.getInteger("SERVER_NPC_SUB_ID");
		SERVER_ITEM_SUB_ID = vars.getInteger("SERVER_ITEM_SUB_ID");
		SERVER_SHOT_SUB_ID = vars.getInteger("SERVER_SHOT_SUB_ID");
		SERVER_OBJECT_SUB_ID = vars.getInteger("SERVER_OBJECT_SUB_ID");
		SERVER_TRAP_SUB_ID = vars.getInteger("SERVER_TRAP_SUB_ID");
		SERVER_RESOURSE_SUB_ID = vars.getInteger("SERVER_RESOURSE_SUB_ID");
		SERVER_PORT = vars.getInteger("SERVER_PORT");

		SERVER_RATE_EXP = vars.getFloat("SERVER_RATE_EXP");
		SERVER_PARTY_RATE_EXP = vars.getFloat("SERVER_PARTY_RATE_EXP");
		SERVER_RATE_MONEY = vars.getFloat("SERVER_RATE_MONEY");
		SERVER_RATE_DROP_ITEM = vars.getFloat("SERVER_RATE_DROP_ITEM");
		SERVER_ONLINE_FAKE = vars.getFloat("SERVER_ONLINE_FAKE");

		SERVER_USE_SNIFFER_OPCODE = vars.getBoolean("SERVER_USE_SNIFFER_OPCODE", false);

		WORLD_LIFE_TIME_DROP_ITEM = vars.getInteger("WORLD_LIFE_TIME_DROP_ITEM");
		WORLD_BLOCK_TIME_DROP_ITEM = vars.getInteger("WORLD_BLOCK_TIME_DROP_ITEM");
		WORLD_MIN_ACCESS_LEVEL = vars.getInteger("WORLD_MIN_ACCESS_LEVEL");
		WORLD_PLAYER_THRESHOLD_ATTACKS = vars.getInteger("WORLD_PLAYER_THRESHOLD_ATTACKS");
		WORLD_PLAYER_THRESHOLD_BLOOKS = vars.getInteger("WORLD_PLAYER_THRESHOLD_BLOOKS");
		WORLD_PLAYER_TIME_BATTLE_STANCE = vars.getInteger("WORLD_PLAYER_TIME_BATTLE_STANCE") * 1000;
		WORLD_MAX_DIFF_LEVEL_ON_DROP = vars.getInteger("WORLD_MAX_DIFF_LEVEL_ON_DROP");
		WORLD_WIDTH_REGION = vars.getInteger("WORLD_WIDTH_REGION");
		WORLD_HEIGHT_REGION = vars.getInteger("WORLD_HEIGHT_REGION");
		WORLD_MAXIMUM_ONLINE = vars.getInteger("WORLD_MAXIMUM_ONLINE");
		WORLD_BANK_MAX_SIZE = vars.getInteger("WORLD_BANK_MAX_SIZE");
		WORLD_GUILD_BANK_MAX_SIZE = vars.getInteger("WORLD_GUILD_BANK_MAX_SIZE");
		WORLD_MAX_COLLECT_LEVEL = vars.getInteger("WORLD_MAX_COLLECT_LEVEL");
		WORLD_PLAYER_MAX_LEVEL = vars.getInteger("WORLD_PLAYER_MAX_LEVEL");
		WORLD_CHANCE_DELETE_CRYSTAL = vars.getInteger("WORLD_CHANCE_DELETE_CRYSTAL", 13);

		WORLD_SHOP_PRICE_MOD = vars.getFloat("WORLD_SHOP_PRICE_MOD");

		WORLD_AUTO_LEARN_SKILLS = vars.getBoolean("WORLD_AUTO_LEARN_SKILLS");
		WORLD_PK_AVAILABLE = vars.getBoolean("WORLD_PK_AVAILABLE");
		WORLD_LEARN_ONLY_IMPLEMENTED_SKILLS = vars.getBoolean("WORLD_LEARN_ONLY_IMPLEMENTED_SKILLS");

		DEVELOPER_FORCE_ATTACK_SPEED = vars.getInteger("DEVELOPER_FORCE_ATTACK_SPEED");
		DEVELOPER_DEBUG_CLIENT_PACKETS = vars.getBoolean("DEVELOPER_DEBUG_CLIENT_PACKETS");
		DEVELOPER_DEBUG_SERVER_PACKETS = vars.getBoolean("DEVELOPER_DEBUG_SERVER_PACKETS");
		DEVELOPER_MAIN_DEBUG = vars.getBoolean("DEVELOPER_MAIN_DEBUG");
		DEVELOPER_DEBUG_TARGET_TYPE = vars.getBoolean("DEVELOPER_DEBUG_TARGET_TYPE");
		DEVELOPER_DEBUG_CASTING_SKILL = vars.getBoolean("DEVELOPER_DEBUG_CASTING_SKILL");
		DEVELOPER_DEBUG_MOVING_PLAYER = vars.getBoolean("DEVELOPER_DEBUG_MOVING_PLAYER");
		DEVELOPER_DEBUG_MOVING_NPC = vars.getBoolean("DEVELOPER_DEBUG_MOVING_NPC");
		DEVELOPER_GEO_LOGING = vars.getBoolean("DEVELOPER_GEO_LOGING");

		Loggers.info("Config", "reloaded.");
	}

	private Config()
	{
		throw new IllegalArgumentException();
	}
}
