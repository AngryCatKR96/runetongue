package com.runehangul;

import com.runehangul.text.Scope;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

/**
 * Translates the text of every visible interface: side tabs, skill guides, settings, lists and
 * hover boxes. Walks the visible widget tree each frame, works out each text widget's scope from
 * the interfaces it sits in, and hands it to {@link WidgetTexts}, which only looks a text up when
 * it changes. The per-widget rules come from RuneLingual ({@link InterfaceRules}). Client thread
 * only.
 */
@Slf4j
@Singleton
class InterfaceTranslator
{
	/** Player names are at most 12 characters; shorter unknown text is not recorded as missing. */
	private static final int SHORTEST_RECORDED = 13;
	private static final int STATS_EVERY = 3000;

	private final Client client;
	private final WidgetTexts texts;
	private long frames;
	private long totalNanos;
	private long maxNanos;
	private int lastVisited;

	@Inject
	InterfaceTranslator(Client client, WidgetTexts texts)
	{
		this.client = client;
		this.texts = texts;
	}

	void update()
	{
		Widget[] roots = client.getWidgetRoots();
		if (roots == null)
		{
			return;
		}
		long start = System.nanoTime();
		lastVisited = 0;
		boolean inFriendsChat = client.getFriendsChatManager() != null;
		for (Widget root : roots)
		{
			visit(root, DialogueTranslator.key(root.getId(), 0), InterfaceRules.GENERAL_UI, inFriendsChat);
		}
		measure(System.nanoTime() - start);
	}

	private void visit(Widget widget, long key, Scope inherited, boolean inFriendsChat)
	{
		if (widget == null || widget.isSelfHidden() || InterfaceRules.skipsSubtree(widget))
		{
			return;
		}
		lastVisited++;
		Scope context = InterfaceRules.context(widget.getId(), inherited);
		if (widget.getType() == WidgetType.TEXT && widget.getFontId() != -1)
		{
			translate(widget, key, context, inFriendsChat);
		}
		Widget[] dynamic = widget.getDynamicChildren();
		if (dynamic != null)
		{
			for (int i = 0; i < dynamic.length; i++)
			{
				visit(dynamic[i], DialogueTranslator.key(widget.getId(), i + 1), context, inFriendsChat);
			}
		}
		visitAll(widget.getStaticChildren(), context, inFriendsChat);
		visitAll(widget.getNestedChildren(), context, inFriendsChat);
	}

	private void visitAll(Widget[] children, Scope context, boolean inFriendsChat)
	{
		if (children != null)
		{
			for (Widget child : children)
			{
				visit(child, DialogueTranslator.key(child.getId(), 0), context, inFriendsChat);
			}
		}
	}

	private void translate(Widget widget, long key, Scope context, boolean inFriendsChat)
	{
		String text = widget.getText();
		if (text == null || !hasLetter(text) || InterfaceRules.isPlayerText(widget, inFriendsChat))
		{
			return;
		}
		boolean chatbox = InterfaceRules.isChatbox(widget.getId());
		if (chatbox && !"chatButtons".equals(context.getSubCategory()))
		{
			return;
		}
		boolean record = GameTextLookup.plain(text).length() >= SHORTEST_RECORDED;
		texts.handle(widget, key, InterfaceRules.request(widget, context, record));
		if (chatbox && widget.getText().contains("<img="))
		{
			InterfaceRules.alignChatButton(widget);
		}
	}

	private static boolean hasLetter(String text)
	{
		for (int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);
			if (c == '<')
			{
				int end = text.indexOf('>', i);
				if (end < 0)
				{
					return false;
				}
				i = end;
			}
			else if (Character.isLetter(c))
			{
				return true;
			}
		}
		return false;
	}

	private void measure(long nanos)
	{
		frames++;
		totalNanos += nanos;
		maxNanos = Math.max(maxNanos, nanos);
		if (frames == STATS_EVERY)
		{
			log.debug("interface walk: {} widgets, avg {} us, max {} us over {} frames",
				lastVisited, totalNanos / frames / 1000, maxNanos / 1000, frames);
			frames = 0;
			totalNanos = 0;
			maxNanos = 0;
		}
	}
}
