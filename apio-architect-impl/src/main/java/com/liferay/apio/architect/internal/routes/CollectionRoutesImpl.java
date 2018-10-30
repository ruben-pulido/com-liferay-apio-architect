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

import static com.liferay.apio.architect.internal.unsafe.Unsafe.unsafeCast;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import com.liferay.apio.architect.alias.IdentifierFunction;
import com.liferay.apio.architect.alias.form.FormBuilderFunction;
import com.liferay.apio.architect.alias.routes.BatchCreateItemFunction;
import com.liferay.apio.architect.alias.routes.CreateItemFunction;
import com.liferay.apio.architect.alias.routes.CustomPageFunction;
import com.liferay.apio.architect.alias.routes.GetPageFunction;
import com.liferay.apio.architect.alias.routes.permission.HasAddingPermissionFunction;
import com.liferay.apio.architect.batch.BatchResult;
import com.liferay.apio.architect.credentials.Credentials;
import com.liferay.apio.architect.custom.actions.CustomRoute;
import com.liferay.apio.architect.form.Body;
import com.liferay.apio.architect.form.Form;
import com.liferay.apio.architect.function.throwable.ThrowableBiFunction;
import com.liferay.apio.architect.function.throwable.ThrowableFunction;
import com.liferay.apio.architect.function.throwable.ThrowableHexaFunction;
import com.liferay.apio.architect.function.throwable.ThrowablePentaFunction;
import com.liferay.apio.architect.function.throwable.ThrowableTetraFunction;
import com.liferay.apio.architect.function.throwable.ThrowableTriFunction;
import com.liferay.apio.architect.identifier.Identifier;
import com.liferay.apio.architect.internal.form.FormImpl;
import com.liferay.apio.architect.internal.pagination.PageImpl;
import com.liferay.apio.architect.internal.single.model.SingleModelImpl;
import com.liferay.apio.architect.internal.wiring.osgi.manager.router.ActionSemantics;
import com.liferay.apio.architect.internal.wiring.osgi.manager.router.Resource;
import com.liferay.apio.architect.pagination.Page;
import com.liferay.apio.architect.pagination.PageItems;
import com.liferay.apio.architect.pagination.Pagination;
import com.liferay.apio.architect.routes.CollectionRoutes;
import com.liferay.apio.architect.single.model.SingleModel;
import com.liferay.apio.architect.uri.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Alejandro Hernández
 */
public class CollectionRoutesImpl<T, S> implements CollectionRoutes<T, S> {

	public CollectionRoutesImpl(BuilderImpl<T, S> builderImpl) {
		_actionSemantics = unmodifiableList(builderImpl._actionSemantics);
	}

	public List<ActionSemantics> getActionSemantics() {
		return _actionSemantics;
	}

	@Override
	public Optional<BatchCreateItemFunction<S>>
		getBatchCreateItemFunctionOptional() {

		return Optional.empty();
	}

	@Override
	public Optional<CreateItemFunction<T>> getCreateItemFunctionOptional() {
		return Optional.empty();
	}

	@Override
	public Optional<Map<String, CustomPageFunction<?>>>
		getCustomPageFunctionsOptional() {

		return Optional.empty();
	}

	@Override
	public Map<String, CustomRoute> getCustomRoutes() {
		return Collections.emptyMap();
	}

	@Override
	public Optional<Form> getFormOptional() {
		return Optional.empty();
	}

	@Override
	public Optional<GetPageFunction<T>> getGetPageFunctionOptional() {
		return Optional.empty();
	}

	public static class BuilderImpl<T, S> implements Builder<T, S> {

		public BuilderImpl(
			Resource.Paged pagedResource,
			Function<Path, ?> pathToIdentifierFunction,
			Function<T, S> modelToIdentifierFunction,
			Function<String, Optional<String>> nameFunction) {

			_pagedResource = pagedResource;
			_pathToIdentifierFunction = pathToIdentifierFunction::apply;
			_modelToIdentifierFunction = modelToIdentifierFunction;
			_nameFunction = nameFunction;
		}

