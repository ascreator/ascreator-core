/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator.core;

import flex2.tools.oem.Library;
import fr.hhdev.ascreator.exceptions.IgnoredClassException;
import fr.hhdev.ascreator.exceptions.IllegalClassException;
import fr.hhdev.ascreator.exceptions.NoCommentClassException;
import fr.hhdev.ascreator.exceptions.NoRemotingClassException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

/**
 *
 * @author HHFrancois
 */
public class ASEnumCreator extends ASCreator {

	public ASEnumCreator(String path, TypeElement typeElement, ProcessingEnvironment environment, Library lib) {
		super(path, typeElement, environment, lib);
	}

	@Override
	protected void checkClass() throws IllegalClassException, IgnoredClassException {
	}

	@Override
	protected boolean isAbstract() {
		return false;
	}

	@Override
	protected boolean isBindable() {
		return true;
	}

	@Override
	protected String getRemotingClass() throws NoRemotingClassException {
		throw new NoRemotingClassException();
	}

	@Override
	protected boolean isInterface() {
		return false;
	}

	@Override
	protected String getASClassName() {
		return this.typeElement.getSimpleName().toString();
	}

	@Override
	protected Collection<String> getASExtendClassNames() {
		return Collections.EMPTY_LIST;
	}

	@Override
	protected Collection<String> getASIImplementClassNames() {
		return Collections.EMPTY_LIST;
	}

	@Override
	protected Set<String> getASImports() {
		return Collections.EMPTY_SET;
	}

	@Override
	protected Map<String, String> getASEvents() {
		return Collections.EMPTY_MAP;
	}

	@Override
	protected Set<String> getASNamespaces() {
		return Collections.EMPTY_SET;
	}

	@Override
	protected void writeASBodyConstructor() {
	}

	@Override
	protected void writeASFieldsAndMethods() {
		Collection<String> enumValues = new ArrayList<String>();
		for (Element c : typeElement.getEnclosedElements()) {
			if(c.getKind().equals(ElementKind.ENUM_CONSTANT)) {
				inserASField(c.getSimpleName().toString(), "String");
				enumValues.add(c.getSimpleName().toString());
			}
		}
					  
		out.println("		public static function get enumConstants():Array {");
		// Ecrit le corp de la methode
		out.print("			return [");
		Iterator<String> enumIterator = enumValues.iterator();
		while(enumIterator.hasNext()) {
			String enumValue = enumIterator.next();
			out.print("\""+enumValue+"\"");
			if(enumIterator.hasNext()) {
				out.print(", ");
			}
		}
		out.println("];");
		out.println("		}");
		out.println("");
	}

	private void inserASField(String fieldName, String fieldType) {
		out.println("		public static const " + fieldName + ":" + fieldType + " = \""+fieldName+"\";");
		out.println("");
	}

	@Override
	protected String getASClassComment() throws NoCommentClassException {
		throw new NoCommentClassException();
	}
}
