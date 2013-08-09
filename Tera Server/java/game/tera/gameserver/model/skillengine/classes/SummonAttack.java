package tera.gameserver.model.skillengine.classes;

import tera.gameserver.model.Character;
import tera.gameserver.model.npc.summons.Summon;
import tera.gameserver.templates.SkillTemplate;

/**
 * @author Ronn
 */
public class SummonAttack extends AbstractSkill
{
	public SummonAttack(SkillTemplate template)
	{
		super(template);
	}

	@Override
	public boolean checkCondition(Character attacker, float targetX, float targetY, float targetZ)
	{
		if(attacker.getSummon() == null)
		{
			attacker.sendMessage("У вас нет вызванных питомцев.");
			return false;
		}

		return super.checkCondition(attacker, targetX, targetY, targetZ);
	}


	@Override
	public void useSkill(Character character, float targetX, float targetY, float targetZ)
	{
		super.useSkill(character, targetX, targetY, targetZ);

		// получаем цель кастера
		Character target = character.getTarget();

		// получаем суммон кастера
		Summon summon = character.getSummon();

		// если кого-то из них нет, выходим
		if(target == null || summon == null)
			return;

		// указываем суммону атаковать
		summon.getAI().startAttack(target);

		// зануляем кастеру цель
		character.setTarget(null);
	}
}
