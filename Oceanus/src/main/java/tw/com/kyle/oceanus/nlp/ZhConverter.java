package tw.com.kyle.oceanus.nlp;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Sean on 2014/4/23.
 */

public class ZhConverter {

    private List<Map<String, String>> zhList = new ArrayList<>();    
    private Pattern TxtRule_Pat = Pattern.compile("\\*?(.*)=>([^;,/]*)(.*)");
    private Pattern PhpRule_Pat = Pattern.compile("'(.*)' => '([^;,/]*)'(.*)");
    private CGroups cg = new CGroups();
    private static ZhConverter inst = null;
    
    public static ZhConverter GetInstance() {
        if(inst == null) {
            inst = new ZhConverter();
        }
        
        return inst;
    }
    
    private ZhConverter(){
        // load_convertTable("/resources/wikipedia/conversionTable_zhtw.txt");
        // load_convertTable("/resources/wikipedia/conversionTable_zh-hant.txt");
        // load_ZhConversion("/resources/wikipedia/ZhConversion.php.txt");
        // load_convertTable("/resources/wikipedia/conversionTable_custom.txt");
        load_unicode_variant("Unihan_Variants.txt");;
        System.out.println("ZhConverter tables are loaded.");

        cg.FromCGroupXml();
        loadAllCGroup();
        
        System.out.printf("Default conversion table: %d entires %n", 
                zhList.stream().map((x)->x.size()).reduce(0, (a,b)->a+b));

    }

    public ZCResult Convert(StringBuilder sb){

        ZCResult zcr = convert_with(sb, zhList);

        return zcr;
    }
    
    public String MapBack(int pos, int win, ZCResult zcr) {
        int new_starti = pos;
        int new_endi = pos + win - 1;
        
        //! determine the index group corresponding to [pos, pos+win)
        int g_start_concur = -1;
        int g_end_concur = -1;
        for(int i = 0; i < zcr.new_idx.size(); ++i){
            List<Integer> idx_grp = zcr.new_idx.get(i);
            if(idx_grp.get(0) == new_starti) g_start_concur = i;
            if(idx_grp.get(idx_grp.size() - 1) == new_endi) g_end_concur = i;
        }
                
        if (g_start_concur >= 0 && g_end_concur >= 0){
            //! Case 1: word boundary concur with conversion boundary
            int ori_str_idx_start = zcr.ori_idx.get(g_start_concur).get(0);
            List<Integer> grp_end = zcr.ori_idx.get(g_end_concur);
            int ori_str_idx_end = grp_end.get(grp_end.size() - 1);
            return zcr.ori_str.substring(ori_str_idx_start, ori_str_idx_end + 1);
        } else {
            //! Case 2: word boundary does not concur with conversion boundary
            
            return null;
        }
                        
    }        
    
