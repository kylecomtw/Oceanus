/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tw.com.kyle.oceanus.nlp;

import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.io.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.pipeline.AnnotationOutputter.Options;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.*;
import edu.stanford.nlp.wordseg.ChineseDictionary;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Sean_S325
 */
public class Oceanus {        
    
    private static StanfordCoreNLP s_segmenter = null;
    private static StanfordCoreNLP s_tagger = null;
    private static StanfordCoreNLP s_parser = null;
    private static DependencyParser dep_parser = null;
    private final static String CHINESE_NNDEP_MODELPATH = "CTB_coNLL_params.txt.gz";
    
    public void InitPipeline() {
        
        Properties props = new Properties();                    
        try {
            // InputStream is = Oceanus.class.getResourceAsStream("/ChineseProp.properties");
            InputStream is = Oceanus.class.getResourceAsStream("/zh-default.properties");
            props.load(is);            
        } catch (IOException ex) {
            Logger.getLogger(Oceanus.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);        
    }
    
    public ParsedDocument Tokenize(String tagged_text) {
        ASTokenizerAnnotator ast = new ASTokenizerAnnotator();
        Annotation annot = new Annotation(tagged_text);        
        
        // "[.]|[!?]+|[。]|[！？]+"
        Annotator ssplit_annotator = new WordsToSentencesAnnotator(false, 
                null, null, null, 
                StanfordCoreNLP.DEFAULT_NEWLINE_IS_SENTENCE_BREAK,
                null, null);                
        Annotator zh_annotator = new ZhAnnotator();
        ast.annotate(annot);
        zh_annotator.annotate(annot);
        ssplit_annotator.annotate(annot);
        ParsedDocument pdoc = new ParsedDocument(annot);
        pdoc.ConvertToTw();
        return pdoc;
    }
    
    public ParsedDocument Segment(String in_text){
        
        ZhConverter zhc = ZhConverter.GetInstance();
        ZCResult zcr = zhc.Convert(in_text);
        System.out.println(zcr);
        String in_text_cn = zcr.conv_str;
        
        Annotation seg_annot = stanford_seg(in_text_cn);
        
        ParsedDocument pdoc = new ParsedDocument(seg_annot);
        pdoc.annotateZh(zcr);
        pdoc.ConvertToTw();
        
        System.out.println(pdoc.toReprStr());
        
        
        return pdoc;
    }           
    
    public ParsedDocument Tag(ParsedDocument pdoc){        
        pdoc.ConvertToCn();
        stanford_pos(pdoc.GetAnnotation());
        pdoc.ConvertToTw();
        return pdoc;
    } 
    
    public ParsedDocument Parse(ParsedDocument pdoc){
        pdoc.ConvertToCn();
        stanford_parse(pdoc.GetAnnotation());
        pdoc.ConvertToTw();
        return pdoc;
    }
    
    public ParsedDocument UDep(ParsedDocument pdoc){
        pdoc.ConvertToCn();
        // stanford_udep(pdoc.GetAnnotation());
        stanford_parse(pdoc.GetAnnotation());
        pdoc.ConvertToTw();
        return pdoc;
    }
    
    private Annotation stanford_seg(String in_text) {
        Annotation annotation = new Annotation(in_text);
        
        if (s_segmenter == null) {
        Properties props = new Properties();
            try {
                // InputStream is = Oceanus.class.getResourceAsStream("/ChineseProp.properties");
                InputStream is = Oceanus.class.getClassLoader().getResourceAsStream("zh-seg.properties");
                props.load(is);            
            } catch (IOException ex) {
                Logger.getLogger(Oceanus.class.getName()).log(Level.SEVERE, null, ex);
            }

            s_segmenter = new StanfordCoreNLP(props);
        }
        s_segmenter.annotate(annotation);
        annotation.keySet().forEach((x)->System.out.println(x));
        
        return annotation;
    }
    
    private Annotation stanford_pos(Annotation seg_annot) {        
        
        if (s_tagger == null){
            Properties props = new Properties();

            try {
                // InputStream is = Oceanus.class.getResourceAsStream("/ChineseProp.properties");
                InputStream is = Oceanus.class.getClassLoader().getResourceAsStream("zh-tag.properties");
                props.load(is);            
            } catch (IOException ex) {
                Logger.getLogger(Oceanus.class.getName()).log(Level.SEVERE, null, ex);
            }

            s_tagger = new StanfordCoreNLP(props);
        }
        
        s_tagger.annotate(seg_annot);        
        
        return seg_annot;
    }
    
    private Annotation stanford_parse(Annotation seg_pos_annot) {        
        if (s_parser == null) {
            Properties props = new Properties();

            try {
                // InputStream is = Oceanus.class.getResourceAsStream("/ChineseProp.properties");
                InputStream is = Oceanus.class.getClassLoader().getResourceAsStream("zh-parse.properties");
                props.load(is);            
            } catch (IOException ex) {
                Logger.getLogger(Oceanus.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            s_parser = new StanfordCoreNLP(props);            
        }
        
        s_parser.annotate(seg_pos_annot);
        return seg_pos_annot;
    }
        
    private Annotation stanford_udep(Annotation annot) {    
        if(dep_parser == null){
            dep_parser = DependencyParser.loadFromModelFile(CHINESE_NNDEP_MODELPATH);
        }
        List<CoreMap> sent_list = annot.get(CoreAnnotations.SentencesAnnotation.class);
        for(CoreMap sent: sent_list){
            GrammaticalStructure gs = dep_parser.predict(sent);                      
            sent.set(UniversalDependenciesAnnotation.class, gs);
            // Logger.getLogger(Oceanus.class.getName()).log(Level.INFO, gs.toString());
        }
        
        return annot;
    }
    
    public static void main(String[] args) throws IOException 
    {
        // run_stanford_nlp("h:/20160815_cn.txt");
        // run_stanford_nlp("");
        // test_Chinese_dict();
        // test_ZhConvert();
        test_stanford("h:/in.txt");
        // test_tokenizer();
        // test_stanford("");
    }
    
    private static void test_stanford(String in_path) throws IOException {
        String intxt = "海馬颱風路徑怎麼走，一張圖告訴你。";
        // String intxt = "海马台风路径怎么走，一张图告诉你。气象局预估，明日下午发布海马台风海警。";
        String in_text = "";
        if (in_path.length() > 0 && Files.exists(Paths.get(in_path))){
            in_text = new String(Files.readAllBytes(Paths.get(in_path)), "UTF-8");
        } else {
            in_text = intxt;
        }
        
        Oceanus inst = new Oceanus();
        ParsedDocument pdoc = inst.Segment(in_text);        
        pdoc = inst.Tag(pdoc);
        pdoc = inst.Parse(pdoc);
        pdoc.GetParsedSentence(0).Mentions();
                
        try {
            PrintWriter pw = new PrintWriter("h:/pdoc.txt");
            pw.print(pdoc.toReprStr());
            pw.flush();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Oceanus.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    private static void test_tokenizer() {
        Oceanus oc = new Oceanus();
        Pattern pat = Pattern.compile("\\([\\w/]+\\)");        
        String in_text = "今天(NT/MISC)　是(SHI/O)　星期天(NT/MISC)　。(PU/O)";                
        ParsedDocument pdoc = oc.Tokenize(in_text);
        oc.Tag(pdoc);
        
        try {
            PrintWriter pw = new PrintWriter("h:/pdoc.txt");
            pw.print(pdoc.toReprStr());
            pw.flush();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Oceanus.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
            
    private static void test_ZhConvert()
    {
        ZhConverter zhc = ZhConverter.GetInstance();
        ZCResult zhr = zhc.Convert("颱風將朝南韓方向前進。");
        System.out.println(zhr.printAlignment());
        System.out.println("Original: " + zhr.ori_str);
        System.out.println("Converted: " + zhr.conv_str);
        String bck = zhc.MapBack(0, 2, zhr);
        System.out.println("MapBack: " + bck);
    }
    
    private static void test_Chinese_dict()
    {
        String serdict = "edu/stanford/nlp/models/segmenter/chinese/dict-chris6.ser.gz";
        ChineseDictionary cdict = new ChineseDictionary(serdict);
        ArrayList<String> words = new ArrayList<>(
                Arrays.asList("網際網路", "應該", "並且", "滑鼠", "光碟", "雷射", "应该", "激光", "并且", "隻身"));
        words.forEach((w) -> {
            System.out.println(w + "contains in cdict: " + cdict.contains(w));
        });
    }
    
    private static void run_stanford_nlp(String in_path) throws IOException
    {
        
        // Create a CoreNLP pipeline. To build the default pipeline, you can just use:
        //   StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        // Here's a more complex setup example:
        //   Properties props = new Properties();
        //   props.put("annotators", "tokenize, ssplit, pos, lemma, ner, depparse");
        //   props.put("ner.model", "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz");
        //   props.put("ner.applyNumericClassifiers", "false");
        //   StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        // Add in sentiment
        // String text = "克林顿说，华盛顿将逐步落实对韩国的经济援助。"
        //        + "金大中对克林顿的讲话报以掌声：克林顿总统在会谈中重申，他坚定地支持韩国摆脱经济危机。";
        
        String intxt = "海马台风路径怎么走，一张图告诉你。气象局预估，明日下午发布海马台风海警。";
        String text_tw = "Google與字型大廠Monotype合作，" + 
                "花超過五年的時間，進行一個名為Noto Project的計畫。" + 
                "上週，這項計畫宣布開放開源字型授權（Open Font License），" + 
                "開放字型的使用與修改，可直接於Google Noto Fonts免費下載使用這套Noto字型。";
        
        String in_text = "";
        if (in_path.length() > 0 && Files.exists(Paths.get(in_path))){
            in_text = new String(Files.readAllBytes(Paths.get(in_path)), "UTF-8");
        } else {
            in_text = intxt;
        }
        
        ZhConverter zhc = ZhConverter.GetInstance();
        ZCResult zcr = zhc.Convert(in_text);
        System.out.println(zcr);
        String in_text_cn = zcr.conv_str;
        Annotation annotation = new Annotation(in_text_cn);
        Properties props = new Properties();
        // InputStream is = Oceanus.class.getResourceAsStream("/ChineseProp.properties");
        InputStream is = Oceanus.class.getResourceAsStream("/zh-coref-default.properties");
        props.load(is);
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        pipeline.annotate(annotation);

        // run all the selected Annotators on this text
        // pipeline.annotate(annotation);
        
        PrintWriter xmlOut = new PrintWriter("h:/snlp.xml");
        PrintWriter out = new PrintWriter("h:/snlp.txt");

        // this prints out the results of sentence analysis to file(s) in good formats
        // pipeline.prettyPrint(annotation, out);
        if (xmlOut != null) {
            XMLOutputter xout = new XMLOutputter();
            Options opts = new AnnotationOutputter.Options();
            opts.constituentTreePrinter = new TreePrint("penn", "", new PennTreebankLanguagePack());
            
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            xout.print(annotation, os, opts); 
            xmlOut.write(new String(os.toByteArray(), "UTF-8"));
            xmlOut.flush();            
        }

        // Access the Annotation in code
        // The toString() method on an Annotation just prints the text of the Annotation
        // But you can see what is in it with other methods like toShorterString()
        // out.println();
        // out.println("The top level annotation");
        // out.println(annotation.toShorterString());
        // out.println();

        // An Annotation is a Map with Class keys for the linguistic analysis types.
        // You can get and use the various analyses individually.
        // For instance, this gets the parse tree of the first sentence in the text.
        out.println("The keys of document annotation are: ");
        annotation.keySet().forEach((x)->out.println(x));   
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        if (sentences != null && !sentences.isEmpty()) {
            int n_sent = sentences.size();
            for(int i = 0; i < n_sent; ++i){
                CoreMap sentence = sentences.get(i);
                out.println("The keys of the first sentence's CoreMap are:");
                sentence.keySet().forEach((x)->out.println(x));                
                
                out.println();
                out.println("The first sentence is:");
                out.println(sentence.toShorterString());
                out.println();
                out.println("The first sentence tokens are:");
                ArrayList<CoreLabel> word_list = new ArrayList<>();
                for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {                    
                    String cn_txt = token.get(CoreAnnotations.TextAnnotation.class);
                    int start_idx = token.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
                    int end_idx = token.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
                    String tw_txt = zhc.MapBack(start_idx, end_idx - start_idx, zcr);
                    token.set(CoreAnnotations.TextAnnotation.class, tw_txt);
                    token.set(CoreAnnotations.ValueAnnotation.class, tw_txt);
                    out.println(token.toShorterString());                                                            
                    word_list.add(token);
                }
                
                Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
                out.println();
                out.println("The first sentence parse tree is:");
                ArrayList<Label> term_list = tree.yield();
                for(int j = 0; j < term_list.size(); ++j){
                    term_list.get(j).setValue(word_list.get(j).value());
                }
                tree.pennPrint(out);
                out.println();
                out.println("The first sentence basic dependencies are:");
                out.println(sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class).toString(SemanticGraph.OutputFormat.LIST));
                out.println("The first sentence collapsed, CC-processed dependencies are:");
                SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
                out.println(graph.toString(SemanticGraph.OutputFormat.LIST));

                // Access coreference. In the coreference link graph,
                // each chain stores a set of mentions that co-refer with each other,
                // along with a method for getting the most representative mention.
                // Both sentence and token offsets start at 1!
                out.println("Coreference information");
                Map<Integer, CorefChain> corefChains
                        = annotation.get(CorefCoreAnnotations.CorefChainAnnotation.class);
                if (corefChains == null) {
                    return;
                }
                for (Map.Entry<Integer, CorefChain> entry : corefChains.entrySet()) {
                    out.println("Chain " + entry.getKey() + " ");
                    for (CorefChain.CorefMention m : entry.getValue().getMentionsInTextualOrder()) {
                        // We need to subtract one since the indices count from 1 but the Lists start from 0
                        List<CoreLabel> tokens = sentences.get(m.sentNum - 1).get(CoreAnnotations.TokensAnnotation.class);
                        // We subtract two for end: one for 0-based indexing, and one because we want last token of mention not one following.
                        out.println("  " + m + ", i.e., 0-based character offsets [" + tokens.get(m.startIndex - 1).beginPosition()
                                + ", " + tokens.get(m.endIndex - 2).endPosition() + ")");
                    }
                }
                out.println();
            }
                                  
                // out.println("The first sentence overall sentiment rating is " + sentence.get(SentimentCoreAnnotations.SentimentClass.class));
        }
        IOUtils.closeIgnoringExceptions(out);
        IOUtils.closeIgnoringExceptions(xmlOut);
    }
}
