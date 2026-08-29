from app.services.embeddings import HashingEmbeddingProvider, cosine_similarity


def test_hashing_provider_is_deterministic():
    provider = HashingEmbeddingProvider()

    first = provider.embed_batch(["Python backend engineer"])
    second = provider.embed_batch(["Python backend engineer"])

    assert first == second


def test_hashing_provider_similar_text_scores_higher_than_unrelated():
    provider = HashingEmbeddingProvider()
    base, similar, unrelated = provider.embed_batch([
        "Python backend engineer with FastAPI experience",
        "Backend engineer skilled in Python and FastAPI",
        "Watercolor painting and violin lessons",
    ])

    assert cosine_similarity(base, similar) > cosine_similarity(base, unrelated)


def test_cosine_similarity_handles_zero_vector():
    assert cosine_similarity([0.0, 0.0], [1.0, 0.0]) == 0.0


def test_hashing_provider_empty_text_yields_zero_vector():
    [vector] = HashingEmbeddingProvider().embed_batch([""])

    assert vector == [0.0] * HashingEmbeddingProvider._DIMENSIONS
