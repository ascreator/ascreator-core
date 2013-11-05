/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.ascreator.visitors;

/**
 *
 * @author hhfrancois
 */
public class FlexError {
	private String classEntry;
	private String methodEntry;
	private String exceptionEntry;
	private String msg;
	private String locale;

	public FlexError(String classEntry, String methodEntry, String exceptionEntry, String msg, String locale) {
		this.classEntry = classEntry;
		this.methodEntry = methodEntry;
		this.exceptionEntry = exceptionEntry;
		this.msg = msg;
		this.locale = locale;
	}

	/**
	 * @return the msg
	 */
	public String getMsg() {
		return msg;
	}

	/**
	 * @param msg the msg to set
	 */
	public void setMsg(String msg) {
		this.msg = msg;
	}

	/**
	 * @return the locale
	 */
	public String getLocale() {
		return locale;
	}

	/**
	 * @param locale the locale to set
	 */
	public void setLocale(String locale) {
		this.locale = locale;
	}

	/**
	 * @return the classEntry
	 */
	public String getClassEntry() {
		return classEntry;
	}

	/**
	 * @param classEntry the classEntry to set
	 */
	public void setClassEntry(String classEntry) {
		this.classEntry = classEntry;
	}

	/**
	 * @return the methodEntry
	 */
	public String getMethodEntry() {
		return methodEntry;
	}

	/**
	 * @param methodEntry the methodEntry to set
	 */
	public void setMethodEntry(String methodEntry) {
		this.methodEntry = methodEntry;
	}

	/**
	 * @return the exceptionEntry
	 */
	public String getExceptionEntry() {
		return exceptionEntry;
	}

	/**
	 * @param exceptionEntry the exceptionEntry to set
	 */
	public void setExceptionEntry(String exceptionEntry) {
		this.exceptionEntry = exceptionEntry;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		if(classEntry!=null) {
			result.append(classEntry);
			result.append(".");
		}
		if(methodEntry!=null) {
			result.append(methodEntry);
			result.append(".");
		}
		if(exceptionEntry!=null) {
			result.append(exceptionEntry);
			result.append("=");
		}
		if(msg!=null) {
			result.append(msg.replaceAll("\\n", "\\\\n"));
		}
		return result.toString();
	}
	
}
