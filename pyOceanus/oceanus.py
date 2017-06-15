import re
import requests

class Oceanus:
    dep_re = re.compile("([a-z:]*)\(([^\s-]*)-(\d+),\s?([^\s-]*)-(\d+)\)")
    def __init__(self, url = None):
        self.url = "http://127.0.0.1:8090/nlp/parse"
        if url is not None:
            self.url = url
        self.nlp = None
        self._tokens = None
        self._deps = None

    def ensure_nlp(self, text):
        if self.nlp is None:
            self.nlp = self.parse(text)

    def parse(self, text):
        resp = requests.post(self.url, 
                {"intext":text})
        self.nlp = resp.json()
        return self.nlp
    
    def tokens(self, text):
        self.ensure_nlp(text)
        sentences = self.nlp["data"]["sentences"]
        pos_list = list(map(lambda x: x["pos"], sentences))
        words_list = list(map(lambda x: x["words"], sentences))
        ner_list = list(map(lambda x: x["ner"], sentences))
        toks = []
        for sent_i in range(len(pos_list)):
            tok_iter = zip(words_list[sent_i], \
                           pos_list[sent_i], ner_list[sent_i])
            tok_vec = []
            for w, p, n in tok_iter:
                tok_vec.append((w, p, n))
            toks.append(tok_vec)        
        self._tokens = toks
        return self._tokens

    def dependency(self, text):
        self.ensure_nlp(text)
        sentences = self.nlp["data"]["sentences"]
        deps_iter = map(lambda x: x["udep"], sentences)
        deps = []
        for sent_dep in deps_iter:
            dep_vec = []
            for dep_x in sent_dep:
                m_dep = Oceanus.dep_re.match(dep_x)

                if m_dep is None:
                    print("WARNING: cannot parse %s" % dep_x)
                    continue
                rel = m_dep.group(1)
                gov = m_dep.group(2)
                gov_i = int(m_dep.group(3))
                dep = m_dep.group(4)
                dep_i = int(m_dep.group(5))
                dep_vec.append((rel, gov, gov_i, dep, dep_i))
            deps.append(dep_vec)

        self._deps = deps
        return self._deps
        
    def get_token(self, sentence_idx, tok_idx):
        if self._tokens is None:
            print("Run tokens first")
            return None
        
        if tok_idx == -1: return ("ROOT", "", "")
        return self._tokens[sentence_idx][tok_idx]

    def get_dep_token(self, sent_idx, dep_tuple):
        return (self.get_token(sent_idx, dep_tuple[2]-1), \
                self.get_token(sent_idx, dep_tuple[4]-1))

    def get_dependency(self, sentence_idx, tok_idx):
        if self._deps is None:
            print("Run dependency first")

        sent_dep = self._deps[sentence_idx]
        deps_x = filter(lambda x: x[2] == tok_idx+1 or x[4] == tok_idx+1, \
                        sent_dep)
        return list(deps_x)

