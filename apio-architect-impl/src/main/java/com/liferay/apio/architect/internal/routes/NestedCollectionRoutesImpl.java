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

import com.liferay.apio.architect.alias.IdentifierFunction;
import com.liferay.apio.architect.alias.form.FormBuilderFunction;
import com.liferay.apio.architect.alias.routes.NestedBatchCreateItemFunction;
import com.liferay.apio.architect.alias.routes.NestedCreateItemFunction;
import com.liferay.apio.architect.alias.routes.NestedGetPageFunction;
import com.liferay.apio.architect.alias.routes.permission.HasNestedAddingPermissionFunction;
import com.liferay.apio.architect.batch.BatchResult;
import com.liferay.apio.architect.form.Body;
import com.liferay.apio.architect.form.Form;
import com.liferay.apio.architect.function.throwable.ThrowableBiFunction;
import com.liferay.apio.architect.function.throwable.ThrowableFunction;
import com.liferay.apio.architect.function.throwable.ThrowableHexaFunction;
import com.liferay.apio.architect.function.throwable.ThrowablePentaFunction;
import com.liferay.apio.architect.function.throwable.ThrowableTetraFunction;
import com.liferay.apio.architect.function.throwable.ThrowableTriFunction;
import com.liferay.apio.architect.internal.form.FormImpl;
import com.liferay.apio.architect.internal.pagination.PageImpl;
import com.liferay.apio.architect.internal.single.model.SingleModelImpl;
import com.liferay.apio.architect.internal.wiring.osgi.manager.router.ActionSemantics;
import com.liferay.apio.architect.internal.wiring.osgi.manager.router.Resource;
import com.liferay.apio.architect.pagination.Page;
import com.liferay.apio.architect.pagination.PageItems;
import com.liferay.apio.architect.pagination.Pagination;
import com.liferay.apio.architect.routes.NestedCollectionRoutes;
import com.liferay.apio.architect.single.model.SingleModel;
import com.liferay.apio.architect.uri.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Alejandro Hernández
 */