		@Override
		public <A, R> Builder<T, S> addCreator(
			ThrowableBiFunction<R, A, T> creatorThrowableBiFunction,
			Class<A> aClass,
			HasAddingPermissionFunction hasAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			ThrowableBiFunction<List<R>, A, List<S>>
				batchCreatorThrowableBiFunction = (formList, a) ->
					_transformList(
						formList, r -> creatorThrowableBiFunction.apply(r, a));

			return addCreator(
				creatorThrowableBiFunction, batchCreatorThrowableBiFunction,
				aClass, hasAddingPermissionFunction, formBuilderFunction);
		}

		@Override
		public <A, R> Builder<T, S> addCreator(
			ThrowableBiFunction<R, A, T> creatorThrowableBiFunction,
			ThrowableBiFunction<List<R>, A, List<S>>
				batchCreatorThrowableBiFunction,
			Class<A> aClass,
			HasAddingPermissionFunction hasAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			Form<R> form = formBuilderFunction.apply(
				new FormImpl.BuilderImpl<>(
					asList("c", _pagedResource.getName()),
					_pathToIdentifierFunction, _nameFunction));

			ActionSemantics batchCreateActionSemantics =
				ActionSemantics.ofResource(
					_pagedResource
				).name(
					"batch-create"
				).method(
					"POST"
				).provide(
					aClass
				).returns(
					BatchResult.class
				).receivesListOf(
					form
				).execute(
					params -> batchCreatorThrowableBiFunction.andThen(
						t -> new BatchResult<>(t, _pagedResource.getName())
					).apply(
						form.getList((Body)params.get(1)),
						unsafeCast(params.get(0))
					)
				);

			_actionSemantics.add(batchCreateActionSemantics);

			ActionSemantics createActionSemantics = ActionSemantics.ofResource(
				_pagedResource
			).name(
				"create"
			).method(
				"POST"
			).provide(
				aClass
			).returns(
				SingleModel.class
			).receivesSingle(
				form
			).execute(
				params -> creatorThrowableBiFunction.andThen(
					t -> new SingleModelImpl<>(
						t, _pagedResource.getName(), emptyList())
				).apply(
					form.get((Body)params.get(1)), unsafeCast(params.get(0))
				)
			);

			_actionSemantics.add(createActionSemantics);

			return this;
		}

		@Override
		public <R> Builder<T, S> addCreator(
			ThrowableFunction<R, T> creatorThrowableFunction,
			HasAddingPermissionFunction hasAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			ThrowableFunction<List<R>, List<S>> batchCreatorThrowableFunction =
				formList -> _transformList(formList, creatorThrowableFunction);

			return addCreator(
				creatorThrowableFunction, batchCreatorThrowableFunction,
				hasAddingPermissionFunction, formBuilderFunction);
		}

		@Override
		public <R> Builder<T, S> addCreator(
			ThrowableFunction<R, T> creatorThrowableFunction,
			ThrowableFunction<List<R>, List<S>> batchCreatorThrowableFunction,
			HasAddingPermissionFunction hasAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			Form<R> form = formBuilderFunction.apply(
				new FormImpl.BuilderImpl<>(
					asList("c", _pagedResource.getName()),
					_pathToIdentifierFunction, _nameFunction));

			ActionSemantics batchCreateActionSemantics =
				ActionSemantics.ofResource(
					_pagedResource
				).name(
					"batch-create"
				).method(
					"POST"
				).provide(
				).returns(
					BatchResult.class
				).receivesListOf(
					form
				).execute(
					params -> batchCreatorThrowableFunction.andThen(
						t -> new BatchResult<>(t, _pagedResource.getName())
					).apply(
						form.getList((Body)params.get(0))
					)
				);

			_actionSemantics.add(batchCreateActionSemantics);

			ActionSemantics createActionSemantics = ActionSemantics.ofResource(
				_pagedResource
			).name(
				"create"
			).method(
				"POST"
			).provideNothing(
			).returns(
				SingleModel.class
			).receivesSingle(
				form
			).execute(
				params -> creatorThrowableFunction.andThen(
					t -> new SingleModelImpl<>(
						t, _pagedResource.getName(), emptyList())
				).apply(
					form.get((Body)params.get(0))
				)
			);

			_actionSemantics.add(createActionSemantics);

			return this;
		}

