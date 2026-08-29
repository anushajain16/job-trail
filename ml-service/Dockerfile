FROM python:3.11-slim

WORKDIR /app

# System deps for sentence-transformers' torch wheel and TLS certs for the
# scraper's outbound HTTPS calls.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY pyproject.toml ./
COPY app ./app

RUN pip install --no-cache-dir .

# Bakes the embedding model into the image so the first real request isn't
# the one paying for a cold model download — matters most in a container
# that gets rebuilt/redeployed often.
RUN python -c "from sentence_transformers import SentenceTransformer; \
    SentenceTransformer('sentence-transformers/all-MiniLM-L6-v2')"

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
