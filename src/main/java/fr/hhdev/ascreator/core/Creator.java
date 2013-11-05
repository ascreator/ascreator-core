/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator.core;

import flex2.tools.oem.Library;
import fr.hhdev.ascreator.annotations.FlexDestination;
import fr.hhdev.ascreator.exceptions.IllegalClassException;
import fr.hhdev.ascreator.exceptions.IllegalMethodException;
import java.io.PrintWriter;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

/**
 *
 * @author hhfrancois
 */
public abstract class Creator {
	protected ResourceBundle ignoreClasses = ResourceBundle.getBundle("ignoreClasses");
	protected final TypeElement typeElement;
	protected final ProcessingEnvironment environment;
	protected String path;
	protected PrintWriter out;
	protected Library lib;

	public Creator(String path, TypeElement typeElement, ProcessingEnvironment environment, Library lib) {
		this.path = path;
		this.typeElement = typeElement;
		this.environment = environment;
		this.lib = lib;
	}
	
	/**
	 * Ecrit la classe
	 */
	public abstract void write() throws IllegalClassException;
	
	/**
	 * Enrichit la listes des imports nécéssaire à la classe. Lors de la découverte des classes, si celle ci n'existe pas, elle est créé.
	 *
	 * @param class
	 * @param imports
	 */
	protected void getASImportsFromClass(final TypeElement clazz, Set<String> imports) {
		try {
			discoverASEntityFromTypeMirror(clazz.getSuperclass(), imports);
		} catch (IllegalClassException ex) {
			environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot Create superclass from class : " + clazz.getSimpleName() + " : " + ex.getMessage());
		}
		for (TypeMirror type : clazz.getInterfaces()) {
			try {
				discoverASEntityFromTypeMirror(type, imports);
			} catch (IllegalClassException ex) {
				environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot Create interface from class : " + clazz.getSimpleName() + " : " + ex.getMessage());
			}
		}
	}

	/**
	 * Enrichit la listes des imports nécéssaire à la classe. Lors de la découverte des classes, si celle ci n'existe pas, elle est créé.
	 *
	 * @param method
	 * @param imports
	 */
	protected void getASImportsFromMethod(final ExecutableElement methodElement, Set<String> imports) throws IllegalMethodException {
		if (!ASCreatorTools.isConsiderateMethod(methodElement, environment)) {
			return;
		}
		TypeMirror returnType = methodElement.getReturnType();
		try {
			discoverASEntityFromTypeMirror(returnType, imports);
		} catch (IllegalClassException ex) {
			throw new IllegalMethodException(String.format("Cannot Create class return from method : %1s.%2s : %3s", methodElement.getEnclosingElement().getSimpleName(), methodElement.getSimpleName(), ex.getMessage()));
		}
		for (VariableElement variableElement : methodElement.getParameters()) {
			try {
				discoverASClassFromVariableElement(variableElement, imports);
			} catch (IllegalClassException ex) {
				environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot Create class argument from method : " + methodElement.getEnclosingElement().getSimpleName() + "." + methodElement.getSimpleName() + " : " + ex.getMessage());
			}
		}
	}

	/**
	 * Enrichit la listes des imports nécéssaire à la classe. Lors de la découverte des classes, si celle ci n'existe pas, elle est créé.
	 *
	 * @param class
	 * @param imports
	 */
	protected void getASImportsFromTypeElement(final TypeElement element, Set<String> imports) {
		try {
			discoverASEntityFromTypeMirror(element.getSuperclass(), imports);
		} catch (IllegalClassException ex) {
			environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot Create superclass from class : " + element.getSimpleName() + " : " + ex.getMessage());
		}
		for (TypeMirror type : element.getInterfaces()) {
			try {
				discoverASEntityFromTypeMirror(type, imports);
			} catch (IllegalClassException ex) {
				environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot Create interface from class : " + element.getSimpleName() + " : " + ex.getMessage());
			}
		}
	}

	/**
	 * Decouvre les variables, les stoke dans imports. Genere les classes nécéssaires.
	 *
	 * @param type
	 * @param imports
	 */
	public void discoverASClassFromVariableElement(VariableElement variableElement, Set<String> imports) throws IllegalClassException {
		discoverASEntityFromTypeMirror(variableElement.asType(), imports);
	}

