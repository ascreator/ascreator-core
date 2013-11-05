/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator.visitors;

import flex2.tools.oem.Library;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

/**
 *
 * @author hhfrancois
 */
public abstract class AbstractFlexMsgVisitor implements ElementVisitor<Void, Set<String>> {
	protected ProcessingEnvironment environment;
	protected String path;
	protected Library lib;
	protected String bundleName;

	public AbstractFlexMsgVisitor(ProcessingEnvironment environment, String path, Library lib) {
		this.environment=environment;
		this.path = path;
		this.lib = lib;
		this.bundleName = environment.getOptions().get("projectName");
	}
	
	/**
	 * Genere le FlexError sans classEntry, ni methoEntry
	 * @param annotationMirror
	 * @param annotationElement
	 * @return 
	 */
	protected FlexError computeFlexError(AnnotationMirror annotationMirror, Element annotationElement) {
		AnnotationValue exceptionValue = null;
		AnnotationValue valueValue = null;
		AnnotationValue localeValue = null;
		List<? extends Element> enclosedElements = annotationElement.getEnclosedElements();
		for (Element enclosedElement : enclosedElements) {
			if (enclosedElement.getKind().equals(ElementKind.METHOD)) {
				ExecutableElement executableElement = (ExecutableElement) enclosedElement;
				if (executableElement.getSimpleName().toString().equals("exception")) {
					exceptionValue = executableElement.getDefaultValue();
				} else if (executableElement.getSimpleName().toString().equals("value")) {
					valueValue = executableElement.getDefaultValue();
				} else if (executableElement.getSimpleName().toString().equals("locale")) {
					localeValue = executableElement.getDefaultValue();
				}
			}
		}
		Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();
		for (ExecutableElement executableElement : elementValues.keySet()) {
			if (executableElement.getSimpleName().toString().equals("exception")) {
				exceptionValue = elementValues.get(executableElement);
			} else if (executableElement.getSimpleName().toString().equals("value")) {
				valueValue = elementValues.get(executableElement);
			} else if (executableElement.getSimpleName().toString().equals("locale")) {
				localeValue = elementValues.get(executableElement);
			}
		}
		return new FlexError(null, null, exceptionValue.getValue().toString(), valueValue.getValue().toString(), localeValue.getValue().toString());
	}

	protected void reportFlexError(FlexError flexError, Set<String> locales) {
		Writer errors = null;
		OutputStream o = null;
		try {
			if(!locales.contains(flexError.getLocale())) {
				locales.add(flexError.getLocale());
			}
			File prop = getBundle(flexError.getLocale());
			o = new FileOutputStream(prop, true);
			errors = new OutputStreamWriter(o, "UTF-8");
			errors.append(flexError.toString());
			errors.append("\n");
			errors.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (o != null) {
				try {
					o.close();
				} catch (IOException ex) {
				}
			}
			if (errors != null) {
				try {
					errors.close();
				} catch (IOException ex) {
				}
			}
		}
	}

	/**
	 * Récupere le fichier d'erreur
	 *
	 * @param loc
	 * @return
	 * @throws IOException
	 */
	private File getBundle(String loc) throws IOException {
		File locales = new File(path, "locale");
		if (!locales.exists()) {
			locales.mkdir();
		}
		File locale = new File(locales, loc);
		if (!locale.exists()) {
			locale.mkdir();
		}
		File errors = new File(locale, bundleName + ".properties");
		if (!errors.exists()) {
			errors.createNewFile();
		}
		return errors;
	}

	@Override
	public Void visit(Element e) {
		return null;
	}

	@Override
	public Void visit(Element e, Set<String> p) {
		return null;
	}

	@Override
	public Void visitVariable(VariableElement e, Set<String> p) {
		return null;
	}


	@Override
	public Void visitTypeParameter(TypeParameterElement e, Set<String> p) {
		return null;
	}

	@Override
	public Void visitUnknown(Element e, Set<String> p) {
		return null;
	}

	@Override
	public Void visitPackage(PackageElement e, Set<String> p) {
		return null;
	}

}
