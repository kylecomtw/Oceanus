/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tw.com.kyle.oceanus.nlp;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sean_S325
 */
public class ASTokenizerAnnotator implements Annotator {

    private static Logger logger = LoggerFactory.getLogger(ASTokenizerAnnotator.class);
    
    // CONSTRUCTORS
    /**
     * Gives a verbose, English tokenizer. Probably no one wants that!
     */
    public ASTokenizerAnnotator() {
        this(null);        
    }

    public ASTokenizerAnnotator(Properties props) {
        if (props == null) {
            props = new Properties();
        }
        // VERBOSE = PropertiesUtils.getBool(props, "tokenize.verbose", verbose);
    }   
    
    private List<CoreLabel> tokenize(String buf) {
        List<CoreLabel> lab_list = new ArrayList<>();
        String[] toks = buf.split("\u3000");
        Pattern pat = Pattern.compile("(.*?)\\((\\w+)(/\\w+)?\\)");
        int counter = 0;
        int ch_counter = 0;
        
        for(String tok :toks){  
            String tok_trim = tok.trim();
            Matcher m = pat.matcher(tok_trim);
            String txt = "", pos = "", ner = "";
            if (m.matches()){
                txt = m.group(1);
                pos = m.group(2);
                ner = m.group(3) != null? m.group(3): "";
            } else {
                txt = tok_trim;
            }
            
            CoreLabel lab = new CoreLabel();
            lab.setWord(txt);
            lab.setValue(txt);
            lab.setLemma(txt);
            lab.setIndex(++counter);
            lab.setBeginPosition(ch_counter); ch_counter += txt.length();
            lab.setEndPosition(ch_counter);
            lab.setTag(pos);
            lab.setNER(ner);                        
            // logger.info(lab.toShortString());
            lab_list.add(lab);
        }
        
        
        return lab_list;
    }
        
    /**
     * Does the actual work of splitting TextAnnotation into CoreLabels, which
     * are then attached to the TokensAnnotation.
     * @param annotation
     */
    @Override
    public void annotate(Annotation annotation) {

        if (annotation.containsKey(CoreAnnotations.TextAnnotation.class)) {
            String text = annotation.get(CoreAnnotations.TextAnnotation.class);                        

            List<CoreLabel> tokens = tokenize(text);            

            annotation.set(CoreAnnotations.TokensAnnotation.class, tokens);
        } else {
            throw new RuntimeException("Tokenizer unable to find text in annotation: " + annotation);
        }
    }

    @Override
    public Set<Class<? extends CoreAnnotation>> requires() {
        return Collections.emptySet();
    }

    @Override
    public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
        return Collections.singleton(CoreAnnotations.TokensAnnotation.class);
    }

}
