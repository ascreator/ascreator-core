package flex.services {
	public interface ItemSelector {
		function set selectedItems(value:Array):void;
		function get selectedItems():Array;

		function set selectedItem(value:*):void;
		function get selectedItem():*;
	}
}