/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator.core;

import flex2.tools.oem.Library;
import fr.hhdev.ascreator.annotations.services.FlexRemote;
import fr.hhdev.ascreator.exceptions.IgnoredClassException;
import fr.hhdev.ascreator.exceptions.IllegalClassException;
import fr.hhdev.ascreator.exceptions.IllegalMethodException;
import fr.hhdev.ascreator.exceptions.NoCommentClassException;
import fr.hhdev.ascreator.exceptions.NoRemotingClassException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

/**
 *
 * @author hhfrancois
 */
public class ASServiceCreator extends ASCreator {

	public ASServiceCreator(String path, TypeElement typeElement, ProcessingEnvironment environment, Library lib) {
		super(path, typeElement, environment, lib);
	}

	private DeclaredType flexCRUDInterface = null;
	
	@Override
	protected String getASClassName() {
		return typeElement.getSimpleName().toString();
	}

	@Override
	protected boolean isAbstract() {
		return false;
	}

	@Override
	protected boolean isInterface() {
		return false;
	}

	@Override
	protected Collection<String> getASExtendClassNames() {
		Collection<String> extendClasses = new ArrayList<String>();
		extendClasses.add(projectName+".AbstractService");
		return extendClasses;
	}

	@Override
	protected Collection<String> getASIImplementClassNames() {
		for (TypeMirror interfType : typeElement.getInterfaces()) {
			TypeElement elt = (TypeElement) environment.getTypeUtils().asElement(interfType);
			if("FlexCRUDInterface".equals(elt.getSimpleName().toString()) && interfType.getKind().equals(TypeKind.DECLARED)) {
				setFlexCRUDInterface((DeclaredType) interfType);
			}
		}
		return Collections.EMPTY_LIST;
	}

	@Override
	protected Set<String> getASImports() {
		Set<String> imports = getASDefaultImports();
		List<ExecutableElement> elements = ElementFilter.methodsIn(typeElement.getEnclosedElements());
		for (ExecutableElement methodElement : elements) {
			try {
				getASImportsFromMethod(methodElement, imports);
			} catch (IllegalMethodException ex) {
				environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot get import from method : "+typeElement.getSimpleName()+"."+methodElement.getSimpleName(), typeElement);
			}
		}
		return imports;
	}

	/**
	 * Retourne une liste d'imports par defaut.
	 *
	 * @return Set<String>
	 */
	private Set<String> getASDefaultImports() {
		Set<String> imports = new HashSet<String>();
		imports.add("flex.services.events.ServiceEvent");
		imports.add(projectName+".AbstractService");
		imports.add("flash.utils.ByteArray");
		imports.add("mx.rpc.Responder");
		imports.add("mx.rpc.events.ResultEvent");
		imports.add("mx.rpc.events.FaultEvent");
		imports.add("mx.rpc.Fault");
		imports.add("mx.core.mx_internal");
		imports.add("mx.collections.ArrayCollection");
		imports.add("mx.utils.ObjectUtil");
		imports.add("mx.rpc.AbstractOperation");
		imports.add("mx.rpc.AsyncToken");
		imports.add("mx.rpc.remoting.Operation");
		imports.add("mx.rpc.remoting.RemoteObject");
		imports.add("mx.utils.UIDUtil");
		imports.add("mx.messaging.Consumer");
		imports.add("mx.messaging.events.MessageAckEvent");
		imports.add("mx.messaging.events.MessageEvent");
		imports.add("mx.messaging.events.MessageFaultEvent");
		imports.add("mx.messaging.messages.AsyncMessage");
		imports.add("mx.messaging.messages.ErrorMessage");
		imports.add("mx.messaging.config.ServerConfig");
		imports.add("mx.messaging.Channel");
		imports.add("mx.messaging.ChannelSet");
		imports.add("mx.messaging.events.ChannelFaultEvent");
		imports.add("mx.messaging.messages.IMessage");
		imports.add("hhf.flex.entities.MethodArguments");
		imports.add("flash.events.TimerEvent");
		imports.add("flash.utils.Timer");
		return imports;
	}

	@Override
	protected Map<String, String> getASEvents() {
		return new HashMap<String, String>();
	}

	@Override
	protected Set<String> getASNamespaces() {
		Set<String> nss = new HashSet<String>();
		nss.add("mx_internal");
		return nss;
	}

	@Override
	protected void writeASBodyConstructor() {
		out.println("			super(\"" + typeElement.getQualifiedName() + "\");");
	}

	@Override
	protected String getASClassComment() throws NoCommentClassException {
		// on regarde si la declaration de la classe comporte un commentaire.
		String comment = environment.getElementUtils().getDocComment(typeElement);
		if (comment == null) { // sinon on regarde sur les interfaces
			List<? extends TypeMirror> interfaces = typeElement.getInterfaces();
			for (TypeMirror interType : interfaces) {
				TypeElement interElement = (TypeElement) environment.getTypeUtils().asElement(interType);
				comment = environment.getElementUtils().getDocComment(interElement);
				if (comment != null) {
					return comment;
				}
			}
		} else {
			return comment;
		}
		throw new NoCommentClassException();
	}

