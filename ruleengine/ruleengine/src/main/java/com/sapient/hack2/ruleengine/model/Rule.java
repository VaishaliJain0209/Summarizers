package com.sapient.hack2.ruleengine.model;

import java.util.List;

public class Rule {

	private String table;
	private String uniqueColumn;
	private String periodColumn;
	private List<ColumnRule> aggrCols;
	
	
	public String getTable() {
		return table;
	}
	
	public String getUniqueColumn() {
		return uniqueColumn;
	}

	public void setUniqueColumn(String uniqueColumn) {
		this.uniqueColumn = uniqueColumn;
	}

	public void setTable(String table) {
		this.table = table;
	}
	public List<ColumnRule> getAggrCols() {
		return aggrCols;
	}
	public void setAggrCols(List<ColumnRule> aggrCols) {
		this.aggrCols = aggrCols;
	}

	public String getPeriodColumn() {
		return periodColumn;
	}

	public void setPeriodColumn(String periodColumn) {
		this.periodColumn = periodColumn;
	}
	
}