	/**
	 * Decouvre les types generiques, les stoke dans imports. Genere les classes nécéssaires.
	 *
	 * @param type
	 * @param imports
	 */
	public void discoverASClassFromTypeParameterElement(TypeParameterElement typeParameterElement, Set<String> imports) throws IllegalClassException {
		List<? extends TypeMirror> bounds = typeParameterElement.getBounds();
		for (TypeMirror typeMirror : bounds) {
			discoverASEntityFromTypeMirror(typeMirror, imports);
		}
	}

	/**
	 * Decouvre les types generiques, les stoke dans imports. Genere les classes nécéssaires.
	 *
	 * @param type
	 * @param imports
	 */
	protected void discoverASEntityFromTypeMirror(TypeMirror type, Set<String> imports) throws IllegalClassException {
		if (type.getKind().equals(TypeKind.VOID)) {
			return;
		}
		boolean ignore = false;
		Element elt = environment.getTypeUtils().asElement(type);
		if (elt == null) {
			return;
		}
		if (type.getKind().equals(TypeKind.TYPEVAR)) {
			discoverASClassFromTypeParameterElement((TypeParameterElement) elt, imports);
			return;
		}
		if (!type.getKind().equals(TypeKind.DECLARED)) {
			return;
		}
		DeclaredType declaredType= (DeclaredType) type;
		List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
		for (TypeMirror typeArgument : typeArguments) {
			discoverASEntityFromTypeMirror(typeArgument, imports);
		}
		TypeElement tElement = (TypeElement) elt;
		try {
			ignore = Boolean.parseBoolean(ignoreClasses.getString(tElement.getQualifiedName().toString()));
		} catch (MissingResourceException mre) {
		}
		if (ignore) {
		} else if (tElement.getKind().equals(ElementKind.ENUM)) {
			imports.add(tElement.getQualifiedName().toString());
			ASEnumCreator asClassCreator = new ASEnumCreator(path, tElement, environment, lib);
			asClassCreator.write();
		} else if (tElement.getAnnotation(FlexDestination.class) != null) { // elle sera traité par APT
			imports.add(tElement.getQualifiedName().toString());
		} else {
			String asClass = ASCreatorTools.getASClassName(type, environment);
			if (asClass == null) { // le type n'existe pas en AS il faut peut être l'importer et la créer.
				if (!type.getKind().equals(TypeKind.VOID)) {
					ASCreator asCreator = new ASEntityCreator(path, tElement, environment, lib);
					asCreator.write();
					imports.add(((TypeElement) environment.getTypeUtils().asElement(type)).getQualifiedName().toString());
				}
			}
		}
	}

	/**
	 * Decouvre les types generiques, les stoke dans imports. Genere les classes nécéssaires.
	 *
	 * @param type
	 * @param imports
	 */
	protected void discoverASClassFromTypeMirror(TypeMirror type, Set<String> imports) throws IllegalClassException {
		if (type.getKind().equals(TypeKind.VOID)) {
			return;
		}
		boolean ignore = false;
		Element elt = environment.getTypeUtils().asElement(type);
		if (elt == null) {
			return;
		}
		if (type.getKind().equals(TypeKind.TYPEVAR)) {
			discoverASClassFromTypeParameterElement((TypeParameterElement) elt, imports);
			return;
		}
		if (!type.getKind().equals(TypeKind.DECLARED)) {
			return;
		}
		TypeElement tElement = (TypeElement) elt;
		try {
			ignore = Boolean.parseBoolean(ignoreClasses.getString(tElement.getQualifiedName().toString()));
		} catch (MissingResourceException mre) {
		}
		if (ignore) {
		} else if (tElement.getKind().equals(ElementKind.ENUM)) {
			ASEnumCreator asClassCreator = new ASEnumCreator(path, tElement, environment, lib);
			asClassCreator.write();
		} else if (tElement.getAnnotation(FlexDestination.class) != null) { // elle sera traité par APT
			imports.add(tElement.getQualifiedName().toString());
		} else {
			String asClass = ASCreatorTools.getASClassName(type, environment);
			if (asClass == null) { // le type n'existe pas en AS il faut peut être l'importer et la créer.
				if (!type.getKind().equals(TypeKind.VOID)) {
					ASCreator asCreator = new ASEntityCreator(path, tElement, environment, lib);
					asCreator.write();
					imports.add(((TypeElement) environment.getTypeUtils().asElement(type)).getQualifiedName().toString());
				}
			}
		}
	}
}
