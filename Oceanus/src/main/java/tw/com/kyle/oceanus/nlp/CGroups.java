package tw.com.kyle.oceanus.nlp;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Sean on 2014/4/29.
 */
public class CGroups {
    private class Zh_STMap extends HashMap<String, String>{
        private Set<String> references = new HashSet<>();
        public void AddReference(String ref){
            references.add(ref);
        }

        public Set<String> GetReferences(){
            return references;
        }
    }
    private String fpath;
    private Pattern citem_pat = Pattern.compile("\\{\\{CItem(Hidden|Lan)?\\|(.*)\\}\\}");
    private Pattern cgroup_pat = Pattern.compile("\\{\\{CGroup/(.*?)\\}\\}");
    private Pattern cnitem_pat = Pattern.compile("zh-cn:([^;|]*)");
    private Pattern twitem_pat = Pattern.compile("zh-tw:([^;|]*)");
    private Pattern hansitem_pat = Pattern.compile("zh-hans:([^;|]*)");
    private Pattern hantitem_pat = Pattern.compile("zh-hant:([^;|]*)");
    private Pattern gname_pat = Pattern.compile("\\{\\{CGroupH\\|name=(.*?)\\|desc=(.*?)\\}\\}");
    private Pattern redir_pat = Pattern.compile("#(重定向|REDIRECT)\\s?\\[\\[(.*)?\\]\\]");
    private Map<String, Zh_STMap> zh_map = new HashMap<>();
    public CGroups(){
        fpath = "cgroups.xml";
    }

    public void clear(){ zh_map = null; zh_map = new HashMap<>(); }
    public int GetCGroupCount(){
        return zh_map.size();
    }

    public int GetSTMapCount(){
        return zh_map.values().stream().map((x)->x.size()).reduce(0, (a,b)->a+b);
    }
    
    public Set<String> GetCGGroupNames(){
        return zh_map.keySet();
    }                

    public Map<String, String> GetCGroup(String group_name){
        if(zh_map.containsKey(group_name)){
            Zh_STMap stmap = zh_map.get(group_name);
            Set<String> refs = stmap.GetReferences();
            if(refs.size()>0){
                for(String ref: refs) {
                    Map<String, String> stmap_r = this.GetCGroup(ref);
                    if(stmap_r!=null) {
                        stmap.putAll(stmap_r);
                    }
                }
            }

            return stmap;
        } else {
            return null;
        }
    }

    public void FromWikitext(String rawWikiText, String wikiTitle) throws CGroupFormatException{

        Zh_STMap stmap_x = new Zh_STMap();
        String group_name = "";
        String group_desc = "";

        for(String ln: rawWikiText.split("\n")) {

            if(ln.contains("#重定向") || ln.contains("#REDIRECT")) {
                Matcher m_redir = redir_pat.matcher(ln);
                if (m_redir.find()) {
                    String redir_ref = m_redir.group(2);
                    stmap_x.AddReference(redir_ref.replace("Template:CGroup/",""));
                }
                break;
            }


            if(ln.startsWith("\uFEFF")){ln = ln.substring(1);}
            if(!ln.startsWith("{{")) {
                continue;
            } else if(ln.startsWith("{{CGroupH")){
                Matcher m_gname = gname_pat.matcher(ln);
                if(m_gname.find()){
                    group_name = m_gname.group(1);
                    group_desc = m_gname.group(2);
                } else {
                    throw new CGroupFormatException();
                }

            } else if (ln.startsWith("{{CItem")) {
                Matcher m_citem = citem_pat.matcher(ln.trim());
                if (m_citem.find()) {
                    String item_text = m_citem.group(2);

                    // Try parse zh_tw and zh_cn first
                    String zh_cn = extract_item_text(cnitem_pat, item_text);
                    String zh_tw = extract_item_text(twitem_pat, item_text);

                    // If either of above is empty, try hans/hant
                    if(zh_cn.length()==0) {
                        zh_cn = extract_item_text(hansitem_pat, item_text);
                    }

                    if(zh_tw.length()==0) {
                        zh_tw = extract_item_text(hantitem_pat, item_text);
                    }

                    if(zh_tw.length()>0 && zh_cn.length() > 0 && !stmap_x.containsKey(zh_cn)) {
                        stmap_x.put(zh_cn, zh_tw);
                    }
                }
                Matcher m_cgroup = cgroup_pat.matcher(ln.trim());
                if (m_cgroup.find()) {
                    stmap_x.AddReference(m_cgroup.group(1));
                }
            }
        }

        if(group_name.length()==0){
            group_name = wikiTitle.replace("Template:CGroup/", "");
        }

        if(!zh_map.containsKey(group_name) &&
                (stmap_x.size()>0 || stmap_x.GetReferences().size()>0)) {
            zh_map.put(group_name, stmap_x);
        }

    }

