/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator.core;

import fr.hhdev.ascreator.annotations.entities.FlexTransient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

/**
 *
 * @author HHFrancois
 */
public class ASCreatorTools {

	private static ResourceBundle mapping = ResourceBundle.getBundle("mappingJavaAs");

	public static boolean isConsideratedIterable(TypeMirror typeMirror, ProcessingEnvironment environment) {
		Element current = environment.getTypeUtils().asElement(typeMirror);
		if (!current.getKind().equals(ElementKind.CLASS) && !current.getKind().equals(ElementKind.INTERFACE)) {
			return false;
		}
		TypeElement elt = (TypeElement) current;
		if (elt.getTypeParameters().size() != 1) {
			return false;
		}
		return typeMirror.toString().startsWith(List.class.getName()) || typeMirror.toString().startsWith(Collection.class.getName()) || typeMirror.toString().startsWith(Enumeration.class.getName());
	}

	/**
	 * Doit on exposer les methodes déprecated
	 *
	 * @param environment
	 * @return
	 */
	public static boolean isIgnoreDeprecated(ProcessingEnvironment environment) {
		if (!environment.getOptions().containsKey("ignoreDeprecated")) {
			return true;
		}
		return environment.getOptions().get("ignoreDeprecated").equalsIgnoreCase("true");
	}

	/**
	 * Détermine si une methode doit être traitée
	 *
	 * @param methodElement
	 * @param environment
	 * @return
	 */
	public static boolean isConsiderateMethod(ExecutableElement methodElement, ProcessingEnvironment environment) {
		if (!methodElement.getModifiers().contains(Modifier.PUBLIC) || methodElement.getModifiers().contains(Modifier.STATIC)) { // la methode n'est pas public ou est static
			return false;
		}
		if (methodElement.getAnnotation(FlexTransient.class) != null) { // on veut pas l'exposé
			return false;
		}
		if (methodElement.getAnnotation(Deprecated.class) != null && isIgnoreDeprecated(environment)) { // elle est déprecated est on veut pas exposer les deprecated
			return false;
		}
		TypeElement objectElement = environment.getElementUtils().getTypeElement(Object.class.getName()); // la methode appartient à Object
		if (objectElement.getEnclosedElements().contains(methodElement)) {
			return false;
		}
		return true;
	}

	/**
	 * Détermine si une classe doit être traitée
	 *
	 * @param typeMirror
	 * @param environment
	 * @return
	 */
	public static boolean isConsiderateClass(TypeMirror typeMirror, ProcessingEnvironment environment) {
		if (typeMirror == null) {
			return false;
		}
		TypeElement objectElement = environment.getElementUtils().getTypeElement(Object.class.getName());
		if (environment.getTypeUtils().isSameType(objectElement.asType(), typeMirror)) {
			return false;
		}
		TypeElement typeElement = (TypeElement) environment.getTypeUtils().asElement(typeMirror);
		if (typeElement == null) {
			return false;
		}
		return true;
	}

	public static String getASClassName(TypeMirror typeMirror, ProcessingEnvironment environment) {
		String search;
		if (typeMirror == null) {
			return null;
		}
		if (typeMirror.getKind().isPrimitive()) {
			search = typeMirror.toString();
			try {
				return mapping.getString(search); // tous les type primitif devraient avoir un mapping
			} catch (MissingResourceException mre) {
				environment.getMessager().printMessage(Diagnostic.Kind.NOTE, "Not found native ASMapping for : " + search);
			}
		} else if (typeMirror.getKind().equals(TypeKind.VOID)) { // pour les void => void
			return "void";
		} else if (typeMirror.getKind().equals(TypeKind.ARRAY)) { // pour les arrays style byte[] ou long
			try {
				ArrayType arrayType = (ArrayType) typeMirror;
				TypeMirror comp = arrayType.getComponentType();
				if (comp.getKind().isPrimitive()) { // pour byte[] et char[]
					search = comp.toString() + "s";
				} else { // pour Byte[], Character[]
					TypeMirror component = (TypeMirror) arrayType.getComponentType();
					search = environment.getTypeUtils().asElement(component).getSimpleName() + "s";
				}
				return mapping.getString(search);
			} catch (MissingResourceException mre) { // dans les autres cas on laisse Array
				return "Array";
			}
		} else { // dans les autre cas, on récupere l'elementType
			Element element = environment.getTypeUtils().asElement(typeMirror);
			ElementKind kind = element.getKind();
			if (kind.equals(ElementKind.ENUM)) { // les enum sont mappé en String
				return "String";
			}
			if (!kind.equals(ElementKind.CLASS) && !kind.equals(ElementKind.INTERFACE)) {
				return null;
			}
			search = ((TypeElement) element).getQualifiedName().toString(); // par defaut la classe cherché est considerée comme generée
			if (isConsideratedIterable(typeMirror, environment)) {
				search = Collection.class.getName();
			}
			try {
				return mapping.getString(search);
			} catch (MissingResourceException mre) {
			}
		}
		return null;
	}

