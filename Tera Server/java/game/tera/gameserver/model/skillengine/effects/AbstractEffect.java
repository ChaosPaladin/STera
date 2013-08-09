package tera.gameserver.model.skillengine.effects;

import rlib.logging.Logger;
import rlib.logging.Loggers;
import rlib.util.array.Arrays;
import tera.gameserver.model.Character;
import tera.gameserver.model.EffectList;
import tera.gameserver.model.Party;
import tera.gameserver.model.skillengine.Effect;
import tera.gameserver.model.skillengine.EffectState;
import tera.gameserver.model.skillengine.EffectType;
import tera.gameserver.model.skillengine.ResistType;
import tera.gameserver.model.skillengine.funcs.Func;
import tera.gameserver.network.serverpackets.AppledEffect;
import tera.gameserver.network.serverpackets.CancelEffect;
import tera.gameserver.taskmanager.EffectTaskManager;
import tera.gameserver.templates.EffectTemplate;
import tera.gameserver.templates.SkillTemplate;

/**
 * Базовая реализация эффектов.
 *
 * @author Ronn
 */
public abstract class AbstractEffect implements Effect
{
	protected static final Logger log = Loggers.getLogger(Effect.class);

	/** тот, кто наложил эффект */
	protected Character effector;
	/** тот, на ком эффект висит */
	protected Character effected;

	/** эффект лист, в котором находится эффект */
	protected EffectList effectList;

	/** заюзан ли эффект */
	protected boolean inUse;

	/** время старта */
	protected long startTime;

	/** период эффекта */
	protected int period;
	/** счетчик */
	protected int count;

	/** статус эффекта */
	protected volatile EffectState state;

	/** скил, с которого был наложен эффект */
	protected SkillTemplate skillTemplate;
	/** функции, которые будут применятся при старте эффекта */
	protected Func[] funcs;

	/** темплейт эффекта */
	protected EffectTemplate template;

	/**
	 * @param effectTemplate темплейт эффекта.
	 * @param effector тот, кто наложил эффект.
	 * @param effected тот, на кого наложили эффект,
	 * @param skillTemplate скил, которым был наложен эффект.
	 */
	public AbstractEffect(EffectTemplate template, Character effector, Character effected, SkillTemplate skillTemplate)
	{
		this.effector = effector;
		this.effected = effected;
		this.skillTemplate = skillTemplate;
		this.state = EffectState.CREATED;
		this.funcs = template.getFuncs();
		this.template = template;
		this.period = template.getTime();
		this.count = template.getCount();
		this.startTime = System.currentTimeMillis();
	}

	@Override
	public void exit()
	{
		// получаем эффект лист
		EffectList effectList = getEffectList();

		// если его нет, выходим
		if(effectList == null)
		{
			log.warning(this, new Exception("not found effect list."));
			return;
		}

		effectList.lock();
		try
		{
			// если уже остановлен эффект, выходим
			if(getState() == EffectState.FINISHED)
				return;

			// ставим статус финиша
			setState(EffectState.FINISHING);

			// запускаем выполнение нового такса
			scheduleEffect();
		}
		finally
		{
			effectList.unlock();
		}
	}

	@Override
	public void finalyze()
	{
		effector = null;
		effected = null;
		effectList = null;
	}

	@Override
	public void fold()
	{
		template.put(this);
	}

	@Override
	public int getChance()
	{
		return template.getChance();
	}

	@Override
	public int getCount()
	{
		return count;
	}

	@Override
	public Character getEffected()
	{
		return effected;
	}

	@Override
	public int getEffectId()
	{
		return template.getId() < 0? skillTemplate.getId() : template.getId();
	}

	@Override
	public EffectList getEffectList()
	{
		return effectList;
	}

	@Override
	public Character getEffector()
	{
		return effector;
	}

	@Override
	public EffectType getEffectType()
	{
		return template.getConstructor();
	}

	@Override
	public Func[] getFuncs()
	{
		return funcs;
	}

	@Override
	public int getOrder()
	{
		return Arrays.indexOf(skillTemplate.getEffectTemplates(), template);
	}

	@Override
	public int getPeriod()
	{
		return period;
	}

	@Override
	public ResistType getResistType()
	{
		return template.getResistType();
	}

	@Override
	public int getSkillClassId()
	{
		return skillTemplate.getClassId();
	}

	@Override
	public int getSkillId()
	{
		return skillTemplate.getId();
	}

	@Override
	public SkillTemplate getSkillTemplate()
	{
		return skillTemplate;
	}

	@Override
	public String getStackType()
	{
		return template.getStackType();
	}

	@Override
	public long getStartTime()
	{
		return startTime;
	}

	@Override
	public EffectState getState()
	{
		return state;
	}

	@Override
	public EffectTemplate getTemplate()
	{
		return template;
	}

	@Override
	public int getTime()
	{
		return (int) ((System.currentTimeMillis() - startTime) / 1000);
	}

	@Override
	public int getTimeEnd()
	{
		return getTotalTime() - getTime();
	}

	@Override
	public int getTimeForPacket()
	{
		return getTotalTime() * 1000;
	}

	@Override
	public int getTotalTime()
	{
		return period * count;
	}

	@Override
	public int getUsingCount()
	{
		return template.getCount() - count;
	}

	@Override
	public boolean hasStackType()
	{
		return !template.getStackType().isEmpty();
	}

	@Override
	public boolean isAura()
	{
		return false;
	}

	@Override
	public boolean isDebuff()
	{
		return template.isDebuff();
	}

