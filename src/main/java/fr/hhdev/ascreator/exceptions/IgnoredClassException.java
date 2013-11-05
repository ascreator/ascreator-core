/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.hhdev.ascreator.exceptions;

/**
 *
 * @author HHFrancois
 */
public class IgnoredClassException extends Exception {

    /**
     * Creates a new instance of <code>IgnoredClassException</code> without detail message.
     */
    public IgnoredClassException() {
    }


    /**
     * Constructs an instance of <code>IgnoredClassException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public IgnoredClassException(String msg) {
        super(msg);
    }
}
