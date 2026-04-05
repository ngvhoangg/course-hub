FROM python:3.11-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Pre-download the model into the Docker image during build
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('all-MiniLM-L6-v2')"

# Copy application code
COPY main.py .

EXPOSE 8000

# Run server
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]