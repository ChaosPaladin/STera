package tera.gameserver.model.skillengine.effects;

import tera.gameserver.manager.ExecutorManager;
import tera.gameserver.manager.ObjectEventManager;
import tera.gameserver.model.AttackInfo;
import tera.gameserver.model.Character;
import tera.gameserver.model.EffectList;
import tera.gameserver.model.listeners.DamageListener;
import tera.gameserver.model.skillengine.Skill;
import tera.gameserver.templates.EffectTemplate;
import tera.gameserver.templates.SkillTemplate;

/**
 * Баф для поглощения урона.
 *
 * @author Ronn
 */
public class DamageAbsorption extends AbstractEffect implements DamageListener, Runnable
{
	/** лимит поглощаемого урона */
	private int limit;
	/** потребление мп */
	private int consume;

	public DamageAbsorption(EffectTemplate template, Character effector, Character effected, SkillTemplate skill)
	{
		super(template, effector, effected, skill);

		// потребляемое мп при поглащении урона
		this.consume = (int) template.getValue();
	}

	@Override
	public boolean onActionTime()
	{
		return true;
	}

	@Override
	public void onDamage(Character attacker, Character attacked, AttackInfo info, Skill skill)
	{
		// если лимит исчерпан или урона нет, выходим
		if(limit < 1 || info.isNoDamage())
			return;

		// получаем исполнительного менеджера
		ExecutorManager executor = ExecutorManager.getInstance();

		// текущее мп
		int current = attacked.getCurrentMp();

		// если по мп ничего не выходит
		if(current < 1 && consume > 0)
		{
			executor.execute(this);
			return;
		}

		// урон
		int damage = info.getDamage();

		// первично поглощаемый урон
		int abs = damage > limit? limit : damage;

		if(consume > 0)
		{
			// получаем необходимое кол-во мп для поглащения
			int mp = Math.max(1, abs / consume);

			// если меньше, чем есть, пересчитываем под все мп
			if(current < mp)
			{
				mp = current;
				abs = mp * consume;
			}

			// забираем мп
			attacked.setCurrentMp(current - mp);

			// получаем менеджера событий
			ObjectEventManager eventManager = ObjectEventManager.getInstance();

			// обновляем полоску мп
			eventManager.notifyMpChanged(attacked);
		}

		// поглощаем урон
		info.setDamage(Math.max(damage - abs, 0));

		// тратим лимит поглащения
		limit -= abs;

		if(limit < 1)
			executor.execute(this);
	}

	@Override
	public void onExit()
	{
		Character effected = getEffected();

		if(effected != null)
			// удаляем прослушку урона
			effected.removeDamageListener(this);

		super.onExit();
	}

	@Override
	public void onStart()
	{
		Character effected = getEffected();

		if(effected != null)
			// добавляем прослушку урона
			effected.addDamageListener(this);

		// обновляем лимит
		this.limit = template.getPower();

		super.onStart();
	}

	@Override
	public void run()
	{
		// тот, на ком висит эффект
		Character effector = getEffector();

		// если его нет, выходим
		if(effector == null)
			return;

		// эффект лист, в котором находится эффект
		EffectList effectList = effector.getEffectList();

		effectList.lock();
		try
		{
			exit();
		}
		finally
		{
			effectList.unlock();
		}
	}
}
