/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.hhdev.ascreator.exceptions;

/**
 *
 * @author fach
 */
public class NoCommentClassException extends Exception {

    /**
     * Creates a new instance of <code>NoFieldMethodException</code> without detail message.
     */
    public NoCommentClassException() {
    }


    /**
     * Constructs an instance of <code>NoFieldMethodException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public NoCommentClassException(String msg) {
        super(msg);
    }
}
