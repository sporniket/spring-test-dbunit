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

package com.github.springtestdbunit.bean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.Set;

import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DefaultMetadataHandler;
import org.dbunit.database.IMetadataHandler;
import org.dbunit.database.IResultSetTableFactory;
import org.dbunit.database.statement.IStatementFactory;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.dataset.filter.IColumnFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * Tests for {@link DatabaseConfigBean}.
 *
 * @author Phillip Webb
 */
public class DatabaseConfigBeanTest {

	private static final Set<Class<?>> CLASS_COMPARE_ONLY;

	static {
		CLASS_COMPARE_ONLY = new HashSet<Class<?>>();
		CLASS_COMPARE_ONLY.add(DefaultMetadataHandler.class);
	}

	private DatabaseConfig defaultConfig = new DatabaseConfig();

	private DatabaseConfigBean configBean;

	private BeanWrapper configBeanWrapper;

	@BeforeEach
	public void setup() {
		configBean = new DatabaseConfigBean();
		configBeanWrapper = new BeanWrapperImpl(configBean);
	}

	@Test
	public void shouldAllowSetOfNonMandatoryFieldToNull() throws Exception {
		configBean.setPrimaryKeyFilter(null);
	}

	@Test
	public void shouldFailWhenSetingMandatoryFieldToNull() throws Exception {
		try {
			configBean.setDatatypeFactory(null);
			fail();
		} catch (IllegalArgumentException ex) {
			assertEquals("dataTypeFactory cannot be null", ex.getMessage());
		}
	}

	@Test
	public void testStatementFactory() throws Exception {
		doTest("statementFactory", DatabaseConfig.PROPERTY_STATEMENT_FACTORY, mock(IStatementFactory.class));
	}

	@Test
	public void testResultsetTableFactory() {
		doTest("resultsetTableFactory", DatabaseConfig.PROPERTY_RESULTSET_TABLE_FACTORY,
				mock(IResultSetTableFactory.class));
	}

	@Test
	public void testDatatypeFactory() {
		doTest("datatypeFactory", DatabaseConfig.PROPERTY_DATATYPE_FACTORY, mock(IDataTypeFactory.class));
	}

	@Test
	public void testEscapePattern() {
		doTest("escapePattern", DatabaseConfig.PROPERTY_ESCAPE_PATTERN, "test");
	}

	@Test
	public void testTableType() {
		doTest("tableType", DatabaseConfig.PROPERTY_TABLE_TYPE, new String[] { "test" });
	}

	@Test
	public void testPrimaryKeyFilter() {
		doTest("primaryKeyFilter", DatabaseConfig.PROPERTY_PRIMARY_KEY_FILTER, mock(IColumnFilter.class));
	}

	@Test
	public void testBatchSize() {
		doTest("batchSize", DatabaseConfig.PROPERTY_BATCH_SIZE, Integer.valueOf(123));
	}

	@Test
	public void testFetchSize() {
		doTest("fetchSize", DatabaseConfig.PROPERTY_FETCH_SIZE, Integer.valueOf(123));
	}

	@Test
	public void testMetadataHandler() {
		doTest("metadataHandler", DatabaseConfig.PROPERTY_METADATA_HANDLER, mock(IMetadataHandler.class));
	}

	@Test
	public void testCaseSensitiveTableNames() {
		doTest("caseSensitiveTableNames", DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, Boolean.TRUE);
	}

	@Test
	public void testQualifiedTableNames() {
		doTest("qualifiedTableNames", DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, Boolean.TRUE);
	}

	@Test
	public void testBatchedStatements() {
		doTest("batchedStatements", DatabaseConfig.FEATURE_BATCHED_STATEMENTS, Boolean.TRUE);
	}

	@Test
	public void testDatatypeWarning() {
		doTest("datatypeWarning", DatabaseConfig.FEATURE_DATATYPE_WARNING, Boolean.FALSE);
	}

	@Test
	public void testSkipOracleRecyclebinTables() {
		doTest("skipOracleRecyclebinTables", DatabaseConfig.FEATURE_SKIP_ORACLE_RECYCLEBIN_TABLES, Boolean.FALSE);
	}

	private void doTest(String propertyName, String databaseConfigProperty, Object newValue) {
		Object initialValue = configBeanWrapper.getPropertyValue(propertyName);
		Object expectedInitialValue = defaultConfig.getProperty(databaseConfigProperty);

		if ((initialValue != null) && CLASS_COMPARE_ONLY.contains(initialValue.getClass())) {
			assertEquals(initialValue.getClass(), expectedInitialValue.getClass(), "Initial value is not as expected");

		} else {
			assertEquals(initialValue, expectedInitialValue, "Initial value is not as expected");
		}

		assertFalse(newValue.equals(initialValue), "Unable to test if new value is same as intial value");
		configBeanWrapper.setPropertyValue(propertyName, newValue);
		DatabaseConfig appliedConfig = new DatabaseConfig();
		configBean.apply(appliedConfig);

		assertEquals(newValue, appliedConfig.getProperty(databaseConfigProperty),
				"Did not replace " + propertyName + " value");

	}

}
