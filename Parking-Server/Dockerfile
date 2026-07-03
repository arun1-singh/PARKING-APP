FROM python:3.9-slim

WORKDIR /app

# Use 'libgl1' instead of 'libgl1-mesa-glx'
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 libglib2.0-0 curl && \
    rm -rf /var/lib/apt/lists/*

COPY . .

RUN pip install --no-cache-dir \
    flask gunicorn opencv-python-headless ultralytics \
    pillow numpy torch --extra-index-url https://download.pytorch.org/whl/cpu

EXPOSE 8000

CMD ["gunicorn", "--bind", "0.0.0.0:8000", "app:app"]
