import os
import json
import gzip
from SPARQLWrapper import SPARQLWrapper, JSON
import pdb

class DBPedia:
    def __init__(self):
        dirpath = os.path.dirname(__file__)
        redir_path = os.path.join(dirpath, "redirection_map.json.gz")
        if not os.path.exists(redir_path):
            raise FileNotFoundError()
        self.redir_map = json.load(gzip.open(redir_path))

    def redirect(self, title):
        if type(title) is list:
            return [self.redir_map.get(x, x) for x in title]
        else:
            return self.redir_map.get(title, title)

    def resolve_LUs(self, lexical_units):
        if type(lexical_units) is str:
            lexical_units = [lexical_units]
        
        redir_map = {lu: self.redirect(lu) for lu in lexical_units}

        sparql = SPARQLWrapper("http://dbpedia.org/sparql")
        query_values = ' '.join(
                ['("{}"@zh)'.format(lu) for lu in redir_map.values()])
        query_str = """
        CONSTRUCT {?x rdfs:label ?title} where {
        values (?title) {%s}
        ?x rdfs:label ?title
        FILTER regex(?x, "dbpedia")
        } 
        """
        sparql.setQuery(query_str % (query_values))
        # print(query_str % (query_values))
        sparql.setReturnFormat(JSON)
        results = sparql.query().response.read().decode("UTF-8")

        to_ue = lambda x: json.dumps(x).upper()\
                        .replace("\"", "")\
                        .replace("U", "u")
        for ori, redir in redir_map.items():
            if ori == redir: continue
            results = results.replace(to_ue(redir), to_ue(ori))
        jobj = json.loads(results)
        res_map = {x["o"]["value"]: x["s"]["value"] \
                    for x in jobj["results"]["bindings"]}
        return res_map

    def ask(self, uris):
        if type(uris) is str:
            uris = [uris]

        sparql = SPARQLWrapper("http://dbpedia.org/sparql")
        query_values = ' '.join(
                  ['(<{}>)'.format(uri) for uri in uris])

        query_str = """
        CONSTRUCT {?s ?p ?o} where {
        values (?s) {%s}
        ?s ?p ?o
        FILTER ((regex(?p, "http://dbpedia.org/ontology/") ||
                 regex(?p, "http://dbpedia.org/property/wordnet_type") ||
                 regex(?p, "http://purl.org/dc/terms/subject") ||
                 regex(?p, "http://www.w3.org/1999/02/22-rdf-syntax-ns#")) &&
                 !regex(?p, "(abstract|label)") &&
                 !regex(?p, "wikiPage")
                )
        } 
        """
        query_expr = query_str % (query_values)
        sparql.setQuery(query_expr)        
        sparql.setReturnFormat(JSON)
        results = sparql.query().response.read().decode("UTF-8")
        results = results\
                    .replace("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdfs:")\
                    .replace("http://www.w3.org/2002/07/owl#", "owl:")
        jobj = json.loads(results)
        retObj = {uri: [] for uri in uris}
        for spo in jobj["results"]["bindings"]:
            data_item = (
                    spo["p"]["value"], spo["o"]["value"]
                )
            retObj[spo["s"]["value"]].append(data_item)
        
        return retObj

