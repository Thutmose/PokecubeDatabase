package pokecube.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class DatabaseConverter
{
    static Connection conn = null;
    static Statement  stmt = null;
    static ResultSet  rslt = null;

    public static void main(String[] args)
    {
        try
        {
            // STEP 2: Register JDBC driver
            Class.forName("org.h2.Driver");
            // STEP 3: Open a connection
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection("jdbc:h2:./databases/pokemobs");
            System.out.println(conn.getMetaData());
            // STEP 4: Execute a query
            System.out.println("Creating statement...");
            stmt = conn.createStatement();
            // drop();
             init();

            convertToXML();
            // drop();
        }
        catch (SQLException se)
        {
            // Handle errors for JDBC
            se.printStackTrace();
        }
        catch (Exception e)
        {
            // Handle errors for Class.forName
            e.printStackTrace();
        }
        finally
        {
            // finally block used to close resources
            try
            {
                if (stmt != null) stmt.close();
            }
            catch (SQLException se2)
            {
            } // nothing we can do
            try
            {
                if (rslt != null) rslt.close();
            }
            catch (SQLException se2)
            {
            } // nothing we can do
            try
            {
                if (conn != null) conn.close();
            }
            catch (SQLException se)
            {
                se.printStackTrace();
            } // end finally try
        } // end try
        System.out.println("Goodbye!");
    }// end main

    static void drop() throws SQLException
    {
        System.out.println("Dropping old tables");
        stmt.executeUpdate("DROP TABLE EVS");
        stmt.executeUpdate("DROP TABLE SPAWNS");
        stmt.executeUpdate("DROP TABLE BASESTATS");
        stmt.executeUpdate("DROP TABLE ABILITES");
        stmt.executeUpdate("DROP TABLE POKEDEX");
    }

    static void init() throws SQLException
    {
        stmt.executeUpdate("CREATE TABLE POKEDEX");
        System.out.println("copying...");
        stmt.executeUpdate("CREATE TABLE EVS AS SELECT * FROM CSVREAD('./databases/evsXp.csv');");
        stmt.executeUpdate("CREATE TABLE SPAWNS AS SELECT * FROM CSVREAD('./databases/spawndata.csv');");
        stmt.executeUpdate("CREATE TABLE ABILITES AS SELECT * FROM  CSVREAD('./databases/abilities.csv');");
        stmt.executeUpdate("CREATE TABLE BASESTATS AS SELECT * FROM CSVREAD('./databases/baseSTats.csv');");
        copyToPokedex();
        outputCSVs();
    }

    private static void outputCSVs() throws SQLException
    {
        System.out.println("Collecting Result...");
        rslt = stmt.executeQuery("SELECT * FROM POKEDEX");
        System.out.println("Printing result...");
        int n = 0;
        ArrayList<String> names = new ArrayList<>();
        while (rslt.next())// && n++ <= 1)
        {
            names.add(rslt.getString(rslt.findColumn("NAME")).replace("'", "''"));
            n++;
        }
        for (String s : names)
        {
            PreparedStatement statement = conn.prepareStatement("CALL CSVWRITE('temp/" + s
                    + ".csv', 'SELECT * FROM POKEDEX WHERE NAME=''" + s.replace("'", "''") + "''');");
            statement.setEscapeProcessing(true);
            statement.execute();
            statement.close();
        }
        System.out.println("number: " + n);
    }

    private static void convertToXML()
    {
        File files = new File(".");
        File nameDir = new File(files, "temp");
        System.out.println(
                files.exists() + " " + files.isDirectory() + " " + nameDir.exists() + " " + nameDir.isDirectory());
        File moveFile = null;
        File databases = new File(files, "databases");
        for (File file : databases.listFiles())
        {
            if (file.getName().contains("moveLists"))
            {
                moveFile = file;
                break;
            }
        }
        try
        {
            ArrayList<ArrayList<String>> rows = getRows(moveFile);

            ArrayList<PokemobData> pokemobs = new ArrayList<>();
            for (int i = 0; i < rows.size() - 1; i += 2)
            {
                ArrayList<String> row = rows.get(i);
                ArrayList<String> row2 = rows.get(i + 1);
                if (row.size() < 2) continue;
                if (row2.size() < 2) continue;

                // if (i > 5) break;

                String[] levels = row.get(1).split(":");
                String[] moves = row2.get(1).split(":");

                if (levels.length > moves.length)
                {
                    System.err.println("Error in moves for " + row.get(0));
                    continue;
                }

                File csv = new File(nameDir, row.get(0) + ".csv");

                Map<Integer, ArrayList<String>> lvlUpMoves = new HashMap<Integer, ArrayList<String>>();
                ArrayList<String> extraMoves = new ArrayList<String>();
                int n = 0;
                for (n = 0; n < levels.length; n++)
                {
                    int level = 0;
                    try
                    {
                        level = Integer.parseInt(levels[n]);
                    }
                    catch (NumberFormatException e1)
                    {
                    }
                    ArrayList<String> movesForLevel = lvlUpMoves.get(level);
                    if (movesForLevel == null)
                    {
                        movesForLevel = new ArrayList<String>();
                        lvlUpMoves.put(level, movesForLevel);
                    }
                    movesForLevel.add(convertName(moves[n].trim()));
                }
                for (n = levels.length; n < moves.length; n++)
                {
                    if (!moves[n].trim().isEmpty() && !extraMoves.contains(convertName(moves[n].trim())))
                        extraMoves.add(convertName(moves[n].trim()));
                }
                ArrayList<ArrayList<String>> csvRows = getRows(csv);
                pokemobs.add(
                        new PokemobData(Integer.parseInt(row2.get(0)), row.get(0), csvRows, extraMoves, lvlUpMoves));
            }
            files:
            for (File file : nameDir.listFiles())
            {
                if (file.getName().contains(".csv"))
                {
                    String name = file.getName().replace(".csv", "");
                    for (PokemobData data : pokemobs)
                    {
                        if (data.name.equalsIgnoreCase(name))
                        {
                            continue files;
                        }
                    }
                    ArrayList<ArrayList<String>> csvRows = getRows(file);
                    int num = Integer.parseInt(csvRows.get(1).get(0).replace("\"", ""));
                    pokemobs.add(new PokemobData(num, name, csvRows, new HashSet<String>(),
                            new HashMap<Integer, ArrayList<String>>()));
                }
            }

            Collections.sort(pokemobs, new Comparator<PokemobData>()
            {
                @Override
                public int compare(PokemobData o1, PokemobData o2)
                {
                    return o1.number - o2.number;
                }
            });

            ArrayList<PokemobData> gen1 = new ArrayList<>();
            ArrayList<PokemobData> gen2 = new ArrayList<>();
            ArrayList<PokemobData> gen3 = new ArrayList<>();
            ArrayList<PokemobData> gen4 = new ArrayList<>();
            ArrayList<PokemobData> gen5 = new ArrayList<>();
            ArrayList<PokemobData> gen6 = new ArrayList<>();

            for (PokemobData data : pokemobs)
            {
                int number = data.number;
                int generation = getGen(number);
                if (generation == 1) gen1.add(data);
                if (generation == 2) gen2.add(data);
                if (generation == 3) gen3.add(data);
                if (generation == 4) gen4.add(data);
                if (generation == 5) gen5.add(data);
                if (generation == 6) gen6.add(data);
            }

            File xml;
            xml = new File(files, "gen1.xml");
            XMLWriter.writeXML(xml, gen1);
            xml = new File(files, "gen2.xml");
            XMLWriter.writeXML(xml, gen2);
            xml = new File(files, "gen3.xml");
            XMLWriter.writeXML(xml, gen3);
            xml = new File(files, "gen4.xml");
            XMLWriter.writeXML(xml, gen4);
            xml = new File(files, "gen5.xml");
            XMLWriter.writeXML(xml, gen5);
            xml = new File(files, "gen6.xml");
            XMLWriter.writeXML(xml, gen6);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static int getGen(int pokedexNb)
    {
        if (pokedexNb < 152) return 1;
        if (pokedexNb < 252) return 2;
        if (pokedexNb < 387) return 3;
        if (pokedexNb < 494) return 4;
        if (pokedexNb < 650) return 5;
        if (pokedexNb < 722) return 6;
        return 0;
    }

    private static void copyToPokedex() throws SQLException
    {
        addColumns("BASESTATS", "POKEDEX", true);
        addColumns("EVS", "POKEDEX", false);
        addColumns("ABILITES", "POKEDEX", false);
        addColumns("SPAWNS", "POKEDEX", false);
    }

    private static void addColumns(String tableFrom, String tableTo, boolean first) throws SQLException
    {
        rslt = stmt.executeQuery("SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='" + tableFrom + "'");
        HashSet<String> columnsFrom = new HashSet<>();

        while (rslt.next())
        {
            columnsFrom.add(rslt.getString(4));
        }

        rslt = stmt.executeQuery("SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='" + tableTo + "'");
        HashSet<String> columnsTo = new HashSet<>();
        while (rslt.next())
        {
            columnsTo.add(rslt.getString(4));
        }
        ArrayList<String> columns = new ArrayList<String>();
        for (String s : columnsFrom)
        {
            if (!columnsTo.contains(s))
            {
                columns.add(s);
            }
        }
        String colmns = "";
        int n = 0;
        for (String s : columns)
        {
            String colm = convertName(s);
            colmns += colm;
            if (n < columns.size() - 1) colmns += ", ";

            try
            {
                stmt.executeUpdate("ALTER TABLE " + tableTo + " ADD " + colm + " VARCHAR");
            }
            catch (Exception e)
            {
                System.out.println(e);
            }
            n++;
        }

        if (first)
        {
            rslt = stmt.executeQuery("SELECT * FROM " + tableFrom);
            // System.out.println("Printing result...");
            ArrayList<ArrayList<String>> rows = new ArrayList<>();
            while (rslt.next())// && n++ <= 1)
            {
                ArrayList<String> row = new ArrayList<>();
                for (n = 0; n < columns.size(); n++)
                {
                    String index = columns.get(n);
                    row.add(rslt.getString(rslt.findColumn(index)));
                }
                rows.add(row);
            }
            for (ArrayList<String> row : rows)
            {
                try
                {
                    String sql1 = "INSERT INTO";
                    String vals = "";
                    for (n = 0; n < row.size(); n++)
                    {
                        vals += "?";
                        if (n < row.size() - 1)
                        {
                            vals += ", ";
                        }
                    }
                    String sql2 = tableTo + " (" + colmns + ") VALUES (" + vals + ")";
                    PreparedStatement statement = conn.prepareStatement(sql1 + " " + sql2);
                    statement.setEscapeProcessing(true);
                    for (int i = 1; i <= row.size(); i++)
                    {
                        statement.setString(i, row.get(i - 1));
                    }
                    statement.execute();
                }
                catch (Exception e)
                {
                    System.out.println(e + " " + row);
                    e.printStackTrace();
                }
            }
        }
        else
        {
            rslt = stmt.executeQuery("SELECT * FROM " + tableFrom);
            // System.out.println("Updating from " + tableFrom);
            while (rslt.next())
            {
                ArrayList<String> row = new ArrayList<>();
                for (n = 0; n < columns.size(); n++)
                {
                    String index = columns.get(n);
                    row.add(rslt.getString(rslt.findColumn(index)));
                }
                String name = rslt.getString(rslt.findColumn("NAME"));
                String sql1 = "UPDATE";
                String vals = "";
                for (n = 0; n < row.size(); n++)
                {
                    String index = columns.get(n);
                    vals += convertName(index) + "=" + "?";
                    if (n < row.size() - 1)
                    {
                        vals += ", ";
                    }
                }
                String sql2 = tableTo + " SET " + vals + " WHERE NAME=?";
                PreparedStatement statement = conn.prepareStatement(sql1 + " " + sql2);
                statement.setEscapeProcessing(true);
                for (int i = 1; i <= row.size(); i++)
                {
                    statement.setString(i, row.get(i - 1));
                }
                statement.setString(row.size() + 1, name);
                statement.execute();
            }
        }

        columnsTo.clear();
        rslt = stmt.executeQuery("SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='" + tableTo + "'");
        while (rslt.next())
        {
            columnsTo.add(rslt.getString(4));
        }
    }

    private static String convertName(String string)
    {
        String ret = "";
        String name = string.trim().toLowerCase().replaceAll("[^\\w\\s ]", "");
        String[] args = name.split(" ");
        for (int i = 0; i < args.length; i++)
        {
            ret += args[i];
        }
        return ret.toUpperCase();
    }

    private static ArrayList<ArrayList<String>> getRows(File file) throws FileNotFoundException
    {
        InputStream res = new FileInputStream(file);

        ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        try
        {

            br = new BufferedReader(new InputStreamReader(res));
            int n = 0;
            while ((line = br.readLine()) != null)
            {
                String[] row = line.split(cvsSplitBy);
                rows.add(new ArrayList<String>());
                for (int i = 0; i < row.length; i++)
                {
                    rows.get(n).add(row[i]);
                }
                n++;
            }

        }
        catch (FileNotFoundException e)
        {
            System.err.println("Missing a Database file " + file);
        }
        catch (NullPointerException e)
        {
            try
            {
                FileReader temp = new FileReader(file);
                br = new BufferedReader(temp);
                int n = 0;
                while ((line = br.readLine()) != null)
                {
                    String[] row = line.split(cvsSplitBy);
                    rows.add(new ArrayList<String>());
                    for (int i = 0; i < row.length; i++)
                    {
                        rows.get(n).add(row[i]);
                    }
                    n++;
                }
            }
            catch (IOException e1)
            {
                e1.printStackTrace();
            }

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (br != null)
            {
                try
                {
                    br.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        return rows;
    }

    public static class XmlFormatter
    {

        public XmlFormatter()
        {
        }

        public String format(String unformattedXml)
        {
            try
            {
                final Document document = parseXmlFile(unformattedXml);

                OutputFormat format = new OutputFormat(document);
                format.setLineWidth(65);
                format.setIndenting(true);
                format.setIndent(4);
                Writer out = new StringWriter();
                XMLSerializer serializer = new XMLSerializer(out, format);
                serializer.serialize(document);

                return out.toString();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        private Document parseXmlFile(String in)
        {
            try
            {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                InputSource is = new InputSource(new StringReader(in));
                return db.parse(is);
            }
            catch (ParserConfigurationException e)
            {
                throw new RuntimeException(e);
            }
            catch (SAXException e)
            {
                throw new RuntimeException(e);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public static class PokemobData
    {
        final int                             number;
        final String                          name;
        final ArrayList<ArrayList<String>>    csv;
        final Collection<String>              moves;
        final Map<Integer, ArrayList<String>> lvlup;

        public PokemobData(int number, String name, ArrayList<ArrayList<String>> csv, Collection<String> moves,
                Map<Integer, ArrayList<String>> lvlup)
        {
            this.number = number;
            this.name = name;
            this.csv = csv;
            this.moves = moves;
            this.lvlup = lvlup;
        }

    }
}
