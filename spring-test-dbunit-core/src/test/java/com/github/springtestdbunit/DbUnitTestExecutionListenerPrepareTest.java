/*
 * Copyright 2002-2016 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.springtestdbunit ;

import static org.junit.jupiter.api.Assertions.assertEquals ;
import static org.junit.jupiter.api.Assertions.assertSame ;
import static org.junit.jupiter.api.Assertions.assertTrue ;
import static org.mockito.ArgumentMatchers.any ;
import static org.mockito.ArgumentMatchers.eq ;
import static org.mockito.BDDMockito.given ;
import static org.mockito.Mockito.mock ;
import static org.mockito.Mockito.verify ;
import static org.mockito.Mockito.verifyNoMoreInteractions ;

import javax.sql.DataSource ;

import org.dbunit.database.DatabaseDataSourceConnection ;
import org.dbunit.database.IDatabaseConnection ;
import org.dbunit.dataset.IDataSet ;
import org.junit.jupiter.api.BeforeEach ;
import org.junit.jupiter.api.Test ;
import org.springframework.context.ApplicationContext ;
import org.springframework.test.context.ContextConfiguration ;
import org.springframework.test.context.ContextLoader ;
import org.springframework.test.context.TestExecutionListeners ;

import com.github.springtestdbunit.annotation.DatabaseOperation ;
import com.github.springtestdbunit.annotation.DbUnitConfiguration ;
import com.github.springtestdbunit.dataset.DataSetLoader ;
import com.github.springtestdbunit.dataset.FlatXmlDataSetLoader ;
import com.github.springtestdbunit.operation.DatabaseOperationLookup ;
import com.github.springtestdbunit.operation.DefaultDatabaseOperationLookup ;
import com.github.springtestdbunit.testutils.ExtendedTestContextManager ;

/**
 * Tests for {@link DbUnitTestExecutionListener} prepare method.
 *
 * @author Phillip Webb
 */
public class DbUnitTestExecutionListenerPrepareTest {

    private static ThreadLocal<ApplicationContext> applicationContextThreadLocal = new ThreadLocal<>() ;

    private ApplicationContext applicationContext ;

    private IDatabaseConnection databaseConnection ;

    private DataSource dataSource ;

    @BeforeEach
    public void setup() {
        applicationContext = mock(ApplicationContext.class) ;
        databaseConnection = mock(IDatabaseConnection.class) ;
        dataSource = mock(DataSource.class) ;
        DbUnitTestExecutionListenerPrepareTest.applicationContextThreadLocal.set(applicationContext) ;
    }

    @SuppressWarnings("unchecked")
    private void addBean(String beanName, Object bean) {
        given(applicationContext.containsBean(beanName)).willReturn(true) ;
        given(applicationContext.getBean(beanName)).willReturn(bean) ;
        given(applicationContext.getBean(eq(beanName), (Class) any())).willReturn(bean) ;
    }

    @Test
    public void shouldUseSensibleDefaultsOnClassWithNoDbUnitConfiguration() throws Exception {
        addBean("dbUnitDatabaseConnection", databaseConnection) ;
        final ExtendedTestContextManager testContextManager = new ExtendedTestContextManager(NoDbUnitConfiguration.class) ;
        testContextManager.prepareTestInstance() ;
        final DatabaseConnections databaseConnections = (DatabaseConnections) testContextManager.getTestContextAttribute(DbUnitTestContextConstants.CONNECTION_ATTRIBUTE) ;
        assertSame(databaseConnection, databaseConnections.get("dbUnitDatabaseConnection")) ;
        assertEquals(FlatXmlDataSetLoader.class, testContextManager.getTestContextAttribute(DbUnitTestContextConstants.DATA_SET_LOADER_ATTRIBUTE).getClass()) ;
        assertEquals(DefaultDatabaseOperationLookup.class, testContextManager.getTestContextAttribute(DbUnitTestContextConstants.DATABASE_OPERATION_LOOKUP_ATTRIBUTE).getClass()) ;
    }

    @Test
    public void shouldTryBeanFactoryForCommonBeanNamesWithNoDbUnitConfiguration() throws Exception {
        testCommonBeanNames(NoDbUnitConfiguration.class, false) ;
    }

    @Test
    public void shouldTryBeanFactoryForCommonBeanNamesWithEmptyDatabaseConnection() throws Exception {
        testCommonBeanNames(EmptyDbUnitConfiguration.class) ;
    }

    private void testCommonBeanNames(Class<?> testClass) throws Exception {
        testCommonBeanNames(testClass, true) ;
    }

    private void testCommonBeanNames(Class<?> testClass, boolean shouldTestForDbUnitRunnerConfig) throws Exception {
        addBean("dataSource", dataSource) ;
        final ExtendedTestContextManager testContextManager = new ExtendedTestContextManager(testClass) ;
        testContextManager.prepareTestInstance() ;
        verify(applicationContext).containsBean("dbUnitDataSetLoader") ;
        verify(applicationContext).containsBean("dbUnitDatabaseConnection") ;
        verify(applicationContext).containsBean("dataSource") ;
        verify(applicationContext).getBean("dataSource") ;
        if (shouldTestForDbUnitRunnerConfig) {
            verify(applicationContext).containsBean("dbUnitRunnerConfig") ;
        }
        verifyNoMoreInteractions(applicationContext) ;
    }

