/*
 * Copyright 2019 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.springtestdbunit.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.github.springtestdbunit.DbUnitTestContext;
import com.github.springtestdbunit.DbUnitTestContextAdapter;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.AbstractDatabaseAnnotationAttributes;
import com.github.springtestdbunit.dataset.DataSetLoader;

/**
 * Utility class for dataset-related annotations.
 *
 * @author Paul Podgorsek
 */
public class DataSetAnnotationUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(DbUnitTestExecutionListener.class);

	/**
	 * Default private constructor to avoid instantiating this class.
	 */
	private DataSetAnnotationUtils() {
		super();
	}

	/**
	 * Fetches the dataset loader which has to be used to handle an annotation. If no dataset loader has been defined on
	 * the annotation, the global one from the test context will be used.
	 *
	 * @param testContext The test context.
	 * @param annotationAttributes The annotation.
	 * @return The dataset loader to use for the annotation.
	 */
	public static DataSetLoader getDataSetLoader(DbUnitTestContext testContext,
			AbstractDatabaseAnnotationAttributes annotationAttributes) {

		if (annotationAttributes != null) {
			if (StringUtils.hasLength(annotationAttributes.getDataSetLoaderBean())
					&& (testContext instanceof DbUnitTestContextAdapter)) {
				return ((DbUnitTestContextAdapter) testContext).getApplicationContext()
						.getBean(annotationAttributes.getDataSetLoaderBean(), DataSetLoader.class);
			} else {
				if ((annotationAttributes.getDataSetLoader() != null)
						&& (annotationAttributes.getDataSetLoader() != DataSetLoader.class)) {
					try {
						return annotationAttributes.getDataSetLoader().newInstance();
					} catch (Exception ex) {
						LOGGER.warn(
								"Unable to create data set loader instance for "
										+ annotationAttributes.getDataSetLoader() + ". Using the global one instead.",
								ex);
					}
				}
			}
		}

		return testContext.getDataSetLoader();
	}

}
