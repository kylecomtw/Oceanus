
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tw.com.kyle.oceanus.nlp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;


/**
 *
 * @author Sean_S325
 */
public class ParsedDocument implements Serializable {

    private Annotation doc_annot = null;
    private List<CoreMap> sentences = null;
    private ZCResult zcr = null;

    public ParsedDocument DeepCopy() {
        ParsedDocument pdoc_cpy = DeepCopyUtil.deep_copy(this);
        return pdoc_cpy;
    }

    public ParsedDocument(Annotation annot) {
        doc_annot = annot;
        sentences = doc_annot.get(CoreAnnotations.SentencesAnnotation.class);
    }

    public void annotateZh(ZCResult zcr) {
        if (sentences != null && !sentences.isEmpty()) {                        
            ZhAnnotator zh_annot = new ZhAnnotator();
            zh_annot.annotate(doc_annot, zcr);
        }
    }
        
    public Annotation GetAnnotation() {
        return doc_annot;
    }

    public int GetSentenceCount() {
        return sentences.size();
    }

    public List<CoreMap> GetSentences() {
        return sentences;
    }

    public CoreMap GetSentenceAnnotation(int idx) {
        return sentences.get(idx);
    }

    public ParsedSentence GetParsedSentence(int idx) {
        return new ParsedSentence(sentences.get(idx));
    }

    public List<ParsedSentence> GetParsedSentences() {
        return sentences.stream()
                .map((x) -> new ParsedSentence(x))
                .collect(Collectors.toList());
    }

    public void ConvertToTw() {
        if (sentences != null && !sentences.isEmpty()) {
            int n_sent = sentences.size();

            for (int i = 0; i < n_sent; ++i) {
                CoreMap sentence = sentences.get(i);
                ParsedSentence psent = GetParsedSentence(i);
                psent.UseZhTw();

                List<String> word_list = psent.Words();
                Tree tree = psent.GetTree();
                if (tree != null) {
                    ArrayList<Label> term_list = tree.yield();                    
                    for (int j = 0; j < term_list.size(); ++j) {
                        term_list.get(j).setValue(word_list.get(j));
                    }
                }
                
                GrammaticalStructure gs = psent.UniversalDep();
                if (gs != null){
                    for(TypedDependency dep: gs.allTypedDependencies()){
                        IndexedWord dep_idxword = dep.dep();
                        //! Position 0 is the ROOT word which was added in the front
                        int dep_i = dep_idxword.index() - 1;
                        if (dep_i >= 0){
                            dep_idxword.setValue(word_list.get(dep_i));
                            dep_idxword.setWord(word_list.get(dep_i));
                            dep_idxword.setLemma(word_list.get(dep_i));
                        }
                        
                        IndexedWord gov_idxword = dep.gov();
                        int gov_i = gov_idxword.index() - 1;
                        if (gov_i >= 0) {
                            gov_idxword.setValue(word_list.get(gov_i));
                            gov_idxword.setWord(word_list.get(gov_i));
                            gov_idxword.setLemma(word_list.get(gov_i));
                        }
                    }
                }
            }
        }

    }

    public void ConvertToCn() {
        if (sentences != null && !sentences.isEmpty()) {
            int n_sent = sentences.size();
            ZhConverter zhc = ZhConverter.GetInstance();

            for (int i = 0; i < n_sent; ++i) {
                CoreMap sentence = sentences.get(i);
                ParsedSentence psent = GetParsedSentence(i);
                psent.UseZhCn();

                Tree tree = psent.GetTree();
                if (tree != null) {
                    ArrayList<Label> term_list = tree.yield();
                    List<String> cn_list = psent.Words();
                    for (int j = 0; j < term_list.size(); ++j) {
                        term_list.get(j).setValue(cn_list.get(j));
                    }
                }
            }
        }
    }

    public String GetCorefRepr() {
        StringBuilder sb = new StringBuilder();
        Map<Integer, CorefChain> corefChains
                = GetAnnotation().get(CorefCoreAnnotations.CorefChainAnnotation.class);

        if (corefChains == null) {
            return "No coreference informaiton available";
        }
        for (Map.Entry<Integer, CorefChain> entry : corefChains.entrySet()) {
            sb.append("Chain " + entry.getKey() + " \n");
            CorefChain cc = entry.getValue();

            for (CorefChain.CorefMention m : entry.getValue().getMentionsInTextualOrder()) {
                // We need to subtract one since the indices count from 1 but the Lists start from 0
                List<CoreLabel> tokens = sentences.get(m.sentNum - 1).get(CoreAnnotations.TokensAnnotation.class);                
                sb.append("  ");
                for (int i = m.startIndex - 1; i < m.endIndex - 1; ++i){
                     sb.append(tokens.get(i).word());
                }
                
                sb.append(String.format("in sentence %d", m.sentNum - 1));
                sb.append(", mention type: ").append(m.mentionType).append("\n");
                // We subtract two for end: one for 0-based indexing, and one because we want last token of mention not one following.                
                sb.append("     i.e., 0-based character offsets [" + tokens.get(m.startIndex - 1).beginPosition()
                        + ", " + tokens.get(m.endIndex - 2).endPosition() + ")");
                sb.append("\n");
            }

            sb.append("\n");
        }

        return sb.toString();
    }
    
    public JsonArray GetCorefObjects() {
        Map<Integer, CorefChain> corefChains
                = GetAnnotation().get(CorefCoreAnnotations.CorefChainAnnotation.class);

        if (corefChains == null) {
            return new JsonArray();
        }
        
        JsonArray jarr = new JsonArray();
        for (Map.Entry<Integer, CorefChain> entry : corefChains.entrySet()) {
            JsonArray chain_arr = new JsonArray();
            for (CorefChain.CorefMention m : entry.getValue().getMentionsInTextualOrder()) {
                JsonObject jobj = new JsonObject();    
                List<CoreLabel> tokens = sentences.get(m.sentNum - 1).get(CoreAnnotations.TokensAnnotation.class);                                
                JsonArray tok_arr = new JsonArray();
                for (int i = m.startIndex - 1; i < m.endIndex - 1; ++i){
                    tok_arr.add(tokens.get(i).word());                    
                }
                jobj.add("words", tok_arr);
                
                jobj.addProperty("sentence", m.sentNum - 1);                
                jobj.addProperty("start_index", m.startIndex - 1);
                jobj.addProperty("end_index", m.endIndex - 1);                                                
                chain_arr.add(jobj);
            }
            jarr.add(chain_arr);
        }
                
        return jarr;
    }
    
    public String toReprStr() {
        StringBuilder sb = new StringBuilder();
        int sentence_counter = 1;
        for (CoreMap sent : sentences) {
            sb.append("Sentence ").append(sentence_counter++).append("\n");
            ParsedSentence psent = new ParsedSentence(sent);
            sb.append(psent.toReprStr());
            sb.append("\nParse Tree: \n");
            sb.append(psent.ConstituentTree()).append("\n");
            sb.append("\nDependency: \n");
            psent.BasicDep().forEach((x) -> sb.append(x).append("\n"));
        }

        sb.append("\nCoreference: \n");
        sb.append(GetCorefRepr()).append("\n");

        return sb.toString();
    }
}