	/**
	 * Récupere les types des arguments. On n'utilise pas discoverClass car ici, on a juste besoin des types directs, pas des type parametrés
	 *
	 * @param method
	 * @return
	 */
	public static List<String> getASArguments(ExecutableElement methodElement, ProcessingEnvironment environment) {
		ExecutableType methodType = (ExecutableType) methodElement.asType();
		List<String> result = new ArrayList<String>();
		for (TypeMirror type : methodType.getParameterTypes()) {
			String c = getASType(type, environment);
			if (!c.equals("void")) {
				result.add(c); // ce sont des arguments, cela doit donner quelques chose.
			} else {
				environment.getMessager().printMessage(Diagnostic.Kind.WARNING, "getASArgument return void : " + type);
			}
		}
		return result;
	}

	/**
	 * Retourne la classe vu par AS, ou le type mappé par BlazeDS ou la classe elle même qui sera créé et importé
	 *
	 * @param typeElement
	 * @param environment
	 * @return
	 */
	public static String getASType(TypeMirror typeMirror, ProcessingEnvironment environment) {
		String returnType = getASClassName(typeMirror, environment);
		if (returnType == null) {
			returnType = typeMirror.toString();
		}
		return returnType;
	}

	/**
	 * Retourne le nom du field à partir d'une methode ne tient compte que des getter
	 *
	 * @param methodElement
	 * @param environment
	 * @return
	 */
	public static String getFieldName(ExecutableElement methodElement, ProcessingEnvironment environment) {
		String fieldName = null;
		if (isMethodPublicGetter(methodElement, environment)) {
			String methodName = methodElement.getSimpleName().toString();
			if (methodName.startsWith("get")) {
				fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
			}
			if (methodName.startsWith("is")) {
				fieldName = methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
			}
		}
		return fieldName;
	}

	/**
	 * Retourne la methode est un getter public
	 *
	 * @param methodElement
	 * @param environment
	 * @return
	 */
	public static boolean isMethodPublicGetter(ExecutableElement methodElement, ProcessingEnvironment environment) {
		ExecutableType methodType = (ExecutableType) methodElement.asType();
		String methodName = methodElement.getSimpleName().toString();
		return methodElement.getModifiers().contains(Modifier.PUBLIC) && (methodName.startsWith("get") || methodName.startsWith("is")) && !methodType.getReturnType().getKind().equals(TypeKind.VOID);
	}

	/**
	 * Retourne le nom du field etant id de l'entity
	 *
	 * @param typeElement
	 * @param environment
	 * @return
	 */
	public static String getIdFieldName(TypeElement typeElement, ProcessingEnvironment environment) {
		List<VariableElement> fieldsIn = ElementFilter.fieldsIn(typeElement.getEnclosedElements());
		for (VariableElement variableElement : fieldsIn) {
			String name = variableElement.getSimpleName().toString();
			List<? extends AnnotationMirror> annotationMirrors = variableElement.getAnnotationMirrors();
			for (AnnotationMirror anno : annotationMirrors) {
				String annoType = anno.getAnnotationType().toString();
				if("javax.persistence.Id".equals(annoType)) {
					return name;
				}
			}
		}
		return null;
	}

	/**
	 * Retourne le nom du field etant id de l'entity
	 *
	 * @param typeElement
	 * @param environment
	 * @return
	 */
	public static boolean isIdGenerated(TypeElement typeElement, ProcessingEnvironment environment) {
		List<VariableElement> fieldsIn = ElementFilter.fieldsIn(typeElement.getEnclosedElements());
		for (VariableElement variableElement : fieldsIn) {
			String name = variableElement.getSimpleName().toString();
			List<? extends AnnotationMirror> annotationMirrors = variableElement.getAnnotationMirrors();
			for (AnnotationMirror anno : annotationMirrors) {
				String annoType = anno.getAnnotationType().toString();
				if("javax.persistence.GeneratedValue".equals(annoType)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Est ce une entity JPA
	 *
	 * @param Element
	 * @param environment
	 * @return
	 */
	public static boolean isEntity(Element element, ProcessingEnvironment environment) {
		List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
		for (AnnotationMirror anno : annotationMirrors) {
			String annoType = anno.getAnnotationType().toString();
			if("javax.persistence.Entity".equals(annoType)) {
				return true;
			}
		}
		return false;
	}
}
