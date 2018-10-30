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

import com.liferay.apio.architect.form.Form;

import java.util.function.Supplier;

/**
 * @author Alejandro Hernández
 */
public interface ActionBodyType {

	public static final ClassBody NOTHING = ClassBody.of(Void.class, false);

	public boolean receivesList();

	public interface ClassBody extends ActionBodyType, Supplier<Class<?>> {

		public static ClassBody of(Class<?> bodyClass, boolean asList) {
			return new ClassBody() {

				@Override
				public Class<?> get() {
					return bodyClass;
				}

				@Override
				public boolean receivesList() {
					return asList;
				}

			};
		}

	}

	public interface FormBody extends ActionBodyType, Supplier<Form<?>> {

		public static FormBody of(Form<?> form, boolean asList) {
			return new FormBody() {

				@Override
				public Form<?> get() {
					return form;
				}

				@Override
				public boolean receivesList() {
					return asList;
				}

			};
		}

	}

}