	/**
	 * A partir des methodes considérées on construit le corps de la methodes en as
	 */
	@Override
	protected void writeASFieldsAndMethods() {
		List<ExecutableElement> methodsIn = ElementFilter.methodsIn(typeElement.getEnclosedElements());
		for (ExecutableElement methodElement : methodsIn) {
			if(!ASCreatorTools.isConsiderateMethod(methodElement, environment)) {
				continue;
			}
			boolean global = false;
			FlexRemote annoRemote = methodElement.getAnnotation(FlexRemote.class);
			boolean async = annoRemote != null;
			if (annoRemote != null) {
				global = annoRemote.global();
			}
			// Recupere les types concernés
			TypeMirror returnMirror = methodElement.getReturnType();

			String returnType = ASCreatorTools.getASType(returnMirror, environment);
			List<String> args = ASCreatorTools.getASArguments(methodElement, environment); // ordonnés
			List<String> argNames = new ArrayList<String>();

			// Récupere le nom des arguments
			for (VariableElement variableElement : methodElement.getParameters()) {
				argNames.add(variableElement.getSimpleName().toString());
			}
			// Ecrit le commentaire de la methode
			writeMethodComment(methodElement, args, argNames, returnType);
			// Ecrit la methode
			out.print("		public function " + methodElement.getSimpleName() + "(");
			int i = 0;
			if (argNames.size() != args.size()) {
				environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot Create service : "+typeElement.getSimpleName()+" cause method "+methodElement.getSimpleName()+" arguments inconsistent - argNames : "+argNames.size()+" / args : "+args.size(),  typeElement);
			}
			if (async && !global) {
				out.print("target_selector:String");
				if (!argNames.isEmpty()) {
					out.print(", ");
				}
			}
			while (i < argNames.size()) {
				out.print(argNames.get(i) + ":" + args.get(i));
				if ((++i) < argNames.size()) {
					out.print(", ");
				}
			}
			out.println("):AsyncToken {");
			// Ecrit le corp de la methode
			if (!async) {
				writeASMethodBody(methodElement, argNames, returnType);
			} else {
				writeASRemoteMethodBody(methodElement, argNames, returnType, annoRemote);
			}
			out.println("			return token;");
			out.println("		}");
			out.println("");
		}
	}

	/**
	 * Ecrit le commentaire de la methode
	 *
	 */
	protected void writeMethodComment(final ExecutableElement methodElement, final List<String> args, final List<String> argNames, final String returnType) {
		// récupération du javadoc
		String methodComment = environment.getElementUtils().getDocComment(methodElement);
		out.println("		/**");
		// un commentaire existe déjà
		if (methodComment != null) {
			String comment = methodComment.replaceAll("\n", "\n		 *");
			out.println("		 *" + comment + "/");
		} else { // on en construit un
			for (int i = 0; i < args.size(); i++) {
				String arg = argNames.get(i) + " : " + args.get(i);
				out.println("		 * @param " + arg);
			}
			if (!"void".equals(returnType)) {
				out.println("		 * @return result:" + returnType);
			}
			List<? extends TypeMirror> exceptions = methodElement.getThrownTypes();
			for (TypeMirror exceptType : exceptions) {
				TypeElement exeptElement = (TypeElement) environment.getTypeUtils().asElement(exceptType);
				out.println("		 * @throws " + exeptElement.getQualifiedName());
			}
			out.println("		 */");
		}
	}

