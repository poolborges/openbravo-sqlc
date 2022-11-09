/*
 ************************************************************************************
 * Copyright (C) 2001-2010 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.exception;

/**
 *
 * @author poolborges
 */
public class CryptoException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new CryptoException with no detail message.
     */
    public CryptoException() {
        super();
    }

    /**
     * Constructs a new CryptoException with the specified detail
     * message.
     *
     * @param s the detail message
     */
    public CryptoException(String s) {
        super(s);
    }
    
    /**
     * Constructs a new CryptoException with the specified detail message.
     *
     * @param s the detail message
     * @param t the exception cause
     */
    public CryptoException(String s, Throwable t) {
        super(s, t);
    }
    
    /**
     * Constructs a new CryptoException with .
     *
     * @param t the exception cause
     */
    public CryptoException(Throwable t) {
        super(t);
    }
}
