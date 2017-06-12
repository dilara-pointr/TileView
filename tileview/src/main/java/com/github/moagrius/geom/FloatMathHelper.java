package com.github.moagrius.geom;

/**
 * @author Mike Dunn, 6/11/17.
 */

public class FloatMathHelper {
  public static int scale(int base, float multiplier) {
    return (int) ((base * multiplier) + 0.5);
  }

  public static int unscale(int base, float multiplier) {
    return (int) ((base / multiplier) + 0.5);
  }
}
