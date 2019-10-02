package eu.openanalytics.phaedra.calculation.formula.model;

public enum Scope {

	PerWell(0, "Per well", "The formula will be evaluated per well"),
	PerPlate(1, "Per plate", "The formula will be evaluated per plate");
	
	private int code;
	private String label;
	private String description;

	private Scope(int code, String label, String description) {
		this.code = code;
		this.label = label;
		this.description = description;
	}
	
	public int getCode() {
		return code;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getDescription() {
		return description;
	}
	
	public static Scope get(int code) {
		return PerWell;
	}
	
}
