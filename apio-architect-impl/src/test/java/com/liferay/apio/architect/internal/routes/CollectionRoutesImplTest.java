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

import static com.liferay.apio.architect.internal.operation.util.OperationUtil.toOperations;
import static com.liferay.apio.architect.internal.routes.RoutesTestUtil.FORM_BUILDER_FUNCTION;
import static com.liferay.apio.architect.internal.routes.RoutesTestUtil.HAS_ADDING_PERMISSION_FUNCTION;
import static com.liferay.apio.architect.internal.routes.RoutesTestUtil.IDENTIFIER_FUNCTION;
import static com.liferay.apio.architect.internal.routes.RoutesTestUtil.PAGINATION;
import static com.liferay.apio.architect.internal.routes.RoutesTestUtil.keyValueFrom;
import static com.liferay.apio.architect.internal.unsafe.Unsafe.unsafeCast;
import static com.liferay.apio.architect.operation.HTTPMethod.GET;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;

import static java.util.Collections.singletonList;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import com.liferay.apio.architect.alias.routes.BatchCreateItemFunction;
import com.liferay.apio.architect.alias.routes.CreateItemFunction;
import com.liferay.apio.architect.batch.BatchResult;
import com.liferay.apio.architect.form.Body;
import com.liferay.apio.architect.form.Form;
import com.liferay.apio.architect.functional.Try;
import com.liferay.apio.architect.internal.annotation.Action;
import com.liferay.apio.architect.internal.annotation.ActionKey;
import com.liferay.apio.architect.internal.operation.RetrieveOperation;
import com.liferay.apio.architect.internal.routes.CollectionRoutesImpl.BuilderImpl;
import com.liferay.apio.architect.operation.Operation;
import com.liferay.apio.architect.pagination.PageItems;
import com.liferay.apio.architect.pagination.Pagination;
import com.liferay.apio.architect.routes.CollectionRoutes;
import com.liferay.apio.architect.routes.CollectionRoutes.Builder;
import com.liferay.apio.architect.single.model.SingleModel;

import io.vavr.control.Either;

import java.util.Arrays;
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
public class CollectionRoutesImplTest extends BaseRoutesTest {

	@Test(expected = NotFoundException.class)
	public void testEmptyBuilderBuildsEmptyRoutes() {
		Builder<String, Long> builder = new BuilderImpl<>(
			"name", __ -> null, IDENTIFIER_FUNCTION, __ -> null, actionManager);

		CollectionRoutes<String, Long> collectionRoutes = builder.build();

		Optional<CreateItemFunction<String>> createItemFunctionOptional =
			collectionRoutes.getCreateItemFunctionOptional();

		assertThat(createItemFunctionOptional, is(emptyOptional()));

		Either<Action.Error, Action> actionEither = actionManager.getAction(
			GET.name(), singletonList("name"));

		assertThat(actionEither.isRight(), is(true));

		Action action = actionEither.get();

		Object result = action.apply(null);

		assertThat(result, is(nullValue()));
	}

	@Test
	public void testFiveParameterBatchCreatorCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long> builder = new BuilderImpl<>(
			"name", __ -> null, IDENTIFIER_FUNCTION, __ -> null, actionManager);

		CollectionRoutes<String, Long> collectionRoutes = builder.addCreator(
			this::_testAndReturnFourParameterCreatorRoute,
			this::_testAndReturnFourParameterBatchCreatorRoute, String.class,
			Long.class, Boolean.class, Integer.class,
			HAS_ADDING_PERMISSION_FUNCTION, FORM_BUILDER_FUNCTION
		).build();

		assertThat(
			neededProviders,
			contains(
				Boolean.class.getName(), Integer.class.getName(),
				Long.class.getName(), String.class.getName()));

