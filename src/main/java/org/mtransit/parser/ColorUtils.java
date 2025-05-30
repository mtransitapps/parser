package org.mtransit.parser;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static org.mtransit.parser.ColorUtils.AColorUtils.AColor;

@SuppressWarnings({"WeakerAccess", "unused", "SameParameterValue", "RedundantSuppression"})
public final class ColorUtils {

	public static final String BLACK = "000000";
	public static final String WHITE = "FFFFFF";

	private static final Map<String, String> colorMap = new HashMap<>();

	@NotNull
	public static String darkenIfTooLight(@NotNull String color) {
		String darkColor = colorMap.get(color);
		if (darkColor == null) {
			int colorInt = AColor.parseColor(color);
			if (isTooLight(colorInt)) {
				darkColor = darkerColor(colorInt);
			} else {
				darkColor = color;
			}
			colorMap.put(color, darkColor);
		}
		return darkColor;
	}

	public static boolean isTooLight(@NotNull String color) {
		return isTooLight(AColor.parseColor(color));
	}

	public static boolean isTooLight(int colorInt) {
		return AColorUtils.calculateLuminance(colorInt) > 0.7d;
	}

	@NotNull
	public static String darkerColor(@NotNull String color) {
		return darkerColor(AColor.parseColor(color));
	}

	@NotNull
	public static String darkerColor(int colorInt) {
		return convertToHEX(manipulateColor(colorInt, 0.7F));
	}

	@NotNull
	public static String convertToHEX(int color) {
		return String.format("%06X", (0xFFFFFF & color));
	}

	public static int manipulateColor(int color, float factor) {
		int a = AColor.alpha(color);
		int r = Math.round(AColor.red(color) * factor);
		int g = Math.round(AColor.green(color) * factor);
		int b = Math.round(AColor.blue(color) * factor);
		return AColor.argb(a,
				Math.min(r, 255),
				Math.min(g, 255),
				Math.min(b, 255));
	}

	// https://developer.android.com/reference/androidx/core/graphics/ColorUtils
	@SuppressWarnings("UnknownNullness")
	public static class AColorUtils {

		private static final ThreadLocal<double[]> TEMP_ARRAY = new ThreadLocal<>();

		/**
		 * Returns the luminance of a color as a float between {@code 0.0} and {@code 1.0}.
		 * <p>Defined as the Y component in the XYZ representation of {@code color}.</p>
		 */
		// @FloatRange(from = 0.0, to = 1.0)
		public static double calculateLuminance(/*@ColorInt */int color) {
			final double[] result = getTempDouble3Array();
			colorToXYZ(color, result);
			// Luminance is the Y component
			return result[1] / 100;
		}

		/**
		 * Convert the ARGB color to its CIE XYZ representative components.
		 *
		 * <p>The resulting XYZ representation will use the D65 illuminant and the CIE
		 * 2° Standard Observer (1931).</p>
		 *
		 * <ul>
		 * <li>outXyz[0] is X [0 ...95.047)</li>
		 * <li>outXyz[1] is Y [0...100)</li>
		 * <li>outXyz[2] is Z [0...108.883)</li>
		 * </ul>
		 *
		 * @param color  the ARGB color to convert. The alpha component is ignored
		 * @param outXyz 3-element array which holds the resulting LAB components
		 */
		public static void colorToXYZ(/*@ColorInt*/ int color, /*@NonNull*/ double[] outXyz) {
			RGBToXYZ(AColor.red(color), AColor.green(color), AColor.blue(color), outXyz);
		}

		/**
		 * Convert RGB components to its CIE XYZ representative components.
		 *
		 * <p>The resulting XYZ representation will use the D65 illuminant and the CIE
		 * 2° Standard Observer (1931).</p>
		 *
		 * <ul>
		 * <li>outXyz[0] is X [0 ...95.047)</li>
		 * <li>outXyz[1] is Y [0...100)</li>
		 * <li>outXyz[2] is Z [0...108.883)</li>
		 * </ul>
		 *
		 * @param r      red component value [0..255]
		 * @param g      green component value [0..255]
		 * @param b      blue component value [0..255]
		 * @param outXyz 3-element array which holds the resulting XYZ components
		 */
		public static void RGBToXYZ(/*@IntRange(from = 0x0, to = 0xFF)*/ int r,
				/*@IntRange(from = 0x0, to = 0xFF)*/ int g, /*@IntRange(from = 0x0, to = 0xFF)*/ int b,
				/*@NonNull*/ double[] outXyz) {
			if (outXyz.length != 3) {
				throw new IllegalArgumentException("outXyz must have a length of 3.");
			}

			double sr = r / 255.0;
			sr = sr < 0.04045 ? sr / 12.92 : Math.pow((sr + 0.055) / 1.055, 2.4);
			double sg = g / 255.0;
			sg = sg < 0.04045 ? sg / 12.92 : Math.pow((sg + 0.055) / 1.055, 2.4);
			double sb = b / 255.0;
			sb = sb < 0.04045 ? sb / 12.92 : Math.pow((sb + 0.055) / 1.055, 2.4);

			outXyz[0] = 100 * (sr * 0.4124 + sg * 0.3576 + sb * 0.1805);
			outXyz[1] = 100 * (sr * 0.2126 + sg * 0.7152 + sb * 0.0722);
			outXyz[2] = 100 * (sr * 0.0193 + sg * 0.1192 + sb * 0.9505);
		}

