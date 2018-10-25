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

package com.liferay.apio.architect.internal.routes;

import static com.liferay.apio.architect.internal.annotation.ActionKey.ANY_ROUTE;
import static com.liferay.apio.architect.internal.operation.util.OperationUtil.toOperations;
import static com.liferay.apio.architect.internal.routes.RoutesTestUtil.FORM_BUILDER_FUNCTION;
import static com.liferay.apio.architect.internal.routes.RoutesTestUtil.IDENTIFIER_FUNCTION;
import static com.liferay.apio.architect.internal.routes.RoutesTestUtil.PAGINATION;
import static com.liferay.apio.architect.internal.routes.RoutesTestUtil.REQUEST_PROVIDE_FUNCTION;
import static com.liferay.apio.architect.internal.routes.RoutesTestUtil.hasNestedAddingPermissionFunction;
import static com.liferay.apio.architect.internal.routes.RoutesTestUtil.keyValueFrom;
import static com.liferay.apio.architect.internal.unsafe.Unsafe.unsafeCast;
import static com.liferay.apio.architect.operation.HTTPMethod.GET;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;

import static java.util.Arrays.asList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import com.liferay.apio.architect.alias.routes.NestedBatchCreateItemFunction;
import com.liferay.apio.architect.alias.routes.NestedCreateItemFunction;
import com.liferay.apio.architect.batch.BatchResult;
import com.liferay.apio.architect.form.Body;
import com.liferay.apio.architect.form.Form;
import com.liferay.apio.architect.functional.Try;
import com.liferay.apio.architect.internal.annotation.Action;
import com.liferay.apio.architect.internal.annotation.ActionKey;
import com.liferay.apio.architect.internal.operation.RetrieveOperation;
import com.liferay.apio.architect.internal.routes.NestedCollectionRoutesImpl.BuilderImpl;
import com.liferay.apio.architect.operation.HTTPMethod;
import com.liferay.apio.architect.operation.Operation;
import com.liferay.apio.architect.pagination.PageItems;
import com.liferay.apio.architect.pagination.Pagination;
import com.liferay.apio.architect.routes.NestedCollectionRoutes;
import com.liferay.apio.architect.routes.NestedCollectionRoutes.Builder;
import com.liferay.apio.architect.single.model.SingleModel;

import io.vavr.control.Either;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.NotFoundException;

import org.junit.Test;

/**
 * @author Alejandro Hernández
 */
public class NestedCollectionRoutesImplTest extends BaseRoutesTest {

	@Test(expected = NotFoundException.class)
	public void testEmptyBuilderBuildsEmptyRoutes() {
		Builder<String, Long, Long> builder = new BuilderImpl<>(
			"name", "nested", REQUEST_PROVIDE_FUNCTION,
			__ -> {
			},
			__ -> null, IDENTIFIER_FUNCTION, actionManager);

		NestedCollectionRoutes<String, Long, Long> nestedCollectionRoutes =
			builder.build();

		Optional<NestedCreateItemFunction<String, Long>> optional1 =
			nestedCollectionRoutes.getNestedCreateItemFunctionOptional();

		assertThat(optional1, is(emptyOptional()));

		Either<Action.Error, Action> actionEither = actionManager.getAction(
			GET.name(), asList("name", ANY_ROUTE, "nested"));

		assertThat(actionEither.isRight(), is(true));

		Action action = actionEither.get();

		Object result = action.apply(null);

		assertThat(result, is(nullValue()));
	}

	@Test
	public void testFiveParameterBatchCreatorCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long, Long> builder = new BuilderImpl<>(
			"name", "nested", REQUEST_PROVIDE_FUNCTION, neededProviders::add,
			__ -> null, IDENTIFIER_FUNCTION, actionManager);

		NestedCollectionRoutes<String, Long, Long> nestedCollectionRoutes =
			builder.addCreator(
				this::_testAndReturnFourParameterCreatorRoute,
				this::_testAndReturnFourParameterBatchCreatorRoute,
				String.class, Long.class, Boolean.class, Integer.class,
				hasNestedAddingPermissionFunction(), FORM_BUILDER_FUNCTION
			).build();

		assertThat(
			neededProviders,
			contains(
				Boolean.class.getName(), Integer.class.getName(),
				Long.class.getName(), String.class.getName()));

