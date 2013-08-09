package tera.gameserver.model.skillengine.classes;

import rlib.util.VarTable;
import tera.gameserver.model.Character;
import tera.gameserver.model.ai.npc.ConfigAI;
import tera.gameserver.model.ai.npc.NpcAIClass;
import tera.gameserver.model.npc.spawn.SummonSpawn;
import tera.gameserver.model.npc.summons.Summon;
import tera.gameserver.tables.ConfigAITable;
import tera.gameserver.tables.NpcTable;
import tera.gameserver.templates.NpcTemplate;
import tera.gameserver.templates.SkillTemplate;

/**
 * Скил для спавна сумона.
 *
 * @author Ronn
 */
public class SpawnSummon extends AbstractSkill
{
	/** спавн суммона */
	private volatile SummonSpawn spawn;

	public SpawnSummon(SkillTemplate template)
	{
		super(template);
	}

	@Override
	public void useSkill(Character character, float targetX, float targetY, float targetZ)
	{
		super.useSkill(character, targetX, targetY, targetZ);

		// получаем спавн суммона
		SummonSpawn spawn = getspawn();

		// если его нет, выходим
		if(spawn == null)
		{
			character.sendMessage("Этот суммон не реализован.");
			return;
		}

		// получаем сумона перса
		Summon summon = character.getSummon();

		// если он есть
		if(summon != null)
			// удаляем его
			summon.remove();

		// запоминаем владельца
		spawn.setOwner(character);

		// обновляем позицию спавна
		spawn.getLocation().setXYZHC(character.getX(), character.getY(), character.getZ(), character.getHeading(), character.getContinentId());

		// запускаем спавн
		spawn.start();
	}

	protected SummonSpawn getspawn()
	{
		if(spawn == null)
			synchronized(this)
			{
				if(spawn == null)
				{
					// получаем таблицу НПС
					NpcTable npcTable = NpcTable.getInstance();

					// получаем шаблон НПС
					NpcTemplate temp = npcTable.getTemplate(template.getSummonId(), template.getSummonType());

					// если шаблона нет, выходим
					if(temp == null)
						return null;

					// получаем таблицу конфигов АИ
					ConfigAITable configTable = ConfigAITable.getInstance();

					// получаем все параметры шаблона скила
					VarTable vars = template.getVars();

					// получаем конфиг АИ суммона
					ConfigAI configAI = configTable.getConfig(vars.getString("configAI", null));

					// если его нет, выходим
					if(configAI == null)
						return null;

					// создаем спавн
					spawn = new SummonSpawn(temp, configAI, vars.getEnum("aiClass", NpcAIClass.class), template.getLifeTime());
				}
			}

		return spawn;
	}
}
