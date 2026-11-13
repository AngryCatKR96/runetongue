package com.runehangul;

import com.runehangul.WidgetTexts.Layout;
import com.runehangul.text.Scope;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetTextAlignment;

/**
 * Per-widget rules for interface text, ported from RuneLingual's {@code Ids} and
 * {@code WidgetCapture} (BSD 2-Clause, Copyright (c) 2024, Y. S. Jack): where in the data a
 * widget's text is, how its lines are laid out, and which widgets show other players' names.
 */
final class InterfaceRules
{
	private InterfaceRules()
	{
	}

	static final Scope GENERAL_UI = Scope.of("interface", "generalUI", "");
	private static final Scope MAIN_TABS = Scope.of("interface", "mainTabs", "");

	/** Widgets whose subtree sets the scope for the text inside it. */
	private static final Map<Integer, Scope> CONTEXTS = Map.ofEntries(
		Map.entry(InterfaceID.Toplevel.SIDE_PANELS, MAIN_TABS),
		Map.entry(InterfaceID.ToplevelPreEoc.SIDE_PANELS, MAIN_TABS),
		Map.entry(InterfaceID.ToplevelOsrsStretch.SIDE_PANELS, MAIN_TABS),
		Map.entry(InterfaceID.CombatInterface.UNIVERSE, tab("combatOptionsTab")),
		Map.entry(InterfaceID.Stats.UNIVERSE, tab("skillsTab")),
		Map.entry(InterfaceID.AccountSummarySidepanel.SUMMARY_CONTENTS, tab("characterSummaryTab")),
		Map.entry(InterfaceID.Questlist.UNIVERSE, tab("questListTab")),
		Map.entry(InterfaceID.AreaTask.TASKBOX, tab("achievementDiaryTab")),
		Map.entry(InterfaceID.Prayerbook.UNIVERSE, tab("prayerTab")),
		Map.entry(InterfaceID.MagicSpellbook.UNIVERSE, tab("spellBookTab")),
		Map.entry(InterfaceID.SideChannelsLarge.UNIVERSE, tab("groupTab")),
		Map.entry(InterfaceID.SideChannels.UNIVERSE, tab("groupTab")),
		Map.entry(InterfaceID.PvpArenaSidepanel.UNIVERSE, tab("groupTab")),
		Map.entry(InterfaceID.Friends.UNIVERSE, tab("friendsTab")),
		Map.entry(InterfaceID.Ignore.UNIVERSE, tab("ignoreTab")),
		Map.entry(InterfaceID.Account.CONTENT, tab("accountManagementTab")),
		Map.entry(InterfaceID.SettingsSide.UNIVERSE, tab("settingsTab")),
		Map.entry(InterfaceID.Emote.UNIVERSE, tab("emotesTab")),
		Map.entry(InterfaceID.Music.UNIVERSE, tab("musicTab")),
		Map.entry(InterfaceID.Logout.UNIVERSE, tab("logoutTab")),
		Map.entry(InterfaceID.Worldswitcher.UNIVERSE, tab("worldSwitcherTab")),
		Map.entry(InterfaceID.SkillGuide.INFINITY, Scope.of("interface", "generalUI", "skillGuide")),
		Map.entry(InterfaceID.Chatbox.CONTROLS, Scope.of("interface", "chatButtons", "")),
		Map.entry(InterfaceID.WelcomeScreen.CONTENT, Scope.of("interface", "loginScreen", "")),
		Map.entry(InterfaceID.CaOverview.INFINITY, Scope.of("interface", "combAchvmt", "")),
		Map.entry(InterfaceID.CaTasks.INFINITY, Scope.of("interface", "combAchvmt", "")),
		Map.entry(InterfaceID.CaBosses.INFINITY, Scope.of("interface", "combAchvmt", "")),
		Map.entry(InterfaceID.CaRewards.INFINITY, Scope.of("interface", "combAchvmt", "")),
		Map.entry(InterfaceID.CaBoss.INFINITY, Scope.of("interface", "combAchvmt", "")),
		Map.entry(InterfaceID.CaTasks.DROPDOWN_BOX, Scope.of("interface", "combAchvmt", "")));

	/** Text widgets that show a name, looked up as one (without color tags). */
	private static final Map<Integer, Scope> NAMES = Map.of(
		InterfaceID.CombatInterface.TITLE, Scope.of("name", "item", ""),
		InterfaceID.CaBosses.BOSSES_NAME, Scope.of("name", "npc", ""),
		InterfaceID.CaBoss.BOSS_NAME, Scope.of("name", "npc", ""),
		InterfaceID.Questlist.LIST, Scope.of("manual", "quest", ""));