		/**
		 * Convert RGB components to HSL (hue-saturation-lightness).
		 * <ul>
		 * <li>outHsl[0] is Hue [0 .. 360)</li>
		 * <li>outHsl[1] is Saturation [0...1]</li>
		 * <li>outHsl[2] is Lightness [0...1]</li>
		 * </ul>
		 *
		 * @param r      red component value [0..255]
		 * @param g      green component value [0..255]
		 * @param b      blue component value [0..255]
		 * @param outHsl 3-element array which holds the resulting HSL components
		 */
		public static void RGBToHSL(/*@IntRange(from = 0x0, to = 0xFF)*/ int r,
				/*@IntRange(from = 0x0, to = 0xFF)*/ int g, /*@IntRange(from = 0x0, to = 0xFF)*/ int b,
				/*@NonNull*/ float[] outHsl) {
			final float rf = r / 255f;
			final float gf = g / 255f;
			final float bf = b / 255f;

			final float max = Math.max(rf, Math.max(gf, bf));
			final float min = Math.min(rf, Math.min(gf, bf));
			final float deltaMaxMin = max - min;

			float h, s;
			float l = (max + min) / 2f;

			if (max == min) {
				// Monochromatic
				h = s = 0f;
			} else {
				if (max == rf) {
					h = ((gf - bf) / deltaMaxMin) % 6f;
				} else if (max == gf) {
					h = ((bf - rf) / deltaMaxMin) + 2f;
				} else {
					h = ((rf - gf) / deltaMaxMin) + 4f;
				}

				s = deltaMaxMin / (1f - Math.abs(2f * l - 1f));
			}

			h = (h * 60f) % 360f;
			if (h < 0) {
				h += 360f;
			}

			outHsl[0] = constrain(h, 0f, 360f);
			outHsl[1] = constrain(s, 0f, 1f);
			outHsl[2] = constrain(l, 0f, 1f);
		}

		private static float constrain(float amount, float low, float high) {
			//noinspection ManualMinMaxCalculation
			return amount < low ? low : (amount > high ? high : amount);
		}

		private static int constrain(int amount, int low, int high) {
			//noinspection ManualMinMaxCalculation
			return amount < low ? low : (amount > high ? high : amount);
		}

		private static double[] getTempDouble3Array() {
			double[] result = TEMP_ARRAY.get();
			if (result == null) {
				result = new double[3];
				TEMP_ARRAY.set(result);
			}
			return result;
		}

		// https://developer.android.com/reference/android/graphics/Color
		public static class AColor {

			/**
			 * </p>Parse the color string, and return the corresponding color-int.
			 * If the string cannot be parsed, throws an IllegalArgumentException
			 * exception. Supported formats are:</p>
			 *
			 * <ul>
			 * <li><code>#RRGGBB</code></li>
			 * <li><code>#AARRGGBB</code></li>
			 * </ul>
			 *
			 * <p>The following names are also accepted: <code>red</code>, <code>blue</code>,
			 * <code>green</code>, <code>black</code>, <code>white</code>, <code>gray</code>,
			 * <code>cyan</code>, <code>magenta</code>, <code>yellow</code>, <code>lightgray</code>,
			 * <code>darkgray</code>, <code>grey</code>, <code>lightgrey</code>, <code>darkgrey</code>,
			 * <code>aqua</code>, <code>fuchsia</code>, <code>lime</code>, <code>maroon</code>,
			 * <code>navy</code>, <code>olive</code>, <code>purple</code>, <code>silver</code>,
			 * and <code>teal</code>.</p>
			 */
			// @ColorInt
			public static int parseColor(/*@Size(min=1)*/ String colorString) {
				// if (colorString.charAt(0) == '#') {
				// Use a long to avoid rollovers on #ffXXXXXX
				// long color = Long.parseLong(colorString.substring(1), 16);
				long color = Long.parseLong(colorString, 16);
				if (colorString.length() == 6) {
					// Set the alpha value
					color |= 0x00000000ff000000;
				} else if (colorString.length() != 8) {
					throw new IllegalArgumentException("Unknown color: " + colorString);
				}
				return (int) color;
				// } else {
				// Integer color = sColorNameMap.get(colorString.toLowerCase(Locale.ROOT));
				// if (color != null) {
				// return color;
				// }
				// }
				// throw new IllegalArgumentException("Unknown color: " + colorString);
			}