public class NestedCollectionRoutesImpl<T, S, U>
	implements NestedCollectionRoutes<T, S, U> {

	public NestedCollectionRoutesImpl(BuilderImpl<T, S, U> builderImpl) {
		_actionSemantics = builderImpl._actionSemantics;
	}

	public List<ActionSemantics> getActionSemantics() {
		return _actionSemantics;
	}

	@Override
	public Optional<Form> getFormOptional() {
		return Optional.empty();
	}

	@Override
	public Optional<NestedBatchCreateItemFunction<S, U>>
		getNestedBatchCreateItemFunctionOptional() {

		return Optional.empty();
	}

	@Override
	public Optional<NestedCreateItemFunction<T, U>>
		getNestedCreateItemFunctionOptional() {

		return Optional.empty();
	}

	@Override
	public Optional<NestedGetPageFunction<T, U>>
		getNestedGetPageFunctionOptional() {

		return Optional.empty();
	}

	public static class BuilderImpl<T, S, U> implements Builder<T, S, U> {

		public BuilderImpl(
			Resource.Nested nestedResource,
			Function<Path, ?> pathToIdentifierFunction,
			Function<T, S> modelToIdentifierFunction,
			Function<String, Optional<String>> nameFunction) {

			_nestedResource = nestedResource;

			_pathToIdentifierFunction = pathToIdentifierFunction::apply;

			_modelToIdentifierFunction = modelToIdentifierFunction;
			_nameFunction = nameFunction;
		}

		@Override
		public <R> Builder<T, S, U> addCreator(
			ThrowableBiFunction<U, R, T> creatorThrowableBiFunction,
			HasNestedAddingPermissionFunction<U>
				hasNestedAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			ThrowableBiFunction<U, List<R>, List<S>>
				batchCreatorThrowableBiFunction = (u, formList) ->
					_transformList(
						formList, r -> creatorThrowableBiFunction.apply(u, r));

			return addCreator(
				creatorThrowableBiFunction, batchCreatorThrowableBiFunction,
				hasNestedAddingPermissionFunction, formBuilderFunction);
		}

		@Override
		public <R> Builder<T, S, U> addCreator(
			ThrowableBiFunction<U, R, T> creatorThrowableBiFunction,
			ThrowableBiFunction<U, List<R>, List<S>>
				batchCreatorThrowableBiFunction,
			HasNestedAddingPermissionFunction<U>
				hasNestedAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			String parentName = _nestedResource.getParentName();
			String nestedName = _nestedResource.getName();

			Form<R> form = formBuilderFunction.apply(
				new FormImpl.BuilderImpl<>(
					asList("c", parentName, nestedName),
					_pathToIdentifierFunction, _nameFunction));

			ActionSemantics batchCreateActionSemantics =
				ActionSemantics.ofResource(
					_nestedResource
				).name(
					"batch-create"
				).method(
					"POST"
				).provideNothing(
				).returns(
					BatchResult.class
				).receivesListOf(
					form
				).execute(
					params -> batchCreatorThrowableBiFunction.andThen(
						t -> new BatchResult<>(t, nestedName)
					).apply(
						unsafeCast(params.get(1)),
						form.getList((Body)params.get(0))
					)
				);

			_actionSemantics.add(batchCreateActionSemantics);

			ActionSemantics createActionSemantics = ActionSemantics.ofResource(
				_nestedResource
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
				params -> creatorThrowableBiFunction.andThen(
					t -> new SingleModelImpl<>(t, nestedName, emptyList())
				).apply(
					unsafeCast(params.get(1)), form.get((Body)params.get(0))
				)
			);

			_actionSemantics.add(createActionSemantics);

			return this;
		}

		@Override
		public <A, B, C, D, R> Builder<T, S, U> addCreator(
			ThrowableHexaFunction<U, R, A, B, C, D, T>
				creatorThrowableHexaFunction,
			Class<A> aClass, Class<B> bClass, Class<C> cClass, Class<D> dClass,
			HasNestedAddingPermissionFunction<U>
				hasNestedAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			ThrowableHexaFunction<U, List<R>, A, B, C, D, List<S>>
				batchCreatorThrowableHexaFunction = (u, formList, a, b, c, d) ->
					_transformList(
						formList,
						r -> creatorThrowableHexaFunction.apply(
							u, r, a, b, c, d));

			return addCreator(
				creatorThrowableHexaFunction, batchCreatorThrowableHexaFunction,
				aClass, bClass, cClass, dClass,
				hasNestedAddingPermissionFunction, formBuilderFunction);
		}

		@Override
		public <A, B, C, D, R> Builder<T, S, U> addCreator(
			ThrowableHexaFunction<U, R, A, B, C, D, T>
				creatorThrowableHexaFunction,
			ThrowableHexaFunction<U, List<R>, A, B, C, D, List<S>>
				batchCreatorThrowableHexaFunction,
			Class<A> aClass, Class<B> bClass, Class<C> cClass, Class<D> dClass,
			HasNestedAddingPermissionFunction<U>
				hasNestedAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			String parentName = _nestedResource.getParentName();
			String nestedName = _nestedResource.getName();

			Form<R> form = formBuilderFunction.apply(
				new FormImpl.BuilderImpl<>(
					asList("c", parentName, nestedName),
					_pathToIdentifierFunction, _nameFunction));

			ActionSemantics batchCreateActionSemantics =
				ActionSemantics.ofResource(
					_nestedResource
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
					params -> batchCreatorThrowableHexaFunction.andThen(
						t -> new BatchResult<>(t, nestedName)
					).apply(
						unsafeCast(params.get(5)),
						form.getList((Body)params.get(4)),
						unsafeCast(params.get(0)), unsafeCast(params.get(1)),
						unsafeCast(params.get(2)), unsafeCast(params.get(3))
					)
				);

			_actionSemantics.add(batchCreateActionSemantics);

			ActionSemantics createActionSemantics = ActionSemantics.ofResource(
				_nestedResource
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
				params -> creatorThrowableHexaFunction.andThen(
					t -> new SingleModelImpl<>(t, nestedName, emptyList())
				).apply(
					unsafeCast(params.get(5)), form.get((Body)params.get(4)),
					unsafeCast(params.get(0)), unsafeCast(params.get(1)),
					unsafeCast(params.get(2)), unsafeCast(params.get(3))
				)
			);

			_actionSemantics.add(createActionSemantics);

			return this;
		}

		@Override
		public <A, B, C, R> Builder<T, S, U> addCreator(
			ThrowablePentaFunction<U, R, A, B, C, T>
				creatorThrowablePentaFunction,
			Class<A> aClass, Class<B> bClass, Class<C> cClass,
			HasNestedAddingPermissionFunction<U>
				hasNestedAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			ThrowablePentaFunction<U, List<R>, A, B, C, List<S>>
				batchCreatorThrowablePentaFunction = (u, formList, a, b, c) ->
					_transformList(
						formList,
						r -> creatorThrowablePentaFunction.apply(
							u, r, a, b, c));

			return addCreator(
				creatorThrowablePentaFunction,
				batchCreatorThrowablePentaFunction, aClass, bClass, cClass,
				hasNestedAddingPermissionFunction, formBuilderFunction);
		}

		@Override
		public <A, B, C, R> Builder<T, S, U> addCreator(
			ThrowablePentaFunction<U, R, A, B, C, T>
				creatorThrowablePentaFunction,
			ThrowablePentaFunction<U, List<R>, A, B, C, List<S>>
				batchCreatorThrowablePentaFunction,
			Class<A> aClass, Class<B> bClass, Class<C> cClass,
			HasNestedAddingPermissionFunction<U>
				hasNestedAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			String parentName = _nestedResource.getParentName();
			String nestedName = _nestedResource.getName();

			Form<R> form = formBuilderFunction.apply(
				new FormImpl.BuilderImpl<>(
					asList("c", parentName, nestedName),
					_pathToIdentifierFunction, _nameFunction));

			ActionSemantics batchCreateActionSemantics =
				ActionSemantics.ofResource(
					_nestedResource
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
					params -> batchCreatorThrowablePentaFunction.andThen(
						t -> new BatchResult<>(t, nestedName)
					).apply(
						unsafeCast(params.get(4)),
						form.getList((Body)params.get(3)),
						unsafeCast(params.get(0)), unsafeCast(params.get(1)),
						unsafeCast(params.get(2))
					)
				);

			_actionSemantics.add(batchCreateActionSemantics);

			ActionSemantics createActionSemantics = ActionSemantics.ofResource(
				_nestedResource
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
				params -> creatorThrowablePentaFunction.andThen(
					t -> new SingleModelImpl<>(t, nestedName, emptyList())
				).apply(
					unsafeCast(params.get(4)), form.get((Body)params.get(3)),
					unsafeCast(params.get(0)), unsafeCast(params.get(1)),
					unsafeCast(params.get(2))
				)
			);

			_actionSemantics.add(createActionSemantics);

			return this;
		}

		@Override
		public <A, B, R> Builder<T, S, U> addCreator(
			ThrowableTetraFunction<U, R, A, B, T> creatorThrowableTetraFunction,
			Class<A> aClass, Class<B> bClass,
			HasNestedAddingPermissionFunction<U>
				hasNestedAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			ThrowableTetraFunction<U, List<R>, A, B, List<S>>
				batchCreatorThrowableTetraFunction = (u, formList, a, b) ->
					_transformList(
						formList,
						r -> creatorThrowableTetraFunction.apply(u, r, a, b));

			return addCreator(
				creatorThrowableTetraFunction,
				batchCreatorThrowableTetraFunction, aClass, bClass,
				hasNestedAddingPermissionFunction, formBuilderFunction);
		}

		@Override
		public <A, B, R> Builder<T, S, U> addCreator(
			ThrowableTetraFunction<U, R, A, B, T> creatorThrowableTetraFunction,
			ThrowableTetraFunction<U, List<R>, A, B, List<S>>
				batchCreatorThrowableTetraFunction,
			Class<A> aClass, Class<B> bClass,
			HasNestedAddingPermissionFunction<U>
				hasNestedAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			String parentName = _nestedResource.getParentName();
			String nestedName = _nestedResource.getName();

			Form<R> form = formBuilderFunction.apply(
				new FormImpl.BuilderImpl<>(
					asList("c", parentName, nestedName),
					_pathToIdentifierFunction, _nameFunction));

			ActionSemantics batchCreateActionSemantics =
				ActionSemantics.ofResource(
					_nestedResource
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
					params -> batchCreatorThrowableTetraFunction.andThen(
						t -> new BatchResult<>(t, nestedName)
					).apply(
						unsafeCast(params.get(3)),
						form.getList((Body)params.get(2)),
						unsafeCast(params.get(0)), unsafeCast(params.get(1))
					)
				);

			_actionSemantics.add(batchCreateActionSemantics);

			ActionSemantics createActionSemantics = ActionSemantics.ofResource(
				_nestedResource
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
				params -> creatorThrowableTetraFunction.andThen(
					t -> new SingleModelImpl<>(t, nestedName, emptyList())
				).apply(
					unsafeCast(params.get(3)), form.get((Body)params.get(2)),
					unsafeCast(params.get(0)), unsafeCast(params.get(1))
				)
			);

			_actionSemantics.add(createActionSemantics);

			return this;
		}

		@Override
		public <A, R> Builder<T, S, U> addCreator(
			ThrowableTriFunction<U, R, A, T> creatorThrowableTriFunction,
			Class<A> aClass,
			HasNestedAddingPermissionFunction<U>
				hasNestedAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			ThrowableTriFunction<U, List<R>, A, List<S>>
				batchCreatorThrowableTriFunction = (u, formList, a) ->
					_transformList(
						formList,
						r -> creatorThrowableTriFunction.apply(u, r, a));

			return addCreator(
				creatorThrowableTriFunction, batchCreatorThrowableTriFunction,
				aClass, hasNestedAddingPermissionFunction, formBuilderFunction);
		}

		@Override
		public <A, R> Builder<T, S, U> addCreator(
			ThrowableTriFunction<U, R, A, T> creatorThrowableTriFunction,
			ThrowableTriFunction<U, List<R>, A, List<S>>
				batchCreatorThrowableTriFunction,
			Class<A> aClass,
			HasNestedAddingPermissionFunction<U>
				hasNestedAddingPermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			String parentName = _nestedResource.getParentName();
			String nestedName = _nestedResource.getName();

			Form<R> form = formBuilderFunction.apply(
				new FormImpl.BuilderImpl<>(
					asList("c", parentName, nestedName),
					_pathToIdentifierFunction, _nameFunction));

			ActionSemantics batchCreateActionSemantics =
				ActionSemantics.ofResource(
					_nestedResource
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
					params -> batchCreatorThrowableTriFunction.andThen(
						t -> new BatchResult<>(t, nestedName)
					).apply(
						unsafeCast(params.get(2)),
						form.getList((Body)params.get(1)),
						unsafeCast(params.get(0))
					)
				);

			_actionSemantics.add(batchCreateActionSemantics);

			ActionSemantics createActionSemantics = ActionSemantics.ofResource(
				_nestedResource
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
				params -> creatorThrowableTriFunction.andThen(
					t -> new SingleModelImpl<>(t, nestedName, emptyList())
				).apply(
					unsafeCast(params.get(2)), form.get((Body)params.get(1)),
					unsafeCast(params.get(0))
				)
			);

			_actionSemantics.add(createActionSemantics);

			return this;
		}

		@Override
		public Builder<T, S, U> addGetter(
			ThrowableBiFunction<Pagination, U, PageItems<T>>
				getterThrowableBiFunction) {

			ActionSemantics actionSemantics = ActionSemantics.ofResource(
				_nestedResource
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
				params -> getterThrowableBiFunction.andThen(
					pageItems -> new PageImpl<>(
						_nestedResource.getName(), pageItems,
						(Pagination)params.get(0), emptyList())
				).apply(
					(Pagination)params.get(0), unsafeCast(params.get(1))
				)
			);

			_actionSemantics.add(actionSemantics);

			return this;
		}

		@Override
		public <A, B, C, D> Builder<T, S, U> addGetter(
			ThrowableHexaFunction<Pagination, U, A, B, C, D, PageItems<T>>
				getterThrowableHexaFunction,
			Class<A> aClass, Class<B> bClass, Class<C> cClass,
			Class<D> dClass) {

			ActionSemantics actionSemantics = ActionSemantics.ofResource(
				_nestedResource
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
				params -> getterThrowableHexaFunction.andThen(
					pageItems -> new PageImpl<>(
						_nestedResource.getName(), pageItems,
						(Pagination)params.get(0), emptyList())
				).apply(
					(Pagination)params.get(0), unsafeCast(params.get(5)),
					unsafeCast(params.get(1)), unsafeCast(params.get(2)),
					unsafeCast(params.get(3)), unsafeCast(params.get(4))
				)
			);

			_actionSemantics.add(actionSemantics);

			return this;
		}

		@Override
		public <A, B, C> Builder<T, S, U> addGetter(
			ThrowablePentaFunction<Pagination, U, A, B, C, PageItems<T>>
				getterThrowablePentaFunction,
			Class<A> aClass, Class<B> bClass, Class<C> cClass) {

			ActionSemantics actionSemantics = ActionSemantics.ofResource(
				_nestedResource
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
				params -> getterThrowablePentaFunction.andThen(
					pageItems -> new PageImpl<>(
						_nestedResource.getName(), pageItems,
						(Pagination)params.get(0), emptyList())
				).apply(
					(Pagination)params.get(0), unsafeCast(params.get(4)),
					unsafeCast(params.get(1)), unsafeCast(params.get(2)),
					unsafeCast(params.get(3))
				)
			);

			_actionSemantics.add(actionSemantics);

			return this;
		}

		@Override
		public <A, B> Builder<T, S, U> addGetter(
			ThrowableTetraFunction<Pagination, U, A, B, PageItems<T>>
				getterThrowableTetraFunction,
			Class<A> aClass, Class<B> bClass) {

			ActionSemantics actionSemantics = ActionSemantics.ofResource(
				_nestedResource
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
				params -> getterThrowableTetraFunction.andThen(
					pageItems -> new PageImpl<>(
						_nestedResource.getName(), pageItems,
						(Pagination)params.get(0), emptyList())
				).apply(
					(Pagination)params.get(0), unsafeCast(params.get(3)),
					unsafeCast(params.get(1)), unsafeCast(params.get(2))
				)
			);

			_actionSemantics.add(actionSemantics);

			return this;
		}

		@Override
		public <A> Builder<T, S, U> addGetter(
			ThrowableTriFunction<Pagination, U, A, PageItems<T>>
				getterThrowableTriFunction,
			Class<A> aClass) {

			ActionSemantics actionSemantics = ActionSemantics.ofResource(
				_nestedResource
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
				params -> getterThrowableTriFunction.andThen(
					pageItems -> new PageImpl<>(
						_nestedResource.getName(), pageItems,
						(Pagination)params.get(0), emptyList())
				).apply(
					(Pagination)params.get(0), unsafeCast(params.get(2)),
					unsafeCast(params.get(1))
				)
			);

			_actionSemantics.add(actionSemantics);

			return this;
		}

		@Override
		public NestedCollectionRoutes<T, S, U> build() {
			return new NestedCollectionRoutesImpl<>(this);
		}

		private <V> List<S> _transformList(
				List<V> list,
				ThrowableFunction<V, T> transformThrowableFunction)
			throws Exception {

			List<S> newList = new ArrayList<>();

			for (V v : list) {
				S s = transformThrowableFunction.andThen(
					_modelToIdentifierFunction::apply
				).apply(
					v
				);

				newList.add(s);
			}

			return newList;
		}

		private final List<ActionSemantics> _actionSemantics =
			new ArrayList<>();
		private final Function<T, S> _modelToIdentifierFunction;
		private final Function<String, Optional<String>> _nameFunction;
		private final Resource.Nested _nestedResource;
		private final IdentifierFunction<?> _pathToIdentifierFunction;

	}

	private final List<ActionSemantics> _actionSemantics;

}