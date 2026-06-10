"""
ParkVision local license-plate recognition service (HyperLPR3).

A thin, self-contained HTTP wrapper around HyperLPR3 so the Java backend can do
real, offline plate recognition over a stable JSON contract:

    POST /recognize   { "imageBase64": "<data-url or raw base64>" }
        -> { "ok": true, "plate": "沪A12345", "confidence": 0.98, "color": "blue" }
        -> { "ok": true, "plate": null }            # nothing detected
    GET  /health      -> { "status": "ok" }

Run:
    pip install -r requirements.txt
    python server.py                 # listens on 0.0.0.0:8715
"""
import base64

import cv2
import numpy as np
import uvicorn
import hyperlpr3 as lpr3
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="ParkVision HyperLPR Service", version="1.0")

# DETECT_LEVEL_HIGH = 640x640, more accurate; LOW = 320x320, faster.
catcher = lpr3.LicensePlateCatcher(detect_level=lpr3.DETECT_LEVEL_HIGH)

# HyperLPR3 plate-type index -> colour label consumed by the backend.
TYPE_COLOR = {0: "blue", 1: "yellow", 2: "white", 3: "green", 4: "black", 9: "yellow"}


class RecognizeRequest(BaseModel):
    imageBase64: str


def _decode(image_b64: str):
    raw = image_b64.strip()
    if raw.startswith("data:") and "," in raw:
        raw = raw.split(",", 1)[1]
    buffer = np.frombuffer(base64.b64decode(raw), np.uint8)
    return cv2.imdecode(buffer, cv2.IMREAD_COLOR)


def _run(image):
    """Detect + recognise. If nothing is found on a tightly-cropped plate image,
    pad it with a margin so the detector has scene context, then retry once."""
    results = catcher(image)
    if results:
        return results
    h, w = image.shape[:2]
    pad_v, pad_h = int(h * 1.4), int(w * 0.5)
    padded = cv2.copyMakeBorder(image, pad_v, pad_v, pad_h, pad_h, cv2.BORDER_CONSTANT, value=(114, 114, 114))
    return catcher(padded)


@app.post("/recognize")
def recognize(req: RecognizeRequest):
    image = _decode(req.imageBase64)
    if image is None:
        return {"ok": False, "error": "image decode failed"}

    results = _run(image)
    if not results:
        return {"ok": True, "plate": None}

    # Pick the most confident detection.
    code, confidence, type_idx, _box = max(results, key=lambda r: r[1])
    return {
        "ok": True,
        "plate": code,
        "confidence": round(float(confidence), 3),
        "color": TYPE_COLOR.get(int(type_idx), "blue"),
    }


@app.get("/health")
def health():
    return {"status": "ok"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8715)
