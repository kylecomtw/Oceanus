/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tw.com.kyle.oceanus.nlp;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import spark.Request;
import spark.Response;
import static spark.Spark.*;
/**
 *
 * @author Sean_S325
 */
public class SparkMain {

    private Oceanus os = null;
    private LinkedList<String> buf = new LinkedList<>();
    private HashMap<String, ParsedDocument> doc_tasks = new HashMap<>();
    private final int BUFFER_SIZE = 50;
    
    public SparkMain(){
        os = new Oceanus();
        // os.InitPipeline();        
        port(8090);
        post("/test", (req, res) -> test_handler(req, res));
        get("/ping", (req, res) -> "Server running: " + get_task_id());
        post("/nlp/tag/:taskid", (req, res) -> nlp_handler("tag", req, res));
        post("/nlp/parse/:taskid", (req, res) -> nlp_handler("parse", req, res));
        post("/nlp/udep/:taskid", (req, res) -> nlp_handler("udep", req, res));
        post("/nlp/tokenize", (req, res) -> tokenize_handler("tokenize", req, res));
        post("/nlp/segment", (req, res) -> nlp_handler("segment", req, res));
        post("/nlp/tag", (req, res) -> nlp_handler("tag", req, res));
        post("/nlp/parse", (req, res) -> nlp_handler("parse", req, res));        
        post("/nlp/udep", (req, res) -> nlp_handler("udep", req, res));        
        get("/nlp/task/:taskid", (req, res) -> task_handler(req, res));
        post("/nlp/task/:taskid", (req, res) -> task_handler(req, res));
        exception(Exception.class, (ex, req, resp) -> System.out.println(ex));
        System.out.println("Oceanus Ready");
    }
    
    private String test_handler(Request req, Response res) {
        String json = req.body();
        List<UpdateAction> u_acts = GsonConverter.ListUpdateActionFromJson(json);
        System.out.println(u_acts);
        return "";
    }
    
    private String nlp_handler(String task, Request req, Response res) {        
        String in_text = req.queryParams("intext");
        String task_id = req.params(":taskid");
        
        System.out.println(task);
        System.out.println(in_text);
        
        ParsedDocument pdoc = null;
        res.type("application/json");
        if (task_id != null){
            if(doc_tasks.containsKey(task_id)) {
                pdoc = query_task(task_id);
            } else {
                res.status(404);
                // res.body("{\"error\": \"Task id not found\"}");
                return "{\"error\": \"Task id not found\"}";
            }
        } else {
            if (in_text != null && in_text.length() > 0) {
                pdoc = os.Segment(in_text);
            } else {
                res.status(400);
                // res.body("{\"error\": \"no in_text\"}");
                return "{\"error\": \"no in_text\"}";
            }
        }
        
        if (pdoc != null){
            if (task.equals("tag") || task.equals("udep") || task.equals("parse")) os.Tag(pdoc);
            if (task.equals("parse") || task.equals("udep")) os.Parse(pdoc);            
            String resp_str = prepare_task_json(null, pdoc);
            res.status(200);
            // res.body(resp_str);
            return resp_str;
        }
        
        return "";


    }
    
    private String tokenize_handler(String task, Request req, Response res) {
        String in_text = req.queryParams("intext");
        ParsedDocument pdoc = os.Tokenize(in_text);
        
        if (pdoc != null){   
            String resp_str = prepare_task_json(null, pdoc);
            res.status(200);
            return resp_str;
        } else {
            res.status(400);
            return "{\"error\": \"tokenize error\"}";
        }
    }

    
    private String task_handler(Request req, Response res) {
        String task_id = req.params(":taskid");
        if (!doc_tasks.containsKey(task_id)) {
            res.status(404);            
            return "Cannot find task id";
        }
        
        if (req.requestMethod().toUpperCase().equals("GET")) {
            res.status(200);            
            return prepare_task_json(task_id, query_task(task_id));
        }
        
        if (req.requestMethod().toUpperCase().equals("POST")) {
            ParsedDocument pdoc = query_task(task_id);
            String update_json_data = req.body();
            List<UpdateAction> u_acts = GsonConverter.ListUpdateActionFromJson(update_json_data);
            UpdateActor uactor = new UpdateActor(pdoc);
            ParsedDocument pdoc_new = uactor.apply(u_acts);
            String new_task_id = add_task(pdoc_new);
            
            res.status(200);
            return prepare_task_json(new_task_id, pdoc_new);
        }
        
        return "";
    }
    
    private String add_task(ParsedDocument pdoc) {
        String task_id = get_task_id();
        buf.addFirst(task_id);
        if(buf.size() > BUFFER_SIZE) {
            String to_remove_id = buf.pollLast();
            doc_tasks.remove(to_remove_id);
        }
        
        doc_tasks.put(task_id, pdoc);
        return task_id;
    }
    
    /* 
     * query_task has two funciton: 1) wraps the lookup in doc_tasks;
     * 2) brings the task_id in buf to the front, 
     * so that it would be the last to remove when buffer exceeding the capacity.
    */
    private ParsedDocument query_task(String task_id) {        
        if (!doc_tasks.containsKey(task_id)) {
            return null;
        }
        
        int idx = buf.indexOf(task_id);
        if(idx < 0) return null;
        
        buf.remove(idx);
        buf.addFirst(task_id);
        
        return doc_tasks.get(task_id);
    }
            
    private String prepare_task_json(String task_id, ParsedDocument pdoc) {
        if (task_id == null) {
            task_id = add_task(pdoc);
        }
        JsonObject jobj = new JsonObject();
        JsonElement json_elem = GsonConverter.FromParsedDocument(pdoc);        
        jobj.add("data", json_elem);
        jobj.addProperty("taskid", task_id);     
        
        return jobj.toString();
    }
    
    private String get_task_id() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        return dateFormat.format(new Date());
    }
    
    
    public static void main(String[] args) {
        SparkMain inst = new SparkMain();        
    }

}
