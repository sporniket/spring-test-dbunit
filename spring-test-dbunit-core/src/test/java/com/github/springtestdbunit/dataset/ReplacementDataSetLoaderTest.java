package com.github.springtestdbunit.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestContext;

import com.github.springtestdbunit.testutils.ExtendedTestContextManager;

/**
 * Tests for {@link ReplacementDataSetLoader}.
 *
 * @author Stijn Van Bael
 */
public class ReplacementDataSetLoaderTest {

	private TestContext testContext;

	private ReplacementDataSetLoader loader;

	@BeforeEach
	public void setup() throws Exception {
		loader = new ReplacementDataSetLoader();
		ExtendedTestContextManager manager = new ExtendedTestContextManager(getClass());
		testContext = manager.accessTestContext();
	}

	@Test
	public void shouldReplaceNulls() throws Exception {
		IDataSet dataset = loader.loadDataSet(testContext.getTestClass(), "test-replacement.xml");
		assertEquals("Sample", dataset.getTableNames()[0]);
		ITable table = dataset.getTable("Sample");
		assertEquals(1, table.getRowCount());
		assertNull(table.getValue(0, "value"));
	}

}
