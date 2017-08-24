package com.sapient.hack2.ruleengine.service;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.sapient.hack2.ruleengine.dao.ApplicationDAO;
import com.sapient.hack2.ruleengine.model.ColumnRule;
import com.sapient.hack2.ruleengine.model.NameValue;
import com.sapient.hack2.ruleengine.model.Rule;
import com.sapient.hack2.ruleengine.util.AggregateOperations;
import com.sapient.hack2.ruleengine.util.ApplicationUtil;
import com.sapient.hack2.ruleengine.util.ColumnType;
import com.sapient.hack2.ruleengine.util.Period;

/**
 * Service class
 * @author ssh150
 *
 */
@Service("applicationService")
public class ApplicationService {
	
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

	@Autowired
	private ApplicationUtil appUtil;
	
	@Autowired
	private ApplicationDAO applicationDAO;
	
	public void processRules() {
		List<Rule> rules = getRuleJson();
		
		System.out.println("Processing rules..");
		if (rules != null && !rules.isEmpty()) {
		
			// TODO Validation of rules
			
			// process rules
			for (Rule rule : rules) {
				processRule(rule);
			}
			
		}
		
		System.out.println("Rule processing completed successfully.");
	}
	
	/**
	 * 
	 * @param rule
	 */
	private void processRule (Rule rule) {
	
		System.out.println("Processing rule " + rule.getTable());
		boolean needAggregation = false;
		int maxCalculateUpto = -1;
		int periodOrdinal = 100;
		Period period = null;
		
		// We need to make query as well as where conditions
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("SELECT ");
		
		Iterator<ColumnRule> itr = rule.getAggrCols().iterator();
		while (itr.hasNext()) {
			
			ColumnRule cr = itr.next();
			
			queryBuilder.append(cr.getColumn().toUpperCase());
			
			if (cr.getAllowedOperations() != null && !cr.getAllowedOperations().isEmpty()) {
				needAggregation = true;
			}
			
			if (cr.getCalculateUpto() != null && cr.getCalculateUpto() > maxCalculateUpto) {
				maxCalculateUpto = cr.getCalculateUpto();
			}
			
			if (cr.getPeriod() != null && cr.getPeriod().ordinal() < periodOrdinal) {
				periodOrdinal = cr.getPeriod().ordinal();
				period = cr.getPeriod();
			}
			
			if (itr.hasNext()) {
				queryBuilder.append(", ");
			}
		}
		
		queryBuilder.append(" from  ").append(rule.getTable());
		
		if (needAggregation && maxCalculateUpto != -1) {
			
			queryBuilder.append(" where ")
						.append(rule.getPeriodColumn())
						.append(" > curdate() - INTERVAL ")
						.append(maxCalculateUpto)
						.append(" ").append(period.name())
						.append(" order by ").append(rule.getPeriodColumn()).append(" desc ");
			
		}
		
		String query = queryBuilder.toString();
		
		System.out.println("Getting data from database " + query);
		
		List<List<NameValue>> rawData = applicationDAO.getData(query, rule.getAggrCols());
		
		System.out.println("Data retrieved successfully... ");
		
		// Iterate over raw data and create the map of raw data. on that map apply the aggregation rules.
		Map<String, Map<String, Map<String, Object>>> dataMap = new HashMap<String, Map<String, Map<String, Object>>>();
		
		for (List<NameValue> record : rawData) {
							
			Optional<NameValue> op = record.stream().filter(r -> r.getName().equalsIgnoreCase(rule.getUniqueColumn())).findFirst();
			if (op.isPresent()) {
				String key = op.get().getValue().toString();
				if (!dataMap.containsKey(key)) {
					dataMap.put(key, new LinkedHashMap<>());
				}
				
				// Get date
				String date = null;
				Optional<NameValue> dateOp = record.stream().filter(r -> r.getName().equalsIgnoreCase(rule.getPeriodColumn())).findFirst();
				if (dateOp.isPresent()) {
					date = dateOp.get().getValue() != null ? toString((Date)dateOp.get().getValue()): null;
				}
				
				if (date == null) {
					continue;
				} else if (!dataMap.get(key).containsKey(date)) {
					dataMap.get(key).put(date, new HashMap<>());
				}
				
				for (NameValue col : record) {
					if (!rule.getPeriodColumn().equalsIgnoreCase(col.getName()) && !rule.getUniqueColumn().equalsIgnoreCase(col.getName())) {
						dataMap.get(key).get(date).put(col.getName(), col.getValue());
					}
				}
			}
		}
		
		System.out.println("Data organized into map... " );
		
		// Get all the dates when a file must be written.
		int exportDateIndex = 0;
		List<String> exportDates = getExportDates(maxCalculateUpto, period);
		
		//Now lets aggregate
		Map<String, Map<String, Map<String, BigDecimal>>> aggregationMap = new HashMap<>();
		
		int testYear = 0;
		String testkey = null;
		if (dataMap != null && !dataMap.isEmpty()) {
			for (Entry<String, Map<String, Map<String, Object>>> entry : dataMap.entrySet()) {
				String uniqueCol = entry.getKey();
				Map<String, Map<String, Object>> dateDataMap = entry.getValue();
				if (dateDataMap != null && !dateDataMap.isEmpty()) {
					exportDateIndex = 0;
					for (Entry<String, Map<String, Object>> dataEntry : dateDataMap.entrySet()) {
						String date = dataEntry.getKey();
						Map<String, Object> nameValueMap = dataEntry.getValue();
						doAggregation(uniqueCol, rule.getAggrCols(), nameValueMap, aggregationMap);			
						if (exportDateIndex < exportDates.size() && exportDates.get(exportDateIndex).equals(date)) {
							testYear = exportDateIndex + 1;
							testkey = uniqueCol;
							formatDataAndSaveOnS3(uniqueCol, dateDataMap, exportDates.get(exportDateIndex), aggregationMap, exportDateIndex + 1);
							exportDateIndex++;
						}
					}
					
					if (exportDateIndex < exportDates.size()) {
						formatDataAndSaveOnS3(uniqueCol, dateDataMap, exportDates.get(exportDateIndex), aggregationMap, exportDateIndex + 1);
					}
				}
			}
		}
		
		System.out.println("Process complete...");
		System.out.println(getSavedData(testYear, testkey));
	}
	
	
	private void formatDataAndSaveOnS3(String uniqueCol,
			Map<String, Map<String, Object>> dateDataMap, String effDt,
			Map<String, Map<String, Map<String, BigDecimal>>> aggregationMap, int exportDateIndex) {
		
		Map<String, Object> dataMap = new HashMap<String, Object>();
		
		if (dateDataMap != null && !dateDataMap.isEmpty()) {
			List<Map<String, Object>> dataList = new LinkedList<Map<String,Object>>();
			for (Entry<String, Map<String, Object>> entry : dateDataMap.entrySet()) {
				if (entry.getValue() != null && !entry.getValue().isEmpty()) {
					Map<String, Object> keyValue = new HashMap<String, Object>();
					keyValue.put("EFF_DT", (String)entry.getKey());
					for (Entry<String, Object> ent : entry.getValue().entrySet()) {
						if (ent.getValue() != null) {
							keyValue.put(ent.getKey(), ent.getValue());
						}
					}
					
					if (!keyValue.isEmpty()) {
						dataList.add(keyValue);
					}
				}
				if (effDt.equals(entry.getKey())) {
					break;
				}
			}
			
			if (!dataList.isEmpty()) {
				dataMap.put("rawData", dataList);
			}
		}
		
		if (aggregationMap != null && !aggregationMap.isEmpty()) {
			
			if (aggregationMap.get(uniqueCol) != null && !aggregationMap.get(uniqueCol).isEmpty()) {
				dataMap.put("aggregations", aggregationMap.get(uniqueCol));
			}
		}
		
		
		if (!dataMap.isEmpty()) {
			try {
				Gson gson = new Gson();
				String json = gson.toJson(dataMap);
				
				byte[] contentAsBytes = json.getBytes("UTF-8");
				
				ObjectMetadata md = new ObjectMetadata();
				md.setContentLength(contentAsBytes.length);
				md.setContentEncoding("UTF-8");
				
				try (final ByteArrayInputStream is = new ByteArrayInputStream(contentAsBytes)) {
					String key = new StringBuilder()
									.append(exportDateIndex)
									.append("Y")
									.append("/")
									.append(uniqueCol)
									.append(".json")
									.toString();
					System.out.println(key);
					appUtil.s3ObjectWrite(key, is, md);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public Map<String, Object> getSavedData (int testYear, String uniqueCol) {
		String key = testYear + "Y" + "/" + uniqueCol + ".json";
		System.out.println("Key: " + key);
		Map<String, Object> dataMap = null;
		
		Object object = appUtil.s3ObjectRead(key, new TypeToken<Map<String, Object>>() {	}.getType());
		if (object != null) {
			dataMap = (Map<String, Object>)object;
		}

		return dataMap;
	}

	private void doAggregation (String uniqueCol,  List<ColumnRule> cols, Map<String, Object> nameValueMap, Map<String, Map<String, Map<String, BigDecimal>>> aggregationMap) {
		
		if (nameValueMap == null || nameValueMap.isEmpty()) {
			return;
		}
		
		if (!aggregationMap.containsKey(uniqueCol)) {
			aggregationMap.put(uniqueCol, new HashMap<>());
		}
		
		for (Entry<String, Object> entry :  nameValueMap.entrySet()) {
			String field = entry.getKey();
			Object val = entry.getValue();
			BigDecimal currValue = val != null ? new BigDecimal(val.toString()) : null;
			
			Optional<ColumnRule> colOp =  cols.stream().filter(c -> c.getColumn().equalsIgnoreCase(field)).findFirst();
			ColumnRule col = colOp.isPresent() ? colOp.get() : null;
			
			if (col != null && col.getAllowedOperations() != null && !col.getAllowedOperations().isEmpty()) {
				
				if (!aggregationMap.get(uniqueCol).containsKey(col.getColumn().toUpperCase())) {
					aggregationMap.get(uniqueCol).put(col.getColumn().toUpperCase(), new HashMap<String, BigDecimal>());
				}
				
				List<AggregateOperations> aggregateOps = col.getAllowedOperations();
				for (AggregateOperations agOp : aggregateOps) {
					switch (agOp) {
					case AVG:
						BigDecimal total = aggregationMap.get(uniqueCol).get(col.getColumn().toUpperCase()).get("TOTAL");
						BigDecimal count = aggregationMap.get(uniqueCol).get(col.getColumn().toUpperCase()).get("COUNT");
						
						if (total == null) {
							total = new BigDecimal(0);
						}
						
						if (count == null) {
							count = new BigDecimal(0);
						}
						
						if (currValue != null) {
							total = total.add(currValue);
							count = count.add(new BigDecimal(1));
							BigDecimal average = total.divide(count, 5, RoundingMode.HALF_UP);
							
							aggregationMap.get(uniqueCol).get(col.getColumn().toUpperCase()).put("TOTAL", total);
							aggregationMap.get(uniqueCol).get(col.getColumn().toUpperCase()).put("COUNT", count);
							aggregationMap.get(uniqueCol).get(col.getColumn().toUpperCase()).put("AVG", average);
						}
						
						break;
					case MIN:
						Object minVal = aggregationMap.get(uniqueCol).get(col.getColumn().toUpperCase()).get("MIN");
						BigDecimal minOldVal = minVal != null ? new BigDecimal(minVal.toString()) : null;
						if (minOldVal == null && currValue != null) {
							aggregationMap.get(uniqueCol).get(col.getColumn().toUpperCase()).put("MIN", currValue);
						} else if (minOldVal != null && currValue != null) {
							aggregationMap.get(uniqueCol).get(col.getColumn().toUpperCase()).put("MIN", currValue.min(minOldVal));
						}
						break;
					case MAX:
						Object maxVal = aggregationMap.get(uniqueCol).get(col.getColumn().toUpperCase()).get("MAX");
						BigDecimal maxOldVal = maxVal != null ? new BigDecimal(maxVal.toString()) : null;
						if (maxOldVal == null && currValue != null) {
							aggregationMap.get(uniqueCol).get(col.getColumn().toUpperCase()).put("MAX", currValue);
						} else if (maxOldVal != null && currValue != null) {
							aggregationMap.get(uniqueCol).get(col.getColumn().toUpperCase()).put("MAX", currValue.max(maxOldVal));
						}
						break;
					}
				}				
				
			}
			
		}
		
	}
	
	
	/**
	 * Provides the dates when file will be created.
	 * @param maxUpto
	 * @param period
	 * @return
	 */
	private List<String> getExportDates(int maxUpto, Period period) {
		
		LocalDate date = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		
		List<String> exportDates = new LinkedList<String>();
		
		for (int x = 0; x < maxUpto; x++) {
			
			switch (period) {
			
			case YEAR:
				date = date.plusYears(-1);
				exportDates.add(formatter.format(date));
				break;
			default:
				
				break;
			
			}
			
		}
		
		return exportDates;
	}
	
	
	private String toString(Date date) {
		String dateStr = null;
		if (date != null) {
			dateStr = SDF.format(date);
		}
		return dateStr;
	}
	
	/**
	 * Create rule file on s3
	 */
	public void createRuleJson() {
		String key = "rules/rule.json";
		
		List<Rule> ruleList = new ArrayList<Rule>();
		
		Rule rule = new Rule();
		rule.setTable("hack2.security_hist");
		rule.setUniqueColumn("id");
		rule.setPeriodColumn("eff_date");
		
		ColumnRule colRule = new ColumnRule();
		colRule.setColumn("id");
		colRule.setType(ColumnType.LONG);
		
		ColumnRule colRule4 = new ColumnRule();
		colRule4.setColumn("eff_date");
		colRule4.setType(ColumnType.DATE);
				
		ColumnRule colRule1 = new ColumnRule();
		colRule1.setColumn("price");
		colRule1.setAllowedOperations(Arrays.asList(AggregateOperations.AVG, AggregateOperations.MAX, AggregateOperations.MIN));
		colRule1.setPeriod(Period.YEAR);
		colRule1.setCalculateUpto(5);
		colRule1.setType(ColumnType.BIGDECIMAL);
		
		ColumnRule colRule2 = new ColumnRule();
		colRule2.setColumn("oas");
		colRule2.setAllowedOperations(Arrays.asList(AggregateOperations.AVG, AggregateOperations.MAX, AggregateOperations.MIN));
		colRule2.setPeriod(Period.YEAR);
		colRule2.setCalculateUpto(5);
		colRule2.setType(ColumnType.BIGDECIMAL);
		
		ColumnRule colRule3 = new ColumnRule();
		colRule3.setColumn("yield");
		colRule3.setAllowedOperations(Arrays.asList(AggregateOperations.AVG, AggregateOperations.MAX, AggregateOperations.MIN));
		colRule3.setPeriod(Period.YEAR);
		colRule3.setCalculateUpto(5);
		colRule3.setType(ColumnType.BIGDECIMAL);
		
		rule.setAggrCols(Arrays.asList(colRule, colRule4, colRule1, colRule2, colRule3));
		
		ruleList.add(rule);
		
		try {
			Gson gson = new Gson();
			String json = gson.toJson(ruleList);
			
			byte[] contentAsBytes = json.getBytes("UTF-8");
			
			ObjectMetadata md = new ObjectMetadata();
			md.setContentLength(contentAsBytes.length);
			md.setContentEncoding("UTF-8");
			
			try (final ByteArrayInputStream is = new ByteArrayInputStream(contentAsBytes)) {
				appUtil.s3ObjectWrite(key, is, md);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Rule created..");
		
	}
	
	@SuppressWarnings({ "serial", "unchecked" })
	public List<Rule> getRuleJson () {
		
		String key = "rules/rule.json";
		List<Rule> ruleList = null;
		
		Object object = appUtil.s3ObjectRead(key, new TypeToken<List<Rule>>() {	}.getType());
		if (object != null) {
			ruleList = (List<Rule>)object;
		}

		return ruleList;
	}
}
