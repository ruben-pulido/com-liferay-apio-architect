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

import static com.liferay.apio.architect.internal.wiring.osgi.manager.router.ActionBodyType.NOTHING;

import static java.util.Arrays.asList;

import com.liferay.apio.architect.internal.wiring.osgi.manager.router.ActionBodyType.ClassBody;
import com.liferay.apio.architect.internal.wiring.osgi.manager.router.ActionSemantics;
import com.liferay.apio.architect.internal.wiring.osgi.manager.router.Resource;

import io.vavr.Function1;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author Alejandro Hernández
 */
public class Predicates {

	public static final Predicate<ActionSemantics> isActionByGET =
		isActionByMethod("GET");
	public static final Predicate<ActionSemantics> isActionForPagedResource =
		actionSemantics -> actionSemantics.resource instanceof Resource.Paged;
	public static final Predicate<ActionSemantics> isRetrieveAction =
		isActionByGET.and(isActionNamed("retrieve"));
	public static final Predicate<ActionSemantics> receivesClassBody =
		actionSemantics -> actionSemantics.actionBodyType instanceof ClassBody;
	public static final Predicate<ActionSemantics> receivesNothing = receives(
		NOTHING);

	public static Predicate<ActionSemantics> isActionByMethod(
		String httpMethod) {

		return actionSemantics -> httpMethod.equals(actionSemantics.method);
	}

	public static Predicate<ActionSemantics> isActionForResource(
		Resource.Paged pagedResource) {

		return isActionForPagedResource.and(
			isActionForResourceNamed(pagedResource.getName()));
	}

	public static Predicate<ActionSemantics> isActionForResourceNamed(
		String name) {

		return actionSemantics -> name.equals(
			actionSemantics.resource.getName());
	}

	public static Predicate<ActionSemantics> isActionNamed(String name) {
		return actionSemantics -> name.equals(actionSemantics.name);
	}

	public static Predicate<ActionSemantics> receives(ClassBody classBody) {
		return receivesClassBody.and(
			_toClassBody.andThen(
				ClassBody::get
			).andThen(
				classBody.get()::equals
			)::apply);
	}

	public static Predicate<ActionSemantics> returnsAnyOf(Class<?>... classes) {
		return actionSemantics -> {
			List<Class<?>> list = asList(classes);

			return list.contains(actionSemantics.returnClass);
		};
	}

	private static final Function1<ActionSemantics, ClassBody> _toClassBody =
		actionSemantics -> (ClassBody)actionSemantics.actionBodyType;

}