	/** Hover displays with several values in one text: each line on its own. */
	private static final Set<Integer> SPLIT_AT_BR = Set.of(InterfaceID.Stats.TOOLTIP, InterfaceID.XpDrops.TOOLTIP);

	/** Text whose data keeps the game's line breaks (descriptions, buttons with two lines). */
	private static final Set<Integer> KEEP_BR = Set.of(
		InterfaceID.Prayerbook.TOOLTIP, InterfaceID.MagicSpellbook.TOOLTIP,
		InterfaceID.Friends.UNIVERSE_TEXT4, InterfaceID.Friends.UNIVERSE_TEXT6,
		InterfaceID.SettingsSide.TOOLTIP, InterfaceID.Emote.TOOLTIP,
		InterfaceID.Worldswitcher.TOOLTIP, InterfaceID.Worldswitcher.INFO,
		InterfaceID.PohOptions.TOOLTIP, InterfaceID.PohOptions.ROOT_TEXT2, InterfaceID.PohOptions.ROOT_TEXT3,
		InterfaceID.Prayerbook.FILTERBUTTON, InterfaceID.MagicSpellbook.FILTERBUTTON,
		InterfaceID.GimSidepanel.INVITE, InterfaceID.GimSidepanel.SETTINGS,
		InterfaceID.ChatchannelCurrent.CHANNELNAME, InterfaceID.ChatchannelCurrent.CHANNELOWNER,
		InterfaceID.ChatchannelCurrent.JOINLEAVE, InterfaceID.ChatchannelCurrent.SETUP,
		InterfaceID.ClansSidepanel.HEADER, InterfaceID.ClansSidepanel.CHAT, InterfaceID.ClansSidepanel.INFO,
		InterfaceID.ClansGuestSidepanel.HEADER, InterfaceID.ClansGuestSidepanel.CHAT,
		InterfaceID.ClansGuestSidepanel.INFO, InterfaceID.ClansGuestSidepanel.SEARCH,
		InterfaceID.Friends.LIST, InterfaceID.Account.NAME_TEXT);

	/** Widgets given a fixed width and left on their own lines (achievement diary area names). */
	private static final Map<Integer, Integer> FIXED_WIDTH = Map.of(InterfaceID.AreaTask.TASKBOX, 110);

	/** Subtrees never translated: chat lines, the player's name, clocks, search boxes. */
	private static final Set<Integer> SKIPPED_WIDGETS = Set.of(
		InterfaceID.Chatbox.CHATDISPLAY, InterfaceID.Chatbox.LINE499, InterfaceID.Chatbox.SCROLLAREA,
		InterfaceID.AccountSummarySidepanel.SUMMARY_PLAYER_NAME, InterfaceID.Ignore.LIST_CONTAINER,
		InterfaceID.GimSidepanel.TITLE, InterfaceID.SideChannelsLarge.CLOCK, InterfaceID.SideChannels.CLOCK,
		InterfaceID.Settings.SEARCH_TEXT, InterfaceID.CaBosses.SEARCH_TEXT, InterfaceID.ToplevelDisplay.LAYERS);

	/** Interfaces translated elsewhere ({@link DialogueTranslator}) or not at all (private chat). */
	private static final Set<Integer> SKIPPED_GROUPS = Set.of(
		InterfaceID.PM_CHAT,
		InterfaceID.CHAT_LEFT, InterfaceID.CHAT_RIGHT, InterfaceID.CHAT_BOTH, InterfaceID.CHATMENU,
		InterfaceID.MESSAGEBOX, InterfaceID.MESSAGEBOX_TITLED, InterfaceID.MESSAGEBOX_URL,
		InterfaceID.OBJECTBOX, InterfaceID.OBJECTBOX_DOUBLE, InterfaceID.GRAPHICBOX, InterfaceID.LEVELUP_DISPLAY);

	/** Interfaces with other players' names in them: translated, but unknown text is not recorded. */
	private static final Set<Integer> UNRECORDED_GROUPS = Set.of(
		InterfaceID.FRIENDS, InterfaceID.IGNORE, InterfaceID.CHATCHANNEL_CURRENT, InterfaceID.CHATCHANNEL_SETUP,
		InterfaceID.CLANS_SIDEPANEL, InterfaceID.CLANS_GUEST_SIDEPANEL, InterfaceID.CLANS_MEMBERS,
		InterfaceID.GIM_SIDEPANEL, InterfaceID.TRADEMAIN, InterfaceID.TRADESIDE, InterfaceID.TRADECONFIRM,
		InterfaceID.HISCORES, InterfaceID.ACCOUNT, InterfaceID.ACCOUNT_SUMMARY_SIDEPANEL, InterfaceID.GROUPING);