    public void FromCGroupXml(){FromCGroupXml(fpath);}
    public void FromCGroupXml(String cg_path){
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();            
            Document doc = docBuilder.parse(this.getClass().getClassLoader().getResourceAsStream(cg_path));

            Element root = doc.getDocumentElement();
            NodeList cgroupList = root.getChildNodes();
            for(int i=0; i<cgroupList.getLength(); i++){
                if(cgroupList.item(i).getNodeType() != Node.ELEMENT_NODE){
                    continue;
                }
                Element cgroup = (Element)cgroupList.item(i);
                String group_name = cgroup.getAttribute("name");
                NodeList mapList = cgroup.getChildNodes();
                Zh_STMap stmap_x = new Zh_STMap();
                for(int j=0; j<mapList.getLength(); j++){
                    if(mapList.item(j).getNodeType() != Node.ELEMENT_NODE){
                        continue;
                    }

                    Element group_sub = (Element) mapList.item(j);

                    if(group_sub.getTagName().equalsIgnoreCase("map")) {
                        NodeList mapEntries = group_sub.getChildNodes();
                        String zh_tw = "";
                        String zh_cn = "";
                        for (int k = 0; k < mapEntries.getLength(); k++) {
                            if (mapEntries.item(k).getNodeType() != Node.ELEMENT_NODE) {
                                continue;
                            }
                            Element ent = (Element) mapEntries.item(k);
                            if (ent.getTagName().endsWith("tw")) {
                                zh_tw = ent.getTextContent();
                            } else if (ent.getTagName().endsWith("cn")) {
                                zh_cn = ent.getTextContent();
                            }
                        }

                        if(zh_tw.length()>0 && zh_cn.length() > 0 && !stmap_x.containsKey(zh_cn)) {
                            stmap_x.put(zh_cn, zh_tw);
                        }
                    } else if(group_sub.getTagName().equalsIgnoreCase("ref")){
                        stmap_x.AddReference(group_sub.getTextContent());
                    }

                }

                if(!zh_map.containsKey(group_name)) {
                    zh_map.put(group_name, stmap_x);
                }
            }

        } catch (Exception ex) {
            System.out.println(ex.toString());
            ex.printStackTrace();
        }
    }

    public void WriteToFile() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element root = doc.createElement("sc_table");
            doc.appendChild(root);

            for(Entry<String, Zh_STMap> kvp:zh_map.entrySet()){
                Element cgroup = doc.createElement("cgroup");
                cgroup.setAttribute("name", kvp.getKey());
                Zh_STMap stmap_x = kvp.getValue();
                for(Entry<String, String> st_map: stmap_x.entrySet()) {
                    Element map = doc.createElement("map");
                    Element tw = doc.createElement("zh_tw");
                    Element cn = doc.createElement("zh_cn");
                    tw.setTextContent(st_map.getValue());
                    cn.setTextContent(st_map.getKey());
                    map.appendChild(tw);
                    map.appendChild(cn);
                    cgroup.appendChild(map);
                }

                for(String ref: stmap_x.GetReferences()){
                    Element ref_elem = doc.createElement("ref");
                    ref_elem.setTextContent(ref);
                    cgroup.appendChild(ref_elem);
                }
                root.appendChild(cgroup);
            }



            StreamResult fout = new StreamResult(new File(fpath));
            TransformerFactory trFactory = TransformerFactory.newInstance();
            Transformer trans = trFactory.newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            trans.transform(new DOMSource(doc), fout);

        } catch(Exception ex){
            System.out.println(ex.toString());
        }

    }

    private String extract_item_text(Pattern pat, String item_text){
        String zh_txt = "";

        Matcher m_twitem = pat.matcher(item_text);

        if(m_twitem.find()){
            zh_txt = m_twitem.group(1);
        }

        return zh_txt;
    }

    public class CGroupFormatException extends Exception{}
}
