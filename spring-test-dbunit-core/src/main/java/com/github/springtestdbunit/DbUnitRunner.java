/*
 * Copyright 2002-2019 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.springtestdbunit ;

import java.io.IOException ;
import java.lang.reflect.InvocationTargetException ;
import java.sql.SQLException ;
import java.util.ArrayList ;
import java.util.Collection ;
import java.util.LinkedList ;
import java.util.List ;

import org.apache.commons.logging.Log ;
import org.apache.commons.logging.LogFactory ;
import org.dbunit.DatabaseUnitException ;
import org.dbunit.database.IDatabaseConnection ;
import org.dbunit.dataset.CompositeDataSet ;
import org.dbunit.dataset.DataSetException ;
import org.dbunit.dataset.IDataSet ;
import org.dbunit.dataset.ITable ;
import org.dbunit.dataset.filter.IColumnFilter ;
import org.springframework.util.Assert ;
import org.springframework.util.StringUtils ;

import com.github.springtestdbunit.annotation.AbstractDatabaseAnnotationAttributes ;
import com.github.springtestdbunit.annotation.Annotations ;
import com.github.springtestdbunit.annotation.DatabaseOperation ;
import com.github.springtestdbunit.annotation.DatabaseSetup ;
import com.github.springtestdbunit.annotation.DatabaseSetupTearDownAnnotationAttributes ;
import com.github.springtestdbunit.annotation.DatabaseSetups ;
import com.github.springtestdbunit.annotation.DatabaseTearDown ;
import com.github.springtestdbunit.annotation.DatabaseTearDowns ;
import com.github.springtestdbunit.annotation.ExpectedDatabase ;
import com.github.springtestdbunit.annotation.ExpectedDatabaseAnnotationAttributes ;
import com.github.springtestdbunit.annotation.ExpectedDatabases ;
import com.github.springtestdbunit.assertion.DatabaseAssertion ;
import com.github.springtestdbunit.bean.DbUnitRunnerConfigBean ;
import com.github.springtestdbunit.dataset.DataSetLoader ;
import com.github.springtestdbunit.dataset.DataSetModifier ;
import com.github.springtestdbunit.util.DataSetAnnotationUtils ;

/**
 * Internal delegate class used to run tests with support for {@link DatabaseSetup &#064;DatabaseSetup}, {@link DatabaseTearDown &#064;DatabaseTearDown} and {@link ExpectedDatabase
 * &#064;ExpectedDatabase} annotations.
 *
 * @author Phillip Webb
 * @author Mario Zagar
 * @author Sunitha Rajarathnam
 * @author Oleksii Lomako
 * @author Paul Podgorsek
 */
public class DbUnitRunner {

    private static final Log logger = LogFactory.getLog(DbUnitTestExecutionListener.class) ;

    private DbUnitRunnerConfigBean defaultConfigBean = null ;

    /**
     * Called before a test method is executed to perform any database setup.
     *
     * @param testContext The test context
     * @throws DatabaseUnitException If a dataset could not be imported into the database.
     * @throws IOException If a dataset could not be loaded.
     * @throws SQLException If the dataset corresponding to the entire database could not be loaded.
     */
    public void beforeTestMethod(DbUnitTestContext testContext) throws IOException, SQLException, DatabaseUnitException {
        final Annotations<DatabaseSetup> annotations = Annotations.get(testContext, DatabaseSetups.class, DatabaseSetup.class) ;
        setupOrTeardown(testContext, true, DatabaseSetupTearDownAnnotationAttributes.get(annotations)) ;
    }

    /**
     * Called after a test method is executed to perform any database teardown and to check expected results.
     *
     * @param testContext The test context
     * @throws SQLException An exception thrown if the test connections cannot be closed.
     * @throws DatabaseUnitException If a dataset could not be imported into the database.
     * @throws IOException An exception thrown if a dataset could not be loaded.
     * @throws IllegalAccessException If a database column filter could not be initialised.
     * @throws InstantiationException If a database column filter could not be initialised.
     * @throws SecurityException If a database column filter could not be initialised.
     * @throws NoSuchMethodException If a database column filter could not be initialised.
     * @throws InvocationTargetException If a database column filter could not be initialised.
     * @throws IllegalArgumentException If a database column filter could not be initialised.
     */
    public void afterTestMethod(DbUnitTestContext testContext) throws SQLException, IOException, DatabaseUnitException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

