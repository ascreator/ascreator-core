/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator.visitors;

import flex2.tools.oem.Library;
import fr.hhdev.ascreator.core.ASCreator;
import fr.hhdev.ascreator.core.ASCreatorTools;
import fr.hhdev.ascreator.core.ASEntityCreator;
import fr.hhdev.ascreator.core.ASEnumCreator;
import fr.hhdev.ascreator.exceptions.IllegalClassException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

/**
 *
 * @author hhfrancois
 */
public class FlexObjectVisitor extends AbstractFlexVisitor {
	
	public FlexObjectVisitor(ProcessingEnvironment environment, String path, Library lib) {
		super(environment, path, lib);
	}

	/**
	 * Visite de classe
	 *
	 * @param e
	 * @param p
	 * @return
	 */
	@Override
	public Void visitType(TypeElement e, Void p) {
		if(ASCreatorTools.isConsiderateClass(e.asType(), environment)) {
			ASCreator ascreator;
			if (e.getKind().equals(ElementKind.ENUM)) {
				ascreator = new ASEnumCreator(path, e, environment, lib);
			} else {
				ascreator = new ASEntityCreator(path, e, environment, lib);
			}
			try {
				ascreator.write();
			} catch (IllegalClassException ex) {
				Logger.getLogger(FlexObjectVisitor.class.getName()).log(Level.SEVERE, "Cannot Create objectClass {0} : {2}", new Object[]{e.getSimpleName(), ex.getMessage()});
			}
		}
		return null;
	}
}