		@Override
		public <A, B, C, D, R> Builder<T, S> addCreator(
			ThrowablePentaFunction<R, A, B, C, D, T>
				creatorThrowablePentaFunction,
			Class<A> aClass, Class<B> bClass, Class<C> cClass, Class<D> dClass,
			HasAddingPermissionFunction hasAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			ThrowablePentaFunction<List<R>, A, B, C, D, List<S>>
				batchCreatorThrowablePentaFunction = (formList, a, b, c, d) ->
					_transformList(
						formList,
						r -> creatorThrowablePentaFunction.apply(
							r, a, b, c, d));

			return addCreator(
				creatorThrowablePentaFunction,
				batchCreatorThrowablePentaFunction, aClass, bClass, cClass,
				dClass, hasAddingPermissionFunction, formBuilderFunction);
		}

		@Override
		public <A, B, C, D, R> Builder<T, S> addCreator(
			ThrowablePentaFunction<R, A, B, C, D, T>
				creatorThrowablePentaFunction,
			ThrowablePentaFunction<List<R>, A, B, C, D, List<S>>
				batchCreatorThrowablePentaFunction,
			Class<A> aClass, Class<B> bClass, Class<C> cClass, Class<D> dClass,
			HasAddingPermissionFunction hasAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			Form<R> form = formBuilderFunction.apply(
				new FormImpl.BuilderImpl<>(
					asList("c", _pagedResource.getName()),
					_pathToIdentifierFunction, _nameFunction));

			ActionSemantics batchCreateActionSemantics =
				ActionSemantics.ofResource(
					_pagedResource
				).name(
					"batch-create"
				).method(
					"POST"
				).provide(
					aClass, bClass, cClass, dClass
				).returns(
					BatchResult.class
				).receivesListOf(
					form
				).execute(
					params -> batchCreatorThrowablePentaFunction.andThen(
						t -> new BatchResult<>(t, _pagedResource.getName())
					).apply(
						form.getList((Body)params.get(4)),
						unsafeCast(params.get(0)), unsafeCast(params.get(1)),
						unsafeCast(params.get(2)), unsafeCast(params.get(3))
					)
				);

			_actionSemantics.add(batchCreateActionSemantics);

			ActionSemantics createActionSemantics = ActionSemantics.ofResource(
				_pagedResource
			).name(
				"create"
			).method(
				"POST"
			).provide(
				aClass, bClass, cClass, dClass
			).returns(
				SingleModel.class
			).receivesSingle(
				form
			).execute(
				params -> creatorThrowablePentaFunction.andThen(
					t -> new SingleModelImpl<>(
						t, _pagedResource.getName(), emptyList())
				).apply(
					form.get((Body)params.get(4)), unsafeCast(params.get(0)),
					unsafeCast(params.get(1)), unsafeCast(params.get(2)),
					unsafeCast(params.get(3))
				)
			);

			_actionSemantics.add(createActionSemantics);

			return this;
		}

		@Override
		public <A, B, C, R> Builder<T, S> addCreator(
			ThrowableTetraFunction<R, A, B, C, T> creatorThrowableTetraFunction,
			Class<A> aClass, Class<B> bClass, Class<C> cClass,
			HasAddingPermissionFunction hasAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			ThrowableTetraFunction<List<R>, A, B, C, List<S>>
				batchCreatorThrowableTetraFunction = (formList, a, b, c) ->
					_transformList(
						formList,
						r -> creatorThrowableTetraFunction.apply(r, a, b, c));

			return addCreator(
				creatorThrowableTetraFunction,
				batchCreatorThrowableTetraFunction, aClass, bClass, cClass,
				hasAddingPermissionFunction, formBuilderFunction);
		}

