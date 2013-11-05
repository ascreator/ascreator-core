package flex.services.events {
	import flash.events.Event;
	
	import mx.events.FlexEvent;
	
	public class ItemEvent extends FlexEvent{
		public static const MERGE:String = "itemMerge";
		public static const PERSIST:String = "itemPersist";
		public static const SELECT:String = "itemsSelect";
		public static const UPDATELIST:String = "itemsUpdate";
		public var data:Object = null;
		public function ItemEvent(type:String, item:Object=null)	{
			super(type, true, true);
			this.data = item;
		}
	}
}