package tera.gameserver.model.skillengine.classes;

import rlib.util.array.Array;
import tera.gameserver.model.AttackInfo;
import tera.gameserver.model.Character;
import tera.gameserver.model.npc.Npc;
import tera.gameserver.templates.SkillTemplate;
import tera.util.LocalObjects;

/**
 * @author Ronn
 */
public class UnAggro extends Strike
{
	public UnAggro(SkillTemplate template)
	{
		super(template);
	}

	@Override
	public AttackInfo applySkill(Character attacker, Character target)
	{
		// получаем локальные объекты
		LocalObjects local = LocalObjects.get();

		// получаем инфо об атаке
		AttackInfo info = local.getNextAttackInfo();

		// получаем список сагренных НПС
		Array<Npc> list = attacker.getLocalHateList();

		// получаем массив НПС
		Npc[] array = list.array();

		// перебираем НПС и удаляем у них хейт на персонажа
		for(int i = 0, length = list.size(); i < length; i++)
			array[i].removeAggro(attacker);

		return info;
	}
}