		@Override
		public <A, B, C, R> Builder<T, S> addCreator(
			ThrowableTetraFunction<R, A, B, C, T> creatorThrowableTetraFunction,
			ThrowableTetraFunction<List<R>, A, B, C, List<S>>
				batchCreatorThrowableTetraFunction,
			Class<A> aClass, Class<B> bClass, Class<C> cClass,
			HasAddingPermissionFunction hasAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			Form<R> form = formBuilderFunction.apply(
				new FormImpl.BuilderImpl<>(
					asList("c", _pagedResource.getName()),
					_pathToIdentifierFunction, _nameFunction));

			ActionSemantics batchCreateActionSemantics =
				ActionSemantics.ofResource(
					_pagedResource
				).name(
					"batch-create"
				).method(
					"POST"
				).provide(
					aClass, bClass, cClass
				).returns(
					BatchResult.class
				).receivesListOf(
					form
				).execute(
					params -> batchCreatorThrowableTetraFunction.andThen(
						t -> new BatchResult<>(t, _pagedResource.getName())
					).apply(
						form.getList((Body)params.get(3)),
						unsafeCast(params.get(0)), unsafeCast(params.get(1)),
						unsafeCast(params.get(2))
					)
				);

			_actionSemantics.add(batchCreateActionSemantics);

			ActionSemantics createActionSemantics = ActionSemantics.ofResource(
				_pagedResource
			).name(
				"create"
			).method(
				"POST"
			).provide(
				aClass, bClass, cClass
			).returns(
				SingleModel.class
			).receivesSingle(
				form
			).execute(
				params -> creatorThrowableTetraFunction.andThen(
					t -> new SingleModelImpl<>(
						t, _pagedResource.getName(), emptyList())
				).apply(
					form.get((Body)params.get(3)), unsafeCast(params.get(0)),
					unsafeCast(params.get(1)), unsafeCast(params.get(2))
				)
			);

			_actionSemantics.add(createActionSemantics);

			return this;
		}

		@Override
		public <A, B, R> Builder<T, S> addCreator(
			ThrowableTriFunction<R, A, B, T> creatorThrowableTriFunction,
			Class<A> aClass, Class<B> bClass,
			HasAddingPermissionFunction hasAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			ThrowableTriFunction<List<R>, A, B, List<S>>
				batchCreatorThrowableTriFunction = (formList, a, b) ->
					_transformList(
						formList,
						r -> creatorThrowableTriFunction.apply(r, a, b));

			return addCreator(
				creatorThrowableTriFunction, batchCreatorThrowableTriFunction,
				aClass, bClass, hasAddingPermissionFunction,
				formBuilderFunction);
		}

		@Override
		public <A, B, R> Builder<T, S> addCreator(
			ThrowableTriFunction<R, A, B, T> creatorThrowableTriFunction,
			ThrowableTriFunction<List<R>, A, B, List<S>>
				batchCreatorThrowableTriFunction,
			Class<A> aClass, Class<B> bClass,
			HasAddingPermissionFunction hasAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			Form<R> form = formBuilderFunction.apply(
				new FormImpl.BuilderImpl<>(
					asList("c", _pagedResource.getName()),
					_pathToIdentifierFunction, _nameFunction));

			ActionSemantics batchCreateActionSemantics =
				ActionSemantics.ofResource(
					_pagedResource
				).name(
					"batch-create"
				).method(
					"POST"
				).provide(
					aClass, bClass
				).returns(
					BatchResult.class
				).receivesListOf(
					form
				).execute(
					params -> batchCreatorThrowableTriFunction.andThen(
						t -> new BatchResult<>(t, _pagedResource.getName())
					).apply(
						form.getList((Body)params.get(2)),
						unsafeCast(params.get(0)), unsafeCast(params.get(1))
					)
				);

			_actionSemantics.add(batchCreateActionSemantics);

			ActionSemantics createActionSemantics = ActionSemantics.ofResource(
				_pagedResource
			).name(
				"create"
			).method(
				"POST"
			).provide(
				aClass, bClass
			).returns(
				SingleModel.class
			).receivesSingle(
				form
			).execute(
				params -> creatorThrowableTriFunction.andThen(
					t -> new SingleModelImpl<>(
						t, _pagedResource.getName(), emptyList())
				).apply(
					form.get((Body)params.get(2)), unsafeCast(params.get(0)),
					unsafeCast(params.get(1))
				)
			);

			_actionSemantics.add(createActionSemantics);

			return this;
		}

