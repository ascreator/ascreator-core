/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator.core;

import flex2.tools.oem.Library;
import fr.hhdev.ascreator.annotations.entities.FlexTransient;
import fr.hhdev.ascreator.exceptions.IgnoredClassException;
import fr.hhdev.ascreator.exceptions.IllegalClassException;
import fr.hhdev.ascreator.exceptions.IllegalMethodException;
import fr.hhdev.ascreator.exceptions.NoCommentClassException;
import fr.hhdev.ascreator.exceptions.NoRemotingClassException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

/**
 *
 * @author HHFrancois
 */
public class ASEntityCreator extends ASCreator {

	public ASEntityCreator(String path, TypeElement typeElement, ProcessingEnvironment environment, Library lib) {
		super(path, typeElement, environment, lib);
		if(ASCreatorTools.isEntity(typeElement, environment)) {
			MXCreator mxCreator = new MXItemUI(path, typeElement, environment, lib);
			mxCreator.write();
			mxCreator = new MXItemsUI(path, typeElement, environment, lib);
			mxCreator.write();
		}
	}

	@Override
	protected boolean isAbstract() {
		return typeElement.getModifiers().contains(Modifier.ABSTRACT) && !isInterface();
	}

	@Override
	protected boolean isInterface() {
		return typeElement.getKind().equals(ElementKind.INTERFACE);
	}

	@Override
	protected String getASClassName() {
		return typeElement.getSimpleName().toString();
	}

	@Override
	protected String getASClassComment() throws NoCommentClassException {
		throw new NoCommentClassException();
	}

	@Override
	protected Collection<String> getASExtendClassNames() {
		Collection<String> superclasses = new ArrayList<String>();
		TypeMirror superclassType = typeElement.getSuperclass();
		if (!ASCreatorTools.isConsiderateClass(superclassType, environment)) {
			return superclasses;
		}
		TypeElement superclassElement = (TypeElement) environment.getTypeUtils().asElement(superclassType);
		ASEntityCreator ascc = new ASEntityCreator(path, superclassElement, environment, lib);
		try {
			ascc.write();
		} catch (IllegalClassException ex) {
			environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot Create superclass " + superclassType + " of class : " + typeElement.getSimpleName() + " : " + ex.getMessage());
		}
		superclasses.add(superclassElement.getQualifiedName().toString());
		return superclasses;
	}

	/**
	 * LEs entities ne supporte pas l'héritage
	 *
	 * @return
	 */
	@Override
	protected Collection<String> getASIImplementClassNames() {
		return Collections.EMPTY_LIST;
	}

	@Override
	protected Set<String> getASImports() {
		Set<String> imports = getASDefaultImports();
		getASImportsFromClass(typeElement, imports);
		for (ExecutableElement methodElement : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
			if (!ASCreatorTools.isMethodPublicGetter(methodElement, environment)) {
				continue;
			}
			try {
				getASImportsFromMethod(methodElement, imports);
			} catch (IllegalMethodException ex) {
				environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot get import from method : " + typeElement.getSimpleName() + "." + methodElement.getSimpleName() + " : " + ex.getMessage());
			}
		}
		return imports;
	}

	/**
	 * Retourne une liste d'imports par defaut.
	 *
	 * @return
	 */
	private Set<String> getASDefaultImports() {
		Set<String> imports = new HashSet<String>();
		imports.add("mx.collections.ArrayCollection");
		imports.add("flash.utils.ByteArray");
		imports.add("flash.errors.IllegalOperationError");
		return imports;
	}

	@Override
	protected Map<String, String> getASEvents() {
		return null;
	}

	@Override
	protected Set<String> getASNamespaces() {
		return null;
	}

	@Override
	protected void writeASBodyConstructor() {
	}

	/**
	 * Si on veut rajouter des methodes personnalisées, c'est ici
	 *
	 */
	protected void writeExtraASMethods() {
	}

