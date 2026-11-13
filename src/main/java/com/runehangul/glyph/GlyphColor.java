package com.runehangul.glyph;

import java.awt.Color;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GlyphColor
{
	BLACK(new Color(33, 33, 33), null),
	// Not pure black: ImageUtil.getImageIndexedSprite maps opaque 0x000000 to the transparent palette slot.
	YELLOW(new Color(255, 255, 0), new Color(1, 1, 1)),
	RED(new Color(255, 0, 0), null),
	ORANGE(new Color(255, 165, 0), null),
	WHITE(new Color(255, 255, 255), null),
	LIGHTBLUE(new Color(0, 255, 255), null),
	GREEN(new Color(0, 255, 0), null),
	BLUE(new Color(0, 0, 255), null);

	private final Color text;
	private final Color shadow;
}
