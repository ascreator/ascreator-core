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

/**
 *
 * @author hhfrancois
 */
public class FlexMsgsVisitor extends AbstractFlexMsgVisitor {
	
	public FlexMsgsVisitor(ProcessingEnvironment environment, String path, Library lib) {
		super(environment, path, lib);
	}

	@Override
	public Void visitType(TypeElement e, Set<String> p) {
		TypeElement typeElement = (TypeElement) e;
		String classEntry = typeElement.getQualifiedName().toString() + "." + e.getSimpleName();
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
							if(!flexError.getExceptionEntry().equals("java.lang.Void")) {
								flexError.setClassEntry(classEntry);
								flexError.setMethodEntry(e.getSimpleName().toString());
								reportFlexError(flexError, p);
							}
						}
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
			if (annotationElement.getSimpleName().toString().equals("FlexMsgs")) {
				Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();
				for (ExecutableElement executableElement : elementValues.keySet()) {
					if (executableElement.getSimpleName().toString().equals("value")) {
						AnnotationValue valueValue = elementValues.get(executableElement);
						List<AnnotationMirror>  list = (List<AnnotationMirror>) valueValue.getValue();
						for (AnnotationMirror annotationMirror1 : list) {
							FlexError flexError = computeFlexError(annotationMirror1, annotationMirror1.getAnnotationType().asElement());
							if(!flexError.getExceptionEntry().equals("java.lang.Void")) {
								flexError.setClassEntry(classEntry);
								flexError.setMethodEntry(e.getSimpleName().toString());
								reportFlexError(flexError, p);
							}
						}
					}
				}
			}
		}
		return null;
	}
}
