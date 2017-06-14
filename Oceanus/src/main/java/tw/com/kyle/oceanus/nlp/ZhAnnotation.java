/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tw.com.kyle.oceanus.nlp;

import edu.stanford.nlp.ling.CoreAnnotation;

public class ZhAnnotation {

    public static class ZhTwAnnotation implements CoreAnnotation<String> {

        @Override
        public Class<String> getType() {
            return String.class;
        }
    }

    public static class ZhCnAnnotation implements CoreAnnotation<String> {

        @Override
        public Class<String> getType() {
            return String.class;
        }

    }
}
