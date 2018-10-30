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

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import com.liferay.apio.architect.form.Form;
import com.liferay.apio.architect.internal.wiring.osgi.manager.router.ActionBodyType.ClassBody;
import com.liferay.apio.architect.internal.wiring.osgi.manager.router.ActionBodyType.FormBody;
import com.liferay.apio.architect.operation.HTTPMethod;

import io.vavr.CheckedFunction1;

import java.util.List;

/**
 * @author Alejandro Hernández
 */
public class ActionSemantics {

	public static NameStep ofResource(Resource resource) {
		return name -> method -> classes -> returnClass -> actionBodyType ->
			executeFunction -> new ActionSemantics(
				resource, name, method, classes, returnClass, actionBodyType,
				executeFunction);
	}

	public final ActionBodyType actionBodyType;
	public final List<Class<?>> classes;
	public final CheckedFunction1<List<Object>, Object> executeFunction;
	public final String method;
	public final String name;
	public final Resource resource;
	public final Class<?> returnClass;

	@FunctionalInterface
	public interface ExecuteStep {

		public ActionSemantics execute(
			CheckedFunction1<List<Object>, Object> executeFunction);

	}

	@FunctionalInterface
	public interface MethodStep {

		public default ProvideMethodStep method(HTTPMethod httpMethod) {
			return method(httpMethod.name());
		}

		public ProvideMethodStep method(String method);

	}

	@FunctionalInterface
	public interface NameStep {

		public MethodStep name(String name);

	}

	@FunctionalInterface
	public interface ProvideMethodStep {

		public ReturnStep provide(Class<?>... classes);

		public default ReturnStep provideNothing() {
			return provide();
		}

	}

	public interface ReceiveStep {

		public ExecuteStep receives(ActionBodyType actionBodyType);

		public default ExecuteStep receivesListOf(Class<?> receiveClass) {
			return receives(ClassBody.of(receiveClass, true));
		}

		public default ExecuteStep receivesListOf(Form<?> form) {
			return receives(FormBody.of(form, true));
		}

		public default ExecuteStep receivesNothing() {
			return receives(ActionBodyType.NOTHING);
		}

		public default ExecuteStep receivesSingle(Class<?> receiveClass) {
			return receives(ClassBody.of(receiveClass, false));
		}

		public default ExecuteStep receivesSingle(Form<?> form) {
			return receives(FormBody.of(form, false));
		}

	}

	@FunctionalInterface
	public interface ReturnStep {

		public ReceiveStep returns(Class<?> returnClass);

		public default ReceiveStep returnsNothing() {
			return returns(Void.class);
		}

	}

	private ActionSemantics(
		Resource resource, String name, String method, Class<?>[] classes,
		Class<?> returnClass, ActionBodyType actionBodyType,
		CheckedFunction1<List<Object>, Object> executeFunction) {

		this.resource = resource;
		this.name = name;
		this.method = method;
		this.classes = unmodifiableList(asList(classes));
		this.returnClass = returnClass;
		this.actionBodyType = actionBodyType;
		this.executeFunction = executeFunction;
	}

}