package tera.gameserver;

import java.lang.management.ThreadInfo;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import tera.Config;
import tera.gameserver.manager.AccountManager;
import tera.gameserver.manager.AnnounceManager;
import tera.gameserver.manager.AutoSaveManager;
import tera.gameserver.manager.BossSpawnManager;
import tera.gameserver.manager.CommandManager;
import tera.gameserver.manager.DataBaseManager;
import tera.gameserver.manager.EventManager;
import tera.gameserver.manager.ExecutorManager;
import tera.gameserver.manager.GameLogManager;
import tera.gameserver.manager.GeoManager;
import tera.gameserver.manager.GuildManager;
import tera.gameserver.manager.ItemExecutorManager;
import tera.gameserver.manager.OnlineManager;
import tera.gameserver.manager.PlayerManager;
import tera.gameserver.manager.QuestManager;
import tera.gameserver.manager.RandomManager;
import tera.gameserver.manager.ServerVarManager;
import tera.gameserver.manager.SkillLearnManager;
import tera.gameserver.model.World;
import tera.gameserver.model.base.Race;
import tera.gameserver.model.items.ItemLocation;
import tera.gameserver.model.playable.DeprecatedPlayerFace;
import tera.gameserver.model.playable.PlayerAppearance;
import tera.gameserver.model.skillengine.Formulas;
import tera.gameserver.network.Network;
import tera.gameserver.network.Opcodes;
import tera.gameserver.parser.ConditionParser;
import tera.gameserver.parser.EffectParser;
import tera.gameserver.parser.FuncParser;
import tera.gameserver.tables.ConfigAITable;
import tera.gameserver.tables.DropTable;
import tera.gameserver.tables.ItemTable;
import tera.gameserver.tables.MessagePackageTable;
import tera.gameserver.tables.MinionTable;
import tera.gameserver.tables.NpcAppearanceTable;
import tera.gameserver.tables.NpcDialogTable;
import tera.gameserver.tables.NpcTable;
import tera.gameserver.tables.PlayerTable;
import tera.gameserver.tables.ResourseTable;
import tera.gameserver.tables.SkillTable;
import tera.gameserver.tables.SpawnTable;
import tera.gameserver.tables.TerritoryTable;
import tera.gameserver.tables.TownTable;
import tera.gameserver.tables.WorldZoneTable;
import tera.gameserver.taskmanager.EffectTaskManager;
import tera.gameserver.taskmanager.RegenTaskManager;
import tera.remotecontrol.ServerControl;
import tera.remotecontrol.handlers.ServerConsoleHandler;

import rlib.DeadLockDetector;
import rlib.DeadLockListener;
import rlib.Monitoring;
import rlib.database.CleaningManager;
import rlib.database.ConnectFactory;
import rlib.database.DBUtils;
import rlib.logging.GameLoggers;
import rlib.logging.Logger;
import rlib.logging.Loggers;
import rlib.util.Util;

/**
 * Игровой сервер Tera-Online от команды STera.
 *
 * @author Ronn
 */
public class GameServer extends ServerThread implements DeadLockListener
{
	private static final Logger log = Loggers.getLogger(GameServer.class);

	/** экщемпляр сервера */
	private static GameServer instance;

	public static GameServer getInstance()
	{
		if(instance == null)
			instance = new GameServer();

		return instance;
	}

