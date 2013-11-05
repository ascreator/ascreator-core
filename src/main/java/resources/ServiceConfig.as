package flex.services {
	import mx.core.Application;
	import mx.managers.BrowserManager;
	import mx.managers.IBrowserManager;
	import mx.messaging.Channel;
	import mx.messaging.config.ServerConfig;
	import mx.messaging.errors.InvalidChannelError;
	import mx.utils.URLUtil;
	import mx.resources.ResourceManager;
	import mx.rpc.Fault;

	public class ServiceConfig {
		public function ServiceConfig()	{
		}
		private static var _serverUrl:String = null;
		private static var _endpoint:String = null;
		private static var _pollingendpoint:String = null;
		public static function get endpoint():String {
			if(!_endpoint) {
				var base:String = getServerUrl()
				_endpoint = base + "/ascreator-war/messagebroker/amf";
			}
			return _endpoint;
		}
		public static function get pollingendpoint():String {
			if(!_pollingendpoint) {
				var base:String = getServerUrl()
				_pollingendpoint = base + "/ascreator-war/messagebroker/amfpolling";
			}
			return _pollingendpoint;	
		}
		public static function set endpoint(value:String):void {
			_endpoint = value;
		}
		public static function set pollingendpoint(value:String):void {
			_pollingendpoint = value;	
		}
		
		public static function setServerUrl(value:String):void {
			_serverUrl = value;
		}

		public static function getServerUrl():String {
			if(!_serverUrl) {
				var url:String = getUrlFromBrowser();
				if(!url) {
					url = getUrlFromChannel();
				}
				if(!url) {
					throw Error("Missing endpoint and pollingendpoint.\nSet on staticClass flex.services.ServiceConfig");
				}
				_serverUrl = url.substring(0, url.lastIndexOf("/"));
			}
			return _serverUrl;
		}
		
		
		private static function getUrlFromChannel():String {
			try {
				var channel:Channel = ServerConfig.getChannel("my-amf");
				if(channel && channel.url) {
					return channel.url.replace("/messagebroker/amf", "");
				}
			} catch(e:InvalidChannelError) {
			}
			return null;
		}

		private static function getUrlFromBrowser():String {
			var browser:IBrowserManager = BrowserManager.getInstance(); 
			browser.init();
			var browserUrl:String = browser.url; // full url in the browser
			var protocol:String = URLUtil.getProtocol(browserUrl);
			if("file" == protocol) {
				return null;
			}
			return browserUrl.substring(0, browserUrl.lastIndexOf("/"));
		}

		/**
		 * Localise une fault
		 */
		public static function getLocalizedFault(bundleName:String, fault:Fault):Fault {
			var msg:String = null;
			var faultString:String = fault.faultString;
			var args:Array = [];
			if(fault.faultCode!=null) {
				args = fault.faultCode.split(",");
			}
			do {
				msg = ResourceManager.getInstance().getString(bundleName, faultString, args);
				faultString = faultString.substring(0, faultString.lastIndexOf("."));
			} while(msg==null && faultString.lastIndexOf(".")!=-1);
			if(msg==null) {
				msg = fault.faultString+"\n"+fault.faultCode;
			}
			return new Fault(fault.faultCode, msg, fault.faultDetail);
		}
	}
}