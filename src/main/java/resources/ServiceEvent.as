package flex.services.events {

	import mx.events.FlexEvent;
	import mx.messaging.messages.IMessage;
	import mx.rpc.AbstractOperation;

	public class ServiceEvent extends FlexEvent {
		public static const START:String = "start";
		public static const SUCCESS:String = "success";
		public static const FAIL:String = "fail";
		public static const CANCEL:String = "cancel";
		public static const CANCEL_ALL:String = "cancelAll";


		public var uid:String;
		public var time:Date;
		public var delay:int = 0;
		public var name:String;
		public var message:IMessage;
		public var operation:AbstractOperation;
		public function ServiceEvent(type:String, uid:String, name:String, operation:AbstractOperation=null, message:IMessage=null){
			super(type, true, true);
			this.uid = uid;
			this.name = name;
			this.message = message;
			this.time = new Date();
			this.operation = operation;
		}
	}
}