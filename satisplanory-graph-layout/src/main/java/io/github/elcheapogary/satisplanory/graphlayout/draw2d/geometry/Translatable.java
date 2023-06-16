/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package io.github.elcheapogary.satisplanory.graphlayout.draw2d.geometry;

/**
 * A translatable object can be translated (or moved) vertically and/or
 * horizontally.
 */
public interface Translatable {
	/**
	 * Translates this object horizontally by <code>point.x</code> and vertically by
	 * <code>point.y</code>.
	 * 
	 * @param point Point which provides translation information
	 * @since 3.13
	 */
	default void performTranslate(Point point) {
		performTranslate(point.x, point.y);
	}

	/**
	 * Translates this object horizontally by <code>dimension.width</code> and
	 * vertically by <code>dimension.height</code>.
	 * 
	 * @param dimension Dimension which provides translation information
	 * @since 3.13
	 */
	default void performTranslate(Dimension dimension) {
		performTranslate(dimension.width, dimension.height);
	}

	/**
	 * Translates this object horizontally by <code>insets.left</code> and
	 * vertically by <code>insets.top</code>.
	 * 
	 * @param insets Insets which provides translation information
	 * @since 3.13
	 */
	default void performTranslate(Insets insets) {
		performTranslate(insets.left, insets.top);
	}

	/**
	 * Translates this object horizontally by <code>dx</code> and vertically by
	 * <code>dy</code>.
	 * 
	 * @param dx The amount to translate horizontally
	 * @param dy The amount to translate vertically
	 * @since 2.0
	 */
	void performTranslate(int dx, int dy);

	/**
	 * Scales this object by the scale factor.
	 * 
	 * @param factor The scale factor
	 * @since 2.0
	 */
	void performScale(double factor);

}
