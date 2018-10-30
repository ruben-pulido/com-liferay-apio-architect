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

package com.liferay.apio.architect.internal.annotation;

import com.liferay.apio.architect.internal.alias.ProvideFunction;
import com.liferay.apio.architect.internal.wiring.osgi.manager.router.ActionSemantics;

import io.vavr.control.Try;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Alejandro Hernández
 */
public class ActionManagerUtil {

	public static Function<ActionSemantics, Action> toAction(
		ProvideFunction provideFunction) {

		return actionSemantics -> request -> Try.of(
			actionSemantics.classes::stream
		).<List<Object>>map(
			stream -> stream.map(
				provideFunction.apply(request)
			).collect(
				Collectors.toList()
			)
		).mapTry(
			actionSemantics.executeFunction
		);
	}

}