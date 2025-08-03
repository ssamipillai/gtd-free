/**
 * 
 */
package org.gtdfree.model;

/**
 * @author ikriznar
 *
 */
public enum ActionField {

	ID("ID","id"),
	FOLDER("Folder",null),
	PROJECT("Project","project"),
	DESCRIPTION("Description","description"),
	REMINDER("Reminder","reminder"),
	PRIORITY("Priority","priority"),
	QUEUE("Queue","queue"),
	RESOLUTION("Resolution","resolution"),
	;
	
	
	
	private String displayName;
	private String fieldName;

	private ActionField(String displayName, String fieldName) {
		this.displayName=displayName;
		this.fieldName = fieldName;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public String getFieldName() {
		return fieldName;
	}
}
