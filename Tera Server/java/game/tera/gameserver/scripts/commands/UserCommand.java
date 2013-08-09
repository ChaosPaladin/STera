package tera.gameserver.scripts.commands;

import java.text.SimpleDateFormat;
import java.util.Date;

import rlib.util.Files;
import rlib.util.table.IntKey;
import rlib.util.table.Table;
import tera.Config;
import tera.gameserver.manager.DataBaseManager;
import tera.gameserver.manager.OnlineManager;
import tera.gameserver.manager.PlayerManager;
import tera.gameserver.model.Account;
import tera.gameserver.model.MessageType;
import tera.gameserver.model.World;
import tera.gameserver.model.base.Race;
import tera.gameserver.model.base.Sex;
import tera.gameserver.model.playable.Player;
import tera.gameserver.model.playable.PlayerAppearance;
import tera.gameserver.model.skillengine.Skill;
import tera.gameserver.network.serverpackets.SkillListInfo;

/**
 * Список команд, доступных для всех игроков.
 *
 * @author Ronn
 */
public class UserCommand extends AbstractCommand
{
	private static final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");

	private static final String CHANGE_APPEARANCE_VAR = "CHANGE_APPEARANCE_VAR";
	private static final String CHANGE_RACE_VAR = "CHANGE_RACE_VAR";

	private static final int CHANGE_APPEARANCE_LIMIT = 3;
	private static final int CHANGE_RACE_LIMIT = 3;

	private static final Date date = new Date();

	private static String help;

	public UserCommand(int access, String[] commands)
	{
		super(access, commands);

		help = Files.read(Config.SERVER_DIR + "/data/help.txt");
	}