		@Override
		public <R, U, I extends Identifier> CollectionRoutes.Builder<T, S>
			addCustomRoute(
				CustomRoute customRoute,
				ThrowableBiFunction<Pagination, R, U> throwableBiFunction,
				Class<I> supplier,
				Function<Credentials, Boolean> permissionFunction,
				FormBuilderFunction<R> formBuilderFunction) {

			Form<R> form = _getForm(
				formBuilderFunction,
				asList("p", _pagedResource.getName(), customRoute.getName()));

			ActionSemantics createActionSemantics = ActionSemantics.ofResource(
				_pagedResource
			).name(
				customRoute.getName()
			).method(
				customRoute.getMethod()
			).provide(
				Pagination.class
			).returns(
				SingleModel.class
			).receivesSingle(
				form
			).execute(
				params -> throwableBiFunction.andThen(
					t -> new SingleModelImpl<>(t, _getResourceName(supplier))
				).apply(
					(Pagination)params.get(0),
					_getModel(form, () -> (Body)params.get(1))
				)
			);

			_actionSemantics.add(createActionSemantics);

			return this;
		}

		@Override
		public <A, B, C, D, R, U, I extends Identifier>
			CollectionRoutes.Builder<T, S> addCustomRoute(
				CustomRoute customRoute,
				ThrowableHexaFunction<Pagination, R, A, B, C, D, U>
					throwableHexaFunction,
				Class<A> aClass, Class<B> bClass, Class<C> cClass,
				Class<D> dClass, Class<I> supplier,
				Function<Credentials, Boolean> permissionFunction,
				FormBuilderFunction<R> formBuilderFunction) {

			Form<R> form = _getForm(
				formBuilderFunction,
				asList("p", _pagedResource.getName(), customRoute.getName()));

			ActionSemantics createActionSemantics = ActionSemantics.ofResource(
				_pagedResource
			).name(
				customRoute.getName()
			).method(
				customRoute.getMethod()
			).provide(
				Pagination.class, aClass, bClass, cClass, dClass
			).returns(
				SingleModel.class
			).receivesSingle(
				form
			).execute(
				params -> throwableHexaFunction.andThen(
					t -> new SingleModelImpl<>(t, _getResourceName(supplier))
				).apply(
					(Pagination)params.get(0),
					_getModel(form, () -> (Body)params.get(5)),
					unsafeCast(params.get(1)), unsafeCast(params.get(2)),
					unsafeCast(params.get(3)), unsafeCast(params.get(4))
				)
			);

			_actionSemantics.add(createActionSemantics);

			return this;
		}

		@Override
		public <A, B, C, R, U, I extends Identifier>
			CollectionRoutes.Builder<T, S> addCustomRoute(
				CustomRoute customRoute,
				ThrowablePentaFunction<Pagination, R, A, B, C, U>
					throwablePentaFunction,
				Class<A> aClass, Class<B> bClass, Class<C> cClass,
				Class<I> supplier,
				Function<Credentials, Boolean> permissionFunction,
				FormBuilderFunction<R> formBuilderFunction) {

			Form<R> form = _getForm(
				formBuilderFunction,
				asList("p", _pagedResource.getName(), customRoute.getName()));

			ActionSemantics createActionSemantics = ActionSemantics.ofResource(
				_pagedResource
			).name(
				customRoute.getName()
			).method(
				customRoute.getMethod()
			).provide(
				Pagination.class, aClass, bClass, cClass
			).returns(
				SingleModel.class
			).receivesSingle(
				form
			).execute(
				params -> throwablePentaFunction.andThen(
					t -> new SingleModelImpl<>(t, _getResourceName(supplier))
				).apply(
					(Pagination)params.get(0),
					_getModel(form, () -> (Body)params.get(4)),
					unsafeCast(params.get(1)), unsafeCast(params.get(2)),
					unsafeCast(params.get(3))
				)
			);

			_actionSemantics.add(createActionSemantics);

			return this;
		}

