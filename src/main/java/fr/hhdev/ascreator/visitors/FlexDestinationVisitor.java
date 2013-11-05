/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator.visitors;

import flex2.tools.oem.Library;
import fr.hhdev.ascreator.core.ASServiceCreator;
import fr.hhdev.ascreator.exceptions.IllegalClassException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 *
 * @author hhfrancois
 */
public class FlexDestinationVisitor implements ElementVisitor<Void, Map<String, String>> {
	
	protected ProcessingEnvironment environment;
	protected String path;
	protected Library lib;
	protected String bundleName;

	public FlexDestinationVisitor(ProcessingEnvironment environment, String path, Library lib) {
		this.environment=environment;
		this.path = path;
		this.lib = lib;
		this.bundleName = environment.getOptions().get("projectName");
	}

	/**
	 * Visite de classe
	 * @param e
	 * @param p
	 * @return 
	 */
	@Override
	public Void visitType(TypeElement e, Map<String, String> p) {
		ASServiceCreator asCreator = new ASServiceCreator(path, e, environment, lib);
		try {
			asCreator.write();
			DeclaredType interfDeclaredType = asCreator.getFlexCRUDInterface();
			if(interfDeclaredType!=null) {
				List<? extends TypeMirror> typeArguments = interfDeclaredType.getTypeArguments();
				for (TypeMirror typeMirror : typeArguments) {
					TypeElement asElement = (TypeElement) environment.getTypeUtils().asElement(typeMirror);
					p.put(asElement.getSimpleName().toString(), e.toString());
				}
			}
		} catch (IllegalClassException ex) {
			Logger.getLogger(FlexDestinationVisitor.class.getName()).log(Level.SEVERE, "Cannot Create serviceClass {0} : {2}", new Object[]{e.getSimpleName(), ex.getMessage()});
		}
		return null;
	}

	@Override
	public Void visit(Element e, Map<String, String> p) {
		return null;
	}

	@Override
	public Void visit(Element e) {
		return null;
	}

	@Override
	public Void visitPackage(PackageElement e, Map<String, String> p) {
		return null;
	}

	@Override
	public Void visitVariable(VariableElement e, Map<String, String> p) {
		return null;
	}

	@Override
	public Void visitExecutable(ExecutableElement e, Map<String, String> p) {
		return null;
	}

	@Override
	public Void visitTypeParameter(TypeParameterElement e, Map<String, String> p) {
		return null;
	}

	@Override
	public Void visitUnknown(Element e, Map<String, String> p) {
		return null;
	}
}
