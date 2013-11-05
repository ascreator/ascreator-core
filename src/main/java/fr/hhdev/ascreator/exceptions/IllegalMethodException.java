/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.hhdev.ascreator.exceptions;

/**
 *
 * @author HHFrancois
 */
public class IllegalMethodException extends Exception {
	private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of <code>IllegalClassException</code> without detail message.
     */
    public IllegalMethodException() {
    }


    /**
     * Constructs an instance of <code>IllegalClassException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public IllegalMethodException(String msg) {
        super(msg);
    }
}
