"""Text-to-vector backends behind one interface.

``SentenceTransformerProvider`` is the real thing this service ships with —
a local sentence-transformers model, no external API call, no per-request
cost. Loading the model is the expensive part (~seconds, one-time), so it's
deferred to first use and cached on the instance rather than done at import
time — importing this module must stay cheap even when the model backend is
never actually invoked (e.g. every test run, see HashingEmbeddingProvider).

``HashingEmbeddingProvider`` is a small, deterministic, dependency-free
fallback: a character-n-gram hashed bag-of-words vector, normalized. It
captures nothing like real semantics, only lexical overlap — good enough to
make matcher logic testable without downloading model weights, and picked
via MLSVC_EMBEDDING_BACKEND=hashing. It is not a substitute for the real
backend in production.
"""

import hashlib
import math
from abc import ABC, abstractmethod

import numpy as np


class EmbeddingProvider(ABC):
    @abstractmethod
    def embed_batch(self, texts: list[str]) -> list[list[float]]: ...


class SentenceTransformerProvider(EmbeddingProvider):
    def __init__(self, model_name: str) -> None:
        self._model_name = model_name
        self._model = None  # lazy — see module docstring

    def _get_model(self):
        if self._model is None:
            from sentence_transformers import SentenceTransformer  # deferred: heavy import

            self._model = SentenceTransformer(self._model_name)
        return self._model

    def embed_batch(self, texts: list[str]) -> list[list[float]]:
        vectors = self._get_model().encode(texts, normalize_embeddings=True)
        return [vector.tolist() for vector in vectors]


class HashingEmbeddingProvider(EmbeddingProvider):
    _DIMENSIONS = 256
    _NGRAM_SIZE = 3

    def embed_batch(self, texts: list[str]) -> list[list[float]]:
        return [self._embed_one(text) for text in texts]

    def _embed_one(self, text: str) -> list[float]:
        vector = np.zeros(self._DIMENSIONS, dtype=np.float64)
        normalized = text.lower()
        for token in normalized.split():
            for start in range(max(1, len(token) - self._NGRAM_SIZE + 1)):
                gram = token[start:start + self._NGRAM_SIZE]
                index = int(hashlib.blake2b(gram.encode(), digest_size=4).hexdigest(), 16) % self._DIMENSIONS
                vector[index] += 1.0
        norm = float(np.linalg.norm(vector))
        if norm == 0.0:
            return vector.tolist()
        return (vector / norm).tolist()


def cosine_similarity(a: list[float], b: list[float]) -> float:
    vec_a, vec_b = np.array(a), np.array(b)
    norm_a, norm_b = np.linalg.norm(vec_a), np.linalg.norm(vec_b)
    if norm_a == 0.0 or norm_b == 0.0:
        return 0.0
    similarity = float(np.dot(vec_a, vec_b) / (norm_a * norm_b))
    # guards against float noise pushing a hair outside [-1, 1]
    return max(-1.0, min(1.0, similarity)) if not math.isnan(similarity) else 0.0