		_testNestedCollectionRoutesCreator(nestedCollectionRoutes);
		_testNestedCollectionRoutesBatchCreator(nestedCollectionRoutes);
	}

	@Test
	public void testFiveParameterBuilderMethodsCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long, Long> builder = new BuilderImpl<>(
			"name", "nested", REQUEST_PROVIDE_FUNCTION, neededProviders::add,
			__ -> null, IDENTIFIER_FUNCTION, actionManager);

		NestedCollectionRoutes<String, Long, Long> nestedCollectionRoutes =
			builder.addCreator(
				this::_testAndReturnFourParameterCreatorRoute, String.class,
				Long.class, Boolean.class, Integer.class,
				hasNestedAddingPermissionFunction(), FORM_BUILDER_FUNCTION
			).addGetter(
				this::_testAndReturnFourParameterGetterRoute, String.class,
				Long.class, Boolean.class, Integer.class
			).build();

		assertThat(
			neededProviders,
			contains(
				Boolean.class.getName(), Integer.class.getName(),
				Long.class.getName(), String.class.getName()));

		_testNestedCollectionRoutes(nestedCollectionRoutes);
	}

	@Test
	public void testFourParameterBatchCreatorCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long, Long> builder = new BuilderImpl<>(
			"name", "nested", REQUEST_PROVIDE_FUNCTION, neededProviders::add,
			__ -> null, IDENTIFIER_FUNCTION, actionManager);

		NestedCollectionRoutes<String, Long, Long> nestedCollectionRoutes =
			builder.addCreator(
				this::_testAndReturnThreeParameterCreatorRoute,
				this::_testAndReturnThreeParameterBatchCreatorRoute,
				String.class, Long.class, Boolean.class,
				hasNestedAddingPermissionFunction(), FORM_BUILDER_FUNCTION
			).build();

		assertThat(
			neededProviders,
			contains(
				Boolean.class.getName(), Long.class.getName(),
				String.class.getName()));

		_testNestedCollectionRoutesCreator(nestedCollectionRoutes);
		_testNestedCollectionRoutesBatchCreator(nestedCollectionRoutes);
	}

	@Test
	public void testFourParameterBuilderMethodsCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long, Long> builder = new BuilderImpl<>(
			"name", "nested", REQUEST_PROVIDE_FUNCTION, neededProviders::add,
			__ -> null, IDENTIFIER_FUNCTION, actionManager);

		NestedCollectionRoutes<String, Long, Long> nestedCollectionRoutes =
			builder.addCreator(
				this::_testAndReturnThreeParameterCreatorRoute, String.class,
				Long.class, Boolean.class, hasNestedAddingPermissionFunction(),
				FORM_BUILDER_FUNCTION
			).addGetter(
				this::_testAndReturnThreeParameterGetterRoute, String.class,
				Long.class, Boolean.class
			).build();

		assertThat(
			neededProviders,
			contains(
				Boolean.class.getName(), Long.class.getName(),
				String.class.getName()));

		_testNestedCollectionRoutes(nestedCollectionRoutes);
	}

	@Test
	public void testOneParameterBatchCreatorCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long, Long> builder = new BuilderImpl<>(
			"name", "nested", REQUEST_PROVIDE_FUNCTION, neededProviders::add,
			__ -> null, IDENTIFIER_FUNCTION, actionManager);

		NestedCollectionRoutes<String, Long, Long> nestedCollectionRoutes =
			builder.addCreator(
				this::_testAndReturnNoParameterCreatorRoute,
				this::_testAndReturnNoParameterBatchCreatorRoute,
				hasNestedAddingPermissionFunction(), FORM_BUILDER_FUNCTION
			).build();

		assertThat(neededProviders.size(), is(0));

		_testNestedCollectionRoutesCreator(nestedCollectionRoutes);
		_testNestedCollectionRoutesBatchCreator(nestedCollectionRoutes);
	}

	@Test
	public void testOneParameterBuilderMethodsCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long, Long> builder = new BuilderImpl<>(
			"name", "nested", REQUEST_PROVIDE_FUNCTION, neededProviders::add,
			__ -> null, IDENTIFIER_FUNCTION, actionManager);

		NestedCollectionRoutes<String, Long, Long> nestedCollectionRoutes =
			builder.addCreator(
				this::_testAndReturnNoParameterCreatorRoute,
				hasNestedAddingPermissionFunction(), FORM_BUILDER_FUNCTION
			).addGetter(
				this::_testAndReturnNoParameterGetterRoute
			).build();

		assertThat(neededProviders.size(), is(0));

		_testNestedCollectionRoutes(nestedCollectionRoutes);
	}

	@Test
	public void testThreeParameterBatchCreatorCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long, Long> builder = new BuilderImpl<>(
			"name", "nested", REQUEST_PROVIDE_FUNCTION, neededProviders::add,
			__ -> null, IDENTIFIER_FUNCTION, actionManager);

		NestedCollectionRoutes<String, Long, Long> nestedCollectionRoutes =
			builder.addCreator(
				this::_testAndReturnTwoParameterCreatorRoute,
				this::_testAndReturnTwoParameterBatchCreatorRoute, String.class,
				Long.class, hasNestedAddingPermissionFunction(),
				FORM_BUILDER_FUNCTION
			).build();

		assertThat(
			neededProviders,
			contains(Long.class.getName(), String.class.getName()));

		_testNestedCollectionRoutesCreator(nestedCollectionRoutes);
		_testNestedCollectionRoutesBatchCreator(nestedCollectionRoutes);
	}

	@Test
	public void testThreeParameterBuilderMethodsCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long, Long> builder = new BuilderImpl<>(
			"name", "nested", REQUEST_PROVIDE_FUNCTION, neededProviders::add,
			__ -> null, IDENTIFIER_FUNCTION, actionManager);

		NestedCollectionRoutes<String, Long, Long> nestedCollectionRoutes =
			builder.addCreator(
				this::_testAndReturnTwoParameterCreatorRoute, String.class,
				Long.class, hasNestedAddingPermissionFunction(),
				FORM_BUILDER_FUNCTION
			).addGetter(
				this::_testAndReturnTwoParameterGetterRoute, String.class,
				Long.class
			).build();

		assertThat(
			neededProviders,
			contains(Long.class.getName(), String.class.getName()));

		_testNestedCollectionRoutes(nestedCollectionRoutes);
	}

	@Test
	public void testTwoParameterBatchCreatorCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long, Long> builder = new BuilderImpl<>(
			"name", "nested", REQUEST_PROVIDE_FUNCTION, neededProviders::add,
			__ -> null, IDENTIFIER_FUNCTION, actionManager);

		NestedCollectionRoutes<String, Long, Long> nestedCollectionRoutes =
			builder.addCreator(
				this::_testAndReturnOneParameterCreatorRoute,
				this::_testAndReturnOneParameterBatchCreatorRoute, String.class,
				hasNestedAddingPermissionFunction(), FORM_BUILDER_FUNCTION
			).build();

		assertThat(neededProviders, contains(String.class.getName()));

		_testNestedCollectionRoutesCreator(nestedCollectionRoutes);
		_testNestedCollectionRoutesBatchCreator(nestedCollectionRoutes);
	}

	@Test
	public void testTwoParameterBuilderMethodsCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long, Long> builder = new BuilderImpl<>(
			"name", "nested", REQUEST_PROVIDE_FUNCTION, neededProviders::add,
			__ -> null, IDENTIFIER_FUNCTION, actionManager);

		NestedCollectionRoutes<String, Long, Long> nestedCollectionRoutes =
			builder.addCreator(
				this::_testAndReturnOneParameterCreatorRoute, String.class,
				hasNestedAddingPermissionFunction(), FORM_BUILDER_FUNCTION
			).addGetter(
				this::_testAndReturnOneParameterGetterRoute, String.class
			).build();

		assertThat(neededProviders, contains(String.class.getName()));

		_testNestedCollectionRoutes(nestedCollectionRoutes);
	}

	private List<Long> _testAndReturnFourParameterBatchCreatorRoute(
		Long identifier, List<Map<String, Object>> bodies, String string,
		Long aLong, Boolean aBoolean, Integer integer) {

		assertThat(integer, is(2017));

		return _testAndReturnThreeParameterBatchCreatorRoute(
			identifier, bodies, string, aLong, aBoolean);
	}

	private String _testAndReturnFourParameterCreatorRoute(
		Long identifier, Map<String, Object> body, String string, Long aLong,
		Boolean aBoolean, Integer integer) {

		assertThat(integer, is(2017));

		return _testAndReturnThreeParameterCreatorRoute(
			identifier, body, string, aLong, aBoolean);
	}

	private PageItems<String> _testAndReturnFourParameterGetterRoute(
		Pagination pagination, Long identifier, String string, Long aLong,
		Boolean aBoolean, Integer integer) {

		assertThat(integer, is(2017));

		return _testAndReturnThreeParameterGetterRoute(
			pagination, identifier, string, aLong, aBoolean);
	}

	private List<Long> _testAndReturnNoParameterBatchCreatorRoute(
		Long identifier, List<Map<String, Object>> bodies) {

		assertThat(identifier, is(42L));

		assertThat(bodies.get(0).get("key"), is(keyValueFrom(_singleBody)));
		assertThat(bodies.get(1).get("key"), is(keyValueFrom(_singleBody)));

		return asList(42L, 42L);
	}

	private String _testAndReturnNoParameterCreatorRoute(
		Long identifier, Map<String, Object> body) {

		assertThat(identifier, is(42L));

		assertThat(body.get("key"), is(keyValueFrom(_singleBody)));

		return "Apio";
	}

	private PageItems<String> _testAndReturnNoParameterGetterRoute(
		Pagination pagination, Long identifier) {

		assertThat(identifier, is(42L));
		assertThat(pagination, is(PAGINATION));

		return new PageItems<>(Collections.singletonList("Apio"), 1);
	}

	private List<Long> _testAndReturnOneParameterBatchCreatorRoute(
		Long identifier, List<Map<String, Object>> bodies, String string) {

		assertThat(string, is("Apio"));

		return _testAndReturnNoParameterBatchCreatorRoute(identifier, bodies);
	}

	private String _testAndReturnOneParameterCreatorRoute(
		Long identifier, Map<String, Object> body, String string) {

		assertThat(string, is("Apio"));

		return _testAndReturnNoParameterCreatorRoute(identifier, body);
	}

	private PageItems<String> _testAndReturnOneParameterGetterRoute(
		Pagination pagination, Long identifier, String string) {

		assertThat(string, is("Apio"));

		return _testAndReturnNoParameterGetterRoute(pagination, identifier);
	}

	private List<Long> _testAndReturnThreeParameterBatchCreatorRoute(
		Long identifier, List<Map<String, Object>> bodies, String string,
		Long aLong, Boolean aBoolean) {

		assertThat(aBoolean, is(true));

		return _testAndReturnTwoParameterBatchCreatorRoute(
			identifier, bodies, string, aLong);
	}

	private String _testAndReturnThreeParameterCreatorRoute(
		Long identifier, Map<String, Object> body, String string, Long aLong,
		Boolean aBoolean) {

		assertThat(aBoolean, is(true));

		return _testAndReturnTwoParameterCreatorRoute(
			identifier, body, string, aLong);
	}

	private PageItems<String> _testAndReturnThreeParameterGetterRoute(
		Pagination pagination, Long identifier, String string, Long aLong,
		Boolean aBoolean) {

		assertThat(aBoolean, is(true));

		return _testAndReturnTwoParameterGetterRoute(
			pagination, identifier, string, aLong);
	}

	private List<Long> _testAndReturnTwoParameterBatchCreatorRoute(
		Long identifier, List<Map<String, Object>> bodies, String string,
		Long aLong) {

		assertThat(aLong, is(42L));

		return _testAndReturnOneParameterBatchCreatorRoute(
			identifier, bodies, string);
	}

	private String _testAndReturnTwoParameterCreatorRoute(
		Long identifier, Map<String, Object> body, String string, Long aLong) {

		assertThat(aLong, is(42L));

		return _testAndReturnOneParameterCreatorRoute(identifier, body, string);
	}

	private PageItems<String> _testAndReturnTwoParameterGetterRoute(
		Pagination pagination, Long identifier, String string, Long aLong) {

		assertThat(aLong, is(42L));

		return _testAndReturnOneParameterGetterRoute(
			pagination, identifier, string);
	}

	private void _testNestedCollectionRoutes(
		NestedCollectionRoutes<String, Long, Long> nestedCollectionRoutes) {

		_testNestedCollectionRoutesCreator(nestedCollectionRoutes);

		_testNestedCollectionRoutesBatchCreator(nestedCollectionRoutes);

		_testNestedCollectionRoutesGetter();
	}

	private void _testNestedCollectionRoutesBatchCreator(
		NestedCollectionRoutes<String, Long, Long> nestedCollectionRoutes) {

		Optional<Form> formOptional = nestedCollectionRoutes.getFormOptional();

		if (!formOptional.isPresent()) {
			throw new AssertionError("Batch Nested Create Form not present");
		}

		Form form = formOptional.get();

		assertThat(form.getId(), is("c/name/nested"));

		List<Map> list = unsafeCast(form.getList(_batchBody));

		assertThat(list, hasSize(2));

		assertThat(list.get(0).get("key"), is(keyValueFrom(_singleBody)));
		assertThat(list.get(1).get("key"), is(keyValueFrom(_singleBody)));

		Optional<NestedBatchCreateItemFunction<Long, Long>>
			batchCreateItemFunctionOptional =
				nestedCollectionRoutes.
					getNestedBatchCreateItemFunctionOptional();

		if (!batchCreateItemFunctionOptional.isPresent()) {
			throw new AssertionError(
				"NestedBatchCreateItemFunction not present");
		}

		NestedBatchCreateItemFunction<Long, Long>
			nestedBatchCreateItemFunction =
				batchCreateItemFunctionOptional.get();

		BatchResult<Long> batchResult = nestedBatchCreateItemFunction.apply(
			null
		).apply(
			_batchBody
		).apply(
			42L
		).getUnchecked();

		assertThat(batchResult.resourceName, is("nested"));

		List<Long> identifiers = batchResult.getIdentifiers();

		assertThat(identifiers, hasSize(2));
		assertThat(identifiers.get(0), is(42L));
		assertThat(identifiers.get(1), is(42L));
	}

	private void _testNestedCollectionRoutesCreator(
		NestedCollectionRoutes<String, Long, Long> nestedCollectionRoutes) {

		Optional<Form> formOptional = nestedCollectionRoutes.getFormOptional();

		if (!formOptional.isPresent()) {
			throw new AssertionError("Create Form not present");
		}

		Form form = formOptional.get();

		assertThat(form.getId(), is("c/name/nested"));

		Map map = (Map)form.get(_singleBody);

		assertThat(map.get("key"), is(keyValueFrom(_singleBody)));

		Optional<NestedCreateItemFunction<String, Long>>
			nestedCreateItemFunctionOptional =
				nestedCollectionRoutes.getNestedCreateItemFunctionOptional();

		if (!nestedCreateItemFunctionOptional.isPresent()) {
			throw new AssertionError("NestedCreateItemFunction not present");
		}

		NestedCreateItemFunction<String, Long> nestedCreateItemFunction =
			nestedCreateItemFunctionOptional.get();

		SingleModel<String> singleModel = nestedCreateItemFunction.apply(
			null
		).apply(
			42L
		).andThen(
			Try::getUnchecked
		).apply(
			_singleBody
		);

		assertThat(singleModel.getResourceName(), is("nested"));
		assertThat(singleModel.getModel(), is("Apio"));
	}

	private void _testNestedCollectionRoutesGetter() {
		Either<Action.Error, Action> actionEither = actionManager.getAction(
			HTTPMethod.GET.name(), asList("name", ANY_ROUTE, "nested"));

		if (actionEither.isLeft()) {
			throw new AssertionError("Action not present");
		}

		Action action = actionEither.get();

		PageItems<String> pageItems = (PageItems)action.apply(null);

		assertThat(pageItems.getItems(), hasSize(1));
		assertThat(pageItems.getItems(), hasItem("Apio"));
		assertThat(pageItems.getTotalCount(), is(1));

		List<Operation> operations = toOperations(
			actionManager.getActions(
				new ActionKey(GET.name(), "name", ANY_ROUTE, "nested"), null));

		assertThat(operations, hasSize(1));

		Operation retrieveOperation = operations.get(0);

		assertThat(retrieveOperation, is(instanceOf(RetrieveOperation.class)));
		assertThat(retrieveOperation.getHttpMethod(), is(GET));
		assertThat(retrieveOperation.getName(), is("name/nested/retrieve"));
		assertThat(
			retrieveOperation.getURIOptional(),
			is(optionalWithValue(equalTo("name/id/nested"))));
	}

	private static final Body _batchBody;
	private static final Body _singleBody;

	static {
		_singleBody = __ -> Optional.of("Apio");

		_batchBody = Body.create(asList(_singleBody, _singleBody));
	}

}