    private ZCResult convert_with(StringBuilder sb, List<Map<String, String>> _zhlist){
        String ori_str = sb.toString();
        int maxMapLen = Math.min(_zhlist.size(), 1);
        List<Integer> oristr_idx = new ArrayList<>();
        List<List<Integer>> map_ori = new ArrayList<>();
        List<String> map_new_buf = new ArrayList<>();
        
        for(int i = 0; i < sb.length(); ++i){
            oristr_idx.add(i);
        }
        
        for(int i=maxMapLen-1; i>=0; i--){
            Map<String, String> m = _zhlist.get(i);
            int win = i+1;
            int pos = 0;
            while(pos<sb.length()-i){
                String probe = sb.substring(pos, pos+win);
                String conv_str = null;
                if (m.containsKey(probe))
                    conv_str = m.get(probe);
                else if (i == 0) {
                    conv_str = probe;
                }
                
                if(conv_str != null){                    
                    sb.delete(pos, pos+win);
                    int ori_idx = oristr_idx.get(pos);
                    remove_list_range(oristr_idx, pos, win);
                    map_new_buf.add(conv_str);
                    map_ori.add(generate_idx(ori_idx, win));                    
                }
            }
        }
        
        /*
        //! handle those character doesn't need SC conversion
        while(sb.length() > 0){
            String ch = sb.substring(0, 1);
            sb.delete(0, 1);
            int ori_idx = oristr_idx.get(0);
            remove_list_range(oristr_idx, 0, 1);
            map_new_buf.add(ch);
            map_ori.add(generate_idx(ori_idx, 1)); 
        }
        */
        
        //! sort map_ori by the first index of each element
        List<Integer> map_ori_arg = generate_idx(0, map_ori.size());
        map_ori_arg.sort((x, y)->map_ori.get(x).get(0) - map_ori.get(y).get(0));
        List<List<Integer>> ord_map_ori = new ArrayList<>();
        List<List<Integer>> ord_map_new = new ArrayList<>();
        StringBuilder sb_conv = new StringBuilder();
        
        for(int arg: map_ori_arg){
            ord_map_ori.add(map_ori.get(arg));
            ord_map_new.add(generate_idx(sb_conv.length(), map_new_buf.get(arg).length()));
            sb_conv.append(map_new_buf.get(arg));
        }
        
        ZCResult zcr = new ZCResult();
        zcr.ori_str = ori_str;
        zcr.conv_str = sb_conv.toString();
        zcr.ori_idx = ord_map_ori;
        zcr.new_idx = ord_map_new;
        return zcr;
    }
    
    private void remove_list_range(List<?> inlist, int pos, int win) {
        for(int i = 0; i < win; ++i){
            inlist.remove(pos);
        }
    }

    private List<Integer> generate_idx(int pos, int win){
        List<Integer> idxlist = new ArrayList<>();
        for(int i = 0; i < win; ++i){
            idxlist.add(pos + i);
        }
        
        return idxlist;
    }    
    
    public ZCResult Convert(String text){
        StringBuilder sb = new StringBuilder(text);
        return Convert(sb);
    }

    public void ExportTable(){
        try {
            OutputStreamWriter ow = new OutputStreamWriter(
                    new FileOutputStream("res/table.txt"), "UTF-8");
            for(int i=0; i<zhList.size(); ++i){
                Map<String, String> m = zhList.get(i);
                for(Map.Entry<String, String> kvp: m.entrySet()){
                    String ss = String.format("[%d]%s: %s", i, kvp.getKey(), kvp.getValue());
                    ow.write(ss + "\r\n");
                }
            }

            ow.close();
        } catch (IOException ex){

        }
    }
    private void resizeList(List<Map<String, String>> _list, int _size){
        while(_list.size() < _size) {
            _list.add(new HashMap<>());
        }
    }

