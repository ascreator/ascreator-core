/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator.visitors;

import flex2.tools.oem.Library;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

/**
 *
 * @author hhfrancois
 */
public abstract class AbstractFlexVisitor implements ElementVisitor<Void, Void> {
	protected ProcessingEnvironment environment;
	protected String path;
	protected Library lib;
	protected String bundleName;

	public AbstractFlexVisitor(ProcessingEnvironment environment, String path, Library lib) {
		this.environment=environment;
		this.path = path;
		this.lib = lib;
		this.bundleName = environment.getOptions().get("projectName");
	}
	
	@Override
	public Void visitType(TypeElement e, Void p) {
		return null;
	}

	@Override
	public Void visit(Element e, Void p) {
		return null;
	}

	@Override
	public Void visit(Element e) {
		return null;
	}

	@Override
	public Void visitPackage(PackageElement e, Void p) {
		return null;
	}

	@Override
	public Void visitVariable(VariableElement e, Void p) {
		return null;
	}

	@Override
	public Void visitExecutable(ExecutableElement e, Void p) {
		return null;
	}

	@Override
	public Void visitTypeParameter(TypeParameterElement e, Void p) {
		return null;
	}

	@Override
	public Void visitUnknown(Element e, Void p) {
		return null;
	}
}