	@Override
	protected void writeASFieldsAndMethods() {
		writeExtraASMethods();
		TypeMirror extendType = typeElement.getSuperclass();
		TypeElement extendElement = (TypeElement) environment.getTypeUtils().asElement(extendType);
		List<VariableElement> fieldElements = ElementFilter.fieldsIn(typeElement.getEnclosedElements());
		// on parcours toutes les methodes de la classe
		for (ExecutableElement methodElement : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
			// seuls les getter public sont considerés
			if (!ASCreatorTools.isMethodPublicGetter(methodElement, environment)) {
				continue;
			}
			// si elle est FlexTransient ou Static, on l'ignore
			if (methodElement.getAnnotation(FlexTransient.class) != null || methodElement.getModifiers().contains(Modifier.STATIC)) {
				continue;
			}
			String fieldName = null;
			// type de retour de la methode
			TypeMirror returnType = methodElement.getReturnType();
			String fieldtype = ASCreatorTools.getASType(returnType, environment);
			// cette methode est elle héritée
			Boolean overrided = extendElement != null && extendElement.getEnclosedElements().contains(methodElement);
			// cette methode est elle abtract
			boolean methodIsAbstract = methodElement.getModifiers().contains(Modifier.ABSTRACT); // la method est abstract
			if (isInterface() || methodIsAbstract) {
				inserASMethod(fieldName, fieldtype, isInterface(), methodIsAbstract, overrided, getASAnnotations(methodElement, null));
			} else { // si la methode est ni abstract et la classe pas une interface, on va chercher le field correspondant
				fieldName = ASCreatorTools.getFieldName(methodElement, environment); // seules les methodes commencant par is ou get, et retrournant quelque chose sont considerées
				if (fieldName != null) {
					for (VariableElement variableElement : fieldElements) { // pour les pojo, seule les methodes avec un field associé sont consideré
						if (variableElement.toString().equals(fieldName)) {
							if (variableElement.getAnnotation(FlexTransient.class) == null && !variableElement.getModifiers().contains(Modifier.TRANSIENT)) {
								inserASField(fieldName, fieldtype, methodElement, variableElement);
								inserASMethod(fieldName, fieldtype, isInterface(), methodIsAbstract, overrided, getASAnnotations(methodElement, variableElement));
							}
							break;
						}
					}
				}
			}
		}
	}

	@Override
	protected boolean isBindable() {
		return true;
	}

	@Override
	protected String getRemotingClass() throws NoRemotingClassException {
		return this.typeElement.getQualifiedName().toString();
	}

	@Override
	protected void checkClass() throws IllegalClassException, IgnoredClassException {
		if (typeElement == null) {
			throw new IgnoredClassException();
		}
		TypeElement serializableElement = environment.getElementUtils().getTypeElement(Serializable.class.getName());
		if (environment.getTypeUtils().isSameType(typeElement.asType(), serializableElement.asType())) {
			throw new IgnoredClassException();
		}
		if (!isInterface() && !environment.getTypeUtils().isAssignable(typeElement.asType(), serializableElement.asType())) {
			throw new IllegalClassException(typeElement.getQualifiedName().toString() + " must implement Serializable interface.");
		}
	}

	/**
	 * Insere les methodes d'acces a un field, avec eventuellement ses annotations
	 *
	 * @param fieldName
	 * @param fieldType
	 * @param override
	 * @param asAnnotations
	 */
	private void inserASMethod(String fieldName, String fieldType, boolean isInterface, boolean isAbstract, boolean override, Collection<String> asAnnotations) {
		inserASGETMethod(fieldName, fieldType, isInterface, isAbstract, override, asAnnotations);
		inserASSETMethod(fieldName, fieldType, isInterface, isAbstract, override);
	}

	/**
	 * Insere la methode get d'acces a un field, avec eventuellement ses annotations
	 *
	 * @param fieldName
	 * @param fieldType
	 * @param override
	 * @param asAnnotations
	 */
	private void inserASGETMethod(String fieldName, String fieldType, boolean isInterface, boolean isAbstract, boolean override, Collection<String> asAnnotations) {
		String overrided = "";
		if (asAnnotations != null) {
			for (String asAnnotation : asAnnotations) {
				out.println("		" + asAnnotation);
			}
		}
		if (override) {
			overrided = "override ";
		}
		out.print("		");
		if (!isInterface) {
			out.print(overrided + "public ");
		}
		out.print("function get " + fieldName + "():" + fieldType);
		if (!isInterface) {
			out.println(" {");
			if (!isAbstract) {
				out.println("			return this._" + fieldName + ";");
			} else {
				out.println("			throw new IllegalOperationError(\"Method get " + fieldName + " must be implemented in concret class " + getASClassName() + "\");");
			}
			out.println("		}");
		} else {
			out.println(";");
		}
	}

	/**
	 * Insere la methode set d'acces a un field
	 *
	 * @param fieldName
	 * @param fieldType
	 * @param override
	 */
	private void inserASSETMethod(String fieldName, String fieldType, boolean isInterface, boolean isAbstract, boolean override) {
		String overrided = "";
		if (override) {
			overrided = "override ";
		}
		out.print("		");
		if (!isInterface) {
			out.print(overrided + "public ");
		}
		out.print("function set " + fieldName + "(" + fieldName + ":" + fieldType + "):void");
		if (!isInterface) {
			out.println(" {");
			if (!isAbstract) {
				out.println("			this._" + fieldName + " = " + fieldName + ";");
			} else {
				out.println("			throw new IllegalOperationError(\"Method set " + fieldName + " must be implemented in concret class " + getASClassName() + "\");");
			}
			out.println("		}");
		} else {
			out.println(";");
		}
		out.println("");
	}

