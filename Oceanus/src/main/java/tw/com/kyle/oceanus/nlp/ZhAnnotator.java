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
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Sean_S325
 */
public class ZhAnnotator implements Annotator {
    
    public void annotate(Annotation annotation, ZCResult zcr) {
        if (annotation.containsKey(CoreAnnotations.TokensAnnotation.class)) {
            List<CoreLabel> tokens = annotation.get(CoreAnnotations.TokensAnnotation.class);                                    
            ZhConverter zhc = ZhConverter.GetInstance();
            for(CoreLabel tok: tokens) {
                int start = tok.beginPosition();
                int end = tok.endPosition();
                String zh_tw = zhc.MapBack(start, end-start, zcr);
                
                tok.set(ZhAnnotation.ZhTwAnnotation.class, zh_tw);
                tok.set(ZhAnnotation.ZhCnAnnotation.class, tok.word());
            }
            
        } else {
            throw new RuntimeException("ZhAnnotator unable to find tokens: " + annotation);
        }
    }
    
    @Override
    public void annotate(Annotation annotation) {
        
        if (annotation.containsKey(CoreAnnotations.TokensAnnotation.class)) {
            List<CoreLabel> tokens = annotation.get(CoreAnnotations.TokensAnnotation.class);                                    
            ZhConverter zhc = ZhConverter.GetInstance();
            for(CoreLabel tok: tokens) {
                String zh_cn = zhc.Convert(tok.word()).conv_str;
                tok.set(ZhAnnotation.ZhTwAnnotation.class, tok.word());
                tok.set(ZhAnnotation.ZhCnAnnotation.class, zh_cn);
                tok.setWord(zh_cn);
                tok.setLemma(zh_cn);
                tok.setValue(zh_cn);                
            }
            
        } else {
            throw new RuntimeException("ZhAnnotator unable to find tokens: " + annotation);
        }
    }

    @Override
    public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {                
        return Collections.singleton(CoreAnnotations.TokensAnnotation.class);        
    }

    @Override
    public Set<Class<? extends CoreAnnotation>> requires() {
        return Collections.emptySet();
    }
    
}
