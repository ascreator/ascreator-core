package flex.services {
	import flash.events.IEventDispatcher;
	import mx.core.IFlexDisplayObject;
	public interface ItemEditor extends IEventDispatcher, IFlexDisplayObject {
		function set item(value:*):void;
		function get item():*;
	}
}