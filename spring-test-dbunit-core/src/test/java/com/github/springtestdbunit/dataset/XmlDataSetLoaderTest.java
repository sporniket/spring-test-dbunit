package com.github.springtestdbunit.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.dbunit.dataset.IDataSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestContext;

import com.github.springtestdbunit.testutils.ExtendedTestContextManager;

/**
 * Tests for {@link XmlDataSetLoader}.
 *
 * @author Phillip Webb
 */
public class XmlDataSetLoaderTest {

	private TestContext testContext;

	private XmlDataSetLoader loader;

	@BeforeEach
	public void setup() throws Exception {
		loader = new XmlDataSetLoader();
		ExtendedTestContextManager manager = new ExtendedTestContextManager(getClass());
		testContext = manager.accessTestContext();
	}

	@Test
	public void shouldLoadFromRelativeFile() throws Exception {
		IDataSet dataset = loader.loadDataSet(testContext.getTestClass(), "non-flat-xmldataset.xml");
		assertEquals("Sample", dataset.getTableNames()[0]);
	}

	@Test
	public void shouldReturnNullOnMissingFile() throws Exception {
		IDataSet dataset = loader.loadDataSet(testContext.getTestClass(), "doesnotexist.xml");
		assertNull(dataset);
	}

}
