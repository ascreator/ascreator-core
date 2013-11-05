/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator.processors;

import flex2.tools.oem.Configuration;
import flex2.tools.oem.Library;
import flex2.tools.oem.Report;
import fr.hhdev.ascreator.annotations.FlexDestination;
import fr.hhdev.ascreator.annotations.FlexMsg;
import fr.hhdev.ascreator.annotations.FlexMsgs;
import fr.hhdev.ascreator.annotations.entities.FlexObject;
import fr.hhdev.ascreator.annotations.entities.FlexSeeAlso;
import fr.hhdev.ascreator.visitors.FlexDestinationVisitor;
import fr.hhdev.ascreator.visitors.FlexMsgVisitor;
import fr.hhdev.ascreator.visitors.FlexMsgsVisitor;
import fr.hhdev.ascreator.visitors.FlexObjectVisitor;
import fr.hhdev.ascreator.visitors.FlexSeeAlsoVisitor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import resources.Resources;

/**
 *
 * @author hhfrancois
 */
@SupportedAnnotationTypes(value = {"hhf.flex.annotations.FlexDestination", "hhf.flex.annotations.FlexObject", "hhf.flex.annotations.FlexSeeAlso", "hhf.flex.annotations.FlexMsgs", "hhf.flex.annotations.FlexMsg"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedOptions({"ignoreDeprecated", "destDir", "projectName"})
public class FlexProcessor extends AbstractProcessor {

	public FlexProcessor() {
		super();
		locales = new HashSet<String>();
		flexCRUDInterfaces = new HashMap<String, String>();
	}
	/**
	 * Taille dubuffer pour copie des fichiers.
	 */
	private final String ASCREATORLIBNAME = "ascreator";
	private final int BUFFERSIZE = 1024;
	private String sourcesDir = File.separator + "sources";
	private String swcDir = File.separator + "swc";
	private String libraryName = null;
	private String projectName = null;
	private ProcessingEnvironment environment;
	private Library lib;
	private Set<String> locales;
	private Map<String, String> flexCRUDInterfaces;
	private boolean includeSvcLib = true;
	private int flexMajorVersion = 3;

	@Override
	public void init(ProcessingEnvironment environment) {
		super.init(environment);
		this.environment = environment;
		// Répertoire de destination des sources génerées et du swc
		if (environment.getOptions().containsKey("destDir")) {
			sourcesDir = environment.getOptions().get("destDir") + sourcesDir;
			swcDir = environment.getOptions().get("destDir") + swcDir;
		}
		File swfdirFile = new File(swcDir);
		if (swfdirFile.exists()) {
			removeRecursively(swfdirFile);
		}
		swfdirFile.mkdir();
		// Creation de la librairie et determination de la version du compilateur
		lib = new Library();
		Report report = lib.getReport();
		Pattern p = Pattern.compile("\\d+");
		Matcher m = p.matcher(report.getCompilerVersion());
		environment.getMessager().printMessage(Diagnostic.Kind.NOTE, report.getCompilerVersion());
		if (m.find()) {
			flexMajorVersion = Integer.parseInt(m.group());
		}
		// Nom de la librairie génerée
		if (environment.getOptions().containsKey("projectName")) {
			projectName = environment.getOptions().get("projectName");
			libraryName = projectName + flexMajorVersion + ".swc";
		} else {
			throw new RuntimeException("Project Name is not defined, specify it on Compiler Options : -AprojectName=projectName.swc");
		}
		// Suppression des fichiers precedement generés
		File sourceDirFile = new File(sourcesDir);
		if (sourceDirFile.exists()) {
			removeRecursively(sourceDirFile);
		}
		sourceDirFile.mkdir();
		// Doit on inclure la librairie generé par blazeds-ejb
		if (!projectName.equals(ASCREATORLIBNAME)) { // si c'est pas la librairie ascreator
			try {
				copyFile(ASCREATORLIBNAME+flexMajorVersion+".swc", "", false);
			} catch (IOException ex) {
				environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error during the copy of the ascreator library : " + ex.getMessage());
			}
		}
	}

	/**
	 * Initialise la librairie flex
	 */
	private void confLibrary() {
		try {
			createAbstractService();
			createFlexCRUDInterface();
			copyFile("ServiceConfig.as", "flex/services", true);
			copyFile("ItemSelector.as", "flex/services", true);
			copyFile("ItemEditor.as", "flex/services", true);
			copyFile("ServiceEvent.as", "flex/services/events", true);
			copyFile("ItemEvent.as", "flex/services/events", true);
			copyFile("ascreatorIcons.properties", "", false);
			copyFile("add16.png", "icons", false);
			copyFile("cancel16.png", "icons", false);
			copyFile("edit16.png", "icons", false);
			copyFile("refresh16.png", "icons", false);
			copyFile("remove16.png", "icons", false);
			copyFile("save16.png", "icons", false);
			copyFile("tools4Flex.swc", "", false);
		} catch (IOException ex) {
			environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error during the copy of the dependance as : " + ex.getMessage());
		}
		Configuration conf = lib.getDefaultConfiguration();
		conf.allowSourcePathOverlap(true);
		conf.setSourcePath(new File[]{new File(sourcesDir)});
		// si il y a des locales, on les ajoute
		if (!locales.isEmpty()) {
			conf.addSourcePath(new File[]{new File(sourcesDir, "locale/{locale}")});
			conf.setLocale(locales.toArray(new String[]{}));
		}
		// metatdata gardées au runtime
		conf.setActionScriptMetadata(new String[]{"Bindable", "Id", "ManyToOne", "OneToOne", "ManyToMany", "OneToOne"});
		// inclusion des librairies
		// ascreator.swc
		File aslib = new File(sourcesDir, ASCREATORLIBNAME+flexMajorVersion+".swc");
		if (aslib.exists()) {
			conf.addLibraryPath(new File[]{aslib});
		}
		// tools4Flex.swc
		aslib = new File(sourcesDir, "tools4Flex.swc");
		if (aslib.exists()) {
			conf.addLibraryPath(new File[]{aslib});
		}
		lib.setConfiguration(conf);
		lib.setOutput(new File(swcDir, libraryName));
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		ElementVisitor visitor = new FlexDestinationVisitor(environment, sourcesDir, lib);
		for (Element element : roundEnv.getElementsAnnotatedWith(FlexDestination.class)) {
			element.accept(visitor, flexCRUDInterfaces);
		}
		visitor = new FlexObjectVisitor(environment, sourcesDir, lib);
		for (Element element : roundEnv.getElementsAnnotatedWith(FlexObject.class)) {
			element.accept(visitor, null);
		}
		visitor = new FlexSeeAlsoVisitor(environment, sourcesDir, lib);
		for (Element element : roundEnv.getElementsAnnotatedWith(FlexSeeAlso.class)) {
			element.accept(visitor, null);
		}
		visitor = new FlexMsgVisitor(environment, sourcesDir, lib);
		for (Element element : roundEnv.getElementsAnnotatedWith(FlexMsg.class)) {
			element.accept(visitor, locales);
		}
		visitor = new FlexMsgsVisitor(environment, sourcesDir, lib);
		for (Element element : roundEnv.getElementsAnnotatedWith(FlexMsgs.class)) {
			element.accept(visitor, locales);
		}
		confLibrary();
		compileLib();
		return true;
	}

	/**
	 * Compile la librairie
	 */
	private void compileLib() {
		try {
			Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
			Report report = lib.getReport();
			environment.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generate sources : ");
			String[] list = report.getSourceNames(Report.COMPILER);
			if (list != null) {
				for (String source : list) {
					environment.getMessager().printMessage(Diagnostic.Kind.NOTE, source);
				}
			}
			lib.build(true);
		} catch (IOException ex) {
			environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error during the compilation of " + projectName + ".swc : " + ex.getMessage());
		}
	}

	/**
	 * Copie un fichier dans le repertoire destination.
	 *
	 * @param source : fichier à copier
	 * @param dest : repertoire de sortie
	 * @throws IOException : erreur d'IO
	 */
	private void copyFile(final String source, final String dest, boolean add) throws IOException {
		OutputStream out = null;
		InputStream in = null;
		try {
			in = Resources.class.getResourceAsStream(source);
			File rep = new File(sourcesDir, dest);
			if (!rep.exists()) {
				rep.mkdirs();
			}
			File asFile = new File(rep, source);
			out = new FileOutputStream(asFile);
			byte[] buffer = new byte[BUFFERSIZE];
			for (int read = in.read(buffer); read > 0; read = in.read(buffer)) {
				out.write(buffer, 0, read);
			}
			if (add) {
				lib.addComponent(asFile);
			}
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
			}
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Supprime les fichiers génerés
	 *
	 * @param file
	 */
	private void removeRecursively(File file) {
		if (!file.isDirectory()) {
			file.delete();
		} else if (file.listFiles().length == 0) {
			file.delete();
		} else {
			for (File subfile : file.listFiles()) {
				removeRecursively(subfile);
			}
		}
	}

	private void createFlexCRUDInterface() {
		File rep = new File(sourcesDir, projectName);
		if (!rep.exists()) {
			rep.mkdirs();
		}
		File asFile = new File(rep, "FlexCRUDInterface.as");
		lib.addComponent(asFile);
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(asFile));
			out.println("package " + projectName + " {");
			for (String impt : flexCRUDInterfaces.values()) {
				out.println("	import "+impt+";");
			}
			out.println("");
			out.println("	import mx.rpc.AsyncToken;");
			out.println("");
			out.println("	public class FlexCRUDInterface {");
			out.println("		protected static var mapService:Object = null;");
			out.println("		protected static function getCrudInterface(entity:String):Object {");
			out.println("			return FlexCRUDInterface.mapService[entity];");
			out.println("		}");
			out.println("		");
			out.println("		public function FlexCRUDInterface() {");
			out.println("			if(FlexCRUDInterface.mapService==null) {");
			out.println("				FlexCRUDInterface.mapService = {};");
			for (String cls : flexCRUDInterfaces.keySet()) {
				out.println("				FlexCRUDInterface.mapService[\""+cls+"\"] = new "+flexCRUDInterfaces.get(cls)+"();");
			}
			out.println("			}");
			out.println("		}");
			out.println("		");
			out.println("");
			out.println("		public var entityClass:String = null;");
			out.println("		public function persist(t:Object):AsyncToken {");
			out.println("			return FlexCRUDInterface.getCrudInterface(entityClass).persist(t);");
			out.println("		}");
			out.println("		public function merge(t:Object):AsyncToken {");
			out.println("			return FlexCRUDInterface.getCrudInterface(entityClass).merge(t);");
			out.println("		}");
			out.println("		public function remove(id:Object):AsyncToken {");
			out.println("			return FlexCRUDInterface.getCrudInterface(entityClass).remove(id);");
			out.println("		}");
			out.println("		public function find(id:Object):AsyncToken {");
			out.println("			return FlexCRUDInterface.getCrudInterface(entityClass).find(id);");
			out.println("		}");
			out.println("		public function findAll():AsyncToken {");
			out.println("			return FlexCRUDInterface.getCrudInterface(entityClass).findAll()");
			out.println("		}");
			out.println("		public function findRange(page:int, nb:int):AsyncToken {");
			out.println("			return FlexCRUDInterface.getCrudInterface(entityClass).findRange(page, nb);");
			out.println("		}");
			out.println("		public function count():AsyncToken {");
			out.println("			return FlexCRUDInterface.getCrudInterface(entityClass).count();");
			out.println("		}");
			out.println("	}");
			out.println("}");
		} catch (FileNotFoundException ex) {
		} catch (IOException ex) {
			environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error during the creation of as dependance : FlexCRUDInterface.as : " + ex.getMessage());
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
	private void createAbstractService() {
		boolean bundle = !locales.isEmpty();
		File rep = new File(sourcesDir, projectName);
		if (!rep.exists()) {
			rep.mkdirs();
		}
		File asFile = new File(rep, "AbstractService.as");
		lib.addComponent(asFile);
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(asFile));
			out.println("package " + projectName + " {");
			out.println("	import flash.events.EventDispatcher;");
			out.println("	import flex.services.events.ServiceEvent;");
			if (flexMajorVersion == 3) {
				out.println("	import mx.core.Application;");
			} else {
				out.println("	import mx.core.FlexGlobals;");
			}
			out.println("	import flex.services.ServiceConfig;"); 
			out.println("	import mx.rpc.AsyncToken;");
			out.println("	import mx.core.mx_internal;");
			out.println("	import mx.messaging.Channel;");
			out.println("	import mx.messaging.ChannelSet;");
			out.println("	import mx.messaging.config.ServerConfig;");
			out.println("	import mx.rpc.AbstractOperation;");
			out.println("	import mx.rpc.IResponder;");
			out.println("	import mx.rpc.Responder;");
			out.println("	import mx.rpc.Fault;");
			out.println("	import mx.rpc.events.FaultEvent;");
			out.println("	import mx.rpc.events.ResultEvent;");
			out.println("	import mx.rpc.remoting.RemoteObject;");
			out.println("	import mx.utils.ObjectUtil;");
			out.println("	import mx.utils.StringUtil;");
			out.println("	use namespace mx_internal;");
			out.println("	[ExcludeClass]");
			if (bundle) {
				out.println("	[ResourceBundle(\"" + projectName + "\")]");
			}
			out.println("	public class AbstractService extends EventDispatcher {");
			out.println("		protected var serviceControl:RemoteObject;");
			out.println("		protected var rserviceControl:RemoteObject;");
			out.println("		public var dispatchEventService:Boolean = true;");
			out.println("		[Inspectable(defaultValue=true, verbose=1, type=\"Boolean\")]");
			out.println("		public function set showBusyCursor(showBusyCursor:Boolean):void {");
			out.println("			this.serviceControl.showBusyCursor = showBusyCursor;");
			out.println("		}");
			out.println("		public function get showBusyCursor():Boolean {");
			out.println("			return this.serviceControl.showBusyCursor;");
			out.println("		}");
			out.println("		public function AbstractService(destination:String) {");
			out.println("			this.serviceControl = new RemoteObject();");
			out.println("			this.rserviceControl = new RemoteObject();");
			out.println("			this.serviceControl.destination = destination;");
			out.println("			this.rserviceControl.destination = \"hhf.flex.ejbs.BlazeDSServices\";");
			out.println("			this.serviceControl.showBusyCursor = false;");
			out.println("		}");
			out.println("		protected function getServiceControl():RemoteObject {");
			out.println("			if(!this.serviceControl.endpoint) {");
			out.println("				this.serviceControl.endpoint = ServiceConfig.endpoint;");
			out.println("			}");
			out.println("			return this.serviceControl;");
			out.println("		}");
			out.println("		protected function getRemoteServiceControl():RemoteObject {");
			out.println("			if(!this.rserviceControl.endpoint) {");
			out.println("				this.rserviceControl.endpoint = ServiceConfig.endpoint;");
			out.println("			}");
			out.println("			return this.rserviceControl;");
			out.println("		}");
			out.println("		protected function getResponder(operationName:String, uid:String, token:AsyncToken):IResponder {");
			out.println("			var obj:AbstractService = this;");
			out.println("			return new Responder(");
			out.println("				function(event:ResultEvent):void {");
			out.println("					token.mx_internal::applyResult(event);");
			out.println("					if(obj.dispatchEventService) {");
			if (flexMajorVersion == 3) {
				out.println("						Application.application.dispatchEvent(new ServiceEvent(ServiceEvent.SUCCESS, uid, ObjectUtil.getClassInfo(obj).name+\".\"+operationName));");
			} else {
				out.println("						FlexGlobals.topLevelApplication.dispatchEvent(new ServiceEvent(ServiceEvent.SUCCESS, uid, ObjectUtil.getClassInfo(obj).name+\".\"+operationName));");
			}
			out.println("					}");
			out.println("				},");
			out.println("				function(event:FaultEvent):void {");
			out.println("					if(event.cancelable) {");
			out.println("						event.stopImmediatePropagation();");
			out.println("						event.preventDefault();");
			out.println("						event.stopPropagation();");
			out.println("						var evt:FaultEvent = new FaultEvent(FaultEvent.FAULT, false, false, getLocalizedFault(event.fault), event.token, event.message);");
			out.println("						token.mx_internal::applyFault(evt);");
			out.println("						if(obj.dispatchEventService) {");
			if (flexMajorVersion == 3) {
				out.println("							Application.application.dispatchEvent(new ServiceEvent(ServiceEvent.FAIL, uid, ObjectUtil.getClassInfo(obj).name+\".\"+operationName, null, event.message));");
			} else {
				out.println("							FlexGlobals.topLevelApplication.dispatchEvent(new ServiceEvent(ServiceEvent.FAIL, uid, ObjectUtil.getClassInfo(obj).name+\".\"+operationName, null, event.message));");
			}
			out.println("						}");
			out.println("					}");
			out.println("				}");
			out.println("			)");
			out.println("		}");
			out.println("		protected function getOperation(operationName:String, uid:String):AbstractOperation {");
			out.println("			var obj:AbstractService = this;");
			out.println("			var operation:AbstractOperation = getServiceControl().getOperation(operationName);");
			out.println("			if(obj.dispatchEventService) {");
			if (flexMajorVersion == 3) {
				out.println("				Application.application.dispatchEvent(new ServiceEvent(ServiceEvent.START, uid, ObjectUtil.getClassInfo(obj).name+\".\"+operationName, operation));");
			} else {
				out.println("				FlexGlobals.topLevelApplication.dispatchEvent(new ServiceEvent(ServiceEvent.START, uid, ObjectUtil.getClassInfo(obj).name+\".\"+operationName, operation));");
			}
			out.println("			}");
			out.println("			return operation;");
			out.println("		}");
			out.println("		protected function getLocalizedFault(fault:Fault):Fault {");
			out.println("			var f:Fault = fault;");
			if (bundle) {
				out.println("			f = ServiceConfig.getLocalizedFault(\""+projectName+"\", fault);");
			}
			out.println("			return f;");
			out.println("		}");
			out.println("	}");
			out.println("}");
		} catch (FileNotFoundException ex) {
		} catch (IOException ex) {
			environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error during the creation of as dependance : AbstractService.as : " + ex.getMessage());
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
}