	private static final Set<Integer> CHAT_BUTTON_NAMES = Set.of(
		InterfaceID.Chatbox.CHAT_GAME_TEXT1, InterfaceID.Chatbox.CHAT_PUBLIC_TEXT1, InterfaceID.Chatbox.CHAT_PRIVATE_TEXT1,
		InterfaceID.Chatbox.CHAT_FRIENDSCHAT_TEXT1, InterfaceID.Chatbox.CHAT_CLAN_TEXT1, InterfaceID.Chatbox.CHAT_TRADE_TEXT);
	private static final Set<Integer> CHAT_BUTTON_FILTERS = Set.of(
		InterfaceID.Chatbox.CHAT_GAME_FILTER, InterfaceID.Chatbox.CHAT_PUBLIC_FILTER, InterfaceID.Chatbox.CHAT_PRIVATE_FILTER,
		InterfaceID.Chatbox.CHAT_FRIENDSCHAT_FILTER, InterfaceID.Chatbox.CHAT_CLAN_FILTER, InterfaceID.Chatbox.CHAT_TRADE_FILTER);

	/** The chatbox itself (lines, input) is skipped; only its tab buttons are translated. */
	static boolean isChatbox(int widgetId)
	{
		return widgetId >>> 16 == InterfaceID.CHATBOX;
	}

	static Scope context(int widgetId, Scope inherited)
	{
		return CONTEXTS.getOrDefault(widgetId, inherited);
	}

	static boolean skipsSubtree(Widget widget)
	{
		return SKIPPED_WIDGETS.contains(widget.getId()) || SKIPPED_GROUPS.contains(widget.getId() >>> 16);
	}

	/** Text widgets that list other players, told apart from the UI text around them as RuneLingual did. */
	static boolean isPlayerText(Widget widget, boolean inFriendsChat)
	{
		int id = widget.getId();
		int color = widget.getTextColor();
		boolean left = widget.getXTextAlignment() == WidgetTextAlignment.LEFT;
		return (id == InterfaceID.Friends.LIST && left)
			|| (id == InterfaceID.GimSidepanel.MEMBERS && left && color == 0xffffff)
			|| (id == InterfaceID.ChatchannelCurrent.LIST && color == 0xffffff)
			|| ((id == InterfaceID.ChatchannelCurrent.CHANNELNAME || id == InterfaceID.ChatchannelCurrent.CHANNELOWNER) && inFriendsChat)
			|| ((id == InterfaceID.ClansSidepanel.HEADER || id == InterfaceID.ClansGuestSidepanel.HEADER) && color == 0xe6981f)
			|| ((id == InterfaceID.ClansSidepanel.PLAYERLIST || id == InterfaceID.ClansGuestSidepanel.PLAYERLIST) && color == 0xffffff)
			|| (id == InterfaceID.PvpArenaSidepanel.PLAYERLIST && (color == 0xffffff || color == 0x9f9f9f))
			|| (id == InterfaceID.Grouping.PLAYERLIST && color == 0xffffff)
			|| id == InterfaceID.Chatbox.SCROLLAREA;
	}

	/** Widgets whose menus are about another player: chat lines, friends, clan and channel lists. */
	static boolean listsPlayers(Widget widget)
	{
		int group = widget.getId() >>> 16;
		return group == InterfaceID.CHATBOX || group == InterfaceID.PM_CHAT || UNRECORDED_GROUPS.contains(group)
			|| isPlayerText(widget, false);
	}

	static WidgetTexts.Request request(Widget widget, Scope context, boolean record)
	{
		int id = widget.getId();
		boolean recordHere = record && !UNRECORDED_GROUPS.contains(id >>> 16);
		Scope name = NAMES.get(id);
		if (name != null && !(id == InterfaceID.CombatInterface.TITLE && "Unarmed".equals(widget.getText())))
		{
			return new WidgetTexts.Request(name, Layout.AS_IS, false, recordHere, true, 0);
		}
		Layout layout = SPLIT_AT_BR.contains(id) ? Layout.SPLIT_AT_BR
			: KEEP_BR.contains(id) ? Layout.KEEP_BR
			: FIXED_WIDTH.containsKey(id) ? Layout.AS_IS
			: Layout.WRAP;
		return new WidgetTexts.Request(context, layout, true, recordHere, true, FIXED_WIDTH.getOrDefault(id, 0));
	}

	/** Korean chat tab names fit only side by side with the filter: name left, filter right. */
	static void alignChatButton(Widget widget)
	{
		if (CHAT_BUTTON_NAMES.contains(widget.getId()))
		{
			widget.setXTextAlignment(WidgetTextAlignment.LEFT);
		}
		else if (CHAT_BUTTON_FILTERS.contains(widget.getId()))
		{
			widget.setXTextAlignment(WidgetTextAlignment.RIGHT);
		}
	}

	private static Scope tab(String source)
	{
		return Scope.of("interface", "mainTabs", source);
	}
}
