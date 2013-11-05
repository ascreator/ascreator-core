/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.hhdev.ascreator.exceptions;

/**
 *
 * @author HHFrancois
 */
public class IllegalClassException extends Exception {

    /**
     * Creates a new instance of <code>IllegalClassException</code> without detail message.
     */
    public IllegalClassException() {
    }


    /**
     * Constructs an instance of <code>IllegalClassException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public IllegalClassException(String msg) {
        super(msg);
    }
}
