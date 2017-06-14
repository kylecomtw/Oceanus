/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tw.com.kyle.oceanus.nlp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 * @author Sean_S325
 */
public class DeepCopyUtil {
    public static <T extends java.io.Serializable> T deep_copy(T obj) {
        T cpy = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(obj);
            out.flush();
            out.close();
            
            ObjectInputStream in = new ObjectInputStream(
                    new ByteArrayInputStream(bos.toByteArray())
            );
            
            cpy = (T)in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        
        return cpy;
    }
        
        
    
}
