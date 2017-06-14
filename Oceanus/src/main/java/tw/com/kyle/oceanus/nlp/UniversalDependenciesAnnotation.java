/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tw.com.kyle.oceanus.nlp;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.trees.GrammaticalStructure;

/**
 *
 * @author Sean
 */
public class UniversalDependenciesAnnotation implements CoreAnnotation<GrammaticalStructure> {
  @Override
  public Class<GrammaticalStructure> getType() {
    return GrammaticalStructure.class;
  }
}