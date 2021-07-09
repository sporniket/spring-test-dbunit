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

import java.util.Arrays ;

import javax.sql.DataSource ;

import org.apache.commons.logging.Log ;
import org.apache.commons.logging.LogFactory ;
import org.dbunit.database.IDatabaseConnection ;
import org.springframework.test.context.TestContext ;
import org.springframework.test.context.support.AbstractTestExecutionListener ;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener ;
import org.springframework.util.Assert ;
import org.springframework.util.ObjectUtils ;
import org.springframework.util.StringUtils ;

import com.github.springtestdbunit.annotation.DatabaseSetup ;
import com.github.springtestdbunit.annotation.DatabaseTearDown ;
import com.github.springtestdbunit.annotation.DbUnitConfiguration ;
import com.github.springtestdbunit.annotation.ExpectedDatabase ;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean ;
import com.github.springtestdbunit.bean.DbUnitRunnerConfigBean ;
import com.github.springtestdbunit.dataset.DataSetLoader ;
import com.github.springtestdbunit.dataset.FlatXmlDataSetLoader ;
import com.github.springtestdbunit.operation.DatabaseOperationLookup ;
import com.github.springtestdbunit.operation.DefaultDatabaseOperationLookup ;

/**
 * <code>TestExecutionListener</code> which provides support for {@link DatabaseSetup &#064;DatabaseSetup}, {@link DatabaseTearDown &#064;DatabaseTearDown} and
 * {@link ExpectedDatabase &#064;ExpectedDatabase} annotations.
 * <p>
 * A bean named "{@code dbUnitDatabaseConnection}" or "{@code dataSource}" is expected in the {@code ApplicationContext} associated with the test. This bean can contain either a
 * {@link IDatabaseConnection} or a {@link DataSource} . A custom bean name can also be specified using the {@link DbUnitConfiguration#databaseConnection()
 * &#064;DbUnitConfiguration} annotation.
 * <p>
 * Datasets are loaded using the {@link FlatXmlDataSetLoader} and DBUnit database operation lookups are performed using the {@link DefaultDatabaseOperationLookup} unless otherwise
 * {@link DbUnitConfiguration#dataSetLoader() configured}.
 * <p>
 * If you are running this listener in combination with the {@link TransactionalTestExecutionListener} then consider using {@link TransactionDbUnitTestExecutionListener} instead.
 *
 * @see TransactionDbUnitTestExecutionListener
 *
 * @author Phillip Webb
 */
public class DbUnitTestExecutionListener extends AbstractTestExecutionListener {

    private static final Log logger = LogFactory.getLog(DbUnitTestExecutionListener.class) ;

    public static final String DEFAULT_DBUNIT_DATABASE_CONNECTION_BEAN_NAME = "dbUnitDatabaseConnection" ;

    public static final String DEFAULT_DATASOURCE_BEAN_NAME = "dataSource" ;

    private static final String[] COMMON_DATABASE_CONNECTION_BEAN_NAMES = {
            DEFAULT_DBUNIT_DATABASE_CONNECTION_BEAN_NAME, DEFAULT_DATASOURCE_BEAN_NAME
    } ;

    private static final String DATA_SET_LOADER_BEAN_NAME = "dbUnitDataSetLoader" ;

    private static DbUnitRunner runner = new DbUnitRunner() ;

    @Override
    public void prepareTestInstance(TestContext testContext) throws Exception {
        prepareTestInstance(new DbUnitTestContextAdapter(testContext)) ;
    }