	/**
	 * Ecrit le corps de la methode pour une methode remote c'est à dire que cette methode peut etre interprété par n'importe quel serveur si global est à true ou par le serveur designé par
	 * target_selector, qui normalement correspond au host de la target
	 *
	 * @param method
	 * @param argNames
	 * @param returnASType
	 * @param flexRemote
	 */
	private void writeASRemoteMethodBody(final ExecutableElement methodElement, final List<String> argNames, String returnASType, FlexRemote flexRemote) {
		String className = methodElement.getSimpleName().toString();
		out.println("			var uid:String = UIDUtil.createUID();");
		out.println("			var methodArguments:MethodArguments = new MethodArguments();");
		out.println("			methodArguments.list = new Array();");
		out.println("			var target_selec:String =\"GLOBAL\";"); // par defaut c'est global
		for (String arg : argNames) {
			if ("target_selector".equals(arg)) {
				out.println("			target_selec=target_selector;");
			} else {
				out.println("			methodArguments.list.push(" + arg + ");");
			}
		}
		out.println("			var token:AsyncToken = new AsyncToken();");
		out.println("			var consumer:Consumer = new Consumer();");
		out.println("			consumer.destination = \"blazedsTopic\";");
		out.println("			consumer.selector = \"target_selector='CLIENT' and clientuid='\"+uid+\"' and destination='" + className + "' and methodName='" + methodElement.getSimpleName() + "'\";");
		out.println("			trace(\"SELECTOR : \"+consumer.selector);");
		out.println("			var channelSet:ChannelSet = new ChannelSet();");
		out.println("			var channel:Channel = ServerConfig.getChannel(\"my-polling-amf\");");
		out.println("			channelSet.addChannel(channel);");
		out.println("			consumer.channelSet = channelSet;");
		// GESTION de la desinscription automatique
		out.println("			var t:Timer = new Timer(" + flexRemote.timeout() + ", 1);");
		out.println("			t.addEventListener(TimerEvent.TIMER_COMPLETE, function(event:TimerEvent):void {");
		out.println("				var cons:Consumer = consumer;");
		out.println("				cons.unsubscribe();");
		out.println("				trace(\"consumer unsubscribe\");");
		if (flexRemote.timeoutException()) {
			out.println("				var fault:Fault = new Fault(\"0\", \"TIMEOUT\", \"This exception would be catch.\");");
			out.println("				var faultEvent:FaultEvent = new FaultEvent(FaultEvent.FAULT, false, true, fault);");
			out.println("				tok.mx_internal::applyFault(faultEvent);");
		}
		out.println("			});");
		// Gestion du resultat
		out.println("			consumer.addEventListener(MessageEvent.MESSAGE, function(event:MessageEvent):void {");
		out.println("				var timer:Timer = t;");
		out.println("				var tok:AsyncToken = token;");
		out.println("				trace(\"recu message JMS via remoteCommand\");");
		out.println("				var message:IMessage = event.message; // message JMS");
		out.println("				var headers:Object = message.headers;  // les attributs du message");
		out.println("				var resultEvent:ResultEvent = new ResultEvent(ResultEvent.RESULT, false, true, message.body);");
		out.println("				tok.mx_internal::applyResult(resultEvent);");
		out.println("				timer.reset();");
		out.println("				timer.start();");
		out.println("			});");
		// Gestion des erreurs
		out.println("			consumer.addEventListener(ChannelFaultEvent.FAULT, function(event:ChannelFaultEvent):void {");
		out.println("				var timer:Timer = t;");
		out.println("				var tok:AsyncToken = token;");
		out.println("				trace(\"recu error JMS via remoteCommand\");");
		out.println("				var fault:Fault = new Fault(event.faultCode, event.faultString, event.faultDetail);");
		out.println("				var faultEvent:FaultEvent = new FaultEvent(FaultEvent.FAULT, false, true, getLocalizedFault(fault));");
		out.println("				tok.mx_internal::applyFault(faultEvent);");
		out.println("				timer.reset();");
		out.println("				timer.start();");
		out.println("			});");
		// GESTION inscription + launch remoteCommand
		out.println("			consumer.subscribe();");
		out.println("			this.getRemoteServiceControl().remoteCommand(\"" + className + "\", \"" + methodElement.getSimpleName() + "\", uid, target_selec, methodArguments);");
		out.println("			t.start();");
	}

	/**
	 * Ecrit le corps de la methode
	 *
	 * @param methods
	 */
	private void writeASMethodBody(final ExecutableElement methodElement, final List<String> argNames, String returnASType) {
		out.println("			var operationName:String = \"" + methodElement.getSimpleName() + "\";");
		out.println("			var uid:String = UIDUtil.createUID();");
		out.println("			var operation:AbstractOperation = getOperation(operationName, uid);");
		if (!"void".equals(returnASType)) {
			out.println("			operation.resultType = " + returnASType + ";");
		}
		out.print("			var tok:AsyncToken = operation.send(");
		int i = 0;
		while (i < argNames.size()) {
			out.print(argNames.get(i));
			if ((++i) < argNames.size()) {
				out.print(", ");
			}
		}
		out.println(");");
//		out.println("			var oldApplyFault:Function = token.mx_internal::applyFault;");
//		out.println("			token.mx_internal::applyFault = function(event:FaultEvent):void {");
//		out.println("				var superMethod:Function = oldApplyFault;");
//		out.println("				trace(\"AsyncToken intercepte les erreur pour les localizer\");");
//		out.println("				superMethod.call(this, event);");
//		out.println("			}");
		out.print("				var token:AsyncToken = new AsyncToken();");
		out.println("			tok.addResponder(getResponder(operationName, uid, token));");
	}

	@Override
	protected boolean isBindable() {
		return false;
	}

	@Override
	protected String getRemotingClass() throws NoRemotingClassException {
		throw new NoRemotingClassException();
	}

	@Override
	protected void checkClass() throws IllegalClassException, IgnoredClassException {
	}

	/**
	 * @return the flexCRUDInterface
	 */
	public DeclaredType getFlexCRUDInterface() {
		return flexCRUDInterface;
	}

	/**
	 * @param flexCRUDInterface the flexCRUDInterface to set
	 */
	public void setFlexCRUDInterface(DeclaredType flexCRUDInterface) {
		this.flexCRUDInterface = flexCRUDInterface;
	}
}
