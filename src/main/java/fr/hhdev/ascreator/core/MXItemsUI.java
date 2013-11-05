/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator.core;

import flex2.tools.oem.Library;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

/**
 *
 * @author hhfrancois
 */
public class MXItemsUI extends MXCreator {

	public MXItemsUI(String path, TypeElement typeElement, ProcessingEnvironment environment, Library lib) {
		super(path, typeElement, environment, lib);
	}

	@Override
	protected String getMXClassName() {
		return typeElement.getSimpleName() + "sUI";
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
		map.put("enhanced", "common.enhanced.*");
		map.put("datagridColumns", "common.enhanced.datagridColumns.*");
		map.put("datagridColumns", "common.enhanced.datagridColumns.*");
		map.put(projectName, projectName + ".*");
		return map;
	}

	@Override
	protected Map<String, String> getHeadAttributes() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("width", "100%");
		map.put("height", "100%");
		map.put("showCloseButton", "true");
		map.put("label", typeElement.getSimpleName() + "s");
		map.put("title", typeElement.getSimpleName() + "s");
		map.put("close", "closeHandler(event)");
		map.put("creationComplete", "creationCompleteHandler()");
		return map;
	}

	@Override
	protected Map<String, String> getEvents() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("itemsUpdate", "flex.services.events.ItemEvent");
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
		interfaces.add("flex.services.ItemSelector");
		return interfaces;
	}

	@Override
	protected Collection<String> getImports() {
		Set<String> imports = new HashSet<String>();
		imports.add("mx.events.CloseEvent");
		imports.add("mx.managers.PopUpManager");
		imports.add("flex.services.events.ItemEvent");
		imports.add("common.enhanced.events.ActionEvent");
		imports.add("flex.services.ItemEditor");
		imports.add("mx.rpc.CallResponder");
		imports.add("mx.rpc.events.FaultEvent");
		imports.add("mx.rpc.events.ResultEvent");
		imports.add("common.enhanced.EnhancedDataGridColumn");
		imports.add(typeElement.getQualifiedName().toString());
		imports.add(typeElement.getQualifiedName().toString()+"UI");
		return imports;
	}

	@Override
	protected void writeBodyScripts() {
		String idFieldName = ASCreatorTools.getIdFieldName(typeElement, environment);
		out.println("			private var idFieldName:String = \"" + idFieldName + "\";");
		out.println();
		out.println("			private var idle:Boolean = true;");
		out.println();
		out.println("			[Bindable]");
		out.println("			private var selectMode:String = \"none\";");
		out.println();
		out.println("			private var _selectedItem:* = null;");
		out.println("			private var _selectedItems:Array = [];");
		out.println("			public function set selectedItems(value:Array):void {");
		out.println("				selectMode = \"multi\";");
		out.println("				_selectedItems = value;");
		out.println("				");
		out.println("			}");
		out.println("			public function get selectedItems():Array {");
		out.println("				return _selectedItems;");
		out.println("			}");
		out.println();
		out.println("			public function set selectedItem(value:*):void {");
		out.println("				selectMode = \"single\";");
		out.println("				_selectedItem = value;");
		out.println("			}");
		out.println("			[Bindable]");
		out.println("			public function get selectedItem():* {");
		out.println("				return _selectedItem;");
		out.println("			}");
		out.println();
		out.println("			private function closeHandler(event:CloseEvent):void{");
		out.println("				PopUpManager.removePopUp(this);");
		out.println("			}");
		out.println("			protected function creationCompleteHandler():void {");
		out.println("				refresh();");
		out.println("			}");
		out.println("			");
		out.println("			/**");
		out.println("			 * Gestion de l'update");
		out.println("			 */");
		out.println("			protected function itemMergeHandler(event:ItemEvent):void {");
		out.println("				itemSaveHandler(event, false);");
		out.println("			}");
		out.println();
		out.println("			/**");
		out.println("			 * Gestion du create");
		out.println("			 */");
		out.println("			protected function itemCreateHandler(event:ItemEvent):void {");
		out.println("				itemSaveHandler(event, true);");
		out.println("			}");
		out.println();
		out.println("			/**");
		out.println("			 * Gestion des creation et update");
		out.println("			 */");
		out.println("			protected function itemSaveHandler(event:ItemEvent, persist:Boolean):void {");
		out.println("				if(!event.data) return;");
		out.println("				var item:Object = event.data;");
		out.println("				var saveResponder:CallResponder = new CallResponder();");
		out.println("				if(persist) {");
		out.println("					saveResponder.token = itemFacade.persist(item);");
		out.println("				} else {");
		out.println("					saveResponder.token = itemFacade.merge(item);");
		out.println("				}");
		out.println("				saveResponder.addEventListener(ResultEvent.RESULT, function(event:ResultEvent):void {");
		out.println("					idle = true;");
		out.println("					trace(\"Item saved\");");
		out.println("					refresh();");
		out.println("				});");
		out.println("				saveResponder.addEventListener(FaultEvent.FAULT, function(event:FaultEvent):void {");
		out.println("					idle = true;");
		out.println("					trace(\"Item not saved\");");
		out.println("					// Alert Error						");
		out.println("				});");
		out.println("			}");
		out.println();
		out.println("			/**");
		out.println("			 * Supprime l'item");
		out.println("			 */");
		out.println("			protected function removeItem(obj:*):void {");
		out.println("				if(!obj) return;");
		out.println("				var item:Object = obj;");
		out.println("				idle = false;");
		out.println("				var removeResponder:CallResponder = new CallResponder();");
		out.println("				removeResponder.token = itemFacade.remove(item[idFieldName]);");
		out.println("				removeResponder.addEventListener(ResultEvent.RESULT, function(event:ResultEvent):void {");
		out.println("					idle = true;");
		out.println("					trace(\"Item removed\");");
		out.println("					items.removeItemAt(items.getItemIndex(item));");
		out.println("					dispatchEvent(new ItemEvent(ItemEvent.UPDATELIST, null));");
		out.println("				});");
		out.println("				removeResponder.addEventListener(FaultEvent.FAULT, function(event:FaultEvent):void {");
		out.println("					idle = true;");
		out.println("					trace(\"Item not removed\");");
		out.println("					// Alert Error						");
		out.println("				});");
		out.println("			}");
		out.println();
		out.println("			/**");
		out.println("			 * Ouvre la fenetre de creation de l'item");
		out.println("			 */");
		out.println("			protected function createItem():void {");
		out.println("				var editor:ItemEditor = PopUpManager.createPopUp(this, " + typeElement.getSimpleName() + "UI, true) as ItemEditor;");
		out.println("				PopUpManager.centerPopUp(editor);");
		out.println("				editor.item = new " + typeElement.getSimpleName() + "();");
		out.println("				editor.addEventListener(ItemEvent.PERSIST, itemCreateHandler); ");
		out.println("			}");
		out.println();
		out.println("			/**");
		out.println("			 * Ouvre la fenetre d'edition de l'item");
		out.println("			 */");
		out.println("			protected function editItem(obj:*):void {");
		out.println("				if(!obj) return;");
		out.println("				var editor:ItemEditor = PopUpManager.createPopUp(this, " + typeElement.getSimpleName() + "UI, true) as ItemEditor;");
		out.println("				PopUpManager.centerPopUp(editor);");
		out.println("				editor.item = obj;");
		out.println("				editor.addEventListener(ItemEvent.MERGE, itemMergeHandler); ");
		out.println("			}");
		out.println();
		out.println("			/**");
		out.println("			 * Recharge la liste des item");
		out.println("			 */");
		out.println("			protected function refresh():void {");
		out.println("				itemsResponder.token = itemFacade.findAll();");
		out.println("				dispatchEvent(new ItemEvent(ItemEvent.UPDATELIST, null));");
		out.println("			}");
		out.println();
		out.println("			/**");
		out.println("			 * Click sur les colonnes actions EDIT et REMOVE");
		out.println("			 */");
		out.println("			protected function item_actionClickHandler(event:ActionEvent):void {");
		out.println("				if(event.action==\"EDIT\") {");
		out.println("					editItem(event.data);");
		out.println("				} else if(event.action==\"REMOVE\") {");
		out.println("					removeItem(event.data);");
		out.println("				} else if(event.action==\"SELECT\" && selectMode==\"single\") {");
		out.println("					if(_selectedItem == event.data) {");
		out.println("						_selectedItem = null;");
		out.println("					} else {");
		out.println("						_selectedItem = event.data;");
		out.println("					}");
		out.println("					items.refresh();");
		out.println("				} else if(event.action==\"SELECT\" && selectMode==\"multi\") {");
		out.println("					if(!_selectedItems) {");
		out.println("						_selectedItems = [];");
		out.println("					}");
		out.println("					for(var idx:int = 0; idx<_selectedItems.length; idx++) {");
		out.println("						var o:Object = _selectedItems[idx];");
		out.println("						if(o[idFieldName] == event.data[idFieldName]) {");
		out.println("							_selectedItems.splice(idx, 1);");
		out.println("							items.refresh();");
		out.println("							return;								");
		out.println("						}");
		out.println("					}");
		out.println("					_selectedItems.push(event.data);");
		out.println("					items.refresh();");
		out.println("				}");
		out.println("					");
		out.println("			}");
		out.println();
		out.println("			/**");
		out.println("			 * Fonction permettant au enhancedDG de savoir si les cases a cocher sont cochees");
		out.println("			 */");
		out.println("			protected function item_checkFunction(data:Object, col:EnhancedDataGridColumn):Boolean {");
		out.println("				if(!data) return false;");
		out.println("				if(selectMode==\"single\" && _selectedItem) {");
		out.println("					return _selectedItem[idFieldName] == data[idFieldName];");
		out.println("				} else if(selectMode==\"multi\" && _selectedItems && _selectedItems.length) {");
		out.println("					for each(var o:Object in _selectedItems) {");
		out.println("						if(o[idFieldName] == data[idFieldName]) {");
		out.println("							return true;");
		out.println("						}");
		out.println("					}");
		out.println("				}");
		out.println("				return false;");
		out.println("			}");
	}

	@Override
	protected void writeBodyXML() {
		out.println("	<" + projectName + ":FlexCRUDInterface id=\"itemFacade\" entityClass=\"" + typeElement.getSimpleName() + "\"/>");
		out.println("	<mx:CallResponder id=\"itemsResponder\"/>");
		out.println("	<mx:ArrayCollection id=\"items\" source=\"{itemsResponder.lastResult}\"/>");
		out.println("	<enhanced:EnhancedDataGrid width=\"100%\" height=\"100%\"");
		out.println("							   dataProvider=\"{items}\" actionClick=\"item_actionClickHandler(event)\">");
		out.println("		<enhanced:columns>");
		out.println("			<datagridColumns:CheckBoxEnhancedDatagridColumn visible=\"{selectMode=='multi'}\" checkedFunction=\"item_checkFunction\" action=\"SELECT\" width=\"20\"/>");
		out.println("			<datagridColumns:RadioButtonEnhancedDatagridColumn visible=\"{selectMode=='single'}\" checkedFunction=\"item_checkFunction\" action=\"SELECT\" width=\"20\"/>");
		out.println("			<enhanced:EnhancedDataGridColumn dataField=\"name\"/>");
		out.println("			<datagridColumns:ButtonEnhancedDatagridColumn icon=\"@Resource(bundle='ascreatorIcons', key='EDIT16')\" action=\"EDIT\" width=\"20\"/>");
		out.println("			<datagridColumns:ButtonEnhancedDatagridColumn icon=\"@Resource(bundle='ascreatorIcons', key='REMOVE16')\" action=\"REMOVE\" width=\"20\"/>");
		out.println("		</enhanced:columns>");
		out.println("	</enhanced:EnhancedDataGrid>");
		out.println("	<mx:ApplicationControlBar width=\"100%\" dock=\"true\">");
		out.println("		<mx:Spacer width=\"100%\"/>");
		out.println("		<mx:Button icon=\"@Resource(bundle='ascreatorIcons', key='ADD16')\" toolTip=\"Create item\" click=\"createItem()\"/>");
		out.println("		<mx:Button icon=\"@Resource(bundle='ascreatorIcons', key='REFRESH16')\" toolTip=\"Refresh items\" click=\"refresh()\"/>");
		out.println("	</mx:ApplicationControlBar>");
	}
}
