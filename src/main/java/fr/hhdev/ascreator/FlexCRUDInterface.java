/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator;

import java.util.List;

/**
 *
 * @author hhfrancois
 */
public interface FlexCRUDInterface<T> {
	void persist(T entity);

	void merge(T entity);

	void remove(Object id);

	T find(Object id);

	List<T> findAll();

	List<T> findRange(int page, int nb);

	int count();
	
}
