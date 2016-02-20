package pokecube.database;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class StatsWriter
{
    static void writeStats(int[] stats, int[] evs, float[] size, Document doc, Element statsNode)
    {
        boolean hasEV = false;
        for (int i = 0; i < 6; i++)
            if (evs[i] != 0) hasEV = true;
        boolean hasStats = false;
        for (int i = 0; i < 6; i++)
            if (evs[i] != 0) hasStats = true;
        boolean hasSizes = false;
        for (int i = 0; i < 3; i++)
            if (size[i] > 0) hasSizes = true;
        Attr attr;
        if (hasStats)
        {
            Element statsEl = doc.createElement("BASESTATS");
            statsNode.appendChild(statsEl);
            attr = doc.createAttribute("hp");
            attr.setValue("" + stats[0]);
            statsEl.setAttributeNode(attr);
            attr = doc.createAttribute("atk");
            attr.setValue("" + stats[1]);
            statsEl.setAttributeNode(attr);
            attr = doc.createAttribute("def");
            attr.setValue("" + stats[2]);
            statsEl.setAttributeNode(attr);
            attr = doc.createAttribute("spatk");
            attr.setValue("" + stats[3]);
            statsEl.setAttributeNode(attr);
            attr = doc.createAttribute("spdef");
            attr.setValue("" + stats[4]);
            statsEl.setAttributeNode(attr);
            attr = doc.createAttribute("spd");
            attr.setValue("" + stats[5]);
            statsEl.setAttributeNode(attr);
        }
        if (hasEV)
        {
            Element evEl = doc.createElement("EVYIELD");
            statsNode.appendChild(evEl);
            attr = doc.createAttribute("hp");
            attr.setValue("" + evs[0]);
            evEl.setAttributeNode(attr);
            attr = doc.createAttribute("atk");
            attr.setValue("" + evs[1]);
            evEl.setAttributeNode(attr);
            attr = doc.createAttribute("def");
            attr.setValue("" + evs[2]);
            evEl.setAttributeNode(attr);
            attr = doc.createAttribute("spatk");
            attr.setValue("" + evs[3]);
            evEl.setAttributeNode(attr);
            attr = doc.createAttribute("spdef");
            attr.setValue("" + evs[4]);
            evEl.setAttributeNode(attr);
            attr = doc.createAttribute("spd");
            attr.setValue("" + evs[5]);
            evEl.setAttributeNode(attr);
        }
        if (hasSizes)
        {
            Element sizeEl = doc.createElement("SIZES");
            statsNode.appendChild(sizeEl);
            if (size[0] > 0)
            {
                attr = doc.createAttribute("width");
                attr.setValue("" + size[0]);
                sizeEl.setAttributeNode(attr);
                attr = doc.createAttribute("length");
                attr.setValue("" + size[2]);
                sizeEl.setAttributeNode(attr);
            }
            if (size[1] > 0)
            {
                attr = doc.createAttribute("height");
                attr.setValue("" + size[1]);
                sizeEl.setAttributeNode(attr);
            }
        }
    }

    static boolean handleNode(String field, String value, String[] types, int[] evs, int[] stats, float[] size)
    {
        if (field.equals("HP"))
        {
            stats[0] = Integer.parseInt(value);
            return true;
        }
        else if (field.equals("ATK"))
        {
            stats[1] = Integer.parseInt(value);
            return true;
        }
        else if (field.equals("DEF"))
        {
            stats[2] = Integer.parseInt(value);
            return true;
        }
        else if (field.equals("SPATK"))
        {
            stats[3] = Integer.parseInt(value);
            return true;
        }
        else if (field.equals("SPDEF"))
        {
            stats[4] = Integer.parseInt(value);
            return true;
        }
        else if (field.equals("SPD"))
        {
            stats[5] = Integer.parseInt(value);
            return true;
        }
        else if (field.equals("HPEV"))
        {
            evs[0] = Integer.parseInt(value);
            return true;
        }
        else if (field.equals("ATKEV"))
        {
            evs[1] = Integer.parseInt(value);
            return true;
        }
        else if (field.equals("DEFEV"))
        {
            evs[2] = Integer.parseInt(value);
            return true;
        }
        else if (field.equals("SPATKEV"))
        {
            evs[3] = Integer.parseInt(value);
            return true;
        }
        else if (field.equals("SPDEFEV"))
        {
            evs[4] = Integer.parseInt(value);
            return true;
        }
        else if (field.equals("SPDEV"))
        {
            evs[5] = Integer.parseInt(value);
            return true;
        }
        else if (field.equals("TYPE1"))
        {
            types[0] = value;
            return true;
        }
        else if (field.equals("TYPE2"))
        {
            types[1] = value;
            return true;
        }
        else if (field.equals("HEIGHT"))
        {
            size[1] = Float.parseFloat(value);
            return true;
        }
        else if (field.equals("WIDTHLENGHT"))
        {
            String[] args = value.split(":");
            size[0] = Float.parseFloat(args[0]);
            if (args.length == 2) size[2] = Float.parseFloat(args[1]);
            else size[2] = size[0];
            return true;
        }
        return false;
    }
}
