package com.github.springtestdbunit.bean ;

import com.github.springtestdbunit.DbUnitRunner ;

/**
 * Configuration to be taken into account by {@link DbUnitRunner}, follow the javadoc instruction to change default behavior.
 *
 * @author spornda
 *
 */
public class DbUnitRunnerConfigBean {

    /**
     * Set a value to define a default class wide setup dataset.
     */
    private String defaultNameOfTestClassSetupDatabase = null ;

    /**
     * Set a value to define a default teardown datasets.
     */
    private String defaultNameOfTestClassTeardown = null ;

    /**
     * Set a value to define a default supplemental setup dataset for a test method.
     */
    private String defaultNameOfTestMethodSetupDatabase = null ;

    /**
     * Set to false to disable column sense.
     */
    private boolean enabledColumnSense = true ;

    /**
     * Set to true when not using DTD in xml datasets.
     */
    private boolean loadXmlFromStreamInsteadOfUrl = false ;

    public String getDefaultNameOfTestClassSetupDatabase() {
        return defaultNameOfTestClassSetupDatabase ;
    }

    public String getDefaultNameOfTestClassTeardown() {
        return defaultNameOfTestClassTeardown ;
    }

    public String getDefaultNameOfTestMethodSetupDatabase() {
        return defaultNameOfTestMethodSetupDatabase ;
    }

    public boolean isEnabledColumnSense() {
        return enabledColumnSense ;
    }

    public boolean isLoadXmlFromStreamInsteadOfUrl() {
        return loadXmlFromStreamInsteadOfUrl ;
    }

    public void setDefaultNameOfTestClassSetupDatabase(String defaultNameOfTestClassSetupDatabase) {
        this.defaultNameOfTestClassSetupDatabase = defaultNameOfTestClassSetupDatabase ;
    }

    public void setDefaultNameOfTestClassTeardown(String defaultNameOfTestClassTeardown) {
        this.defaultNameOfTestClassTeardown = defaultNameOfTestClassTeardown ;
    }

    public void setDefaultNameOfTestMethodSetupDatabase(String defaultNameOfTestMethodSetupDatabase) {
        this.defaultNameOfTestMethodSetupDatabase = defaultNameOfTestMethodSetupDatabase ;
    }

    public void setEnabledColumnSense(boolean enabledColumnSense) {
        this.enabledColumnSense = enabledColumnSense ;
    }

    public void setLoadXmlFromStreamInsteadOfUrl(boolean loadXmlFromStreamInsteadOfUrl) {
        this.loadXmlFromStreamInsteadOfUrl = loadXmlFromStreamInsteadOfUrl ;
    }

}
