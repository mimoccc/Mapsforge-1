///*
// * Copyright 2010 mapsforge.org
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package org.mapsforge.android.routing.blockedHighwayHierarchies;
//
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.LineNumberReader;
//import java.io.PrintStream;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//
//import org.mapsforge.core.DBConnection;
//
//public class x {
//
//	private static void getAvgPhase1(Connection conn, PrintStream ps)
//			throws SQLException {
//		System.out.println("getAvgPhase1()");
//		ps.println("#parameterset\t avg_bytes \t avg_reads \t avg_time_s");
//		for (int i = 1; i <= 27; i++) {
//			String sql = "select r.file_name, b.h, b.hop_limit, b.c, avg(r.p1_bytes_read)::INTEGER as avg_bytes, avg(r.p1_num_cluster_reads)::INTEGER as avg_reads, avg(r.p1_read_time_s) as avg_time_s from hh_binary b NATURAL JOIN test_route r WHERE r.file_name "
//					+ "like('%ger_"
//					+ ((i < 10) ? "0" : "")
//					+ i
//					+ "%') group by b.h, b.hop_limit, b.c, r.file_name ORDER BY avg(r.p1_bytes_read);";
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(sql);
//
//			while (rs.next()) {
//				int avgBytes = rs.getInt("avg_bytes");
//				int avgReads = rs.getInt("avg_reads");
//				double avgTimeS = rs.getDouble("avg_time_s");
//				ps.println(i + "\t" + avgBytes + "\t" + avgReads + "\t" + avgTimeS);
//			}
//			System.out.println(i + "/" + 27);
//		}
//	}
//
//	private static void getAvgPhase2(Connection conn, PrintStream ps)
//			throws SQLException {
//		System.out.println("getAvgPhase2()");
//		ps.println("#parameterset\t avg_bytes \t avg_reads \t avg_time_s");
//		for (int i = 1; i <= 27; i++) {
//			String sql = "select r.file_name, b.h, b.hop_limit, b.c, avg(r.p2_bytes_read)::INTEGER as avg_bytes, avg(r.p2_num_cluster_reads)::INTEGER as avg_reads, avg(r.p2_read_time_s) as avg_time_s from hh_binary b NATURAL JOIN test_route r WHERE r.file_name "
//					+ "like('%ger_"
//					+ ((i < 10) ? "0" : "")
//					+ i
//					+ "%') group by b.h, b.hop_limit, b.c, r.file_name ORDER BY avg(r.p2_bytes_read);";
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(sql);
//
//			while (rs.next()) {
//				int avgBytes = rs.getInt("avg_bytes");
//				int avgReads = rs.getInt("avg_reads");
//				double avgTimeS = rs.getDouble("avg_time_s");
//				ps.println(i + "\t" + avgBytes + "\t" + avgReads + "\t" + avgTimeS);
//			}
//			System.out.println(i + "/" + 27);
//		}
//	}
//
//	private static void getAvgPhasesCombined(Connection conn, PrintStream ps)
//			throws SQLException {
//		System.out.println("getAvgPhasesCombined()");
//		ps.println("#parameterset\t avg_bytes \t avg_reads \t avg_time_s");
//		for (int i = 1; i <= 27; i++) {
//			String sql = "select r.file_name, b.h, b.hop_limit, b.c, avg(r.p1_bytes_read + r.p2_bytes_read)::INTEGER as avg_bytes, avg(r.p1_num_cluster_reads + r.p2_num_cluster_reads)::INTEGER as avg_reads, avg(r.p1_read_time_s + r.p2_read_time_s) as avg_time_s from hh_binary b NATURAL JOIN test_route r WHERE r.file_name "
//					+ "like('%ger_"
//					+ ((i < 10) ? "0" : "")
//					+ i
//					+ "%') group by b.h, b.hop_limit, b.c, r.file_name ORDER BY avg(r.p1_bytes_read + r.p2_bytes_read);";
//
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(sql);
//
//			while (rs.next()) {
//				int avgBytes = rs.getInt("avg_bytes");
//				int avgReads = rs.getInt("avg_reads");
//				double avgTimeS = rs.getDouble("avg_time_s");
//				ps.println(i + "\t" + avgBytes + "\t" + avgReads + "\t" + avgTimeS);
//			}
//			System.out.println(i + "/" + 27);
//		}
//	}
//
//	private static void insertNexusData(File f, Connection conn) throws IOException,
//			SQLException {
//		LineNumberReader lnr = new LineNumberReader(new FileReader(new File(
//				"evaluation/nexus.txt")));
//		String sql = "INSERT INTO data (rank, overall, read) VALUES (?, ?, ?);";
//		PreparedStatement pstmt = conn.prepareStatement(sql);
//
//		String line = lnr.readLine();
//		while ((line = lnr.readLine()) != null) {
//			String[] tmp = line.split("\t");
//			long rank = Long.parseLong(tmp[0]);
//			long overallNanos = Long.parseLong(tmp[1]);
//			long readNanos = Long.parseLong(tmp[2]);
//			pstmt.setLong(1, rank);
//			pstmt.setLong(2, overallNanos);
//			pstmt.setLong(3, readNanos);
//			pstmt.addBatch();
//		}
//		pstmt.executeBatch();
//	}
//
//	private static void getAvgClustering(Connection conn, PrintStream ps) throws SQLException {
//		// cols threshold kc-t kc-f quad-t quad-f
//		ps
//				.print("#threshold \t kc-t_time \t kc-t_reads \t kc-t_bytes \t kc-f_time \t kc-f_reads \t kc-f_bytes \t quad-t_time \t quad-t_reads \t quad-t_bytes \t quad-f_time \t quad-f_reads \t quad-f_bytes\n");
//		int[][] vals = new int[13][11];
//		vals[0][0] = 50;
//		for (int i = 1; i < vals[0].length; i++) {
//			vals[0][i] = vals[0][i - 1] + 50;
//		}
//
//		String sql = "select b.clustering, b.clustering_threshold, b.hop_indices, avg(r.p1_read_time_s + r.p2_read_time_s) as avg_time_s, avg(r.p1_num_cluster_reads + r.p2_num_cluster_reads)::integer as avg_reads, avg(r.p1_bytes_read + r.p2_bytes_read)::integer as avg_bytes from hh_binary b natural join test_route r where b.file_name like('%12%') and b.c = 1.5 and b.h=100 and b.hop_limit = 15 group by b.clustering, b.clustering_threshold, b.hop_indices;";
//		Statement stmt = conn.createStatement();
//		ResultSet rs = stmt.executeQuery(sql);
//		while (rs.next()) {
//			int threshold = rs.getInt("clustering_threshold");
//			String algo = rs.getString("clustering");
//			boolean hopIndices = rs.getBoolean("hop_indices");
//			double avgTimeS = rs.getDouble("avg_time_s");
//			int avgReads = rs.getInt("avg_reads");
//			int avgBytes = rs.getInt("avg_bytes");
//
//			int startCol;
//			if (hopIndices) {
//				if (algo.equals("k_center")) {
//					startCol = 1;
//				} else {
//					startCol = 4;
//				}
//			} else {
//				if (algo.equals("k_center")) {
//					startCol = 7;
//				} else {
//					startCol = 10;
//				}
//			}
//			int row = (threshold / 50) - 1;
//			if (threshold == 75) {
//				row = 10;
//			}
//
//			vals[startCol][row] = (int) (avgTimeS * 1000);
//			vals[startCol + 1][row] = avgBytes;
//			vals[startCol + 2][row] = avgReads;
//		}
//		for (int i = 0; i < vals[0].length; i++) {
//			for (int j = 0; j < vals.length; j++) {
//				ps.print(vals[j][i] + "\t");
//			}
//			ps.print("\n");
//		}
//	}
//
//	public static void main(String[] args) throws SQLException, IOException {
//		Connection conn1 = DBConnection.getJdbcConnectionPg("localhost", 5432,
//				"eval_params_separate_phases",
//				"osm", "osm");
//		Connection conn2 = DBConnection.getJdbcConnectionPg("localhost", 5432,
//				"eval_params_both_phses",
//				"osm", "osm");
//		Connection conn3 = DBConnection.getJdbcConnectionPg("localhost", 5432,
//				"eval_clustering",
//				"osm", "osm");
//		Connection conn4 = DBConnection.getJdbcConnectionPg("localhost", 5432,
//				"eval_nexus",
//				"osm", "osm");
//
//		PrintStream ps1 = new PrintStream(new File("evaluation/phase1.dat"));
//		PrintStream ps2 = new PrintStream(new File("evaluation/phase2.dat"));
//		PrintStream ps3 = new PrintStream(new File("evaluation/phasesCombined.dat"));
//		PrintStream ps4 = new PrintStream(new File("evaluation/clustering.dat"));
//
//		// getAvgPhase1(conn1, ps1);
//		// getAvgPhase2(conn1, ps2);
//		// getAvgPhasesCombined(conn2, ps3);
//		// getAvgClustering(conn3, ps4);
//		insertNexusData(new File("evaluation/nexus.txt"), conn4);
//		conn1.close();
//		conn2.close();
//		conn3.close();
//		conn4.close();
//		ps1.close();
//		ps2.close();
//		ps3.close();
//		ps4.close();
//	}
//
// }
