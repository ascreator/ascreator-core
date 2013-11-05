/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator.core;

import flex2.tools.oem.Library;
import fr.hhdev.ascreator.exceptions.ASFileExistException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

/**
 *
 * @author hhfrancois
 */
public abstract class MXCreator extends Creator {

	protected String projectName = null;

	public MXCreator(String path, TypeElement typeElement, ProcessingEnvironment environment, Library lib) {
		super(path, typeElement, environment, lib);
		projectName = environment.getOptions().get("projectName");
	}

	/**
	 * Retourne le nom de la classe as a créer.
	 *
	 * @return
	 */
	protected abstract String getMXClassName();

	protected abstract String getMXSuperClassName();

	protected abstract String getMXNSSuperClass();

	protected abstract Map<String, String> getNamespaces();

	protected abstract Map<String, String> getHeadAttributes();

	protected abstract Map<String, String> getEvents();

	protected abstract Collection<String> getBundles();

	protected abstract Collection<String> getInterfaces();

	protected abstract Collection<String> getImports();

	protected abstract void writeBodyScripts();

	protected abstract void writeBodyXML();

	private void writeMetadatas() {
		out.println("	<mx:Metadata>");
		Map<String, String> events = getEvents();
		for (String event : events.keySet()) {
			out.println("		[Event(name=\"" + event + "\", type=\"" + events.get(event) + "\")]");
		}
		Collection<String> bundles = getBundles();
		for (String bundle : bundles) {
			out.println("		[ResourceBundle(\"" + bundle + "\")]");
		}
		out.println("	</mx:Metadata>");
	}

	private void writeXML() {
		out.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		out.print("<" + getMXNSSuperClass() + ":" + getMXSuperClassName());
		Map<String, String> namespaces = getNamespaces();
		for (String ns : namespaces.keySet()) {
			out.print(" xmlns:" + ns + "=\"" + namespaces.get(ns) + "\"");
		}
		Collection<String> interfaces = getInterfaces();
		if (!interfaces.isEmpty()) {
			out.println();
			out.print(" implements=\"");
			Iterator<String> iterator = interfaces.iterator();
			do {
				out.print(iterator.next());
				if (iterator.hasNext()) {
					out.print(",");
				}
			} while (iterator.hasNext());
			out.print("\"");
		}
		Map<String, String> headAttributes = getHeadAttributes();
		if(!headAttributes.isEmpty()) {
			out.println();
			for (String attr : headAttributes.keySet()) {
				out.print(" " + attr + "=\"" + headAttributes.get(attr) + "\"");
			}
		}
		out.println(">");
		writeMetadatas();
		writeScripts();
		writeBodyXML();
		out.print("</" + getMXNSSuperClass() + ":" + getMXSuperClassName() + ">");
	}

	private void writeScripts() {
		out.println("	<mx:Script>");
		out.println("		<![CDATA[");
		Collection<String> imports = getImports();
		for (String imp : imports) {
			out.println("			import " + imp + ";");
		}
		writeBodyScripts();
		out.println("		]]>");
		out.println("	</mx:Script>");
	}

	/**
	 * Ecrit la classe
	 */
	@Override
	public void write() {
		try {
			String pack = getMXPackage();
			String className = getMXClassName();
			createFile(pack, className);
			writeXML();
		} catch (ASFileExistException ex) {
			// la classe existe déja, on ignore
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	/**
	 * Retourne le package pour mxml
	 *
	 * @return
	 */
	protected String getMXPackage() {
		return environment.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
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
		File file = new File(rep, className + ".mxml");
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
}
