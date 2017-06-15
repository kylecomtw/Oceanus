/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tw.com.kyle.oceanus.nlp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.Mention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.TypesafeMap.Key;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 *
 * @author Sean_S325
 */
public class ParsedSentence {
    
    private CoreMap annot = null;
    public ParsedSentence(CoreMap an){
        annot = an;
    }
    
    public int size() {
        return annot.get(CoreAnnotations.TokensAnnotation.class).size();
    }
    public Integer SentenceIndex() {
        return SentenceIndices().get(0);
    }
    
    public String Text() {
        return annot.get(CoreAnnotations.TextAnnotation.class);
    }
    
    public List<String> Words() { 
        return collect_token_data(CoreAnnotations.TextAnnotation.class);
    }
    
    public List<String> POS() {
        return collect_token_data(CoreAnnotations.PartOfSpeechAnnotation.class);
    }        
    
    public List<String> BasicDep() {
        SemanticGraph sgraph = annot.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
        if (sgraph != null) {
            return Arrays.asList(sgraph.toString(SemanticGraph.OutputFormat.LIST).split("\\n"));
        } else {
            return new ArrayList<>();
        }
    }
    
    public GrammaticalStructure UniversalDep() {
        GrammaticalStructure gs = annot.get(UniversalDependenciesAnnotation.class);
        return gs;
    }
    
    public List<String> NER() {
        return collect_token_data(CoreAnnotations.NamedEntityTagAnnotation.class);
    }
    
    public List<String> Lemma() {
        return collect_token_data(CoreAnnotations.LemmaAnnotation.class);
    }
        
    public List<String> WikipediaEntities() {
        return collect_token_data(CoreAnnotations.WikipediaEntityAnnotation.class);
    }
    