    private void load_unicode_variant(String fpath) {
        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(fpath);
            BufferedReader isr = new BufferedReader(
                             new InputStreamReader(is, "UTF-8"));            
            String ln = null;
            
            resizeList(zhList, 1);
            Pattern u_pat = Pattern.compile("U\\+([0-9A-F]+)");
            Map<String, String> zh_map = zhList.get(0);
            while ((ln = isr.readLine()) != null) {
                if (ln.startsWith("#")) continue;
                if (!ln.contains("kSimplifiedVariant")) continue;
                                
                String[] parts = ln.split("\t");
                String unicp = parts[0].replace("U+", "");
                String valcp = parts[2];
                Matcher m = u_pat.matcher(valcp);
                ArrayList<String> valcp_list = new ArrayList<>();
                while (m.find()) {
                    String valcp_x = m.group(1);
                    if (valcp_x.length() > 4) {
                        Logger.getLogger(ZhConverter.class.getName())
                            .log(Level.WARNING, "Ignore codepoint not in BMP");
                        continue;                        
                    }
                    valcp_list.add(m.group(1));                    
                }
                
                if (valcp_list.size() > 1) {
                    Logger.getLogger(ZhConverter.class.getName())
                            .log(Level.WARNING, "More than one mapping: {0} => {1}, {2} used", 
                                    new Object[] {unicp, valcp_list, valcp_list.get(0)});
                } else if (valcp_list.size() < 1) {
                    Logger.getLogger(ZhConverter.class.getName())
                            .log(Level.WARNING, "No mapping found");
                    continue;
                }
                
                int code = Integer.parseInt(unicp, 16);
                String unistr = new String(Character.toChars(code));
                code = Integer.parseInt(valcp_list.get(0), 16);
                String valstr = new String(Character.toChars(code));
                // System.out.printf("%s => %s %n", unistr, valstr);
                zh_map.put(unistr, valstr);
                
            }
        } catch (IOException ex) {
            
        }
        
    }        
    
    private void load_convertTable(String fpath) {
        try {
            InputStream is = this.getClass().getResourceAsStream(fpath);
            BufferedReader isr = new BufferedReader(
                                    new InputStreamReader(is, "UTF-8"));            
            String ln = null;
            while((ln = isr.readLine()) != null){
                //ln.indexOf("")

                Matcher m = TxtRule_Pat.matcher(ln);
                while(m.find()){
                    String zh_hans = m.group(1);
                    String zh_hant = m.group(2);
                    String comment = m.group(3);
                    resizeList(zhList, zh_hans.length());
                    Map<String, String> zhmap = zhList.get(zh_hans.length()-1);
                    zhmap.put(zh_hans, zh_hant);
                }
            }
        } catch (IOException ex){

        }
    }

    private void load_ZhConversion(String fpath) {
        try {
            boolean inTargetRange = true;
            InputStream is = this.getClass().getResourceAsStream(fpath);
            BufferedReader isr = new BufferedReader(
                                    new InputStreamReader(is, "UTF-8"));            
            String ln = null;            
            while((ln = isr.readLine()) != null){
                if(ln.contains("$zh2Hant")){
                    inTargetRange = true;
                } else if(ln.contains("$zh2TW")){
                    inTargetRange = true;
                } else if(ln.contains("$zh2")){
                    inTargetRange = false;
                }

                if(!inTargetRange){continue;}
                Matcher m = PhpRule_Pat.matcher(ln);

                while(m.find()){
                    String zh_hans = m.group(1);
                    String zh_hant = m.group(2);
                    String comment = m.group(3);
                    resizeList(zhList, zh_hans.length());
                    int wlen = zh_hant.length();
                    Map<String, String> zhmap = zhList.get(wlen-1);
                    zhmap.put(zh_hant, zh_hans);
                }
            }
        } catch (IOException ex){
            System.out.println(ex.toString());
        }
    }
    
    private void loadAllCGroup() {
        cg.GetCGGroupNames().forEach((gname) -> {
            AddCGroup(gname);
        });
    }
        
    
    public int AddCGroup(String group_name) {
        Map<String, String> cmap = cg.GetCGroup(group_name);
        int counter = 0;
        if(cmap!=null){
            for(Map.Entry<String, String> kvp:cmap.entrySet()){
                String zh_tw = kvp.getValue();
                String zh_cn = kvp.getKey();
                //! skip all mapping entries with only single-character in zh_tw
                if (zh_tw.length() == 1) continue;
                
                resizeList(zhList, zh_tw.length());
                Map<String, String> zhmap = zhList.get(zh_tw.length()-1);
                zhmap.put(zh_tw, zh_cn);
                counter += 1;
            }

            return counter;
        } else {
            return 0;
        }
    }

    public int AddRule(String zh_cn, String zh_tw) {

        if(zh_cn.length()==0 || zh_tw.length()==0) return 0;

        resizeList(zhList, zh_cn.length());
        Map<String, String> zhmap = zhList.get(zh_cn.length()-1);
        if(!zhmap.containsKey(zh_cn)) {
            zhmap.put(zh_cn, zh_tw);
            return 1;
        }

        return 0;
    }
}