		@Override
		public <A, B, R, U, I extends Identifier> CollectionRoutes.Builder<T, S>
			addCustomRoute(
				CustomRoute customRoute,
				ThrowableTetraFunction<Pagination, R, A, B, U>
					throwableTetraFunction,
				Class<A> aClass, Class<B> bClass, Class<I> supplier,
				Function<Credentials, Boolean> permissionFunction,
				FormBuilderFunction<R> formBuilderFunction) {

			Form<R> form = _getForm(
				formBuilderFunction,
				asList("p", _pagedResource.getName(), customRoute.getName()));

			ActionSemantics createActionSemantics = ActionSemantics.ofResource(
				_pagedResource
			).name(
				customRoute.getName()
			).method(
				customRoute.getMethod()
			).provide(
				Pagination.class, aClass, bClass
			).returns(
				SingleModel.class
			).receivesSingle(
				form
			).execute(
				params -> throwableTetraFunction.andThen(
					t -> new SingleModelImpl<>(t, _getResourceName(supplier))
				).apply(
					(Pagination)params.get(0),
					_getModel(form, () -> (Body)params.get(3)),
					unsafeCast(params.get(1)), unsafeCast(params.get(2))
				)
			);

			_actionSemantics.add(createActionSemantics);

			return this;
		}

		@Override
		public <A, R, U, I extends Identifier> CollectionRoutes.Builder<T, S>
			addCustomRoute(
				CustomRoute customRoute,
				ThrowableTriFunction<Pagination, R, A, U> throwableTriFunction,
				Class<A> aClass, Class<I> supplier,
				Function<Credentials, Boolean> permissionFunction,
				FormBuilderFunction<R> formBuilderFunction) {

			Form<R> form = _getForm(
				formBuilderFunction,
				asList("p", _pagedResource.getName(), customRoute.getName()));

			ActionSemantics createActionSemantics = ActionSemantics.ofResource(
				_pagedResource
			).name(
				customRoute.getName()
			).method(
				customRoute.getMethod()
			).provide(
				Pagination.class, aClass
			).returns(
				SingleModel.class
			).receivesSingle(
				form
			).execute(
				params -> throwableTriFunction.andThen(
					t -> new SingleModelImpl<>(t, _getResourceName(supplier))
				).apply(
					(Pagination)params.get(0),
					_getModel(form, () -> (Body)params.get(2)),
					unsafeCast(params.get(1))
				)
			);

			_actionSemantics.add(createActionSemantics);

			return this;
		}

		@Override
		public <A> Builder<T, S> addGetter(
			ThrowableBiFunction<Pagination, A, PageItems<T>>
				getterThrowableBiFunction,
			Class<A> aClass) {

			ActionSemantics actionSemantics = ActionSemantics.ofResource(
				_pagedResource
			).name(
				"retrieve"
			).method(
				"GET"
			).provide(
				Pagination.class, aClass
			).returns(
				Page.class
			).receivesNothing(
			).execute(
				params -> getterThrowableBiFunction.andThen(
					pageItems -> new PageImpl<>(
						_pagedResource.getName(), pageItems,
						(Pagination)params.get(0), emptyList())
				).apply(
					(Pagination)params.get(0), unsafeCast(params.get(1))
				)
			);

			_actionSemantics.add(actionSemantics);

			return this;
		}

		@Override
		public Builder<T, S> addGetter(
			ThrowableFunction<Pagination, PageItems<T>>
				getterThrowableFunction) {

			ActionSemantics actionSemantics = ActionSemantics.ofResource(
				_pagedResource
			).name(
				"retrieve"
			).method(
				"GET"
			).provide(
				Pagination.class
			).returns(
				Page.class
			).receivesNothing(
			).execute(
				params -> getterThrowableFunction.andThen(
					pageItems -> new PageImpl<>(
						_pagedResource.getName(), pageItems,
						(Pagination)params.get(0), emptyList())
				).apply(
					(Pagination)params.get(0)
				)
			);

			_actionSemantics.add(actionSemantics);

			return this;
		}

