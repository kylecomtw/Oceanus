/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tw.com.kyle.oceanus.nlp;

import java.util.HashSet;
import java.util.List;

/**
 *
 * @author Sean_S325
 */
public class UpdateActor {
    private ParsedDocument pdoc = null;
    public UpdateActor(ParsedDocument _pdoc) {
        pdoc = _pdoc;
    }
    
    public ParsedDocument apply(List<UpdateAction> act_list) {
        ParsedDocument pdoc_cpy = pdoc.DeepCopy();
        act_list.forEach((x)->apply(x, pdoc_cpy));
        return pdoc_cpy;
    }
    
    private void apply(UpdateAction act, ParsedDocument pd) {
        if (act.SentenceIdx < 0 || act.WordIdx < 0) {
            return;
        }
        
        if (act.Action.equals("WADD")) {
            apply_word_addition(act, pd);
        } else if (act.Action.equals("WDEL")) {
            apply_word_removal(act, pd);
        } else if (act.Action.equals("WMOD")) {
            apply_word_modification(act, pd);
        } else {
            // pass
        }
    }
    
    private void apply_word_addition(UpdateAction act, ParsedDocument pd) {
        ParsedSentence ps = pd.GetParsedSentence(act.SentenceIdx);
        ps.AddWord(act.Word, act.POS, act.NER, act.Start, act.End);
    }
    
    private void apply_word_modification(UpdateAction act, ParsedDocument pd) {
        ParsedSentence ps = pd.GetParsedSentence(act.SentenceIdx);        
        if (act.Word != null) ps.SetWord(act.WordIdx, act.Word);
        if (act.POS  != null) ps.SetPOS(act.WordIdx, act.POS);
        if (act.NER != null) ps.SetNER(act.WordIdx, act.NER);
        if (act.Start >= 0) ps.SetStart(act.WordIdx, act.Start);
        if (act.End >= 0) ps.SetEnd(act.WordIdx, act.End);                    
    }
    
    private void apply_word_removal(UpdateAction act, ParsedDocument pd) {
        ParsedSentence ps = pd.GetParsedSentence(act.SentenceIdx);
        ps.RemoveWord(act.WordIdx);
    }            
}
