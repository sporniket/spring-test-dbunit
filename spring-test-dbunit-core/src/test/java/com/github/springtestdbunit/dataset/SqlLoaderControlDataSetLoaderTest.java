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
 *
 */
package com.github.springtestdbunit.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.dbunit.dataset.IDataSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestContext;

import com.github.springtestdbunit.testutils.ExtendedTestContextManager;

/**
 * Tests for {@link SqlLoaderControlDataSetLoader}.
 *
 * @author Paul Podgorsek
 */
public class SqlLoaderControlDataSetLoaderTest {

	private TestContext testContext;

	private SqlLoaderControlDataSetLoader loader;

	@BeforeEach
	public void setup() throws Exception {
		loader = new SqlLoaderControlDataSetLoader();
		ExtendedTestContextManager manager = new ExtendedTestContextManager(getClass());
		testContext = manager.accessTestContext();
	}

	@Test
	public void shouldLoadFromRelativeFile() throws Exception {
		IDataSet dataset = loader.loadDataSet(testContext.getTestClass(), "sql/");
		assertEquals(2, dataset.getTableNames().length, "Wrong number of tables");
		assertEquals("Sample_2", dataset.getTableNames()[0]);
		assertEquals("Sample_1", dataset.getTableNames()[1]);
	}

	@Test
	public void shouldReturnNullOnMissingFile() throws Exception {
		IDataSet dataset = loader.loadDataSet(testContext.getTestClass(), "doesnotexist/");
		assertNull(dataset);
	}

}