	@Override
	public void execution(String command, Player player, String values)
	{
		switch(command)
		{
//			case "change_appearance":
//			{
//				if(values == null || values.isEmpty())
//					return;
//
//				// получаем кол-во использованных изменений
//				int count = player.getVar(CHANGE_APPEARANCE_VAR, 0);
//
//				if(count >= CHANGE_APPEARANCE_LIMIT)
//				{
//					player.sendMessage("Вы исчерпали возможность менять внешность.");
//					return;
//				}
//
//				try
//				{
//					// получаем менеджера БД
//					DataBaseManager dbManager = DataBaseManager.getInstance();
//
//					int objectId = dbManager.getPlayerObjectId(values);
//
//					if(objectId == 0)
//					{
//						player.sendMessage("Игрока с таким именем в БД не обнаружено.");
//						return;
//					}
//
//					PlayerAppearance appearance = dbManager.loadPlayerAppearance(objectId);
//
//					if(appearance == null)
//					{
//						player.sendMessage("Не удалось получить внешность этого игрока.");
//						return;
//					}
//
//					appearance.setObjectId(player.getObjectId());
//
//					if(dbManager.updatePlayerAppearance(appearance))
//						player.sendMessage("Для вступления изменений, перезайдите в игру.");
//				}
//				finally
//				{
//					player.setVar(CHANGE_APPEARANCE_VAR, ++count);
//					player.sendMessage("У вас осталось изменений: " + (CHANGE_APPEARANCE_LIMIT - count) + ".");
//				}
//
//				break;
//			}
//			case "change_race":
//			{
//				if(values == null || values.isEmpty())
//					return;
//
//				Race race = Race.valueOf(values);
//
//				if(race == player.getRace())
//					return;
//
//				// получаем кол-во использованных изменений
//				int count = player.getVar(CHANGE_RACE_VAR, 0);
//
//				if(count >= CHANGE_RACE_LIMIT)
//				{
//					player.sendMessage("Вы исчерпали возможность менять расу.");
//					return;
//				}
//
//				try
//				{
//					Sex sex = player.getSex();
//
//					if(race == Race.BARAKA || race == Race.POPORI)
//						sex = Sex.MALE;
//					else if(race == Race.ELIN)
//						sex = Sex.FEMALE;
//
//					// получаем менеджера БД
//					DataBaseManager dbManager = DataBaseManager.getInstance();
//
//					if(dbManager.updatePlayerRace(player.getObjectId(), race, sex))
//					{
//						// получаем текущие скилы игрока
//						Table<IntKey, Skill> old = player.getSkills();
//
//						// перебираем их
//						for(Skill skill : old)
//						{
//							// если это маунт, прпускаем
//							if(skill.getClassId() == -15)
//								continue;
//
//							// удаляем скил
//							player.removeSkill(skill, false);
//
//							// удаляем в базе скил
//							dbManager.deleteSkill(player, skill);
//
//							// ложим в пул
//							skill.fold();
//						}
//
//						// очищаем таблицу
//						old.clear();
//
//						// отправляем пакет со скилами
//						player.sendPacket(SkillListInfo.getInstance(player), true);
//
//						player.sendMessage("Для вступления изменений в силу, перезайдите в игру.");
//					}
//				}
//				finally
//				{
//					player.setVar(CHANGE_RACE_VAR, ++count);
//					player.sendMessage("У вас осталось изменений: " + (CHANGE_RACE_LIMIT - count) + ".");
//				}
//
//				break;
//			}
			case "restore_characters":
			{
				// получаем менеджера игроков
				PlayerManager playerManager = PlayerManager.getInstance();

				// восстанавливаем его персонажей
				playerManager.restoreCharacters(player);

				break;
			}
//			case "pk":
//			{
//				if("on".equals(values) && !player.isPkMode())
//				{
//					if(!player.isPvPMode())
//						player.sendMessage("Вы должны быть в режиме PvP.");
//					else
//					{
//						player.setPkMode(true);
//						player.sendMessage("Вы активировали режим PK.");
//					}
//				}
//				else if("off".equals(values) && player.isPkMode())
//				{
//					if(player.isPK())
//						player.sendMessage("Вы не можете отключить режим PK при наличии кармы.");
//					else
//					{
//						player.setPkMode(false);
//						player.sendMessage("Вы отключили режим PK.");
//					}
//				}
//
//				break;
//			}
			case "help": player.sendMessage(help); break;
			case "time":
			{
				synchronized(date)
				{
					// устанавливаем текущее время
					date.setTime(System.currentTimeMillis());

					// отправляем сообщение с ним
					player.sendMessage(timeFormat.format(date));
				}

				break;
			}
			case "end_pay":
			{
				// получаем аккаунт игрока
				Account account = player.getAccount();

				// получаем время окончания проплаты
				long time = account.getEndPay();

				// если время проплаты уже прошло
				if(System.currentTimeMillis() > time)
					player.sendMessage("У вас не проплаченный аккаунт.");
				else
				{
					synchronized(date)
					{
						// обновляем время
						date.setTime(time);

						// отправляем дату
						player.sendMessage("Дата завершения проплаты: " + timeFormat.format(date));
					}
				}

				break;
			}
			case "restore_skills":
			{
				// получаем текущие скилы игрока
				Table<IntKey, Skill> old = player.getSkills();

				// получаем менеджера БД
				DataBaseManager dbManager = DataBaseManager.getInstance();

				// перебираем их
				for(Skill skill : old)
				{
					// если это маунт, прпускаем
					if(skill.getClassId() == -15)
						continue;

					// удаляем скил
					player.removeSkill(skill, false);

					// удаляем в базе скил
					dbManager.deleteSkill(player, skill);

					// ложим в пул
					skill.fold();
				}

				// очищаем таблицу
				old.clear();

				// выдаем заново базовые скилы
				player.getTemplate().giveSkills(player);

				// отправляем пакет со скилами
				player.sendPacket(SkillListInfo.getInstance(player), true);

				break;
			}
			case "kill_me" :
			{
				if(player.isBattleStanced())
				{
					player.sendMessage("Нельзя использовать в бою.");
					return;
				}

				synchronized(player)
				{
					player.setCurrentHp(0);
					player.doDie(player);
				}

				break;
			}
			case "version": player.sendMessage("Текущая версия сервера: " + Config.SERVER_VERSION); break;
			case "online":
			{
				// получаем менеджер онлайна
				OnlineManager onlineManager = OnlineManager.getInstance();

				player.sendMessage("Текущий онлаин: " + onlineManager.getCurrentOnline());

				break;
			}
			case "player_info":
			{
				if(player.getName().equals(values))
					return;

				Player target = World.getAroundByName(Player.class, player, values);

				if(target == null)
					target = World.getPlayer(values);

				if(target == null)
				{
					player.sendMessage(MessageType.THAT_CHARACTER_ISNT_ONLINE);
					return;
				}

				StringBuilder builder = new StringBuilder("\n--------\nPlayer: \"").append(target.getName()).append("\":\n");

				builder.append("attack:").append(target.getAttack(null, null)).append(";\n");
				builder.append("defense:").append(target.getDefense(null, null)).append(";\n");
				builder.append("impact:").append(target.getImpact(null, null)).append(";\n");
				builder.append("balance:").append(target.getBalance(null, null)).append(";\n");
				builder.append("max hp:").append(target.getMaxHp()).append(".\n");
				builder.append("--------");

				player.sendMessage(builder.toString());

				break;
			}
		}
	}
}
