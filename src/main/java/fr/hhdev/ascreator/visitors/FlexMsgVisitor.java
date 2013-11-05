/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator.visitors;

import flex2.tools.oem.Library;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;

/**
 *
 * @author hhfrancois
 */
public class FlexMsgVisitor extends AbstractFlexMsgVisitor {

	public FlexMsgVisitor(ProcessingEnvironment environment, String path, Library lib) {
		super(environment, path, lib);
	}

	@Override
	public Void visitType(TypeElement e, Set<String> p) {
		TypeElement typeElement = (TypeElement) e;
		List<? extends AnnotationMirror> annotationMirrors = e.getAnnotationMirrors();
		for (AnnotationMirror annotationMirror : annotationMirrors) {
			DeclaredType annotationType = annotationMirror.getAnnotationType();
			Element annotationElement = annotationType.asElement();
			if (annotationElement.getSimpleName().toString().equals("FlexMsg")) {
				FlexError flexError = computeFlexError(annotationMirror, annotationElement);
				flexError.setClassEntry(typeElement.getQualifiedName().toString());
//				reportFlexError(flexError, p);
				String commonException = flexError.getMsg();
				List<ExecutableElement> methodsIn = ElementFilter.methodsIn(typeElement.getEnclosedElements());
				for (ExecutableElement executableElement : methodsIn) {
					FlexError flexError2 = getGenericMsgForMethod(executableElement, flexError.getLocale());
					if(flexError2!=null) {
						flexError.setMethodEntry(executableElement.getSimpleName().toString());
						flexError.setMsg(flexError2.getMsg()+"\n"+commonException);
						reportFlexError(flexError, p);
					}
				}
			}
		}
		return null;
	}

	@Override
	public Void visitExecutable(ExecutableElement e, Set<String> p) {
		TypeElement typeElement = (TypeElement) e.getEnclosingElement();
		String classEntry = typeElement.getQualifiedName().toString();
		List<? extends AnnotationMirror> annotationMirrors = e.getAnnotationMirrors();
		for (AnnotationMirror annotationMirror : annotationMirrors) {
			DeclaredType annotationType = annotationMirror.getAnnotationType();
			Element annotationElement = annotationType.asElement();
			if (annotationElement.getSimpleName().toString().equals("FlexMsg")) {
				FlexError flexError = computeFlexError(annotationMirror, annotationElement);
				flexError.setClassEntry(classEntry);
				flexError.setMethodEntry(e.getSimpleName().toString());
				if(!flexError.getExceptionEntry().equals("java.lang.Void")) {
					reportFlexError(flexError, p);
				}
			}
		}
		return null;
	}

	/**
	 * Récupere uniquement le FlexError generic pour une methode et une locale donnée
	 * @param e
	 * @param locale
	 * @return 
	 */
	private FlexError getGenericMsgForMethod(ExecutableElement e, String locale) {
		List<? extends AnnotationMirror> annotationMirrors = e.getAnnotationMirrors();
		for (AnnotationMirror annotationMirror : annotationMirrors) {
			DeclaredType annotationType = annotationMirror.getAnnotationType();
			Element annotationElement = annotationType.asElement();
			if (annotationElement.getSimpleName().toString().equals("FlexMsgs")) {
				Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();
				for (ExecutableElement executableElement : elementValues.keySet()) {
					if (executableElement.getSimpleName().toString().equals("value")) {
						AnnotationValue valueValue = elementValues.get(executableElement);
						List<AnnotationMirror>  list = (List<AnnotationMirror>) valueValue.getValue();
						for (AnnotationMirror annotationMirror1 : list) {
							FlexError flexError = computeFlexError(annotationMirror1, annotationMirror1.getAnnotationType().asElement());
							if(flexError.getExceptionEntry().equals("java.lang.Void") && flexError.getLocale().equals(locale)) {
								return flexError;
							}
						}
					}
				}
			} else if (annotationElement.getSimpleName().toString().equals("FlexMsg")) {
				FlexError flexError = computeFlexError(annotationMirror, annotationElement);
				if(flexError.getExceptionEntry().equals("java.lang.Void") && flexError.getLocale().equals(locale)) {
					return flexError;
				}
			}
		}
		return null;
	}
}
