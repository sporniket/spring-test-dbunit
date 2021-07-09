package com.github.springtestdbunit.dataset ;

/**
 * Interface to mark dataset loader that understands enabling/disabling column sense.
 * 
 * @author spornda
 *
 */
public interface AcceptColumSensingEnabling {

    boolean isColumnSensingEnabled() ;

    void setColumnSensingEnabled(boolean columnSensingEnabled) ;
}
