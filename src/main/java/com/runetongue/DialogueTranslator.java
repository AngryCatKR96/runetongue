package com.runetongue;

import com.runetongue.text.Scope;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/**
 * Translates the chatbox dialogue: NPC lines, player lines, options, the NPC name, the
 * "Click here to continue" prompt, and the message, item, picture and level-up boxes. Runs every
 * frame on the client thread but only looks at those few widgets; {@link WidgetTexts} does the
 * rest.
 */
@Singleton
class DialogueTranslator
{
	private static final String DIALOGUE = "dialogue";
	private static final Scope FIXED = Scope.of(DIALOGUE, "manual", "");
	private static final Scope BOX = Scope.of(DIALOGUE, "", "");
	private static final Scope LEVEL_UP = Scope.of("lvl_up_msg", "", "");
	private static final String PLAYER = "Player";

	private final Client client;
	private final WidgetTexts texts;
	/** The NPC in the current conversation, in English; player lines and options are looked up under it. */
	private String npc = "";

	@Inject
	DialogueTranslator(Client client, WidgetTexts texts)
	{
		this.client = client;
		this.texts = texts;
	}

	void update()
	{
		String name = handle(InterfaceID.ChatLeft.NAME, Scope.of("name", "npc", ""), false);
		if (name != null)
		{
			npc = GameTextLookup.plain(name);
		}
		handle(InterfaceID.ChatLeft.TEXT, Scope.of(DIALOGUE, npc, npc), true);
		handle(InterfaceID.ChatLeft.CONTINUE, FIXED, false);
		handle(InterfaceID.ChatRight.TEXT, Scope.of(DIALOGUE, npc, PLAYER), true);
		handle(InterfaceID.ChatRight.CONTINUE, FIXED, false);

		Widget options = client.getWidget(InterfaceID.Chatmenu.OPTIONS);
		Widget[] children = options == null || options.isHidden() ? null : options.getDynamicChildren();
		if (children != null)
		{
			for (int i = 0; i < children.length; i++)
			{
				texts.handle(children[i], key(InterfaceID.Chatmenu.OPTIONS, i),
					WidgetTexts.Request.dialogue(i == 0 ? FIXED : Scope.of(DIALOGUE, npc, PLAYER), false));
			}
		}

		// Boxes without a speaker: plain and titled messages, item and picture boxes, level-ups.
		handle(InterfaceID.Messagebox.TEXT, BOX, true);
		handle(InterfaceID.Messagebox.CONTINUE, FIXED, false);
		handle(InterfaceID.MessageboxTitled.TITLE, BOX, false);
		handle(InterfaceID.MessageboxTitled.TEXT, BOX, true);
		handle(InterfaceID.MessageboxTitled.CONTINUE, FIXED, false);
		handle(InterfaceID.Objectbox.TEXT, BOX, true);
		handle(InterfaceID.ObjectboxDouble.TEXT, BOX, true);
		handle(InterfaceID.Graphicbox.TEXT, BOX, true);
		handle(InterfaceID.LevelupDisplay.TEXT1, LEVEL_UP, true);
		handle(InterfaceID.LevelupDisplay.TEXT2, LEVEL_UP, true);
		handle(InterfaceID.LevelupDisplay.CONTINUE, FIXED, false);
	}

	void reset()
	{
		npc = "";
	}

	private String handle(int widgetId, Scope scope, boolean wrap)
	{
		return texts.handle(client.getWidget(widgetId), key(widgetId, 0), WidgetTexts.Request.dialogue(scope, wrap));
	}

	/** Widget ids fill 32 bits; the low 16 bits of the key leave room for a dynamic child's index. */
	static long key(int widgetId, int index)
	{
		return ((long) widgetId << 16) | index;
	}
}
