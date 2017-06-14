/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tw.com.kyle.oceanus.nlp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.stanford.nlp.coref.data.Mention;
import edu.stanford.nlp.trees.GrammaticalStructure;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Sean_S325
 */
public class GsonConverter {
    public static JsonElement FromParsedDocument(ParsedDocument pdoc) {
        JsonObject j_obj = new JsonObject();
        JsonArray j_arr = new JsonArray();
        
        for (ParsedSentence psent: pdoc.GetParsedSentences()) {
            j_arr.add(FromParsedSentence(psent));
        }
        j_obj.add("sentences", j_arr);
        j_obj.add("coref", pdoc.GetCorefObjects());
        
        return j_obj;        
    }
    
    public static JsonElement FromParsedSentence(ParsedSentence psent) {
        JsonObject jobj = new JsonObject();
        JsonArray jarr = new JsonArray();
        jobj.add("words", toJsonStringArray(psent.Words()));
        jobj.add("pos", toJsonStringArray(psent.POS()));
        jobj.add("ner", toJsonStringArray(psent.NER()));
        jobj.add("chstart", toJsonIntArray(psent.StartOffsets()));
        jobj.add("chend", toJsonIntArray(psent.EndOffsets()));
        jobj.addProperty("tree", psent.ConstituentTree().replaceAll("\r\n[^(]*", ""));
        jobj.add("dep", toJsonStringArray(psent.BasicDep()));
        jobj.add("entity", toJsonStringArray(psent.WikipediaEntities()));
        //! start from StanfordCoreNLP 3.7.0, it provided uu-dep by default.
        //! the distinction between basic and uudep became uncessary.
        jobj.add("udep", toJsonStringArray(psent.BasicDep()));
        
        jobj.add("mentions", psent.GetMentionObjects());
        return jobj;
    }
    
    public static List<UpdateAction> ListUpdateActionFromJson(String json_str) {
        JsonElement jelem = new JsonParser().parse(json_str);
        JsonArray jarr = jelem.getAsJsonArray();
        Gson gson = new Gson();
        List<UpdateAction> ac_list = new ArrayList<>();
        for(JsonElement el: jarr){
            UpdateAction uac = gson.fromJson(el, UpdateAction.class);
            ac_list.add(uac);
        }
        
        return ac_list;
    }
    
    private static JsonArray toJsonStringArray(List<String> sarr) {
        JsonArray ja = new JsonArray();        
        sarr.forEach((s) -> ja.add(s));
        
        return ja;
    }    
    
    private static JsonArray toJsonIntArray(List<Integer> sarr) {
        JsonArray ja = new JsonArray();
        sarr.forEach((s) -> ja.add(s));        
        
        return ja;
    }
    
    private static JsonArray toJsonStringArray(GrammaticalStructure gs) {
        JsonArray ja = new JsonArray();
        if (gs == null) return ja;
        
        gs.typedDependencies().forEach((dep)->{
            String dep_str = String.format("%s(%s-%d, %s-%d)", dep.reln(),                     
                    dep.gov().word(), dep.gov().index(),
                    dep.dep().word(), dep.dep().index());
            ja.add(dep_str);
        });
        
        return ja;
    }    
}
