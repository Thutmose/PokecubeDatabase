package pokecube.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pokecube.database.DatabaseConverter.PokemobData;
import pokecube.database.DatabaseConverter.XmlFormatter;

public class XMLWriter
{
    static void addPokemobXML(Document doc, Element rootElement, PokemobData data)
    {
        Element pokemon = doc.createElement("Pokemon");
        Attr attr;
        rootElement.appendChild(pokemon);
        Element statsNode = doc.createElement("STATS");
        pokemon.appendChild(statsNode);

        ArrayList<String> fields = data.csv.get(0);
        ArrayList<String> values = data.csv.get(1);

        int[] evs = new int[6];
        int[] stats = new int[6];
        float[] size = { -1, -1, -1 };
        String[] types = { "", "" };
        String ability = "";
        String hiddenAbility = "";
        for (int i = 0; i < fields.size(); i++)
        {
            if (i >= values.size()) break;
            String field = fields.get(i).replace("\"", "");
            String value = values.get(i).replace("\"", "");

            try
            {
                if (StatsWriter.handleNode(field, value, types, evs, stats, size))
                {

                }
                else if (field.contains("LITY") || field.contains("ABILITIY"))
                {
                    if (field.contains("3"))
                    {
                        hiddenAbility += value;
                    }
                    else if (!value.trim().isEmpty())
                    {
                        ability += value + ", ";
                    }
                }
                else if (field.equals("NAME"))
                {
                    attr = doc.createAttribute("name");
                    attr.setValue(value);
                    pokemon.setAttributeNode(attr);
                }
                else if (field.equals("NUMBER"))
                {
                    attr = doc.createAttribute("number");
                    attr.setValue(value);
                    pokemon.setAttributeNode(attr);
                }
                else if (field.equals("SPECIESFOODSPECIES"))
                {
                    String[] vals = value.split(":");
                    if (!vals[0].isEmpty())
                    {
                        Element stat = doc.createElement("SPECIES");
                        stat.appendChild(doc.createTextNode(vals[0]));
                        statsNode.appendChild(stat);
                        if (vals.length > 1)
                        {
                            stat = doc.createElement("PREY");
                            stat.appendChild(doc.createTextNode(vals[1]));
                            statsNode.appendChild(stat);
                        }
                    }
                }
                else if (field.equals("LOGICSHOULDERFLYDIVEDYEDEFAULTCOLOURSTATIONARY"))
                {
                    String[] vals = value.split(":");
                    Element logic = doc.createElement("LOGIC");
                    statsNode.appendChild(logic);
                    for (int i1 = 0; i1 < vals.length; i1++)
                    {
                        String attrib = "";
                        if (i1 == 0) attrib = "shoulder";
                        if (i1 == 1) attrib = "fly";
                        if (i1 == 2) attrib = "dive";
                        if (i1 == 3) attrib = "dye";
                        if (i1 == 4) attrib = "stationary";

                        if (!vals[i1].trim().isEmpty())
                        {
                            attr = doc.createAttribute(attrib);
                            attr.setValue(vals[i1].trim());
                            logic.setAttributeNode(attr);
                        }
                    }
                }
                else if (!field.equals("UNUSED") && !value.isEmpty())
                {
                    Element stat = doc.createElement(field);
                    stat.appendChild(doc.createTextNode(value));
                    statsNode.appendChild(stat);
                }
            }
            catch (NumberFormatException e)
            {
            }
            catch (DOMException e)
            {
                e.printStackTrace();
            }
        }
        StatsWriter.writeStats(stats, evs, size, doc, statsNode);
        if (!types[0].isEmpty())
        {
            Element typesEl = doc.createElement("TYPE");
            statsNode.appendChild(typesEl);
            attr = doc.createAttribute("type1");
            attr.setValue(types[0]);
            typesEl.setAttributeNode(attr);
            if (!types[1].isEmpty())
            {
                attr = doc.createAttribute("type2");
                attr.setValue(types[1]);
                typesEl.setAttributeNode(attr);
            }
        }
        if (!ability.isEmpty() || !hiddenAbility.isEmpty())
        {
            Element abilityEl = doc.createElement("ABILITY");
            statsNode.appendChild(abilityEl);
            if (!ability.isEmpty())
            {
                if (ability.endsWith(", "))
                {
                    ability = ability.trim().substring(0, ability.length() - 2);
                }

                attr = doc.createAttribute("normal");
                attr.setValue(ability);
                abilityEl.setAttributeNode(attr);
            }
            if (!hiddenAbility.isEmpty())
            {
                attr = doc.createAttribute("hidden");
                attr.setValue(hiddenAbility);
                abilityEl.setAttributeNode(attr);
            }
        }

        Element moveStats = doc.createElement("MOVES");
        if (!data.lvlup.isEmpty() || !data.moves.isEmpty()) pokemon.appendChild(moveStats);

        if (!data.lvlup.isEmpty())
        {
            Element levelMoves = doc.createElement("LVLUP");
            moveStats.appendChild(levelMoves);
            ArrayList<Integer> levels = new ArrayList<>(data.lvlup.keySet());
            Collections.sort(levels);
            for (Integer i : levels)
            {
                attr = doc.createAttribute("lvl_" + i);
                attr.setValue(data.lvlup.get(i).toString().replace("[", "").replace("]", ""));
                levelMoves.setAttributeNode(attr);
            }
        }
        if (!data.moves.isEmpty())
        {
            Element moves = doc.createElement("MISC");
            moveStats.appendChild(moves);
            attr = doc.createAttribute("moves");
            attr.setValue(data.moves.toString().replace("[", "").replace("]", ""));
            moves.setAttributeNode(attr);
        }
    }

    static void writeXML(File file, ArrayList<PokemobData> dataset) throws IOException
    {
        try
        {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("Document");
            doc.appendChild(rootElement);

            for (PokemobData data : dataset)
            {
                addPokemobXML(doc, rootElement, data);
                numberdone++;
                if (numberdone % 50 == 0) System.out.println();
                System.out.print(".");
            }

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);

            // Output to console for testing
            transformer.transform(source, result);

            try
            {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();

                reader.close();
                line = new XmlFormatter().format(line);
                FileWriter writer;
                try
                {
                    writer = new FileWriter(file);
                    writer.write(line);
                    writer.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            // System.out.println("File saved!");

        }
        catch (ParserConfigurationException pce)
        {
            pce.printStackTrace();
        }
        catch (TransformerException tfe)
        {
            tfe.printStackTrace();
        }
    }

    static int numberdone = 0;
}