    public List<Integer> StartOffsets() {
        return collect_token_data(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
    }
    
    public List<Integer> EndOffsets() {
        return collect_token_data(CoreAnnotations.CharacterOffsetEndAnnotation.class);
    }
    
    public List<Integer> SentenceIndices() {
        return collect_token_data(CoreAnnotations.SentenceIndexAnnotation.class);
    }
    
    public List<Mention> Mentions() {
        Class corefMenClazz = CorefCoreAnnotations.CorefMentionsAnnotation.class;
        List<Mention> mentions = null;
        if (annot.containsKey(corefMenClazz)) {
            mentions = annot.get(CorefCoreAnnotations.CorefMentionsAnnotation.class);
        } else {
            mentions = new ArrayList<>();
        }
            
        return mentions;
    }
    
    public String ConstituentTree() { 
        Tree tree = GetTree();
        if (tree != null){
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            OutputStreamWriter bosw = new OutputStreamWriter(bos, Charset.forName("UTF-8"));
            PrintWriter pw = new PrintWriter(bosw);            
            tree.pennPrint(pw);
            
            return new String(bos.toByteArray(), Charset.forName("UTF-8"));
        } else {
            return "No Tree Representation Available";
        }
        
    }   
    
    public Tree GetTree(){
        Tree tree = annot.get(TreeCoreAnnotations.TreeAnnotation.class);
        return tree;
    }
    
    private <T> List<T> collect_token_data(Class<? extends Key<T>> annot_class) {
        if (annot.containsKey(CoreAnnotations.TokensAnnotation.class)){
            return annot.get(CoreAnnotations.TokensAnnotation.class).stream()
                .map((x)->x.get(annot_class))
                .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }
    
    private <T> void update_token_data(int word_i, Class<? extends Key<T>> annot_class, T val) {
        List<CoreLabel> tok_list = annot.get(CoreAnnotations.TokensAnnotation.class);
        for(int token_i = 0; token_i < tok_list.size(); ++token_i){
            if (tok_list.get(token_i).index() != word_i) continue;
            tok_list.get(token_i).set(annot_class, val);
            break;
        }                
    }
    
    /*
     * UseZhVersion(int mode); mode == 0: Zh-tw, mode == 1: Zh-cn
    */
    private void UseZhVersion(int mode) {        
        Class<? extends CoreAnnotation<String>> clz; 
        if (mode == 1) {
            clz = ZhAnnotation.ZhCnAnnotation.class;
        } else {
            clz = ZhAnnotation.ZhTwAnnotation.class;
        }
        List<CoreLabel> tok_list = annot.get(CoreAnnotations.TokensAnnotation.class);
        for(CoreLabel tok: tok_list){
            if (tok.containsKey(ZhAnnotation.ZhTwAnnotation.class)){
                String zh_ver = tok.get(clz);
                tok.setValue(zh_ver);
                tok.setWord(zh_ver);
            }
        }
    }
    
    public void UseZhTw() {
        int USE_ZH_TW = 0;
        UseZhVersion(USE_ZH_TW);
    }
    
    public void UseZhCn() {
        int USE_ZH_CN = 1;
        UseZhVersion(USE_ZH_CN);
    }
    
    public boolean RemoveWord(int word_index) {
        List<CoreLabel> lab_list = annot.get(CoreAnnotations.TokensAnnotation.class);
        boolean has_removed = false;
        for(CoreLabel lab: lab_list){
            if (lab.index() == word_index) {                
                lab_list.remove(lab);
                has_removed = true;
                break;
            }
        }
        
        if(has_removed) {
            reindex_word_annotations(lab_list);            
            return true;
        }
        
        return false;
    }
    
    public boolean AddWord(String word, String pos, String ner, int start, int end) {
        List<CoreLabel> lab_list = annot.get(CoreAnnotations.TokensAnnotation.class);
        boolean has_added = false;
        for(int i = 0; i < lab_list.size(); ++i) {
            CoreLabel lab = lab_list.get(i);
            if (lab.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class) > start) {
                lab_list.add(i, lab);
                has_added = true;
                break;
            }
        }
        
        if(has_added) {
            reindex_word_annotations(lab_list); 
            return true;
        }
        
        return false;
    }
    
    public void SetWord(int word_i, String word) { 
        update_token_data(word_i, CoreAnnotations.ValueAnnotation.class, word);
        update_token_data(word_i, CoreAnnotations.TextAnnotation.class, word);        
    }
    
    public void SetPOS(int word_i, String pos){
        update_token_data(word_i, CoreAnnotations.PartOfSpeechAnnotation.class, pos);
    }
    
    public void SetDep(int src_i, int targ_i, String relation) {
        
    }
    
    public void SetNER(int word_i, String ner) {
        update_token_data(word_i, CoreAnnotations.NamedEntityTagAnnotation.class, ner);
    }
    
    public void SetLemma(int word_i, String lemma) {
        update_token_data(word_i, CoreAnnotations.LemmaAnnotation.class, lemma);
    }
    
    public void SetStart(int word_i, int val) {
        update_token_data(word_i, CoreAnnotations.CharacterOffsetBeginAnnotation.class, val);
        int start = StartOffsets().get(word_i - 1);
        int end = EndOffsets().get(word_i - 1);
        SetWord(word_i, Text().substring(start, end));
    }
    
    public void SetEnd(int word_i, int val) {
        update_token_data(word_i, CoreAnnotations.CharacterOffsetEndAnnotation.class, val);
        int start = StartOffsets().get(word_i - 1);
        int end = EndOffsets().get(word_i - 1);
        SetWord(word_i, Text().substring(start, end));
    }
    
    public void SetSentence(int word_i, int val) {
        update_token_data(word_i, CoreAnnotations.SentenceIndexAnnotation.class, val);
    }
    
    public String toReprStr() {
        List<CoreLabel> tokens = annot.get(CoreAnnotations.TokensAnnotation.class);
        StringBuilder sb = new StringBuilder();
        for(CoreLabel tok: tokens) {
            sb.append(tok.toShorterString());
            sb.append("\n");
        }
        
        List<Mention> mentions = Mentions();
        if (mentions != null) {
            mentions.forEach((m)-> {
                sb.append("Mention ")
                  .append(m.mentionID).append(": ")
                  .append(mention_to_string(m))
                  .append(", token index: [").append(m.startIndex+ 1)
                  .append(", ").append(m.endIndex + 1).append(")")
                  .append("\n");
            });                    
        }
                
        return sb.toString();
    }
    
    public JsonArray GetMentionObjects(){        
        JsonArray jarr = new JsonArray();        
        for (Mention m: Mentions()){
            JsonObject m_obj = new JsonObject();
            m_obj.addProperty("word", mention_to_string(m));
            m_obj.addProperty("id", m.mentionID);
            m_obj.addProperty("animacy", m.animacy.name());
            m_obj.addProperty("start_index", m.startIndex);
            m_obj.addProperty("end_index", m.endIndex);
            m_obj.addProperty("isSubject", m.isSubject);
            m_obj.addProperty("isDObj", m.isDirectObject);
            m_obj.addProperty("isIObj", m.isIndirectObject);
            m_obj.addProperty("isPrepObj", m.isPrepositionObject);
            jarr.add(m_obj);
        }
        
        return jarr;
    }
    
    private String mention_to_string(Mention m) {             
        //! this is actually a copy of implementation in Mention.class
        //! Original implementation cache the string after first call to toString(),
        //! so it doesn't reflect the data updated in CoreLabel.
        StringBuilder os = new StringBuilder();
        for(int i = 0; i < m.originalSpan.size(); i ++){
            if(i > 0) os.append(" ");
            os.append(m.originalSpan.get(i).get(CoreAnnotations.TextAnnotation.class));
        }        
        
        return os.toString();
    }
    
    private void reindex_word_annotations(List<CoreLabel> lab_list) {
        for(int i = 0; i < lab_list.size(); ++i) {
            lab_list.get(i).setIndex(i+1);            
        }        
    }
      
}
