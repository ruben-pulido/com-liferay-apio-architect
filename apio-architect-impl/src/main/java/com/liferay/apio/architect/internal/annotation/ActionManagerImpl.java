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

import static com.liferay.apio.architect.internal.annotation.ActionKey.ANY_ROUTE;
import static com.liferay.apio.architect.internal.wiring.osgi.manager.cache.ManagerCache.INSTANCE;
import static com.liferay.apio.architect.operation.HTTPMethod.GET;

import com.liferay.apio.architect.credentials.Credentials;
import com.liferay.apio.architect.documentation.APIDescription;
import com.liferay.apio.architect.documentation.APITitle;
import com.liferay.apio.architect.function.throwable.ThrowableTriFunction;
import com.liferay.apio.architect.functional.Try;
import com.liferay.apio.architect.internal.documentation.Documentation;
import com.liferay.apio.architect.internal.entrypoint.EntryPoint;
import com.liferay.apio.architect.internal.operation.CreateOperation;
import com.liferay.apio.architect.internal.operation.DeleteOperation;
import com.liferay.apio.architect.internal.operation.RetrieveOperation;
import com.liferay.apio.architect.internal.operation.UpdateOperation;
import com.liferay.apio.architect.internal.url.ApplicationURL;
import com.liferay.apio.architect.internal.wiring.osgi.manager.documentation.contributor.CustomDocumentationManager;
import com.liferay.apio.architect.internal.wiring.osgi.manager.provider.ProviderManager;
import com.liferay.apio.architect.internal.wiring.osgi.manager.representable.RepresentableManager;
import com.liferay.apio.architect.internal.wiring.osgi.manager.uri.mapper.PathIdentifierMapperManager;
import com.liferay.apio.architect.operation.HTTPMethod;
import com.liferay.apio.architect.operation.Operation;
import com.liferay.apio.architect.uri.Path;

import io.vavr.control.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.NotFoundException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Provides methods to get the different actions provided by the different
 * routers.
 *
 * @author Javier Gamarra
 * @review
 */
@Component(service = ActionManager.class)
public class ActionManagerImpl implements ActionManager {

	public ActionManagerImpl(
		PathIdentifierMapperManager pathIdentifierMapperManager) {

		this._pathIdentifierMapperManager = pathIdentifierMapperManager;
	}

	@Override
	public void add(
		ActionKey actionKey,
		ThrowableTriFunction<Object, ?, List<Object>, ?> throwableTriFunction,
		Class... providers) {

		getActions().put(actionKey, throwableTriFunction);

		_providers.put(actionKey, providers);
	}

	public void addCollectionGetter(
		String name,
		ThrowableTriFunction<Object, ?, List<Object>, ?> throwableTriFunction,
		Class... providers) {

		ActionKey actionKey = new ActionKey(GET.name(), name);

		add(actionKey, throwableTriFunction, providers);
	}

	public void addItemGetter(
		String name,
		ThrowableTriFunction<Object, ?, List<Object>, ?> throwableTriFunction,
		Class... providers) {

		ActionKey actionKey = new ActionKey(GET.name(), name, ANY_ROUTE);

		add(actionKey, throwableTriFunction, providers);
	}

	public void addItemRemover(
		String name,
		ThrowableTriFunction<Object, ?, List<Object>, ?> throwableTriFunction,
		Class... providers) {

		ActionKey actionKey = new ActionKey(
			HTTPMethod.DELETE.name(), name, ANY_ROUTE);

		add(actionKey, throwableTriFunction, providers);
	}

	public void addNestedGetter(
		String name, String nestedName,
		ThrowableTriFunction<Object, ?, List<Object>, ?> throwableTriFunction,
		Class... providers) {

		ActionKey actionKey = new ActionKey(
			GET.name(), name, ANY_ROUTE, nestedName);

		add(actionKey, throwableTriFunction, providers);
	}

	@Override
	public Either<Action.Error, Action> getAction(
		String method, String param1) {

		ActionKey actionKey = new ActionKey(method, param1);

		return Either.right(_getAction(actionKey, null));
	}

	@Override
	public Either<Action.Error, Action> getAction(
		String method, String param1, String param2) {

		ActionKey actionKey = new ActionKey(method, param1, param2);
		Object id = _getId(param1, param2);

		return Either.right(_getAction(actionKey, id));
	}

	@Override
	public Either<Action.Error, Action> getAction(
		String method, String param1, String param2, String param3) {

		ActionKey actionKey = new ActionKey(method, param1, param2, param3);
		Object id = _getId(param1, param2);

		return Either.right(_getAction(actionKey, id));
	}

	@Override
	public Either<Action.Error, Action> getAction(
		String method, String param1, String param2, String param3,
		String param4) {

		ActionKey actionKey = new ActionKey(
			method, param1, param2, param3, param4);
		Object id = _getId(param1, param2);

		return Either.right(_getAction(actionKey, id));
	}

