package tera.gameserver.model.skillengine.classes;

import rlib.util.array.Array;
import tera.gameserver.model.AttackInfo;
import tera.gameserver.model.Character;
import tera.gameserver.model.skillengine.Formulas;
import tera.gameserver.templates.SkillTemplate;
import tera.util.LocalObjects;

/**
 * Оснавная модель ударных скилов.
 *
 * @author Ronn
 */
public class Strike extends AbstractSkill
{
	/**
	 * @param template темплейт скила.
	 */
	public Strike(SkillTemplate template)
	{
		super(template);
	}

	@Override
	public AttackInfo applySkill(Character attacker, Character target)
	{
		// получаем локальные объекты
		LocalObjects local = LocalObjects.get();

		// получаем формулы
		Formulas formulas = Formulas.getInstance();

		// рассчитываем урон
		AttackInfo info = formulas.calcDamageSkill(local.getNextAttackInfo(), this, attacker, target);

		// применяем атаку
		target.causingDamage(this, info, attacker);

		// если не заблокирован
		if(!info.isBlocked())
			// добавляем эффекты
			addEffects(attacker, target);

		return info;
	}

	@Override
	public void useSkill(Character character, float targetX, float targetY, float targetZ)
	{
		// получаем локальные объекты
		LocalObjects local = LocalObjects.get();

		// получаем список для целей
		Array<Character> targets = local.getNextCharList();

		// рассчитываем цели
		addTargets(targets, character, targetX, targetY, targetZ);

		// получаем массив целей
		Character[] array = targets.array();

		// перебираем их
		for(int i = 0, length = targets.size(); i < length; i++)
		{
			// получаем цель
			Character target = array[i];

			// еси не подходит по условиям, пропускаем
			if(target == null || target.isDead() || target.isInvul() || target.isEvasioned())
				continue;

			// применяем скил
    		applySkill(character, target);
		}

		super.useSkill(character, targetX, targetY, targetZ);
	}
}