		_testCollectionRoutesCreator(collectionRoutes);
		_testCollectionRoutesBatchCreator(collectionRoutes);
	}

	@Test
	public void testFiveParameterBuilderMethodsCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long> builder = new BuilderImpl<>(
			"name", __ -> null, IDENTIFIER_FUNCTION, __ -> null, actionManager);

		CollectionRoutes<String, Long> collectionRoutes = builder.addCreator(
			this::_testAndReturnFourParameterCreatorRoute, String.class,
			Long.class, Boolean.class, Integer.class,
			HAS_ADDING_PERMISSION_FUNCTION, FORM_BUILDER_FUNCTION
		).addGetter(
			this::_testAndReturnFourParameterGetterRoute, String.class,
			Long.class, Boolean.class, Integer.class
		).build();

		assertThat(
			neededProviders,
			contains(
				Boolean.class.getName(), Integer.class.getName(),
				Long.class.getName(), String.class.getName()));

		_testCollectionRoutes(collectionRoutes);
	}

	@Test
	public void testFourParameterBatchCreatorCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long> builder = new BuilderImpl<>(
			"name", __ -> null, IDENTIFIER_FUNCTION, __ -> null, actionManager);

		CollectionRoutes<String, Long> collectionRoutes = builder.addCreator(
			this::_testAndReturnThreeParameterCreatorRoute,
			this::_testAndReturnThreeParameterBatchCreatorRoute, String.class,
			Long.class, Boolean.class, HAS_ADDING_PERMISSION_FUNCTION,
			FORM_BUILDER_FUNCTION
		).build();

		assertThat(
			neededProviders,
			contains(
				Boolean.class.getName(), Long.class.getName(),
				String.class.getName()));

		_testCollectionRoutesCreator(collectionRoutes);
		_testCollectionRoutesBatchCreator(collectionRoutes);
	}

	@Test
	public void testFourParameterBuilderMethodsCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long> builder = new BuilderImpl<>(
			"name", __ -> null, IDENTIFIER_FUNCTION, __ -> null, actionManager);

		CollectionRoutes<String, Long> collectionRoutes = builder.addCreator(
			this::_testAndReturnThreeParameterCreatorRoute, String.class,
			Long.class, Boolean.class, HAS_ADDING_PERMISSION_FUNCTION,
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

		_testCollectionRoutes(collectionRoutes);
	}

	@Test
	public void testOneParameterBatchCreatorCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long> builder = new BuilderImpl<>(
			"name", __ -> null, IDENTIFIER_FUNCTION, __ -> null, actionManager);

		CollectionRoutes<String, Long> collectionRoutes = builder.addCreator(
			this::_testAndReturnNoParameterCreatorRoute,
			this::_testAndReturnNoParameterBatchCreatorRoute,
			HAS_ADDING_PERMISSION_FUNCTION, FORM_BUILDER_FUNCTION
		).build();

		assertThat(neededProviders.size(), is(0));

		_testCollectionRoutesCreator(collectionRoutes);
		_testCollectionRoutesBatchCreator(collectionRoutes);
	}

	@Test
	public void testOneParameterBuilderMethodsCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long> builder = new BuilderImpl<>(
			"name", __ -> null, IDENTIFIER_FUNCTION, __ -> null, actionManager);

		CollectionRoutes<String, Long> collectionRoutes = builder.addCreator(
			this::_testAndReturnNoParameterCreatorRoute,
			HAS_ADDING_PERMISSION_FUNCTION, FORM_BUILDER_FUNCTION
		).addGetter(
			this::_testAndReturnNoParameterGetterRoute
		).build();

		assertThat(neededProviders.size(), is(0));

		_testCollectionRoutes(collectionRoutes);
	}

	@Test
	public void testThreeParameterBatchCreatorCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long> builder = new BuilderImpl<>(
			"name", __ -> null, __ -> null, __ -> null, actionManager);

		CollectionRoutes<String, Long> collectionRoutes = builder.addCreator(
			this::_testAndReturnTwoParameterCreatorRoute,
			this::_testAndReturnTwoParameterBatchCreatorRoute, String.class,
			Long.class, HAS_ADDING_PERMISSION_FUNCTION, FORM_BUILDER_FUNCTION
		).build();

		assertThat(
			neededProviders,
			contains(Long.class.getName(), String.class.getName()));

		_testCollectionRoutesCreator(collectionRoutes);
		_testCollectionRoutesBatchCreator(collectionRoutes);
	}

	@Test
	public void testThreeParameterBuilderMethodsCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long> builder = new BuilderImpl<>(
			"name", __ -> null, IDENTIFIER_FUNCTION, __ -> null, actionManager);

		CollectionRoutes<String, Long> collectionRoutes = builder.addCreator(
			this::_testAndReturnTwoParameterCreatorRoute, String.class,
			Long.class, HAS_ADDING_PERMISSION_FUNCTION, FORM_BUILDER_FUNCTION
		).addGetter(
			this::_testAndReturnTwoParameterGetterRoute, String.class,
			Long.class
		).build();

		assertThat(
			neededProviders,
			contains(Long.class.getName(), String.class.getName()));

		_testCollectionRoutes(collectionRoutes);
	}

	@Test
	public void testTwoParameterBatchCreatorCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long> builder = new BuilderImpl<>(
			"name", __ -> null, __ -> null, __ -> null, actionManager);

		CollectionRoutes<String, Long> collectionRoutes = builder.addCreator(
			this::_testAndReturnOneParameterCreatorRoute,
			this::_testAndReturnOneParameterBatchCreatorRoute, String.class,
			HAS_ADDING_PERMISSION_FUNCTION, FORM_BUILDER_FUNCTION
		).build();

		assertThat(neededProviders, contains(String.class.getName()));

		_testCollectionRoutesCreator(collectionRoutes);
		_testCollectionRoutesBatchCreator(collectionRoutes);
	}

	@Test
	public void testTwoParameterBuilderMethodsCreatesValidRoutes() {
		Set<String> neededProviders = new TreeSet<>();

		Builder<String, Long> builder = new BuilderImpl<>(
			"name", __ -> null, IDENTIFIER_FUNCTION, __ -> null, actionManager);

		CollectionRoutes<String, Long> collectionRoutes = builder.addCreator(
			this::_testAndReturnOneParameterCreatorRoute, String.class,
			HAS_ADDING_PERMISSION_FUNCTION, FORM_BUILDER_FUNCTION
		).addGetter(
			this::_testAndReturnOneParameterGetterRoute, String.class
		).build();

		assertThat(neededProviders, contains(String.class.getName()));

		_testCollectionRoutes(collectionRoutes);
	}

	private List<Long> _testAndReturnFourParameterBatchCreatorRoute(
		List<Map<String, Object>> bodies, String string, Long aLong,
		Boolean aBoolean, Integer integer) {

		assertThat(integer, is(2017));

		return _testAndReturnThreeParameterBatchCreatorRoute(
			bodies, string, aLong, aBoolean);
	}

	private String _testAndReturnFourParameterCreatorRoute(
		Map<String, Object> body, String string, Long aLong, Boolean aBoolean,
		Integer integer) {

		assertThat(integer, is(2017));

		return _testAndReturnThreeParameterCreatorRoute(
			body, string, aLong, aBoolean);
	}

	private PageItems<String> _testAndReturnFourParameterGetterRoute(
		Pagination pagination, String string, Long aLong, Boolean aBoolean,
		Integer integer) {

		assertThat(integer, is(2017));

		return _testAndReturnThreeParameterGetterRoute(
			pagination, string, aLong, aBoolean);
	}

	private List<Long> _testAndReturnNoParameterBatchCreatorRoute(
		List<Map<String, Object>> bodies) {

		assertThat(bodies, hasSize(2));

		assertThat(bodies.get(0).get("key"), is(keyValueFrom(_singleBody)));
		assertThat(bodies.get(1).get("key"), is(keyValueFrom(_singleBody)));

		return Arrays.asList(42L, 42L);
	}

	private String _testAndReturnNoParameterCreatorRoute(
		Map<String, Object> body) {

		assertThat(body.get("key"), is(keyValueFrom(_singleBody)));

		return "Apio";
	}

	private PageItems<String> _testAndReturnNoParameterGetterRoute(
		Pagination pagination) {

		assertThat(pagination, is(PAGINATION));

		return new PageItems<>(singletonList("Apio"), 1);
	}

	private List<Long> _testAndReturnOneParameterBatchCreatorRoute(
		List<Map<String, Object>> bodies, String string) {

		assertThat(string, is("Apio"));

		return _testAndReturnNoParameterBatchCreatorRoute(bodies);
	}

	private String _testAndReturnOneParameterCreatorRoute(
		Map<String, Object> body, String string) {

		assertThat(string, is("Apio"));

		return _testAndReturnNoParameterCreatorRoute(body);
	}

	private PageItems<String> _testAndReturnOneParameterGetterRoute(
		Pagination pagination, String string) {

		assertThat(string, is("Apio"));

		return _testAndReturnNoParameterGetterRoute(pagination);
	}

	private List<Long> _testAndReturnThreeParameterBatchCreatorRoute(
		List<Map<String, Object>> bodies, String string, Long aLong,
		Boolean aBoolean) {

		assertThat(aBoolean, is(true));

		return _testAndReturnTwoParameterBatchCreatorRoute(
			bodies, string, aLong);
	}

	private String _testAndReturnThreeParameterCreatorRoute(
		Map<String, Object> body, String string, Long aLong, Boolean aBoolean) {

		assertThat(aBoolean, is(true));

		return _testAndReturnTwoParameterCreatorRoute(body, string, aLong);
	}

	private PageItems<String> _testAndReturnThreeParameterGetterRoute(
		Pagination pagination, String string, Long aLong, Boolean aBoolean) {

		assertThat(aBoolean, is(true));

		return _testAndReturnTwoParameterGetterRoute(pagination, string, aLong);
	}

	private List<Long> _testAndReturnTwoParameterBatchCreatorRoute(
		List<Map<String, Object>> bodies, String string, Long aLong) {

		assertThat(aLong, is(42L));

		return _testAndReturnOneParameterBatchCreatorRoute(bodies, string);
	}

	private String _testAndReturnTwoParameterCreatorRoute(
		Map<String, Object> body, String string, Long aLong) {

		assertThat(aLong, is(42L));

		return _testAndReturnOneParameterCreatorRoute(body, string);
	}

	private PageItems<String> _testAndReturnTwoParameterGetterRoute(
		Pagination pagination, String string, Long aLong) {

		assertThat(aLong, is(42L));

		return _testAndReturnOneParameterGetterRoute(pagination, string);
	}

	private void _testCollectionRoutes(
		CollectionRoutes<String, Long> collectionRoutes) {

		_testCollectionRoutesCreator(collectionRoutes);

		_testCollectionRoutesBatchCreator(collectionRoutes);

		_testCollectionRoutesGetter();
	}

	private void _testCollectionRoutesBatchCreator(
		CollectionRoutes<String, Long> collectionRoutes) {

		Optional<Form> formOptional = collectionRoutes.getFormOptional();

		if (!formOptional.isPresent()) {
			throw new AssertionError("Batch Create Form not present");
		}

		Form form = formOptional.get();

		assertThat(form.getId(), is("c/name"));

		List<Map> list = unsafeCast(form.getList(_batchBody));

		assertThat(list, hasSize(2));

		assertThat(list.get(0).get("key"), is(keyValueFrom(_singleBody)));
		assertThat(list.get(1).get("key"), is(keyValueFrom(_singleBody)));

		Optional<BatchCreateItemFunction<Long>>
			batchCreateItemFunctionOptional =
				collectionRoutes.getBatchCreateItemFunctionOptional();

		if (!batchCreateItemFunctionOptional.isPresent()) {
			throw new AssertionError("BatchCreateItemFunction not present");
		}

		BatchCreateItemFunction<Long> batchCreateItemFunction =
			batchCreateItemFunctionOptional.get();

		BatchResult<Long> batchResult = batchCreateItemFunction.apply(
			null
		).andThen(
			Try::getUnchecked
		).apply(
			_batchBody
		);

		assertThat(batchResult.resourceName, is("name"));

		List<Long> identifiers = batchResult.getIdentifiers();

		assertThat(identifiers, hasSize(2));
		assertThat(identifiers.get(0), is(42L));
		assertThat(identifiers.get(1), is(42L));
	}

	private void _testCollectionRoutesCreator(
		CollectionRoutes<String, Long> collectionRoutes) {

		Optional<Form> formOptional = collectionRoutes.getFormOptional();

		if (!formOptional.isPresent()) {
			throw new AssertionError("Create Form not present");
		}

		Form form = formOptional.get();

		assertThat(form.getId(), is("c/name"));

		Map map = (Map)form.get(_singleBody);

		assertThat(map.get("key"), is(keyValueFrom(_singleBody)));

		Optional<CreateItemFunction<String>> createItemFunctionOptional =
			collectionRoutes.getCreateItemFunctionOptional();

		if (!createItemFunctionOptional.isPresent()) {
			throw new AssertionError("CreateItemFunction not present");
		}

		CreateItemFunction<String> createItemFunction =
			createItemFunctionOptional.get();

		SingleModel<String> singleModel = createItemFunction.apply(
			null
		).andThen(
			Try::getUnchecked
		).apply(
			_singleBody
		);

		assertThat(singleModel.getResourceName(), is("name"));
		assertThat(singleModel.getModel(), is("Apio"));
	}

	private void _testCollectionRoutesGetter() {
		Either<Action.Error, Action> actionEither = actionManager.getAction(
			GET.name(), singletonList("name"));

		if (actionEither.isLeft()) {
			throw new AssertionError("Action not present");
		}

		Action action = actionEither.get();

		PageItems<String> pageItems = (PageItems)action.apply(null);

		assertThat(pageItems.getItems(), hasSize(1));
		assertThat(pageItems.getItems(), hasItem("Apio"));
		assertThat(pageItems.getTotalCount(), is(1));

		List<Operation> operations = toOperations(
			actionManager.getActions(new ActionKey(GET.name(), "name"), null));

		assertThat(operations, hasSize(1));

		Operation createOperation = operations.get(0);

		assertThat(createOperation, is(instanceOf(RetrieveOperation.class)));
		assertThat(createOperation.getHttpMethod(), is(GET));
		assertThat(createOperation.getName(), is("name/retrieve"));
	}

	private static final Body _batchBody;
	private static final Body _singleBody;

	static {
		_singleBody = __ -> Optional.of("Apio");

		_batchBody = Body.create(Arrays.asList(_singleBody, _singleBody));
	}

}