	public static void main(String[] args) throws Exception
	{
		System.out.println(System.currentTimeMillis());
		// инициализируем мониторинг процесса сервера
		Monitoring.init();

		// загружаем конфиг
		Config.init();

		// устанавливаем папку для игровых логгеров
		GameLoggers.setDirectory(Config.SERVER_DIR + "/log");

		// устанавливаем выходной фаил для логгинга
		Loggers.setFile(Config.SERVER_DIR, true);

		// если включено дистанционное управление, активируем его
		if(Config.DIST_CONTROL_ENABLED)
		{
			// добавляем испорт консоли
			Loggers.addListener(ServerConsoleHandler.instance);
			// инициализируем дист. админку
			ServerControl.init();
		}

		// проверка на свободность порта
		Util.checkFreePorts("*", Config.SERVER_PORT);

		// инициализируем исполняющего менеджера
		ExecutorManager.getInstance();

		// получаем менеджера БД
		DataBaseManager dataBaseManager = DataBaseManager.getInstance();

		// чистим БД
		if(Config.DATA_BASE_CLEANING_START)
		{
			// добавляем запросы для очистки БД
			CleaningManager.addQuery("delete {count} reuses from a character_skill_reuses", "DELETE FROM `character_skill_reuses` WHERE `end_time` < " + System.currentTimeMillis());
			CleaningManager.addQuery("delete {count} spawns from a boss_spawn", "DELETE FROM `boss_spawn` WHERE `spawn` < " + System.currentTimeMillis());
			CleaningManager.addQuery("delete {count} appearances from a character_appearances", "DELETE FROM `character_appearances` WHERE `object_id` NOT IN (SELECT `object_id` FROM `characters`)");
			CleaningManager.addQuery("delete {count} effects from a character_save_effects", "DELETE FROM `character_save_effects` WHERE `object_id` NOT IN (SELECT `object_id` FROM `characters`)");
			CleaningManager.addQuery("delete {count} inventors from a character_inventors", "DELETE FROM `character_inventors` WHERE `owner_id` NOT IN (SELECT `object_id` FROM `characters`)");
			CleaningManager.addQuery("delete {count} skills from a character_skills", "DELETE FROM `character_skills` WHERE `object_id` NOT IN (SELECT `object_id` FROM `characters`)");
			CleaningManager.addQuery("delete {count} hotkeys from a character_hotkey", "DELETE FROM `character_hotkey` WHERE `object_id` NOT IN (SELECT `object_id` FROM `characters`)");
			CleaningManager.addQuery("delete {count} chars from a character_friends", "DELETE FROM `character_friends` WHERE `object_id` NOT IN (SELECT `object_id` FROM `characters`)");
			CleaningManager.addQuery("delete {count} friends from a character_friends", "DELETE FROM `character_friends` WHERE `friend_id` NOT IN (SELECT `object_id` FROM `characters`)");
			CleaningManager.addQuery("delete {count} settings from a character_settings", "DELETE FROM `character_settings` WHERE `object_id` NOT IN (SELECT `object_id` FROM `characters`)");
			CleaningManager.addQuery("delete {count} emty items from a items", "DELETE FROM `items` WHERE `owner_id` = 0");
			CleaningManager.addQuery("delete {count} no owner items from a items", "DELETE FROM `items` WHERE `owner_id` NOT IN (SELECT `object_id` FROM `characters`) AND `location` < " + ItemLocation.BANK.ordinal());
			CleaningManager.addQuery("delete {count} no owner guild items from a items", "DELETE FROM `items` WHERE `owner_id` NOT IN (SELECT `id` FROM `guilds`) AND `location` = " + ItemLocation.GUILD_BANK.ordinal());
			CleaningManager.addQuery("create buffer table items", "CREATE TABLE `buffer_items` LIKE `items");
			CleaningManager.addQuery("copy {count} items to buffer_items", "INSERT `buffer_items` SELECT * FROM `items`");
			CleaningManager.addQuery("delete {count} no owner items from a buffer_items", "DELETE FROM `items` WHERE `owner_id` NOT IN (SELECT `object_id` FROM `buffer_items`) AND `location` = " + ItemLocation.CRYSTAL.ordinal());
			CleaningManager.addQuery("drop buffer table items", "DROP TABLE IF EXISTS `buffer_items`");
			CleaningManager.addQuery("delete {count} from a quests", "DELETE FROM `character_quests` WHERE `object_id` NOT IN (SELECT `object_id` FROM `characters`)");
			CleaningManager.addQuery("delete {count} from a quest variables", "DELETE FROM `character_quest_vars` WHERE `object_id` NOT IN (SELECT `object_id` FROM `characters`)");
			CleaningManager.addQuery("delete {count} from a guild ranks", "DELETE FROM `guild_ranks` WHERE `guild_id` NOT IN (SELECT `id` FROM `guilds`)");

			// выполняем очистку
			CleaningManager.cleaning(dataBaseManager.getConnectFactory());
		}

		//TODO
		{
			ConnectFactory connoctFactory = dataBaseManager.getConnectFactory();

			Connection con = connoctFactory.getConnection();

			Statement stmt = con.createStatement();

			ResultSet rset = stmt.executeQuery("SELECT objectId FROM `character_faces`");

			while(rset.next())
			{
				int objectId = rset.getInt(1);

				DeprecatedPlayerFace deprFace = dataBaseManager.restoreFace(objectId);

				if(deprFace == null)
					continue;

				deprFace.setObjectId(objectId);

				PlayerAppearance appearance = deprFace.toAppearance();

				dataBaseManager.insertPlayerAppearance(appearance);
			}

			DBUtils.closeResultSet(rset);

			stmt.execute("DELETE FROM character_faces");

			DBUtils.closeDatabaseCS(con, stmt);
		}

		// подготавливаем опкоды
		Opcodes.prepare();

		// запускаем сервер
		getInstance().start();

		// запускам сборщика мусора
		System.gc();
	}

