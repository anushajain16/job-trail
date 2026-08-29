"""A small, static tech-skill vocabulary shared by the LLM stub (heuristic
extraction when no OPENAI_API_KEY is configured) and the matcher (deriving a
candidate required-skills list from raw JD text when the caller doesn't pass
one from a prior /parse call).

Deliberately not exhaustive — it's a keyword-matching aid, not a taxonomy.
Real extraction quality for the LLM path comes from the model itself; this
list only backs the dependency-free fallback paths.
"""

SKILL_VOCAB: tuple[str, ...] = (
    # languages
    "python", "java", "javascript", "typescript", "go", "golang", "rust", "c++", "c#", "c",
    "kotlin", "swift", "ruby", "php", "scala", "r", "sql",
    # web / backend frameworks
    "spring boot", "spring", "django", "flask", "fastapi", "express", "express.js",
    "next.js", "nestjs", "rails", "laravel", ".net", "asp.net",
    # frontend
    "react", "vue", "angular", "svelte", "redux", "tailwind", "html", "css",
    # data / ml
    "pandas", "numpy", "pytorch", "tensorflow", "scikit-learn", "sentence-transformers",
    "nlp", "machine learning", "deep learning", "llm", "langchain",
    # databases
    "postgresql", "postgres", "mysql", "mongodb", "redis", "elasticsearch", "dynamodb",
    "cassandra", "sqlite",
    # infra / devops
    "docker", "kubernetes", "terraform", "aws", "gcp", "azure", "ci/cd", "github actions",
    "jenkins", "nginx", "linux", "bash",
    # messaging / streaming
    "kafka", "rabbitmq", "grpc", "graphql", "rest", "websockets",
    # practices
    "microservices", "system design", "unit testing", "tdd", "agile", "scrum",
    "oop", "distributed systems",
    # tools
    "git", "jira", "figma", "postman",
)