	public Map<ActionKey, ThrowableTriFunction<Object, ?, List<Object>, ?>>
		getActions() {

		return _actions;
	}

	@Override
	public List<Operation> getActions(
		ActionKey actionKey, Credentials credentials) {

		return Stream.of(
			HTTPMethod.values()
		).map(
			httpMethod -> actionKey.getActionKeyWithHttpMethodName(
				httpMethod.name())
		).filter(
			this::_getActionKeyPredicate
		).map(
			this::_getOperation
		).collect(
			Collectors.toList()
		);
	}

	@Override
	public Documentation getDocumentation(
		Supplier<Optional<APITitle>> apiTitleOptionalSupplier,
		Supplier<Optional<APIDescription>> apiDescriptionOptionalSupplier,
		Supplier<Optional<ApplicationURL>> applicationUrlOptionalSupplier) {

		return new Documentation(
			apiTitleOptionalSupplier, apiDescriptionOptionalSupplier,
			applicationUrlOptionalSupplier,
			() -> _representableManager.getRepresentors(), () -> this,
			() -> _customDocumentationManager.getCustomDocumentation());
	}

	@Override
	public EntryPoint getEntryPoint() {
		return () -> {
			List<String> list = new ArrayList<>();

			list.addAll(INSTANCE.getRootResourceNamesSdk());
			list.addAll(INSTANCE.getRootResourceNames());

			return list;
		};
	}

	@Reference
	protected PathIdentifierMapperManager _pathIdentifierMapperManager;

	@Reference
	protected ProviderManager providerManager;

	private Action _getAction(ActionKey actionKey, Object id) {
		return httpServletRequest -> Try.fromFallible(
			() -> _getActionThrowableTriFunction(actionKey)
		).map(
			action -> action.apply(
				id, null,
				(List<Object>)_getProviders(httpServletRequest, actionKey))
		).orElseThrow(
			NotFoundException::new
		);
	}

	private boolean _getActionKeyPredicate(ActionKey actionKey) {
		if (actionKey.isCollection()) {
			return _actions.containsKey(actionKey);
		}

		if (_getActionThrowableTriFunction(actionKey) != null) {
			return true;
		}

		return false;
	}

	private ThrowableTriFunction
		<Object, ?, List<Object>, ?> _getActionThrowableTriFunction(
			ActionKey actionKey) {

		if (getActions().containsKey(actionKey)) {
			return getActions().get(actionKey);
		}

		return getActions().get(actionKey.getGenericActionKey());
	}

	private Object _getId(String param1, String param2) {
		try {
			return _pathIdentifierMapperManager.mapToIdentifierOrFail(
				new Path(param1, param2));
		}
		catch (Error e) {
			return null;
		}
	}

	private List<Object> _getListThrowableFunction(
		Class<Object>[] value, HttpServletRequest httpServletRequest) {

		return Stream.of(
			value
		).map(
			provider -> providerManager.provideMandatory(
				httpServletRequest, provider)
		).collect(
			Collectors.toList()
		);
	}

	private Operation _getOperation(ActionKey actionKey) {
		String param1 = actionKey.getParam1();

		String resourceName = param1 + (actionKey.getParam3() == null ?
			"" : "/" + actionKey.getParam3());

		String uri = _pathIdentifierMapperManager.mapToPath(
			param1, actionKey.getParam2()).map(
			path -> path.asURI() + "/" + actionKey.getParam3()
		).orElse(
			resourceName
		);

		if ("GET".equals(actionKey.getHttpMethodName()) &&
			actionKey.isCollection()) {

			return new RetrieveOperation(resourceName, true, uri, null);
		}
		else if ("POST".equals(actionKey.getHttpMethodName())) {
			return new CreateOperation(null, resourceName, uri, null);
		}
		else if ("DELETE".equals(actionKey.getHttpMethodName())) {
			return new DeleteOperation(resourceName, uri, null);
		}
		else if ("PUT".equals(actionKey.getHttpMethodName())) {
			return new UpdateOperation(null, resourceName, uri, null);
		}

		return new RetrieveOperation(resourceName, false, uri, null);
	}

	private List _getProviders(
		HttpServletRequest httpServletRequest, ActionKey actionKey) {

		return Try.fromFallible(
			() -> _getProvidersByParam(actionKey)
		).map(
			value -> _getListThrowableFunction(value, httpServletRequest)
		).orElseGet(
			ArrayList::new
		);
	}

	private Class<Object>[] _getProvidersByParam(ActionKey actionKey) {
		if (_providers.containsKey(actionKey)) {
			return _providers.get(actionKey);
		}

		return _providers.get(actionKey.getGenericActionKey());
	}

	private final Map
		<ActionKey, ThrowableTriFunction<Object, ?, List<Object>, ?>>
			_actions = new HashMap<>();

	@Reference
	private CustomDocumentationManager _customDocumentationManager;

	private final Map<ActionKey, Class[]> _providers = new HashMap<>();

	@Reference
	private RepresentableManager _representableManager;

}