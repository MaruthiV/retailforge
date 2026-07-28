import math
from collections import Counter

from .chunk import tokenize


# hybrid: bm25 lexical score + jaccard token-overlap as a lightweight semantic proxy.
# a real deployment swaps the jaccard term for pgvector embeddings; the interface stays the same.
class HybridIndex:
    def __init__(self, chunks, k1=1.5, b=0.75, alpha=0.7):
        self.chunks = chunks
        self.k1 = k1
        self.b = b
        self.alpha = alpha
        self.N = len(chunks)
        self.doc_tokens = [Counter(c.tokens) for c in chunks]
        self.doc_len = [sum(tc.values()) for tc in self.doc_tokens]
        self.avg_len = (sum(self.doc_len) / self.N) if self.N else 0
        self.df = Counter()
        for tc in self.doc_tokens:
            for term in tc:
                self.df[term] += 1
        self.token_sets = [set(c.tokens) for c in chunks]

    def _idf(self, term):
        n = self.df.get(term, 0)
        return math.log(1 + (self.N - n + 0.5) / (n + 0.5))

    def _bm25(self, i, q_terms):
        tc = self.doc_tokens[i]
        dl = self.doc_len[i] or 1
        score = 0.0
        for term in q_terms:
            f = tc.get(term, 0)
            if not f:
                continue
            idf = self._idf(term)
            score += idf * (f * (self.k1 + 1)) / (f + self.k1 * (1 - self.b + self.b * dl / self.avg_len))
        return score

    def search(self, query, k=5, service=None, doc_type=None, exclude_tests=True):
        q_terms = tokenize(query)
        q_set = set(q_terms)
        scored = []
        bm_raw = []
        for i, c in enumerate(self.chunks):
            if service and c.service != service:
                continue
            if doc_type and c.doc_type != doc_type:
                continue
            if exclude_tests and c.doc_type == "test":
                continue
            bm = self._bm25(i, q_terms)
            bm_raw.append((i, bm))
        if not bm_raw:
            return []
        max_bm = max(b for _, b in bm_raw) or 1.0
        for i, bm in bm_raw:
            jac = len(q_set & self.token_sets[i]) / (len(q_set | self.token_sets[i]) or 1)
            score = self.alpha * (bm / max_bm) + (1 - self.alpha) * jac
            scored.append((score, i))
        scored.sort(reverse=True)
        return [self.chunks[i] for _, i in scored[:k]]

    def search_with_scores(self, query, k=5, **kw):
        results = self.search(query, k=k, **kw)
        return results
