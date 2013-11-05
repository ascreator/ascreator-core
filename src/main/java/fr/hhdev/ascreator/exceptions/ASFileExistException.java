/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.hhdev.ascreator.exceptions;

/**
 *
 * @author HHFrancois
 */
public class ASFileExistException extends Exception {

    /**
     * Creates a new instance of <code>ASFileExistException</code> without detail message.
     */
    public ASFileExistException() {
    }


    /**
     * Constructs an instance of <code>ASFileExistException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ASFileExistException(String msg) {
        super(msg);
    }
}