    @Test
    public void shouldConvertDatasetDatabaseConnection() throws Exception {
        addBean("dataSource", dataSource) ;
        final ExtendedTestContextManager testContextManager = new ExtendedTestContextManager(NoDbUnitConfiguration.class) ;
        testContextManager.prepareTestInstance() ;
        final DatabaseConnections databaseConnections = (DatabaseConnections) testContextManager.getTestContextAttribute(DbUnitTestContextConstants.CONNECTION_ATTRIBUTE) ;
        final Object connection = databaseConnections.get("dataSource") ;
        assertEquals(DatabaseDataSourceConnection.class, connection.getClass()) ;
    }

    @Test
    public void shouldFailIfNoDbConnectionBeanIsFound() throws Exception {
        final ExtendedTestContextManager testContextManager = new ExtendedTestContextManager(NoDbUnitConfiguration.class) ;
        try {
            testContextManager.prepareTestInstance() ;
        } catch (final IllegalStateException ex) {
            assertTrue(ex.getMessage().startsWith("Unable to find a DB Unit database connection")) ;
        }
    }

    @Test
    public void shouldFailIfDatabaseConnectionOfWrongTypeIsFound() throws Exception {
        addBean("dbUnitDatabaseConnection", Integer.valueOf(0)) ;
        final ExtendedTestContextManager testContextManager = new ExtendedTestContextManager(NoDbUnitConfiguration.class) ;
        try {
            testContextManager.prepareTestInstance() ;
        } catch (final IllegalArgumentException ex) {
            assertEquals("Object of class [java.lang.Integer] must be an instance of interface " + "org.dbunit.database.IDatabaseConnection", ex.getMessage()) ;
        }
    }

    @Test
    public void shouldSupportAllDbUnitConfigurationAttributes() throws Exception {
        addBean("customBean", databaseConnection) ;
        final ExtendedTestContextManager testContextManager = new ExtendedTestContextManager(CustomConfiguration.class) ;
        testContextManager.prepareTestInstance() ;
        verify(applicationContext).getBean("customBean") ;
        final DatabaseConnections databaseConnections = (DatabaseConnections) testContextManager.getTestContextAttribute(DbUnitTestContextConstants.CONNECTION_ATTRIBUTE) ;
        assertSame(databaseConnection, databaseConnections.get("customBean")) ;
        assertEquals(CustomDataSetLoader.class, testContextManager.getTestContextAttribute(DbUnitTestContextConstants.DATA_SET_LOADER_ATTRIBUTE).getClass()) ;
        assertEquals(CustomDatabaseOperationLookup.class, testContextManager.getTestContextAttribute(DbUnitTestContextConstants.DATABASE_OPERATION_LOOKUP_ATTRIBUTE).getClass()) ;
    }

    @Test
    public void shouldFailIfDatasetLoaderCannotBeCreated() throws Exception {
        addBean("dbUnitDatabaseConnection", databaseConnection) ;
        final ExtendedTestContextManager testContextManager = new ExtendedTestContextManager(NonCreatableDataSetLoader.class) ;
        try {
            testContextManager.prepareTestInstance() ;
        } catch (final IllegalArgumentException ex) {
            assertEquals(
                    "Unable to create data set loader instance for class " + "com.github.springtestdbunit.DbUnitTestExecutionListenerPrepareTest$" + "AbstractCustomDataSetLoader",
                    ex.getMessage()) ;
        }
    }

    @Test
    public void shouldSupportCustomLoaderBean() throws Exception {
        addBean("dataSource", dataSource) ;
        addBean("dbUnitDataSetLoader", new CustomDataSetLoader()) ;
        final ExtendedTestContextManager testContextManager = new ExtendedTestContextManager(EmptyDbUnitConfiguration.class) ;
        testContextManager.prepareTestInstance() ;
        assertEquals(CustomDataSetLoader.class, testContextManager.getTestContextAttribute(DbUnitTestContextConstants.DATA_SET_LOADER_ATTRIBUTE).getClass()) ;
    }

    private static class LocalApplicationContextLoader implements ContextLoader {

        @Override
        public String[] processLocations(Class<?> clazz, String... locations) {
            return new String[] {
                    "mock"
            } ;
        }

        @Override
        public ApplicationContext loadContext(String... locations) throws Exception {
            return applicationContextThreadLocal.get() ;
        }
    }

    public abstract static class AbstractCustomDataSetLoader implements DataSetLoader {

        @Override
        public IDataSet loadDataSet(Class<?> testClass, String location) {
            return null ;
        }
    }

    public static class CustomDataSetLoader extends AbstractCustomDataSetLoader {
    }

    public static class CustomDatabaseOperationLookup implements DatabaseOperationLookup {

        @Override
        public org.dbunit.operation.DatabaseOperation get(DatabaseOperation operation) {
            return null ;
        }
    }

    @ContextConfiguration(loader = LocalApplicationContextLoader.class)
    @TestExecutionListeners(DbUnitTestExecutionListener.class)
    private static class NoDbUnitConfiguration {

    }

    @ContextConfiguration(loader = LocalApplicationContextLoader.class)
    @TestExecutionListeners(DbUnitTestExecutionListener.class)
    @DbUnitConfiguration
    private static class EmptyDbUnitConfiguration {

    }

    @ContextConfiguration(loader = LocalApplicationContextLoader.class)
    @TestExecutionListeners(DbUnitTestExecutionListener.class)
    @DbUnitConfiguration(databaseConnection = "customBean", dataSetLoader = CustomDataSetLoader.class, databaseOperationLookup = CustomDatabaseOperationLookup.class)
    private static class CustomConfiguration {

    }

    @ContextConfiguration(loader = LocalApplicationContextLoader.class)
    @TestExecutionListeners(DbUnitTestExecutionListener.class)
    @DbUnitConfiguration(dataSetLoader = AbstractCustomDataSetLoader.class)
    private static class NonCreatableDataSetLoader {

    }

}
