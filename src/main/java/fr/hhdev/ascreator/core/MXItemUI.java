/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator.core;

import flex2.tools.oem.Library;
import fr.hhdev.ascreator.annotations.entities.FlexTransient;
import fr.hhdev.ascreator.exceptions.IllegalMethodException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

/**
 *
 * @author hhfrancois
 */
public class MXItemUI extends MXCreator {

	public MXItemUI(String path, TypeElement typeElement, ProcessingEnvironment environment, Library lib) {
		super(path, typeElement, environment, lib);
	}

	@Override
	protected String getMXClassName() {
		return typeElement.getSimpleName() + "UI";
	}

	@Override
	protected String getMXSuperClassName() {
		return "TitleWindow";
	}

	@Override
	protected String getMXNSSuperClass() {
		return "mx";
	}

	@Override
	protected Map<String, String> getNamespaces() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("mx", "http://www.adobe.com/2006/mxml");
		return map;
	}

	@Override
	protected Map<String, String> getHeadAttributes() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("width", "100%");
		map.put("height", "100%");
		map.put("showCloseButton", "true");
		map.put("close", "closeHandler(event)");
		map.put("label", typeElement.getSimpleName()+"s");
		map.put("title", typeElement.getSimpleName()+"s");
		return map;
	}

	@Override
	protected Map<String, String> getEvents() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("itemPersist", "flex.services.events.ItemEvent");
		map.put("itemMerge", "flex.services.events.ItemEvent");
		return map;
	}

	@Override
	protected Collection<String> getBundles() {
		Collection<String> bundles = new ArrayList<String>();
		bundles.add("ascreatorIcons");
		return bundles;
	}

	@Override
	protected Collection<String> getInterfaces() {
		Collection<String> interfaces = new ArrayList<String>();
		interfaces.add("flex.services.ItemEditor");
		return interfaces;
	}

	@Override
	protected Collection<String> getImports() {
		Set<String> imports = new HashSet<String>();
		imports.add("mx.events.CloseEvent");
		imports.add("mx.managers.PopUpManager");
		imports.add("flex.services.events.ItemEvent");
		imports.add("flex.services.ItemSelector");
		imports.add("mx.core.IFlexDisplayObject");
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

	@Override
	protected void writeBodyScripts() {
		out.println("			private var _item:* = null;");
		out.println("			public function set item(value:*):void {");
		out.println("				this._item = value;");
		out.println("				this.tmp = value;");
		out.println("			}");
		out.println("			[Bindable]");
		out.println("			public function get item():* {");
		out.println("				return this._item;");
		out.println("			}");
		out.println("			private var tmp:Object = null;");
		String idFieldName = ASCreatorTools.getIdFieldName(typeElement, environment);
		out.println("			private var idFieldName:String= \"" + idFieldName + "\";");
		out.println();
		out.println("			private function closeHandler(event:CloseEvent):void{");
		out.println("				PopUpManager.removePopUp(this);");
		out.println("			}");
		out.println();
		out.println("			/**");
		out.println("			 * Click sur le bouton save");
		out.println("			 */");
		out.println("			protected function save_clickHandler(event:MouseEvent):void {");
		out.println("				if(!item) return;");
		out.println("				var eventType:String = (item[idFieldName])?ItemEvent.MERGE:ItemEvent.PERSIST;");
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
			// type de retour de la methode
			TypeMirror returnType = methodElement.getReturnType();
			String fieldName = ASCreatorTools.getFieldName(methodElement, environment); // seules les methodes commencant par is ou get, et retrournant quelque chose sont considerées
			String fieldtype = ASCreatorTools.getASClassName(returnType, environment);
			if (fieldName != null) {
				for (VariableElement variableElement : fieldElements) { // pour les pojo, seule les methodes avec un field associé sont consideré
					if (variableElement.toString().equals(fieldName)) {
						if (variableElement.getAnnotation(FlexTransient.class) == null && !variableElement.getModifiers().contains(Modifier.TRANSIENT)) {
							Element elt = environment.getTypeUtils().asElement(returnType);
							if (fieldtype != null) { // le type existe en AS
								if (fieldtype.equals("Boolean")) {
									out.println("				item." + fieldName + " = " + fieldName + "ID.selected;");
								} else if (fieldtype.equals("Date")) {
									out.println("				item." + fieldName + " = " + fieldName + "ID.selectedDate;");
								} else if (fieldtype.equals("int") || fieldtype.equals("Number")) {
									out.println("				item." + fieldName + " = " + fieldName + "ID.value;");
								} else if (fieldtype.equals("Array")) {
									DeclaredType declType = (DeclaredType) returnType;
									List<? extends TypeMirror> typeArguments = declType.getTypeArguments();
									for (TypeMirror typeMirror : typeArguments) {
										if (ASCreatorTools.isEntity(environment.getTypeUtils().asElement(typeMirror), environment)) {
											out.println("				item." + fieldName + " = tmp." + fieldName + ";");
										}
										break;
									}
								} else if (fieldtype.equals("String")) {
									ElementKind kind = elt.getKind();
									if (kind.equals(ElementKind.ENUM)) {
										out.println("				item." + fieldName + " = " + fieldName + "ID.selectedItem;");
									} else {
										out.println("				item." + fieldName + " = " + fieldName + "ID.text;");
									}
								}
							} else {
								if (ASCreatorTools.isEntity(elt, environment)) {
									out.println("				item." + fieldName + " = tmp." + fieldName + ";");
								}
							}
						}
						break;
					}
				}
			}
		}
		out.println("				var evt:ItemEvent = new ItemEvent(eventType, item);");
		out.println("				dispatchEvent(evt);");
		out.println("			}");
		out.println();
		out.println("			/**");
		out.println("			 * Remet les valeurs d'origine");
		out.println("			 */");
		out.println("			protected function cancel_clickHandler(event:MouseEvent):void {");
		out.println("				var i:Object = item;");
		out.println("				item = null;");
		out.println("				item = i;");
		out.println("			}");
		out.println();
		out.println("			/**");
		out.println("			 * Ouvre la fenetre d'ajout/suppression d'items enfants");
		out.println("			 */");
		out.println("			protected function openadd_clickHandler(event:MouseEvent, fieldName:String, type:Class, multi:Boolean):void {");
		out.println("				var uis:IFlexDisplayObject = PopUpManager.createPopUp(this, type, true);");
		out.println("				var itemSelector:ItemSelector = uis as ItemSelector");
		out.println("				if(multi) {");
		out.println("					itemSelector.selectedItems = item[fieldName];");
		out.println("				} else {");
		out.println("					itemSelector.selectedItem = item[fieldName];");
		out.println("				}");
		out.println("				PopUpManager.centerPopUp(uis);");
		out.println("				uis.addEventListener(ItemEvent.SELECT, function(event:ItemEvent):void {;");
		out.println("					if(multi) {");
		out.println("						tmp[fieldName] = itemSelector.selectedItems;");
		out.println("					} else {");
		out.println("						tmp[fieldName] = itemSelector.selectedItem;");
		out.println("					}");
		out.println("				});");
		out.println("			}");
	}

	@Override
	protected void writeBodyXML() {
		out.println("	<mx:Form width=\"100%\" height=\"100%\">");
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
			// type de retour de la methode
			TypeMirror returnType = methodElement.getReturnType();
			String fieldName = ASCreatorTools.getFieldName(methodElement, environment); // seules les methodes commencant par is ou get, et retrournant quelque chose sont considerées
			String fieldtype = ASCreatorTools.getASClassName(returnType, environment);
			if (fieldName != null) {
				for (VariableElement variableElement : fieldElements) { // pour les pojo, seule les methodes avec un field associé sont consideré
					if (variableElement.toString().equals(fieldName)) {
						if (variableElement.getAnnotation(FlexTransient.class) == null && !variableElement.getModifiers().contains(Modifier.TRANSIENT)) {
							Element elt = environment.getTypeUtils().asElement(returnType);
							out.println("		<mx:FormItem label=\"" + fieldName + "\" direction=\"horizontal\">");
							if (fieldtype != null) { // le type existe en AS
								if (fieldtype.equals("Boolean")) {
									out.println("			<mx:CheckBox id=\"" + fieldName + "ID\" selected=\"{item." + fieldName + "}\"/>");
								} else if (fieldtype.equals("Date")) {
									out.println("			<mx:DateChooser id=\"" + fieldName + "ID\" selectedDate=\"{item." + fieldName + "}\"/>");
								} else if (fieldtype.equals("int")) {
									out.println("			<mx:NumericStepper id=\"" + fieldName + "ID\" value=\"{item." + fieldName + "}\" stepSize=\"1\"/>");
								} else if (fieldtype.equals("Number")) {
									out.println("			<mx:NumericStepper id=\"" + fieldName + "ID\" value=\"{item." + fieldName + "}\"/>");
								} else if (fieldtype.equals("Array")) {
									String typeTarget = null;
									DeclaredType declType = (DeclaredType) returnType;
									List<? extends TypeMirror> typeArguments = declType.getTypeArguments();
									for (TypeMirror typeMirror : typeArguments) {
										if (ASCreatorTools.isEntity(environment.getTypeUtils().asElement(typeMirror), environment)) {
											typeTarget = typeMirror.toString() + "sUI";
										}
										break;
									}
									if(typeTarget!=null) {
										out.println("			<mx:TextInput enabled=\"false\" id=\"" + fieldName + "ID\" text=\"{item." + fieldName + ".length}\"/><mx:Button label=\"+\" click=\"openadd_clickHandler(event, '" + fieldName + "', " + typeTarget + ", true)\"/>");
									}
								} else if (fieldtype.equals("String")) {
									ElementKind kind = elt.getKind();
									if (kind.equals(ElementKind.ENUM)) {
										String enumName = elt.getSimpleName().toString();
										out.println("			<mx:ComboBox id=\"" + fieldName + "ID\" dataProvider=\"{" + enumName + ".enumConstants}\" selectedIndex=\"{" + enumName + ".enumConstants.indexOf(item." + fieldName + ")}\"/>");
									} else {
										boolean enabled = !ASCreatorTools.isIdGenerated((TypeElement) elt, environment);
										out.println("			<mx:TextInput enabled=\"" + enabled + "\" id=\"" + fieldName + "ID\" text=\"{item." + fieldName + "}\"/>");
									}
								}
							} else {
								if (ASCreatorTools.isEntity(elt, environment)) {
									String typeTarget = ((TypeElement) elt).getQualifiedName().toString() + "sUI";
									out.println("			<mx:TextInput enabled=\"false\" id=\"" + fieldName + "ID\" text=\"{item." + fieldName + "?1:0}\"/><mx:Button label=\"+\" click=\"openadd_clickHandler(event, '" + fieldName + "', " + typeTarget + ", false)\"/>");
								}
							}
							out.println("		</mx:FormItem>");
						}
						break;
					}
				}
			}
		}
		out.println("	</mx:Form>");
		out.println("	<mx:ApplicationControlBar width=\"100%\" dock=\"true\">");
		out.println("		<mx:Spacer width=\"100%\"/>");
		out.println("		<mx:Button icon=\"@Resource(bundle='ascreatorIcons', key='CANCEL16')\" toolTip=\"Cancel\" click=\"cancel_clickHandler(event)\"/>");
		out.println("		<mx:Button icon=\"@Resource(bundle='ascreatorIcons', key='SAVE16')\" toolTip=\"Save\" click=\"save_clickHandler(event)\"/>");
		out.println("	</mx:ApplicationControlBar>");
	}
}
