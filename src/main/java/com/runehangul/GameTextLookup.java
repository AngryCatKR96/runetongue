package com.runehangul;

import com.runehangul.text.Normalizer;
import com.runehangul.text.TextLookup;
import com.runehangul.text.Scope;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.util.Text;

/**
 * Looks up one piece of game text as the game shows it: tags are dropped, the player's own name
 * is looked up as {@code [player name]}, numbers are put back into the translation, and a miss is
 * recorded for the missing-text file. Shared by every part of the plugin that replaces text.
 * Client thread only.
 */
@Singleton
class GameTextLookup
{
	private static final char NBSP = '\u00A0';
	/** Spaces game or wiki text may use instead of a plain one (RuneLingual's replaceSpecialSpaces). */
	private static final Pattern SPECIAL_SPACES = Pattern.compile("[\\t\\u00A0\\u2000-\\u200A\\u202F\\u205F\\u3000]");
	private static final Pattern ZERO_WIDTH = Pattern.compile("[\\u200B-\\u200D\\uFEFF]");

	private final Client client;
	private final TranslationStore store;
	private final MissingTexts missing;

	@Inject
	GameTextLookup(Client client, TranslationStore store, MissingTexts missing)
	{
		this.client = client;
		this.store = store;
		this.missing = missing;
	}

	boolean isReady()
	{
		return store.isLoaded();
	}

	/** The translation of {@code shown}, or null. {@code record} is false for text that may hold other players' words. */
	String find(String shown, Scope scope, boolean record)
	{
		return lookUp(plain(shown), scope, record);
	}

	/**
	 * Like {@link #find} but for interface text, whose data keeps color tags (as {@code <colNum0>})
	 * and, for widgets whose lines are laid out by hand, {@code <br>}. The translation comes back
	 * with the same tags.
	 */
	String findKeepingTags(String shown, Scope scope, boolean keepBr, boolean record)
	{
		String english = spaces(shown);
		if (!keepBr)
		{
			english = english.replace(" <br>", " ").replace("<br> ", " ").replace("<br>", " ");
		}
		return lookUp(english.trim(), scope, record);
	}

	private String lookUp(String english, Scope scope, boolean record)
	{
		if (english.isEmpty())
		{
			return null;
		}
		String player = playerName();
		if (player != null)
		{
			english = english.replace(player, Normalizer.PLAYER_NAME);
		}
		String text = TextLookup.translate(store.get(), english, scope);
		if (text == null)
		{
			if (record && store.isLoaded())
			{
				missing.record(Normalizer.normalizeSource(english).getKey(), scope);
			}
			return null;
		}
		return player == null ? text : text.replace(Normalizer.PLAYER_NAME, player);
	}

	/** Game text without tags; a {@code <br>} the game put in place of a space becomes a space again. */
	static String plain(String shown)
	{
		return spaces(Text.removeTags(shown.replaceAll("(?<=\\S)<br>(?=\\S)", " "))).trim();
	}

	private static String spaces(String text)
	{
		return ZERO_WIDTH.matcher(SPECIAL_SPACES.matcher(text).replaceAll(" ")).replaceAll("");
	}

	private String playerName()
	{
		Player local = client.getLocalPlayer();
		String name = local == null ? null : local.getName();
		return name == null || name.isEmpty() ? null : name.replace(NBSP, ' ');
	}
}
