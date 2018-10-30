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

import static com.liferay.apio.architect.internal.wiring.osgi.manager.cache.ManagerCache.INSTANCE;

import static org.slf4j.LoggerFactory.getLogger;

import com.liferay.apio.architect.internal.routes.CollectionRoutesImpl;
import com.liferay.apio.architect.internal.routes.CollectionRoutesImpl.BuilderImpl;
import com.liferay.apio.architect.internal.wiring.osgi.manager.base.ClassNameBaseManager;
import com.liferay.apio.architect.internal.wiring.osgi.manager.representable.NameManager;
import com.liferay.apio.architect.internal.wiring.osgi.manager.representable.RepresentableManager;
import com.liferay.apio.architect.internal.wiring.osgi.manager.uri.mapper.PathIdentifierMapperManager;
import com.liferay.apio.architect.representor.Representor;
import com.liferay.apio.architect.router.CollectionRouter;
import com.liferay.apio.architect.routes.CollectionRoutes;
import com.liferay.apio.architect.routes.CollectionRoutes.Builder;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.slf4j.Logger;

/**
 * Provides methods to retrieve the routes information provided by the different
 * {@link CollectionRouter} instances.
 *
 * @author Alejandro Hernández
 * @see    CollectionRouter
 */
@Component(service = CollectionRouterManager.class)
public class CollectionRouterManager
	extends ClassNameBaseManager<CollectionRouter> {

	public CollectionRouterManager() {
		super(CollectionRouter.class, 2);
	}

	public Stream<ActionSemantics> getActionSemantics() {
		Map<String, CollectionRoutes> collectionRoutes = getCollectionRoutes();

		Collection<CollectionRoutes> values = collectionRoutes.values();

		Stream<CollectionRoutes> stream = values.stream();

		return stream.map(
			CollectionRoutesImpl.class::cast
		).map(
			CollectionRoutesImpl::getActionSemantics
		).flatMap(
			Collection::stream
		);
	}

	public Map<String, CollectionRoutes> getCollectionRoutes() {
		return INSTANCE.getCollectionRoutes(this::_computeCollectionRoutes);
	}

	private void _computeCollectionRoutes() {
		forEachService(
			(className, collectionRouter) -> {
				Optional<String> nameOptional = _nameManager.getNameOptional(
					className);

				if (!nameOptional.isPresent()) {
					_logger.warn(
						"Unable to find a Representable for class name {}",
						className);

					return;
				}

				String name = nameOptional.get();

				Optional<Representor<Object>> representorOptional =
					_representableManager.getRepresentorOptional(name);

				if (!representorOptional.isPresent()) {
					_logger.warn(
						"Unable to find a Representable for class name {}",
						className);

					return;
				}

				Representor<Object> representor = representorOptional.get();

				Builder builder = new BuilderImpl<>(
					Resource.paged(name),
					_pathIdentifierMapperManager::mapToIdentifierOrFail,
					representor::getIdentifier, _nameManager::getNameOptional);

				@SuppressWarnings("unchecked")
				CollectionRoutes collectionRoutes =
					collectionRouter.collectionRoutes(builder);

				INSTANCE.putRootResourceNameSdk(name);
				INSTANCE.putCollectionRoutes(name, collectionRoutes);
			});
	}

	private Logger _logger = getLogger(getClass());

	@Reference
	private NameManager _nameManager;

	@Reference
	private PathIdentifierMapperManager _pathIdentifierMapperManager;

	@Reference
	private RepresentableManager _representableManager;

}