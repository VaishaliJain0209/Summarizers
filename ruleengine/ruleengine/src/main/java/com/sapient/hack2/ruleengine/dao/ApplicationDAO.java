package com.sapient.hack2.ruleengine.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.sapient.hack2.ruleengine.model.ColumnRule;
import com.sapient.hack2.ruleengine.model.NameValue;

/**
 * Our DAO layer
 * @author ssh150
 *
 */

@Repository
public class ApplicationDAO {

	private static final String DB_NAME = "hack2";
	private static final int DB_PORT = 3306;
	private static final String DB_HOST = "10.207.94.216"; //"hack21.cabvnnxdzue9.ap-south-1.rds.amazonaws.com";
	private static final String DB_USER = "hack2017";
	private static final String DB_PASS = "hack2017";
	
	public List<List<NameValue>> getData (String query, List<ColumnRule> cols) {
		
		List<List<NameValue>> dataList = new ArrayList<List<NameValue>>();
		
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			Class.forName("com.mysql.jdbc.Driver");  
			con = DriverManager.getConnection("jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME, DB_USER, DB_PASS);
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			
			if (rs != null) {
				while (rs.next()) {
					
					List<NameValue> nameValueList = new ArrayList<NameValue>();
					
					for (ColumnRule col : cols) {
						NameValue val = getNameValue(col, rs);
						nameValueList.add(val);
					}
					
					dataList.add(nameValueList);
				}
			}
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			closeQuietly(rs);
			closeQuietly(stmt);
			closeQuietly(con);
		}
		
		return dataList;
	}
	
	/**
	 * 
	 * @param col
	 * @param rs
	 * @return
	 * @throws SQLException 
	 */
	private NameValue getNameValue (ColumnRule col, ResultSet rs) throws SQLException {
		
		NameValue nv = new NameValue();
		nv.setName(col.getColumn().toUpperCase());
		
		Object value = null;
		
		switch (col.getType()) {
		
		case INTEGER:
			value = rs.getInt(nv.getName());	
			break;
		case LONG:
			value = rs.getLong(nv.getName());
			break;
		case DOUBLE:
			value = rs.getDouble(nv.getName());
			break;
		case BIGDECIMAL:
			value = rs.getBigDecimal(nv.getName());
			break;
		case DATE:
			java.sql.Date dt = rs.getDate(nv.getName());
			if (dt != null) {
				value = new Date(dt.getTime());
			}
			break;
		default:
			value = rs.getString(nv.getName());
		}
		
		if (rs.wasNull()) {
			value = null;
		}
		
		nv.setValue(value);
		
		return nv;
	}
	
	/**
	 * 
	 * @param obj
	 */
	private void closeQuietly (AutoCloseable obj) {
		try {
			if (obj != null) {
				obj.close();
			}
		} catch (Exception ex) {
			// all silent
		}
	}
	
}
