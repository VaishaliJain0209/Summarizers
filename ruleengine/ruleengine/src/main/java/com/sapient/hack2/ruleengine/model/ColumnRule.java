package com.sapient.hack2.ruleengine.model;

import java.util.List;

import com.sapient.hack2.ruleengine.util.AggregateOperations;
import com.sapient.hack2.ruleengine.util.ColumnType;
import com.sapient.hack2.ruleengine.util.Period;

/**
 * Base class to represent processing rules.
 * @author ssh150
 *
 */
public class ColumnRule {

	private String column;
	private ColumnType type;
	private List<AggregateOperations> allowedOperations;
	private Period period;
	private Integer calculateUpto;
	
	// useful variables for easy deserialization
	private List<String> operations;
	private String calculateOver;
	public String getColumn() {
		return column;
	}
	public void setColumn(String column) {
		this.column = column;
	}
	public List<AggregateOperations> getAllowedOperations() {
		return allowedOperations;
	}
	public void setAllowedOperations(List<AggregateOperations> allowedOperations) {
		this.allowedOperations = allowedOperations;
	}
	public Period getPeriod() {
		return period;
	}
	public void setPeriod(Period period) {
		this.period = period;
	}
	public Integer getCalculateUpto() {
		return calculateUpto;
	}
	public void setCalculateUpto(Integer calculateUpto) {
		this.calculateUpto = calculateUpto;
	}
	public List<String> getOperations() {
		return operations;
	}
	public void setOperations(List<String> operations) {
		this.operations = operations;
	}
	public String getCalculateOver() {
		return calculateOver;
	}
	public void setCalculateOver(String calculateOver) {
		this.calculateOver = calculateOver;
	}
	public ColumnType getType() {
		return type;
	}
	public void setType(ColumnType type) {
		this.type = type;
	}
	
}
