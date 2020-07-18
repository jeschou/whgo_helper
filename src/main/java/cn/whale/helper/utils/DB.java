package cn.whale.helper.utils;

import java.sql.*;
import java.util.*;

public class DB {

    static String host = "postgres.develop.meetwhale.com";
    static String port = "5432";
    static String username = "postgres";
    static String password = "Buzhongyao123";
    static Map<String, String> PG_GO_TYPE = new HashMap<>();

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    static {
        PG_GO_TYPE.put("varchar", "string");
        PG_GO_TYPE.put("int", "int32");
        PG_GO_TYPE.put("int2", "int32");
        PG_GO_TYPE.put("int4", "int32");
        PG_GO_TYPE.put("int8", "int64");
        PG_GO_TYPE.put("serial", "int64");
        PG_GO_TYPE.put("bigserial", "int64");
        PG_GO_TYPE.put("float4", "float32");
        PG_GO_TYPE.put("float8", "float64");
        PG_GO_TYPE.put("bool", "bool");
        PG_GO_TYPE.put("_varchar", "pg.StringArray");
        PG_GO_TYPE.put("jsonb", "postgres.Jsonb");
        PG_GO_TYPE.put("json", "string");
        PG_GO_TYPE.put("timestamp", "*time.Time");
    }

    public static Connection getConnection(String database) {
        try {
            return DriverManager.getConnection("jdbc:postgresql://postgres.develop.meetwhale.com:5432/" + database, username, password);
        } catch (SQLException throwables) {
            throw new RuntimeException(throwables);
        }
    }

    static int doWithSql(ResultSetHandler resultSetHandler, String database, String sql, Object... args) {
        try (Connection conn = getConnection(database); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            ps.execute();
            int uc = ps.getUpdateCount();
            if (resultSetHandler == null) {
                return uc;
            }
            try (ResultSet rs = ps.getResultSet()) {
                ResultSetMetaData rsmd = rs.getMetaData();
                List<String> cnames = new ArrayList<>(rsmd.getColumnCount());
                int c = rsmd.getColumnCount();
                for (int i = 1; i <= c; i++) {
                    cnames.add(rsmd.getColumnName(i).toLowerCase());
                }

                while (rs.next()) {
                    resultSetHandler.handleRow(rs, cnames);
                }
            }
            return uc;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void handleResultSet(ResultSet rs, ResultSetHandler handler) throws Exception {
        ResultSetMetaData rsmd = rs.getMetaData();
        List<String> cnames = new ArrayList<>(rsmd.getColumnCount());
        int c = rsmd.getColumnCount();
        for (int i = 1; i <= c; i++) {
            cnames.add(rsmd.getColumnName(i).toLowerCase());
        }

        while (rs.next()) {
            handler.handleRow(rs, cnames);
        }
        safeClose(rs);
    }

    static void safeClose(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static List<String> getDatabases() {
        final List<String> list = new ArrayList<>();
        doWithSql((rs, cnames) -> {
            list.add(rs.getString(1));
        }, "", "select datname from pg_database");
        Collections.sort(list);
        return list;
    }

    public static List<String> getTables(String database) {
        final List<String> list = new ArrayList<>();
        doWithSql((rs, cnames) -> {
            list.add(rs.getString(1));
        }, database, "select tablename from pg_tables where schemaname='public'");
        Collections.sort(list);
        return list;
    }

    public static List<Column> getColumns(String database, String table) {
        final List<Column> list = new ArrayList<>();
        try (Connection conn = DB.getConnection(database)) {
            ResultSet rs = conn.getMetaData().getColumns(null, "public", table, "%");
            handleResultSet(rs, (rs0, cnames) -> {
                Column col = new Column();
                col.name = rs0.getString("COLUMN_NAME");
                col.type = rs0.getInt("DATA_TYPE");
                col.typeName = rs0.getString("TYPE_NAME");
                col.comment = rs0.getString("REMARKS");
                list.add(col);
            });
            rs = conn.getMetaData().getPrimaryKeys(null, null, table);
            handleResultSet(rs, (rs0, cnames) -> {
                String pkc = rs0.getString("COLUMN_NAME");
                list.stream().filter(c -> c.name.equals(pkc)).forEach(c -> c.isPk = true);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void main(String[] args) throws Exception {


        List<Column> columnList = getColumns("mango", "mango_shop");
        for (Column column : columnList) {
            System.out.println(column);
        }
    }

    public static String pgToGoType(String pGtype) {
        return PG_GO_TYPE.getOrDefault(pGtype, "string");
    }

    interface ResultSetHandler {
        void handleRow(ResultSet rs, List<String> colNames) throws Exception;
    }

    public static class Column {
        public String name;
        public int type;
        public String typeName;
        public boolean isPk;
        public String comment;

        @Override
        public String toString() {
            return "Column{" +
                    "name='" + name + '\'' +
                    ", type=" + type +
                    ", typeName='" + typeName + '\'' +
                    ", isPk=" + isPk +
                    ", comment='" + comment + '\'' +
                    '}';
        }
    }


}