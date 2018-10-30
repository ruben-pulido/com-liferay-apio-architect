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

package com.liferay.apio.architect.test.util.internal.writer;

import static com.liferay.apio.architect.internal.annotation.ActionKey.ANY_ROUTE;
import static com.liferay.apio.architect.test.util.writer.MockWriterUtil.getRequestInfo;

import com.liferay.apio.architect.documentation.contributor.CustomDocumentation;
import com.liferay.apio.architect.function.throwable.ThrowableTriFunction;
import com.liferay.apio.architect.internal.annotation.ActionKey;
import com.liferay.apio.architect.internal.annotation.ActionManager;
import com.liferay.apio.architect.internal.annotation.ActionManagerImpl;
import com.liferay.apio.architect.internal.documentation.Documentation;
import com.liferay.apio.architect.internal.documentation.contributor.CustomDocumentationImpl;
import com.liferay.apio.architect.internal.message.json.DocumentationMessageMapper;
import com.liferay.apio.architect.internal.wiring.osgi.manager.uri.mapper.PathIdentifierMapperManager;
import com.liferay.apio.architect.internal.writer.DocumentationWriter;
import com.liferay.apio.architect.representor.Representor;
import com.liferay.apio.architect.test.util.model.RootModel;
import com.liferay.apio.architect.test.util.representor.MockRepresentorCreator;
import com.liferay.apio.architect.uri.Path;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Provides methods that test {@code DocumentationMessageMapper} objects.
 *
 * <p>
 * This class shouldn't be instantiated.
 * </p>
 *
 * @author Alejandro Hernández
 */
public class MockDocumentationWriter {

	/**
	 * Writes a {@code Documentation} object.
	 *
	 * @param  documentationMessageMapper the {@code DocumentationMessageMapper}
	 *         to use for writing the JSON object
	 * @return the string containing the JSON object
	 */
	public static String write(
		DocumentationMessageMapper documentationMessageMapper) {

		CustomDocumentation.Builder customDocumentationBuilder =
			new CustomDocumentationImpl.BuilderImpl();

		customDocumentationBuilder.addDescription(
			"binary1", "binary description");
		customDocumentationBuilder.addLocalizedDescription(
			"root/retrieve", __ -> "retrieve description");

		Representor<RootModel> rootModelRepresentor =
			MockRepresentorCreator.createRootModelRepresentor(false);

		Map<String, Representor> root = Collections.singletonMap(
			"root", rootModelRepresentor);

		ActionManager actionManager = ActionManagerImpl.newTestInstance(
			new PathIdentifierMapperManager() {

				@Override
				public boolean hasPathIdentifierMapper(String name) {
					return false;
				}

				@Override
				public <T> T mapToIdentifierOrFail(Path path) {
					return null;
				}

				@Override
				public <T> Optional<Path> mapToPath(String name, T identifier) {
					return Optional.empty();
				}

			},
			null);

		CustomDocumentation customDocumentation =
			customDocumentationBuilder.build();

		Set<ActionKey> actionKeys = new HashSet<>();

		_addActions(actionManager, actionKeys);

		Documentation documentation = new Documentation(
			() -> Optional.of(() -> "Title"),
			() -> Optional.of(() -> "Description"),
			() -> Optional.of(() -> "Entrypoint"), () -> root, () -> actionKeys,
			actionKey -> actionManager.getActions(actionKey, null),
			() -> customDocumentation);

		DocumentationWriter documentationWriter = DocumentationWriter.create(
			builder -> builder.documentation(
				documentation
			).documentationMessageMapper(
				documentationMessageMapper
			).requestInfo(
				getRequestInfo()
			).build());

		return documentationWriter.write();
	}

	private static void _addAction(
		ActionKey actionKey, ActionManager actionManager,
		Set<ActionKey> actionKeys) {

		actionKeys.add(actionKey);
		actionManager.add(actionKey, _getEmptyThrowableTriFunction());
	}

	private static void _addActions(
		ActionManager actionManager, Set<ActionKey> actionKeys) {

		_addAction(new ActionKey("GET", "root"), actionManager, actionKeys);

		_addAction(
			new ActionKey("DELETE", "root", ANY_ROUTE), actionManager,
			actionKeys);

		_addAction(
			new ActionKey("GET", "root", ANY_ROUTE), actionManager, actionKeys);

		_addAction(
			new ActionKey("DELETE", "root", ANY_ROUTE), actionManager,
			actionKeys);
	}

	private static ThrowableTriFunction<Object, Object, List<Object>, Object>
		_getEmptyThrowableTriFunction() {

		return (id, body, providers) -> null;
	}

	private MockDocumentationWriter() {
		throw new UnsupportedOperationException();
	}

}