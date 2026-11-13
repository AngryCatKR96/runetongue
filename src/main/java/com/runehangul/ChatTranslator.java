package com.runehangul;

import com.runehangul.glyph.GlyphIcons;
import com.runehangul.text.Scope;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.NPC;
import net.runelite.api.gameval.VarbitID;

/**
 * Translates game text that arrives as chat messages: examine text, game messages, level-up and
 * NPC lines echoed into the chatbox. Anything a player typed is never touched, and messages that
 * name other players (broadcasts, notifications, requests) are neither translated nor recorded.
 * Client thread only.
 */
@Singleton
class ChatTranslator
{
	private static final int OVERHEAD_YELLOW = 0xFFFF00;
	private static final Pattern LEADING_COLOR = Pattern.compile("^<col=([0-9a-fA-F]{6})>");

	private final Client client;
	private final GlyphIcons glyphs;
	private final GameTextLookup lookup;

	@Inject
	ChatTranslator(Client client, GlyphIcons glyphs, GameTextLookup lookup)
	{
		this.client = client;
		this.glyphs = glyphs;
		this.lookup = lookup;
	}

	/** Returns true if the message text was replaced. */
	boolean translate(MessageNode node)
	{
		Scope scope = scopeOf(node);
		String text = node.getValue();
		if (scope == null || text == null || text.contains("<img="))
		{
			return false;
		}
		String found = lookup.find(text, scope, !namesPlayers(node.getType()));
		if (found == null)
		{
			return false;
		}
		Matcher m = LEADING_COLOR.matcher(text);
		String tag = m.find() ? m.group() : "";
		int color = tag.isEmpty() ? defaultColor() : Integer.parseInt(m.group(1), 16);
		node.setValue(tag + glyphs.toChatText(found, color, false));
		return true;
	}

	/**
	 * Translates an NPC's overhead line, drawn yellow with a shadow like the game's. Players'
	 * overhead text is their own chat and is never touched.
	 */
	void translateOverhead(Actor actor, String text)
	{
		if (!(actor instanceof NPC) || text == null || text.contains("<img=") || actor.getName() == null)
		{
			return;
		}
		String name = GameTextLookup.plain(actor.getName());
		String found = lookup.find(text, Scope.of("dialogue", name, name), true);
		if (found != null)
		{
			actor.setOverheadText(glyphs.toChatText(found, OVERHEAD_YELLOW, true));
		}
	}

	/** Messages about other players: translated through templates, never recorded. */
	private static boolean namesPlayers(ChatMessageType type)
	{
		switch (type)
		{
			case BROADCAST:
			case CLAN_MESSAGE:
			case CLAN_GUEST_MESSAGE:
			case CLAN_GIM_MESSAGE:
			case FRIENDSCHATNOTIFICATION:
			case LOGINLOGOUTNOTIFICATION:
			case TRADEREQ:
			case CHALREQ_TRADE:
			case CHALREQ_FRIENDSCHAT:
			case CHALREQ_CLANCHAT:
			case PLAYERRELATED:
			case TENSECTIMEOUT:
			case CLAN_CREATION_INVITATION:
			case CLAN_GIM_FORM_GROUP:
			case CLAN_GIM_GROUP_WITH:
			case TRADE:
				return true;
			default:
				return false;
		}
	}

	/** Where the message's text is found in the data, or null for messages we leave alone. */
	private static Scope scopeOf(MessageNode node)
	{
		ChatMessageType type = node.getType();
		switch (type)
		{
			case ITEM_EXAMINE:
				return Scope.of("examine", "item", "");
			case NPC_EXAMINE:
				return Scope.of("examine", "npc", "");
			case OBJECT_EXAMINE:
				return Scope.of("examine", "object", "");
			case LEVELUPMESSAGE:
				return Scope.of("lvl_up_msg", "", "");
			case NPC_SAY:
			case DIALOG:
			{
				String name = node.getName() == null ? "" : GameTextLookup.plain(node.getName());
				return Scope.of("dialogue", name, name);
			}
			case GAMEMESSAGE:
			case ENGINE:
			case SPAM:
			case MESBOX:
			case WELCOME:
			case DIDYOUKNOW:
				return Scope.ANY;
			default:
				// System lines about other players, but never anything a player typed.
				return namesPlayers(type) ? Scope.ANY : null;
		}
	}

	/** Game messages are black on the opaque chatbox and white on the transparent one. */
	private int defaultColor()
	{
		boolean transparent = client.isResized() && client.getVarbitValue(VarbitID.CHATBOX_TRANSPARENCY) == 1;
		return transparent ? 0xFFFFFF : 0x000000;
	}
}