    public void prepareTestInstance(DbUnitTestContextAdapter testContext) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Preparing test instance " + testContext.getTestClass() + " for DBUnit") ;
        }
        String[] databaseConnectionBeanNames = null ;
        String dataSetLoaderBeanName = null ;
        Class<? extends DataSetLoader> dataSetLoaderClass = FlatXmlDataSetLoader.class ;
        Class<? extends DatabaseOperationLookup> databaseOperationLookupClass = DefaultDatabaseOperationLookup.class ;
        String dbUnitRunnerConfigBeanName = null ;

        final DbUnitConfiguration configuration = testContext.getTestClass().getAnnotation(DbUnitConfiguration.class) ;
        if (configuration != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using @DbUnitConfiguration configuration") ;
            }
            databaseConnectionBeanNames = configuration.databaseConnection() ;
            dataSetLoaderClass = configuration.dataSetLoader() ;
            dataSetLoaderBeanName = configuration.dataSetLoaderBean() ;
            databaseOperationLookupClass = configuration.databaseOperationLookup() ;
            dbUnitRunnerConfigBeanName = configuration.dbUnitRunnerConfigBean() ;
        }

        if (ObjectUtils.isEmpty(databaseConnectionBeanNames) || ((databaseConnectionBeanNames.length == 1) && StringUtils.isEmpty(databaseConnectionBeanNames[0]))) {
            databaseConnectionBeanNames = new String[] {
                    getDatabaseConnectionUsingCommonBeanNames(testContext)
            } ;
        }

        if (!StringUtils.hasLength(dataSetLoaderBeanName)) {
            if (testContext.getApplicationContext().containsBean(DATA_SET_LOADER_BEAN_NAME)) {
                dataSetLoaderBeanName = DATA_SET_LOADER_BEAN_NAME ;
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("DBUnit tests will run using databaseConnection \"" + StringUtils.arrayToCommaDelimitedString(databaseConnectionBeanNames)
                    + "\", datasets will be loaded using " + (StringUtils.hasLength(dataSetLoaderBeanName) ? "'" + dataSetLoaderBeanName + "'" : dataSetLoaderClass)) ;
        }
        prepareDatabaseConnection(testContext, databaseConnectionBeanNames) ;
        prepareDataSetLoader(testContext, dataSetLoaderBeanName, dataSetLoaderClass) ;
        prepareDatabaseOperationLookup(testContext, databaseOperationLookupClass) ;
        attachRunnerConfiguration(testContext, dbUnitRunnerConfigBeanName) ;
    }

    private void attachRunnerConfiguration(DbUnitTestContextAdapter testContext, String dbUnitRunnerConfigBeanName) {
        logger.info("try getting bean '" + dbUnitRunnerConfigBeanName + "'") ;
        if (StringUtils.hasText(dbUnitRunnerConfigBeanName) && testContext.getApplicationContext().containsBean(dbUnitRunnerConfigBeanName)) {
            runner.setDefaultConfigBean((DbUnitRunnerConfigBean) testContext.getApplicationContext().getBean(dbUnitRunnerConfigBeanName)) ;
        } else {
            runner.setDefaultConfigBean(new DbUnitRunnerConfigBean()) ;
        }
    }

    private String getDatabaseConnectionUsingCommonBeanNames(DbUnitTestContextAdapter testContext) {
        for (final String beanName : COMMON_DATABASE_CONNECTION_BEAN_NAMES) {
            if (testContext.getApplicationContext().containsBean(beanName)) {
                return beanName ;
            }
        }
        throw new IllegalStateException("Unable to find a DB Unit database connection, missing one the following beans: " + Arrays.asList(COMMON_DATABASE_CONNECTION_BEAN_NAMES)) ;
    }

    private void prepareDatabaseConnection(DbUnitTestContextAdapter testContext, String[] connectionBeanNames) throws Exception {

        final IDatabaseConnection[] connections = new IDatabaseConnection[connectionBeanNames.length] ;

        for (int i = 0; i < connectionBeanNames.length; i++) {
            Object databaseConnection = testContext.getApplicationContext().getBean(connectionBeanNames[i]) ;
            if (databaseConnection instanceof DataSource) {
                databaseConnection = DatabaseDataSourceConnectionFactoryBean.newConnection((DataSource) databaseConnection) ;
            }
            Assert.isInstanceOf(IDatabaseConnection.class, databaseConnection) ;
            connections[i] = (IDatabaseConnection) databaseConnection ;
        }
        testContext.setAttribute(DbUnitTestContextConstants.CONNECTION_ATTRIBUTE, new DatabaseConnections(connectionBeanNames, connections)) ;
    }

    private void prepareDataSetLoader(DbUnitTestContextAdapter testContext, String beanName, Class<? extends DataSetLoader> dataSetLoaderClass) {
        if (StringUtils.hasLength(beanName)) {
            testContext.setAttribute(DbUnitTestContextConstants.DATA_SET_LOADER_ATTRIBUTE, testContext.getApplicationContext().getBean(beanName, DataSetLoader.class)) ;
        } else {
            try {
                testContext.setAttribute(DbUnitTestContextConstants.DATA_SET_LOADER_ATTRIBUTE, dataSetLoaderClass.getDeclaredConstructor().newInstance()) ;
            } catch (final Exception ex) {
                throw new IllegalArgumentException("Unable to create data set loader instance for " + dataSetLoaderClass, ex) ;
            }
        }
    }

    private void prepareDatabaseOperationLookup(DbUnitTestContextAdapter testContext, Class<? extends DatabaseOperationLookup> databaseOperationLookupClass) {
        try {
            testContext.setAttribute(DbUnitTestContextConstants.DATABASE_OPERATION_LOOKUP_ATTRIBUTE, databaseOperationLookupClass.getDeclaredConstructor().newInstance()) ;
        } catch (final Exception ex) {
            throw new IllegalArgumentException("Unable to create database operation lookup instance for " + databaseOperationLookupClass, ex) ;
        }
    }

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        runner.beforeTestMethod(new DbUnitTestContextAdapter(testContext)) ;
    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        runner.afterTestMethod(new DbUnitTestContextAdapter(testContext)) ;
    }

}
