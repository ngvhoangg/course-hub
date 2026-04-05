from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
import logging

# Configure logging for easier debugging in Docker
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="CourseHub AI Service")

# Load the model only once when the server starts
MODEL_NAME = 'all-MiniLM-L6-v2'
logger.info(f"Loading model {MODEL_NAME} into RAM...")
model = SentenceTransformer(MODEL_NAME)
logger.info("Model loaded successfully!")

class EmbedRequest(BaseModel):
    text: str

class EmbedResponse(BaseModel):
    embedding: list[float]

@app.post("/embed", response_model=EmbedResponse)
async def embed(request: EmbedRequest):
    try:
        # Convert text to vector, then convert numpy array to a standard Python list
        vector = model.encode(request.text).tolist()
        return {"embedding": vector}
    except Exception as e:
        logger.error(f"Error while embedding text: {str(e)}")
        raise HTTPException(status_code=500, detail="Internal server error")

@app.get("/health")
async def health():
    return {"status": "healthy", "model": MODEL_NAME}