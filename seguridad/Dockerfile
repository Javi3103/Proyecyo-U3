FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# api_modelo.py, cwe_forense.py, feature_extractor.py, modelo_mineria_seguro.pkl
COPY . .

EXPOSE 8000

CMD ["uvicorn", "api_modelo:app", "--host", "0.0.0.0", "--port", "8000"]
