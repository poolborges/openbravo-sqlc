/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbravo.data;

/**
 *
 * @author pauloborges
 */
public class SqlcException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new SqlcException with no detail message.
     */
    public SqlcException() {
        super();
    }

    /**
     * Constructs a new SqlcException with the specified detail message.
     *
     * @param s the detail message
     */
    public SqlcException(String s) {
        super(s);
    }

    /**
     * Constructs a new SqlcException with the specified detail message.
     *
     * @param s the detail message
     * @param t the exception cause
     */
    public SqlcException(String s, Throwable t) {
        super(s, t);
    }
}