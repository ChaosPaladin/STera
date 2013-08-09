package tera.gameserver.model.actions.dialogs;

import rlib.util.random.Random;
import tera.gameserver.model.items.ItemInstance;
import tera.gameserver.model.playable.Player;
import tera.gameserver.network.serverpackets.DialogPanel;
import tera.gameserver.network.serverpackets.DialogPanel.PanelType;
import tera.gameserver.network.serverpackets.EnchatItemInfo;

/**
 * Модель диалога по зачарованию вещей.
 *
 * @author Ronn
 */
public class EnchantItemDialog extends AbstractActionDialog
{
	public static EnchantItemDialog newInstance(Player player)
	{
		EnchantItemDialog dialog = (EnchantItemDialog) ActionDialogType.ENCHANT_ITEM_DIALOG.newInstance();

		if(dialog == null)
			dialog = new EnchantItemDialog();

		dialog.actor = player;
		dialog.enemy = player;

		return dialog;
	}

	/** рандоминайзер диалога */
	private Random random;

	/** целевой затачиваемый итем */
	private ItemInstance target;
	/** средство для заточки */
	private ItemInstance alkahest;
	/** ресурс для заточки */
	private ItemInstance source;

	@Override
	public boolean apply()
	{
		// TODO Автоматически созданная заглушка метода
		return false;
	}

	@Override
	public ActionDialogType getType()
	{
		return ActionDialogType.ENCHANT_ITEM_DIALOG;
	}

	@Override
	public synchronized boolean init()
	{
		System.out.println("init 1");

		if(super.init())
		{
			System.out.println("init 2");
			actor.sendPacket(DialogPanel.getInstance(actor, PanelType.ENCHANT_ITEM), true);

			try
			{
				Thread.sleep(1000);
			}
			catch(InterruptedException e)
			{
				// TODO Автоматически созданный блок catch
				e.printStackTrace();
			}

			updateDialog();

			return true;
		}

		return false;
	}

	public void setAlkahest(Object object)
	{

	}

	public void setSource(Object object)
	{

	}

	public void setTarget(Object object)
	{

	}

	private void updateDialog()
	{
		System.out.println("update dialog");

		actor.sendPacket(EnchatItemInfo.getInstance(this), true);
	}

}
