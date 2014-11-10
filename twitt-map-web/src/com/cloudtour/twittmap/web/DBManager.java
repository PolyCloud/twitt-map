package com.cloudtour.twittmap.web;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class DBManager {
	Connection conn;
	Statement stmt;
	String url;
	String user;
	String password;
	String sql;
	ResultSet rs;
	String queryFormat = "(select sLatitude, sLongitude from status "
			+ "where sDate>\'%s\' and sDate<\'%s\') " + "union "
			+ "(select sLatitude, sLongitude from status "
			+ "where sDate='%s' and sTime>='%s') " + "union "
			+ "(select sLatitude, sLongitude from status "
			+ "where sDate='%s' and sTime<='%s')";
	String queryFormatSameDate = "select sLatitude, sLongitude from status "
			+ "where sDate=\'%s\' and sTime between \'%s\' and \'%s\' ";

	public DBManager() {
		conn = null;
		stmt = null;
		url = null;
		user = null;
		password = null;
		sql = null;
		rs = null;
	}

	public void getDirver() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			System.out.println("load driver error");
			e.printStackTrace();
		}
	}

	public void connect() {
		try {
			url = "jdbc:mysql://localhost/twittmap?user=root&password=root?useUnicode=true&characterEncoding=UTF-8";
			user = "root";
			password = "root";
			conn = DriverManager.getConnection(url, user, password);
		} catch (SQLException e) {
			System.out.println("connect error");
			e.printStackTrace();
		}
	}

	public void connectAWS() {
		try {
			url = "jdbc:mysql://twittmap.ca3jfiqjrcc5.us-west-2.rds.amazonaws.com:3306/twittMap";
			user = "root";
			password = "rootroot";
			conn = DriverManager.getConnection(url, user, password);
		} catch (SQLException e) {
			System.out.println("connect error");
			e.printStackTrace();
		}
	}

	public String queryNum() {
		String out = null;
		try {
			stmt = conn.createStatement();
			sql = "select count(*) as num from status ";
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				out = rs.getString("num") + "";
				// System.out.println(out);
			}

		} catch (SQLException e) {
			System.out.println("sql error");
			e.printStackTrace();
		}
		return out;
	}

	public List<List<String>> queryLatLng(String begindate, String begintime,
			String enddate, String endtime) {
		List<List<String>> result = new ArrayList<List<String>>();
		try {
			stmt = conn.createStatement();
			if (begindate.equals(enddate))
				sql = String.format(queryFormatSameDate, begindate, begintime,
						endtime);
			else
				sql = String.format(queryFormat, begindate, enddate, begindate,
						begintime, enddate, endtime);
			System.out.println(sql);

			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				List<String> pair = new ArrayList<String>();
				pair.add(rs.getString("sLatitude"));
				pair.add(rs.getString("sLongitude"));
				result.add(pair);
			}
		} catch (SQLException e) {
			System.out.println("sql error");
			e.printStackTrace();
		}
		return result;
	}

	public void shutdown() {
		try {
			if (stmt != null) {
				stmt.close();
				stmt = null;
			}
			if (conn != null) {
				conn.close();
				conn = null;
			}
		} catch (Exception e) {
			System.out.println("close db error");
			e.printStackTrace();
		}
	}

	// public static void main(String args[]) {
	// DBManager ma = new DBManager();
	// ma.getDirver();
	// ma.connectAWS();
	// String s = ma.queryLatLng();
	// ma.shutdown();
	// }

}
