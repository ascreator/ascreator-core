/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator.visitors;

import flex2.tools.oem.Library;
import fr.hhdev.ascreator.annotations.entities.FlexSeeAlso;
import fr.hhdev.ascreator.core.ASCreator;
import fr.hhdev.ascreator.core.ASCreatorTools;
import fr.hhdev.ascreator.core.ASEntityCreator;
import fr.hhdev.ascreator.core.ASEnumCreator;
import fr.hhdev.ascreator.exceptions.IllegalClassException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

/**
 *
 * @author hhfrancois
 */
public class FlexSeeAlsoVisitor extends AbstractFlexVisitor {

	public FlexSeeAlsoVisitor(ProcessingEnvironment environment, String path, Library lib) {
		super(environment, path, lib);
	}

	@Override
	public Void visit(Element e) {
		FlexSeeAlso seeAlso = e.getAnnotation(FlexSeeAlso.class);
		if (seeAlso != null) {
			for (Class subClass : seeAlso.value()) {
				TypeElement subClassElement = (TypeElement) environment.getElementUtils().getTypeElement(subClass.getName());
				if (!ASCreatorTools.isConsiderateClass(subClassElement.asType(), environment)) {
					continue;
				}
				ASCreator ascreator;
				if (subClassElement.getKind().equals(ElementKind.ENUM)) {
					ascreator = new ASEnumCreator(path, subClassElement, environment, lib);
				} else {
					ascreator = new ASEntityCreator(path, subClassElement, environment, lib);
				}
				try {
					ascreator.write();
				} catch (IllegalClassException ex) {
					Logger.getLogger(ASCreator.class.getName()).log(Level.SEVERE, "Cannot Create subClass {0} of class : {1} : {2}", new Object[]{subClass.getSimpleName(), e.getSimpleName(), ex.getMessage()});
				}
			}
		}
		return null;
	}

	@Override
	public Void visitType(TypeElement e, Void p) {
		return visit(e);
	}

	@Override
	public Void visit(Element e, Void p) {
		return visit(e);
	}

	@Override
	public Void visitVariable(VariableElement e, Void p) {
		return visit(e);
	}

	@Override
	public Void visitExecutable(ExecutableElement e, Void p) {
		return visit(e);
	}

	@Override
	public Void visitTypeParameter(TypeParameterElement e, Void p) {
		return visit(e);
	}

	@Override
	public Void visitUnknown(Element e, Void p) {
		return visit(e);
	}
}
