/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator.core;

import flex2.tools.oem.Library;
import fr.hhdev.ascreator.exceptions.ASFileExistException;
import fr.hhdev.ascreator.exceptions.IgnoredClassException;
import fr.hhdev.ascreator.exceptions.IllegalClassException;
import fr.hhdev.ascreator.exceptions.NoCommentClassException;
import fr.hhdev.ascreator.exceptions.NoRemotingClassException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 *
 * @author HHFrancois
 */
public abstract class ASCreator extends Creator {

	protected String projectName = null;

	public ASCreator(String path, TypeElement typeElement, ProcessingEnvironment environment, Library lib) {
		super(path, typeElement, environment, lib);
		projectName = environment.getOptions().get("projectName");
	}

	/**
	 * Retourne le package pour as
	 *
	 * @return
	 */
	protected String getASPackage() {
		return environment.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
	}

	/**
	 * test la class pour savoir s'il faut la créer.
	 *
	 * @return
	 */
	protected abstract void checkClass() throws IllegalClassException, IgnoredClassException;

	/**
	 * Retourne si la classe est abstraite
	 *
	 * @return
	 */
	protected abstract boolean isAbstract();

	/**
	 * Retourne si la classe est bindable cas des entities
	 *
	 * @return
	 */
	protected abstract boolean isBindable();

	/**
	 * Retourne la classe java correspondante cas des entities
	 *
	 * @return
	 */
	protected abstract String getRemotingClass() throws NoRemotingClassException;

	/**
	 * Retourne si la classe est une interface
	 *
	 * @return
	 */
	protected abstract boolean isInterface();

	/**
	 * Retourne le nom de la classe as a créer.
	 *
	 * @return
	 */
	protected abstract String getASClassName();

	/**
	 * Retourne le commentaire de la classe as a créer.
	 *
	 * @return
	 */
	protected abstract String getASClassComment() throws NoCommentClassException;

	/**
	 * Retourne les noms des classes à étendre.
	 *
	 * @return
	 */
	protected abstract Collection<String> getASExtendClassNames();

	/**
	 * Retourne les nom des classes à implementer.
	 *
	 * @return
	 */
	protected abstract Collection<String> getASIImplementClassNames();

	/**
	 * Retourne la liste des classes à rajouter dans les imports
	 *
	 * @return
	 */
	protected abstract Set<String> getASImports();

	/**
	 * Retourne la liste des evenement que la classe as doit exposer. Retourne des couples name/type
	 *
	 * @return
	 */
	protected abstract Map<String, String> getASEvents();

	/**
	 * Retourne la liste des espaces de nom nécessaire à la classe. ex : mx_internal
	 *
	 * @return
	 */
	protected abstract Set<String> getASNamespaces();

	/**
	 * Ecrit le corp du constructeur de la classe.
	 *
	 */
	protected abstract void writeASBodyConstructor();

	/**
	 * Ecrit les champs et les methodes de la classe.
	 *
	 */
	protected abstract void writeASFieldsAndMethods();

	/**
	 * Ecrit la classe
	 */
	@Override
	public void write() throws IllegalClassException {
		try {
			checkClass();
			String pack = getASPackage();
			String className = getASClassName();
			createFile(pack, className);
			out.println("package " + pack + " {");
			out.println("");
			insertASImports();
			insertASNamespaces();
			insertASEvents();
			insertClass();
			out.print("}");
		} catch (ASFileExistException ex) {
			// la classe existe déja, on ignore
		} catch (IgnoredClassException ex) {
			// la classe n'a pas à etre créer
			environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error during creation Class Files : " + ex.getMessage());
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	protected void insertClass() {
		try {
			String comment = getASClassComment().replaceAll("\n", "\n	 *");
			out.println("	/**");
			out.println("	 *" + comment + "/");
		} catch (NoCommentClassException e) {
		}
		if (isBindable()) {
			out.println("	[Bindable]");
		}
		if (isAbstract()) {
			out.println("	[ExcludeClass]");
		}
		try {
			String remotingClass = getRemotingClass();
			out.println("	[RemoteClass(alias=\"" + remotingClass + "\")]");
		} catch (NoRemotingClassException ex) {
		}
		out.print("	public " + (isInterface() ? "interface" : "class") + " " + getASClassName() + " ");
		Collection<String> superclasses = getASExtendClassNames();
		if (superclasses != null && !superclasses.isEmpty()) {
			out.print("extends");
			Iterator<String> ite = superclasses.iterator();
			while (ite.hasNext()) {
				String superclass = ite.next();
				out.print(" " + superclass);
				if (ite.hasNext()) {
					out.print(",");
				} else {
					out.print(" ");
				}
			}
		}
		Collection<String> interfaces = getASIImplementClassNames();
		if (interfaces != null && !interfaces.isEmpty()) {
			out.print("implements");
			Iterator<String> ite = interfaces.iterator();
			while (ite.hasNext()) {
				String interf = ite.next();
				out.print(" " + interf);
				if (ite.hasNext()) {
					out.print(",");
				} else {
					out.print(" ");
				}
			}
		}
		out.println("{");
		out.println("");
		insertASConstructor();
		writeASFieldsAndMethods();
		out.println("	}");
	}

	/**
	 * Constructeur de la classe AS
	 *
	 */
	protected void insertASConstructor() {
		if (!isInterface()) {
			out.println("		public function " + getASClassName() + "() {");
			writeASBodyConstructor();
			out.println("		}");
			out.println("");
		}
	}

	/**
	 * Créé le fichier .as representant la classe
	 *
	 * @param pack
	 * @param className
	 * @return
	 */
	protected void createFile(final String pack, final String className) throws ASFileExistException {
		File rep = new File(path, pack.replaceAll("\\.", "/"));
		if (!rep.exists()) {
			rep.mkdirs();
		}
		File file = new File(rep, className + ".as");
		if (file.exists()) {
			throw new ASFileExistException();
		}
		if (lib != null) {
			lib.addComponent(file);
		}
		try {
			out = new PrintWriter(new FileWriter(file));
		} catch (FileNotFoundException ex) {
		} catch (IOException ex) {
		}
	}

	/**
	 * insert les imports.
	 */
	protected void insertASImports() {
		Set<String> imports = getASImports();
		if (imports != null) {
			for (String imp : imports) {
				out.println("	import " + imp + ";");
			}
			if (imports.size() > 0) {
				out.println("");
			}
		}
	}

	/**
	 * insert les evenements.
	 */
	protected void insertASEvents() {
		Map<String, String> events = getASEvents();
		if (events != null) {
			for (String name : events.keySet()) {
				String event = events.get(name);
				out.println("	[Event(name=\"" + name + "\", type=\"" + event + "\")]");
			}
			if (events.size() > 0) {
				out.println("");
			}
		}
	}

	/**
	 * insert les evenements.
	 */
	protected void insertASNamespaces() {
		Set<String> nss = getASNamespaces();
		if (nss != null) {
			for (String ns : nss) {
				out.println("	use namespace " + ns + ";");
			}
			if (nss.size() > 0) {
				out.println("");
			}
		}
	}
}