			/**
			 * Return a color-int from alpha, red, green, blue components.
			 * These component values should be \([0..255]\), but there is no
			 * range check performed, so if they are out of range, the
			 * returned color is undefined.
			 *
			 * @param alpha Alpha component \([0..255]\) of the color
			 * @param red   Red component \([0..255]\) of the color
			 * @param green Green component \([0..255]\) of the color
			 * @param blue  Blue component \([0..255]\) of the color
			 */
			// @ColorInt
			public static int argb(
					/*@IntRange(from = 0, to = 255)*/ int alpha,
					/*@IntRange(from = 0, to = 255)*/ int red,
					/*@IntRange(from = 0, to = 255)*/ int green,
					/*@IntRange(from = 0, to = 255)*/ int blue) {
				return (alpha << 24) | (red << 16) | (green << 8) | blue;
			}

			/**
			 * Return a color-int from alpha, red, green, blue float components
			 * in the range \([0..1]\). If the components are out of range, the
			 * returned color is undefined.
			 *
			 * @param alpha Alpha component \([0..1]\) of the color
			 * @param red   Red component \([0..1]\) of the color
			 * @param green Green component \([0..1]\) of the color
			 * @param blue  Blue component \([0..1]\) of the color
			 */
			// @ColorInt
			public static int argb(float alpha, float red, float green, float blue) {
				return ((int) (alpha * 255.0f + 0.5f) << 24) |
						((int) (red * 255.0f + 0.5f) << 16) |
						((int) (green * 255.0f + 0.5f) << 8) |
						(int) (blue * 255.0f + 0.5f);
			}

			/**
			 * Return the alpha component of a color int. This is the same as saying
			 * color >>> 24
			 */
			// @IntRange(from = 0, to = 255)
			public static int alpha(int color) {
				return color >>> 24;
			}

			/**
			 * Return the red component of a color int. This is the same as saying
			 * (color >> 16) & 0xFF
			 */
			// @IntRange(from = 0, to = 255)
			public static int red(int color) {
				return (color >> 16) & 0xFF;
			}

			/**
			 * Return the green component of a color int. This is the same as saying
			 * (color >> 8) & 0xFF
			 */
			// @IntRange(from = 0, to = 255)
			public static int green(int color) {
				return (color >> 8) & 0xFF;
			}

			/**
			 * Return the blue component of a color int. This is the same as saying
			 * color & 0xFF
			 */
			// @IntRange(from = 0, to = 255)
			public static int blue(int color) {
				return color & 0xFF;
			}

			// /**
			// * Returns the red component encoded in the specified color long.
			// * The range of the returned value depends on the color space
			// * associated with the specified color. The color space can be
			// * queried by calling {link #colorSpace(long)}.
			// *
			// * @param color The color long whose red channel to extract
			// * @return A float value with a range defined by the specified color's
			// * color space
			// *
			// * see #colorSpace(long)
			// * see #green(long)
			// * see #blue(long)
			// * see #alpha(long)
			// */
			// public static float red(/*@ColorLong*/ long color) {
			// if ((color & 0x3fL) == 0L) return ((color >> 48) & 0xff) / 255.0f;
			// return Half.toFloat((short) ((color >> 48) & 0xffff));
			// }
			//
			// /**
			// * Returns the green component encoded in the specified color long.
			// * The range of the returned value depends on the color space
			// * associated with the specified color. The color space can be
			// * queried by calling {link #colorSpace(long)}.
			// *
			// * @param color The color long whose green channel to extract
			// * @return A float value with a range defined by the specified color's
			// * color space
			// *
			// * see #colorSpace(long)
			// * see #red(long)
			// * see #blue(long)
			// * see #alpha(long)
			// */
			// public static float green(/*@ColorLong*/ long color) {
			// if ((color & 0x3fL) == 0L) return ((color >> 40) & 0xff) / 255.0f;
			// return Half.toFloat((short) ((color >> 32) & 0xffff));
			// }
			//
			// /**
			// * Returns the blue component encoded in the specified color long.
			// * The range of the returned value depends on the color space
			// * associated with the specified color. The color space can be
			// * queried by calling {link #colorSpace(long)}.
			// *
			// * @param color The color long whose blue channel to extract
			// * @return A float value with a range defined by the specified color's
			// * color space
			// *
			// * see #colorSpace(long)
			// * see #red(long)
			// * see #green(long)
			// * see #alpha(long)
			// */
			// public static float blue(/*@ColorLong*/ long color) {
			// if ((color & 0x3fL) == 0L) return ((color >> 32) & 0xff) / 255.0f;
			// return Half.toFloat((short) ((color >> 16) & 0xffff));
			// }
		}
	}
}