	@Override
	public boolean isEffect()
	{
		return true;
	}

	@Override
	public boolean isEnded()
	{
		return state == EffectState.FINISHED || state == EffectState.FINISHING;
	}

	@Override
	public boolean isFinished()
	{
		return state == EffectState.FINISHED;
	}

	@Override
	public boolean isInUse()
	{
		return inUse;
	}

	@Override
	public boolean isNoAttack()
	{
		return template.isNoAttack();
	}

	@Override
	public boolean isNoAttacked()
	{
		return template.isNoAttacked();
	}

	@Override
	public boolean isNoOwerturn()
	{
		return template.isNoOwerturn();
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}

	@Override
	public void onExit()
	{
		// получаем персонажа. на котром висел эффект
		Character effected = getEffected();

		// если его нет, выходим
		if(effected == null)
		{
			log.warning(this, new Exception("not found effected"));
			return;
		}

		// получаем функции темплейта
		Func[] funcs = template.getFuncs();

		// если их нет, выходим
		if(funcs.length < 1)
			return;

		// удаляем функции у персонажа
		for(int i = 0, length=  funcs.length; i < length; i++)
			funcs[i].removeFuncTo(effected);

		// обновляем инфу у него
		effected.updateInfo();
	}

	@Override
	public void onStart()
	{
		// получаем того, на кого эффект повесили
		Character effected = getEffected();

		// если его нет, выходим
		if(effected == null)
		{
			log.warning(this, new Exception("not found effected"));
			return;
		}

		// получаем функции темплейта
		Func[] funcs = template.getFuncs();

		// если их нет ,выходим
		if(funcs.length < 1)
			return;

		// добавляем все функции
		for(int i = 0, length=  funcs.length; i < length; i++)
			funcs[i].addFuncTo(effected);

		// обновляем ему информацию
		effected.updateInfo();
	}

	@Override
	public void reinit()
	{
		this.state = EffectState.CREATED;
		this.period = template.getTime();
		this.count = template.getCount();
		this.startTime = System.currentTimeMillis();
	}

	@Override
	public void scheduleEffect()
	{
		// получаем того, на кого повесили эффект
		Character effected = getEffected();

		// получаем того, кто повесил эффект
		Character effector = getEffector();

		// если нет эффктеда, выходим
		if(effected == null)
		{
			log.warning(this, new Exception("not found effected"));
			return;
		}

		// если нет эффектора, выходим
		if(effector == null)
		{
			log.warning(this, new Exception("not found effector"));
			return;
		}

		// получаем эффект лист, в котором находится эффект
		EffectList effectList = getEffectList();

		// если его нет, выходим
		if(effectList == null)
		{
			log.warning(this, new Exception("not found effect list."));
			return;
		}

		effectList.lock();
		try
		{
			switch(getState())
			{
				// стадия запуска эффекта
				case CREATED:
				{
					// запускаем эффект
					onStart();

					// ставим активную фазу
					setState(EffectState.ACTING);

					// отправляем пакет отображения бафа
					effected.broadcastPacket(AppledEffect.getInstance(effector, effected, this));

					// получаем пати эффектеда
					Party party = effected.getParty();

					// если пати есть
					if(party != null)
						// обновляем в ней эффект лист
						party.updateEffects(effected.getPlayer());

					// получаем менеджера эффетков
					EffectTaskManager effectManager = EffectTaskManager.getInstance();

					// добавляем на обработку
					effectManager.addTask(this, period);

					break;
				}
				// стадия работы эффекта
				case ACTING:
				{
					// если еще остались приминения
					if(count > 0)
					{
						// уменьшаем на 1
						count--;

						// применяем
						if(onActionTime() && count > 0)
							break;
					}

					// если коунтер кончился, меняем стадию на звершение
					setState(EffectState.FINISHING);
				}
				// если стадия завершения
				case FINISHING:
				{
					// ставим стадию завершенности
					setState(EffectState.FINISHED);

					// флаг не использования
					setInUse(false);

					// выполняем метод выхода
					onExit();

					// удаляем с эффект листа
					effected.removeEffect(this);

					// отправляем пакет
					effected.broadcastPacket(CancelEffect.getInstance(effected, getEffectId()));

					// получаем пати эффектеда
					Party party = effected.getParty();

					// если пати есть
					if(party != null)
						// обновляем в ней эффект лист
						party.updateEffects(effected.getPlayer());

					break;
				}
				default:
					log.warning(this, new Exception("incorrect effect state " + state));
			}
		}
		finally
		{
			effectList.unlock();
		}
	}

	@Override
	public void setCount(int count)
	{
		this.count = count;
	}

	@Override
	public void setEffected(Character effected)
	{
		this.effected = effected;
	}

	@Override
	public void setEffectList(EffectList effectList)
	{
		this.effectList = effectList;
	}

	@Override
	public void setEffector(Character effector)
	{
		this.effector = effector;
	}

	@Override
	public void setInUse(boolean inUse)
	{
		this.inUse = inUse;
	}

	@Override
	public void setPeriod(int period)
	{
		this.period = period;
	}

	@Override
	public void setStartTime(long startTime)
	{
		this.startTime = startTime;
	}

	@Override
	public void setState(EffectState state)
	{
		this.state = state;
	}

	@Override
	public boolean isDynamicCount()
	{
		return template.isDynamicCount();
	}

	@Override
	public boolean isDynamicTime()
	{
		return template.isDynamicTime();
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}
}