        try {
            verifyExpected(testContext, Annotations.get(testContext, ExpectedDatabases.class, ExpectedDatabase.class)) ;
        } finally {
            final Annotations<DatabaseTearDown> annotations = Annotations.get(testContext, DatabaseTearDowns.class, DatabaseTearDown.class) ;

            try {
                setupOrTeardown(testContext, false, DatabaseSetupTearDownAnnotationAttributes.get(annotations)) ;
            } catch (final RuntimeException ex) {
                if (testContext.getTestException() == null) {
                    throw ex ;
                }
                if (logger.isWarnEnabled()) {
                    logger.warn("Unable to throw database cleanup exception due to existing test error", ex) ;
                }
            } finally {
                testContext.getConnections().closeAll() ;
            }
        }
    }

    private void verifyExpected(DbUnitTestContext testContext, Annotations<ExpectedDatabase> annotations) throws DataSetException, SQLException, DatabaseUnitException,
            InstantiationException, IllegalAccessException, IOException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

        if (testContext.getTestException() != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Skipping @DatabaseTest expectation due to test exception " + testContext.getTestException().getClass()) ;
            }
            return ;
        }

        final DatabaseConnections connections = testContext.getConnections() ;
        final DataSetModifier modifier = getModifier(testContext, annotations) ;
        boolean override = false ;

        for (final ExpectedDatabase annotation : annotations.getMethodAnnotations()) {
            verifyExpected(testContext, connections, modifier, annotation) ;
            override |= annotation.override() ;
        }

        if (!override) {
            for (final ExpectedDatabase annotation : annotations.getClassAnnotations()) {
                verifyExpected(testContext, connections, modifier, annotation) ;
            }
        }
    }

    private void verifyExpected(DbUnitTestContext testContext, DatabaseConnections connections, DataSetModifier modifier, ExpectedDatabase annotation)
            throws DataSetException, SQLException, DatabaseUnitException, InstantiationException, IllegalAccessException, IOException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {

        final String query = annotation.query() ;
        final String table = annotation.table() ;
        final IDataSet expectedDataSet = loadDataset(testContext, new ExpectedDatabaseAnnotationAttributes(annotation), annotation.value(), modifier) ;
        final IDatabaseConnection connection = connections.get(annotation.connection()) ;

        if (expectedDataSet != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Veriftying @DatabaseTest expectation using " + annotation.value()) ;
            }

            final DatabaseAssertion assertion = annotation.assertionMode().getDatabaseAssertion() ;
            final List<IColumnFilter> columnFilters = getColumnFilters(annotation) ;

            if (StringUtils.hasLength(query)) {
                Assert.hasLength(table, "The table name must be specified when using a SQL query") ;
                final ITable expectedTable = expectedDataSet.getTable(table) ;
                final ITable actualTable = connection.createQueryTable(table, query) ;
                assertion.assertEquals(expectedTable, actualTable, columnFilters) ;
            } else if (StringUtils.hasLength(table)) {
                final ITable actualTable = connection.createTable(table) ;
                final ITable expectedTable = expectedDataSet.getTable(table) ;
                assertion.assertEquals(expectedTable, actualTable, columnFilters) ;
            } else {
                final IDataSet actualDataSet = connection.createDataSet() ;
                assertion.assertEquals(expectedDataSet, actualDataSet, columnFilters) ;
            }
        }
    }

    private DataSetModifier getModifier(DbUnitTestContext testContext, Annotations<ExpectedDatabase> annotations) {
        final DataSetModifiers modifiers = new DataSetModifiers() ;
        for (final ExpectedDatabase annotation : annotations) {
            for (final Class<? extends DataSetModifier> modifierClass : annotation.modifiers()) {
                modifiers.add(testContext.getTestInstance(), modifierClass) ;
            }
        }
        return modifiers ;
    }

    private void setupOrTeardown(DbUnitTestContext testContext, boolean isSetup, Collection<DatabaseSetupTearDownAnnotationAttributes> annotations)
            throws IOException, SQLException, DatabaseUnitException {

        final DatabaseConnections connections = testContext.getConnections() ;

        for (final DatabaseSetupTearDownAnnotationAttributes annotation : annotations) {
            final List<IDataSet> datasets = loadDataSets(testContext, annotation) ;
            final DatabaseOperation operation = annotation.getType() ;
            final org.dbunit.operation.DatabaseOperation dbUnitOperation = getDbUnitDatabaseOperation(testContext, operation) ;

            if (!datasets.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Executing " + (isSetup ? "Setup" : "Teardown") + " of @DatabaseTest using " + operation + " on " + datasets.toString()) ;
                }

                final IDatabaseConnection connection = connections.get(annotation.getConnection()) ;
                final IDataSet dataSet = new CompositeDataSet(datasets.toArray(new IDataSet[datasets.size()])) ;
                dbUnitOperation.execute(connection, dataSet) ;
            }
        }
    }

    private List<IDataSet> loadDataSets(DbUnitTestContext testContext, DatabaseSetupTearDownAnnotationAttributes annotation) throws DataSetException, IOException, SQLException {

        final List<IDataSet> datasets = new ArrayList<>() ;

        for (final String dataSetLocation : annotation.getValue()) {
            datasets.add(loadDataset(testContext, annotation, dataSetLocation, DataSetModifier.NONE)) ;
        }

        if (datasets.isEmpty()) {
            datasets.add(getFullDatabaseDataSet(testContext, annotation.getConnection())) ;
        }

        return datasets ;
    }

    private IDataSet getFullDatabaseDataSet(DbUnitTestContext testContext, String name) throws SQLException {
        final IDatabaseConnection connection = testContext.getConnections().get(name) ;
        return connection.createDataSet() ;
    }

    /**
     * Loads a dataset using the configuration defined in the test annotation and the global test context.
     *
     * @param testContext The test context.
     * @param annotation The annotation which is currently being processed for the test.
     * @param dataSetLocation The location of the dataset.
     * @param modifier The dataset modifier.
     * @return The loaded dataset.
     * @throws DataSetException An exception thrown if the dataset itself has a problem.
     * @throws IOException An exception thrown if the dataset could not be loaded.
     */
    private IDataSet loadDataset(final DbUnitTestContext testContext, final AbstractDatabaseAnnotationAttributes annotation, final String dataSetLocation,
            final DataSetModifier modifier) throws DataSetException, IOException {

        final DataSetLoader dataSetLoader = DataSetAnnotationUtils.getDataSetLoader(testContext, annotation) ;

        if (StringUtils.hasLength(dataSetLocation)) {
            IDataSet dataSet = dataSetLoader.loadDataSet(testContext.getTestClass(), dataSetLocation) ;
            dataSet = modifier.modify(dataSet) ;
            Assert.notNull(dataSet, "Unable to load dataset from \"" + dataSetLocation + "\" using " + dataSetLoader.getClass()) ;
            return dataSet ;
        }

        return null ;
    }

    private List<IColumnFilter> getColumnFilters(ExpectedDatabase annotation)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

        final Class<? extends IColumnFilter>[] columnFilterClasses = annotation.columnFilters() ;
        final List<IColumnFilter> columnFilters = new LinkedList<>() ;

        for (final Class<? extends IColumnFilter> columnFilterClass : columnFilterClasses) {
            columnFilters.add(columnFilterClass.getDeclaredConstructor().newInstance()) ;
        }

        return columnFilters ;
    }

    private org.dbunit.operation.DatabaseOperation getDbUnitDatabaseOperation(DbUnitTestContext testContext, DatabaseOperation operation) {
        final org.dbunit.operation.DatabaseOperation databaseOperation = testContext.getDatabaseOperationLookup().get(operation) ;
        Assert.state(databaseOperation != null, "The database operation " + operation + " is not supported") ;
        return databaseOperation ;
    }

    public DbUnitRunnerConfigBean getDefaultConfigBean() {
        return defaultConfigBean ;
    }

    public void setDefaultConfigBean(DbUnitRunnerConfigBean defaultConfigBean) {
        this.defaultConfigBean = defaultConfigBean ;
    }

}
