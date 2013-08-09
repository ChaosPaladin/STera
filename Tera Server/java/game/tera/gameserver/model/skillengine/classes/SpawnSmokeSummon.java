package tera.gameserver.model.skillengine.classes;

import tera.gameserver.model.Character;
import tera.gameserver.templates.SkillTemplate;

/**
 * Скил для спавна тени варриора.
 *
 * @author Ronn
 */
public class SpawnSmokeSummon extends AbstractSkill
{
	public SpawnSmokeSummon(SkillTemplate template)
	{
		super(template);
	}

	@Override
	public void useSkill(Character character, float targetX, float targetY, float targetZ)
	{
		super.useSkill(character, targetX, targetY, targetZ);

		// если кастующий не игрок, выходим
		if(!character.isPlayer())
			return;

		/*// получаем таблицу сумонов
		SummonTable summonTable = SummonTable.getInstance();

		// получаем темплейт сумона
		SummonTemplate temp = summonTable.getSummon(template.getSummonId(), template.getSummonType());

		// если такого нет, выходим
		if(temp == null)
			return;

		// получаем затрачиваемое хп
		int consume = character.getMaxHp() * 20 / 100;

		// если не хватает
		if(character.getCurrentHp() <= consume + 2)
		{
			character.sendMessage("У вас не достаточно HP.");
			return;
		}

		// отнимаем ХП
		character.setCurrentHp(character.getCurrentHp() - consume);

		// получаем менеджера событий
		ObjectEventManager eventManager = ObjectEventManager.getInstance();

		// обновляем хп
		eventManager.notifyHpChanged(character);

		// получаем сумона
		Summon summon = character.getSummon();

		// если уже есть вызванный
		if(summon != null)
			// удаляем его
			summon.remove();

		// создаем нового
		summon = temp.newInstance();

		// если не получилось создать, выходим
		if(summon == null)
			return;

		// устанавливаем ид континента
		summon.setContinentId(character.getContinentId());

		// спавним сумона
		summon.spawnMe((Player) character);

		// получаем список сагренных нпс
		Array<Npc> hateList = character.getLocalHateList();

		// получаем их массив
		Npc[] npcs = hateList.array();

		// перебираем нпс
		for(int i = 0, length = hateList.size(); i < length; i++)
		{
			// получаем нпс
			Npc npc = npcs[i];

			// если нпс нет, прпускаем
			if(npc == null)
				continue;

			// узнаем сколько хейта у него
			long hate = Math.max(npc.getAggro(character), 1);

			// удаляем кастера из агр листа
			npc.removeAggro(character);

			// переносим агр на сумона
			npc.addAggro(summon, hate, false);
		}*/
	}
}
