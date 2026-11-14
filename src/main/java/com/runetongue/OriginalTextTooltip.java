package com.runetongue;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

/**
 * Shows the English of what the mouse is over in a tooltip, next to the Korean the game now
 * shows in its top-left text. RuneLingual's MouseTooltipOverlay did the reverse (English in the
 * game, Korean in the tooltip); either way both are on screen, which players used to check a
 * translation. The conditions for showing it are RuneLingual's.
 */
class OriginalTextTooltip extends Overlay
{
	private final Client client;
	private final TooltipManager tooltipManager;
	private final MenuTranslator menu;
	private final RuneTongueConfig config;

	@Inject
	OriginalTextTooltip(Client client, TooltipManager tooltipManager, MenuTranslator menu, RuneTongueConfig config)
	{
		this.client = client;
		this.tooltipManager = tooltipManager;
		this.menu = menu;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		// also above the full screen world map and the welcome screen
		drawAfterInterface(InterfaceID.TOPLEVEL_DISPLAY);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.originalTooltip() || client.isMenuOpen())
		{
			return null;
		}
		MenuEntry[] entries = client.getMenu().getMenuEntries();
		if (entries.length == 0)
		{
			return null;
		}
		MenuEntry top = entries[entries.length - 1];
		String option = menu.originalOf(top.getOption());
		String target = menu.originalOf(top.getTarget());
		if ((option == null && target == null) || !worthShowing(top))
		{
			return null;
		}
		option = option != null ? option : top.getOption();
		target = target != null ? target : top.getTarget();
		tooltipManager.addFront(new Tooltip(target == null || target.isEmpty() ? option : option + " " + target));
		return null;
	}

	private boolean worthShowing(MenuEntry entry)
	{
		MenuAction type = entry.getType();
		if (type == MenuAction.RUNELITE_OVERLAY || type == MenuAction.CC_OP_LOW_PRIORITY
			|| type == MenuAction.WALK || type == MenuAction.CANCEL)
		{
			return false;
		}
		String option = menu.originalOf(entry.getOption());
		String target = menu.originalOf(entry.getTarget());
		if ("Continue".equals(option) || (target != null && target.contains("Slide")))
		{
			return false;
		}
		// The game is about to show, or is showing, its own tooltip.
		return client.getVarcIntValue(VarClientID.TOOLTIP_TIME) <= client.getGameCycle()
			&& client.getVarcIntValue(VarClientID.TOOLTIP_BUILT) != 1;
	}
}
