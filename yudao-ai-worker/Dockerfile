FROM python:3.11-slim

WORKDIR /app
COPY worker.py /app/worker.py

EXPOSE 8099
CMD ["python", "/app/worker.py", "--host", "0.0.0.0", "--port", "8099", "--token", "worker-secret"]
