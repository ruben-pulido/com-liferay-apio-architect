/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.apio.architect.internal.wiring.osgi.manager.router;

/**
 * @author Alejandro Hernández
 */
public interface Resource {

	public static Item item(String name) {
		return () -> name;
	}

	public static Nested nested(String parentName, String nestedName) {
		return new Nested() {

			@Override
			public String getName() {
				return nestedName;
			}

			@Override
			public String getParentName() {
				return parentName;
			}

		};
	}

	public static Paged paged(String name) {
		return () -> name;
	}

	public String getName();

	public interface Item extends Resource {
	}

	public interface Nested extends Resource {

		public String getParentName();

	}

	public interface Paged extends Resource {
	}

}