/*
 * Copyright 2002-2016 the original author or authors
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

package com.github.springtestdbunit.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.dbunit.dataset.IDataSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestContext;

import com.github.springtestdbunit.testutils.ExtendedTestContextManager;

/**
 * Tests for {@link FlatXmlDataSetLoader}.
 *
 * @author Phillip Webb
 */
public class FlatXmlDataSetLoaderTest {

	private TestContext testContext;

	private FlatXmlDataSetLoader loader;

	@BeforeEach
	public void setup() throws Exception {
		loader = new FlatXmlDataSetLoader();
		ExtendedTestContextManager manager = new ExtendedTestContextManager(getClass());
		testContext = manager.accessTestContext();
	}

	@Test
	public void shouldSenseColumns() throws Exception {
		IDataSet dataset = loader.loadDataSet(testContext.getTestClass(), "test-column-sensing.xml");
		assertEquals(null, dataset.getTable("Sample").getValue(0, "name"));
		assertEquals("test", dataset.getTable("Sample").getValue(1, "name"));
	}

	@Test
	public void shouldLoadFromRelativeFile() throws Exception {
		IDataSet dataset = loader.loadDataSet(testContext.getTestClass(), "test.xml");
		assertEquals("Sample", dataset.getTableNames()[0]);
	}

	@Test
	public void shouldReturnNullOnMissingFile() throws Exception {
		IDataSet dataset = loader.loadDataSet(testContext.getTestClass(), "doesnotexist.xml");
		assertNull(dataset);
	}

}