	private GameServer()
	{
		new DeadLockDetector(300000).addListener(this);
	}

	@Override
	public void onDetected(ThreadInfo info)
	{
		// получаем менеджер онлайна
		OnlineManager onlineManager = OnlineManager.getInstance();

		// останавливаем и зануляем онлайн
		onlineManager.stop();

		// уведомляем игроков
		World.sendAnnounce("ВНИМАНИЕ! На сервере произошла критическая ошибка, ждите перезапуска.");
	}

	@Override
	public void run()
	{
		try
		{
			// инициализируем парсер функций
			FuncParser funcManager = FuncParser.getInstance();

			// инициализируем парсер эффектов
			EffectParser.getInstance();

			// инициализируем менеджер рандома
			RandomManager.getInstance();

			// инициализируем фабрику ид
			IdFactory.getInstance();

			// инициализируем серверные переменные
			ServerVarManager.getInstance();

			// инициализируем менеджера по спавну РБ
			BossSpawnManager.getInstance();

			// инициализируем таблицу территорий
			TerritoryTable.getInstance();

			// инициализируем таблицу регионов мира
			WorldZoneTable.getInstance();

			// инициализируем парсер условий
			ConditionParser.getInstance();

			// инициализиуем таблицу скилов
			SkillTable.getInstance();

			// инициализируем таблицу итемов
			ItemTable.getInstance();

			// инициализируем расы
			Race.init();

			// инициализируем таблицу игроков
			PlayerTable.getInstance();

			// инициализируем таблицу дропа
			DropTable.getInstance();

			// инициализируем таблицу городов
			TownTable.getInstance();

			// инициализируем таблицу диалогов
			NpcDialogTable.getInstance();

			// инициализируем таблицу пакетов сообщений для НПС
			MessagePackageTable.getInstance();

			// инициализируем таблицу конфигов АИ
			ConfigAITable.getInstance();

			// инициализируем таблицу внешностей НПС
			NpcAppearanceTable.getInstance();

			// инициализируем таблицу НПС
			NpcTable.getInstance();

			// инициализируем таблицу шаблонов ресурсов
			ResourseTable.getInstance();

			// инициализируем таблицу данных о минионах
			MinionTable.getInstance();

			// инициализируем менеджер регена персонажей
			RegenTaskManager.getInstance();

			// инициализируем формулы
			Formulas.getInstance();

			// инициализируем менеджер эффектов
			EffectTaskManager.getInstance();

			// подготавливаем шансовые функции
			funcManager.prepareChanceFunc();

			// инициализируем таблицу спавна
			SpawnTable.getInstance();

			// инициализируем менеджер геодаты
			GeoManager.getInstance();

			// инициализируем менеджера по обработке активных итемов
			ItemExecutorManager.getInstance();

			// инициализируем менеджера команд
			CommandManager.getInstance();

			// инициализируем менеджер по изучаемым скилам
			SkillLearnManager.getInstance();

			// инициализируем менеджер аннонсов
			AnnounceManager.getInstance();

			// инициализируем менеджера по аккаунтам
			AccountManager.getInstance();

			// инициализируем менеджер гильдий
			GuildManager.getInstance();

			// инициализируем менеджер автовсохранения
			AutoSaveManager.getInstance();

			// инициализируем менеджер квестов
			QuestManager.getInstance();

			// инициализируем менеджер онлайна
			OnlineManager.getInstance();

			// инициализируем менеджер игрового логирования
			GameLogManager.getInstance();

			// инициализируем менеджер ивентов
			EventManager.getInstance();

			// инициализируем менеджера игроков
			PlayerManager.getInstance();

			// запускаем сеть
			Network.getInstance();

			log.info("started.");
		}
		catch(Exception e)
		{
			log.warning(e);
			System.exit(0);
		}
	}
}