	/**
	 * Insere les methodes d'acces a un field, avec eventuellement ses annotations
	 *
	 * @param fieldName
	 * @param fieldType
	 * @param override
	 * @param asAnnotations
	 */
	private void inserASField(String fieldName, String fieldType, ExecutableElement methodElement, VariableElement field) {
		String value = null;
		TypeMirror returnType = methodElement.getReturnType();
		Element returnElement = environment.getTypeUtils().asElement(returnType);
		if (returnElement != null && returnElement.getKind().equals(ElementKind.ENUM)) {
			for (Element c : returnElement.getEnclosedElements()) {
				if (c.getKind().equals(ElementKind.ENUM_CONSTANT)) {
					value = returnElement+"."+c.getSimpleName().toString();
					break;
				}
			}
		}
		if(value!=null) {
			out.println("		private var _" + fieldName + ":" + fieldType + " = "+value+";");
		} else {
			out.println("		private var _" + fieldName + ":" + fieldType + ";");
		}
	}

	/**
	 * Retourne les annotations java au format actionScriptt
	 *
	 * @param field
	 * @param fieldType
	 * @return
	 */
	private Collection<String> getASAnnotations(ExecutableElement methodElement, VariableElement field) {
		Collection<String> asAnnotations = new ArrayList<String>();
		TypeMirror returnType = methodElement.getReturnType();
		Element returnElement = environment.getTypeUtils().asElement(returnType);
		if (returnElement != null && returnElement.getKind().equals(ElementKind.ENUM)) {
			Collection<String> enumValues = new ArrayList<String>();
			for (Element c : returnElement.getEnclosedElements()) {
				if (c.getKind().equals(ElementKind.ENUM_CONSTANT)) {
					enumValues.add(c.getSimpleName().toString());
				}
			}
			if (!enumValues.isEmpty()) {
				StringBuilder inspectable = new StringBuilder("[Inspectable(enumeration=\"");
				Iterator<String> enumIterator = enumValues.iterator();
				String defaultValue = enumIterator.next();
				inspectable.append(defaultValue);
				while (enumIterator.hasNext()) {
					inspectable.append(",");
					inspectable.append(enumIterator.next());
				}
				inspectable.append("\", ");
				inspectable.append("defaultValue=\"").append(defaultValue).append("\", category=\"General\", type=\"").append(returnElement).append("\")]");
				asAnnotations.add(inspectable.toString());
			}
		}
		if (field != null) { // ajoute les annotation JPA/FPA
			List<? extends AnnotationMirror> annotations = field.getAnnotationMirrors();
			for (AnnotationMirror annotation : annotations) {
				String asAnno = getASAnnotation(annotation, methodElement);
				if (asAnno != null) {
					asAnnotations.add(asAnno);
				}

			}
		}
		return asAnnotations;
	}

	/**
	 * Retourne l'annotation java au format actionScript
	 *
	 * @param anno
	 * @param fieldType
	 * @return
	 */
	private String getASAnnotation(AnnotationMirror anno, ExecutableElement methodElement) {
		String result = null;
		String collectionOf = null;
		TypeMirror returnType = methodElement.getReturnType();
		try {
			if (ASCreatorTools.isConsideratedIterable(returnType, environment)) {
				Set<String> returnTypes = new HashSet<String>();
				discoverASEntityFromTypeMirror(returnType, returnTypes);
				for (String collectionOfTmp : returnTypes) {
					collectionOf = collectionOfTmp;
					break;
				}
			}
		} catch (IllegalClassException ex) {
			environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot discover asClass fromTypeMirror : " + ex.getMessage(), typeElement);
		}
		String annoType = anno.getAnnotationType().toString();
		if (annoType.equals("javax.persistence.Id")) {
			result = "[Id]";
		} else if (annoType.equals("javax.persistence.ManyToMany")) {
			result = "[ManyToMany";
			if (collectionOf != null) {
				result += "(type=\"" + collectionOf + "\", cascade=\"all\")";
			}
			result += "]";
		} else if (annoType.equals("javax.persistence.OneToMany")) {
			result = "[OneToMany";
			if (collectionOf != null) {
				result += "(type=\"" + collectionOf + "\", cascade=\"all\")";
			}
			result += "]";
		} else if (annoType.equals("javax.persistence.ManyToOne")) {
			result = "[ManyToOne]";
		} else if (annoType.equals("javax.persistence.OneToOne")) {
			result = "[OneToOne]";
		} else if (annoType.equals("javax.persistence.GeneratedValue")) {
		} else if (annoType.equals("javax.persistence.Basic")) {
		} else if (annoType.equals("javax.persistence.Enumerated")) {
		} else {
			environment.getMessager().printMessage(Diagnostic.Kind.NOTE, "Not supported Annotation : " + typeElement + " : " + anno);
		}
		return result;
	}
}
