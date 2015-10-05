/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.eb.ime.pfc.domain;

/**
 *
 * @author arthurfernandes
 */
public class RepeatedItemException extends RuntimeException {

    /**
     * Creates a new instance of <code>RepetitionInCollectionException</code>
     * without detail message.
     */
    public RepeatedItemException() {
    }

    /**
     * Constructs an instance of <code>RepetitionInCollectionException</code>
     * with the specified detail message.
     *
     * @param msg the detail message.
     */
    public RepeatedItemException(String msg) {
        super(msg);
    }
}