		@Override
		public <A, B, C, D> Builder<T, S> addGetter(
			ThrowablePentaFunction<Pagination, A, B, C, D, PageItems<T>>
				getterThrowablePentaFunction,
			Class<A> aClass, Class<B> bClass, Class<C> cClass,
			Class<D> dClass) {

			ActionSemantics actionSemantics = ActionSemantics.ofResource(
				_pagedResource
			).name(
				"retrieve"
			).method(
				"GET"
			).provide(
				Pagination.class, aClass, bClass, cClass, dClass
			).returns(
				Page.class
			).receivesNothing(
			).execute(
				params -> getterThrowablePentaFunction.andThen(
					pageItems -> new PageImpl<>(
						_pagedResource.getName(), pageItems,
						(Pagination)params.get(0), emptyList())
				).apply(
					(Pagination)params.get(0), unsafeCast(params.get(1)),
					unsafeCast(params.get(2)), unsafeCast(params.get(3)),
					unsafeCast(params.get(4))
				)
			);

			_actionSemantics.add(actionSemantics);

			return this;
		}

		@Override
		public <A, B, C> Builder<T, S> addGetter(
			ThrowableTetraFunction<Pagination, A, B, C, PageItems<T>>
				getterThrowableTetraFunction,
			Class<A> aClass, Class<B> bClass, Class<C> cClass) {

			ActionSemantics actionSemantics = ActionSemantics.ofResource(
				_pagedResource
			).name(
				"retrieve"
			).method(
				"GET"
			).provide(
				Pagination.class, aClass, bClass, cClass
			).returns(
				Page.class
			).receivesNothing(
			).execute(
				params -> getterThrowableTetraFunction.andThen(
					pageItems -> new PageImpl<>(
						_pagedResource.getName(), pageItems,
						(Pagination)params.get(0), emptyList())
				).apply(
					(Pagination)params.get(0), unsafeCast(params.get(1)),
					unsafeCast(params.get(2)), unsafeCast(params.get(3))
				)
			);

			_actionSemantics.add(actionSemantics);

			return this;
		}

		@Override
		public <A, B> Builder<T, S> addGetter(
			ThrowableTriFunction<Pagination, A, B, PageItems<T>>
				getterThrowableTriFunction,
			Class<A> aClass, Class<B> bClass) {

			ActionSemantics actionSemantics = ActionSemantics.ofResource(
				_pagedResource
			).name(
				"retrieve"
			).method(
				"GET"
			).provide(
				Pagination.class, aClass, bClass
			).returns(
				Page.class
			).receivesNothing(
			).execute(
				params -> getterThrowableTriFunction.andThen(
					pageItems -> new PageImpl<>(
						_pagedResource.getName(), pageItems,
						(Pagination)params.get(0), emptyList())
				).apply(
					(Pagination)params.get(0), unsafeCast(params.get(1)),
					unsafeCast(params.get(2))
				)
			);

			_actionSemantics.add(actionSemantics);

			return this;
		}

		@Override
		public CollectionRoutes<T, S> build() {
			return new CollectionRoutesImpl<>(this);
		}

		private <R> Form<R> _getForm(
			FormBuilderFunction<R> formBuilderFunction, List<String> paths) {

			if (formBuilderFunction != null) {
				return formBuilderFunction.apply(
					new FormImpl.BuilderImpl<>(
						paths, _pathToIdentifierFunction, _nameFunction));
			}

			return null;
		}

		private <R> R _getModel(Form<R> form, Supplier<Body> body) {
			if (form != null) {
				return form.get(body.get());
			}

			return null;
		}

		private <I extends Identifier> String _getResourceName(
			Class<I> supplier) {

			return _nameFunction.apply(
				supplier.getName()
			).orElse(
				null
			);
		}

		private <U> List<S> _transformList(
				List<U> list,
				ThrowableFunction<U, T> transformThrowableFunction)
			throws Exception {

			List<S> newList = new ArrayList<>();

			for (U u : list) {
				S s = transformThrowableFunction.andThen(
					_modelToIdentifierFunction::apply
				).apply(
					u
				);

				newList.add(s);
			}

			return newList;
		}

		private final List<ActionSemantics> _actionSemantics =
			new ArrayList<>();
		private final Function<T, S> _modelToIdentifierFunction;
		private final Function<String, Optional<String>> _nameFunction;
		private final Resource.Paged _pagedResource;
		private final IdentifierFunction<?> _pathToIdentifierFunction;

	}

	private final List<ActionSemantics> _actionSemantics;

}