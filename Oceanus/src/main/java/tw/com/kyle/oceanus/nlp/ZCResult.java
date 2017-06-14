/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tw.com.kyle.oceanus.nlp;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Sean_S325
 */
    public class ZCResult implements Serializable {
        public List<List<Integer>> ori_idx;
        public List<List<Integer>> new_idx;
        public String ori_str;
        public String conv_str;
                
        @Override
        public String toString() {            
            return conv_str;
        }
        
        public String printAlignment(){
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < ori_idx.size(); ++i){                
                sb.append(ori_idx.get(i)); sb.append(": ");
                int ori_start = ori_idx.get(i).get(0);
                int new_start = new_idx.get(i).get(0);
                sb.append(ori_str.substring(ori_start, ori_start + ori_idx.get(i).size()));
                sb.append(" -> ");                
                sb.append(new_idx.get(i)); sb.append(": ");
                sb.append(conv_str.substring(new_start, new_start + new_idx.get(i).size()));
                sb.append("\n");                
            }
            return sb.toString();
        }
            
    }
