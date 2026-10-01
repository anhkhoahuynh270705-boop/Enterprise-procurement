"""
Face Recognition Service (OpenCV YuNet + SFace Engine)

Models:
  - Detection: YuNet (cv2.FaceDetectorYN) ~400KB, ~5-10ms
  - Recognition: SFace (cv2.FaceRecognizerSF) ~37MB, 128-d embeddings, ~10ms
  - Auto-downloads models to FaceService/models/ on first launch

Features:
  - Multi-pose template (straight, left tilt, right tilt)
  - Instant zero-lag recognition (~15-20ms total inference on CPU)
  - 1:1 verification & 1:N automatic identification
"""

from __future__ import annotations

import base64
import logging
import os
import time
import urllib.request
from pathlib import Path
from typing import Annotated

import cv2
import numpy as np
from fastapi import Depends, FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from sqlalchemy import Column, DateTime, String, Text, create_engine, func
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("face-service")

# Config
DB_URL = os.getenv("DATABASE_URL", "postgresql+psycopg2://postgres:Khoa123@localhost:5432/shopping_db")
SIMILARITY_THRESHOLD = float(os.getenv("FACE_THRESHOLD", "0.38"))
LIVENESS_THRESHOLD = float(os.getenv("LIVENESS_TEXTURE", "30.0"))
QUALITY_THRESHOLD = float(os.getenv("QUALITY_BLUR", "25.0"))
ALLOWED_ORIGINS = os.getenv("ALLOWED_ORIGINS", "http://localhost:4200,http://localhost:8080").split(",")

# Model Paths & Auto-Download
BASE_DIR = Path(__file__).resolve().parent
MODELS_DIR = BASE_DIR / "models"
MODELS_DIR.mkdir(parents=True, exist_ok=True)

# Face detection and recognition models (ONNX format)
YUNET_PATH = MODELS_DIR / "face_detection_yunet_2023mar.onnx"
SFACE_PATH = MODELS_DIR / "face_recognition_sface_2021dec.onnx"

YUNET_URLS = [
    "https://media.githubusercontent.com/media/opencv/opencv_zoo/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx",
    "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx",
]
SFACE_URLS = [
    "https://media.githubusercontent.com/media/opencv/opencv_zoo/main/models/face_recognition_sface/face_recognition_sface_2021dec.onnx",
    "https://github.com/opencv/opencv_zoo/raw/main/models/face_recognition_sface/face_recognition_sface_2021dec.onnx",
]


def download_file(urls: list[str], dest: Path):
    if dest.exists() and dest.stat().st_size > 100000:
        return
    log.info("Downloading AI model...", dest.name)
    for url in urls:
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=60) as resp, open(dest, "wb") as f:
                f.write(resp.read())
            log.info("Successfully downloaded %s ", dest.name, dest.stat().st_size / (1024 * 1024))
            return
        except Exception as e:
            log.warning("Failed download from %s: %s", url, e)
    raise RuntimeError(f"Could not download model {dest.name}. Please check internet connection.")


def ensure_models():
    download_file(YUNET_URLS, YUNET_PATH)
    download_file(SFACE_URLS, SFACE_PATH)


# Database
engine = create_engine(DB_URL, pool_pre_ping=True)
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)


class Base(DeclarativeBase):
    pass


class FaceTemplate(Base):
    __tablename__ = "face_templates"
    user_id = Column(String(36), primary_key=True, nullable=False)
    # Stored as multi-vector string: vector1;vector2;vector3
    embedding = Column(Text, nullable=False)
    enrolled_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())


Base.metadata.create_all(bind=engine)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


DB = Annotated[Session, Depends(get_db)]

# OpenCV AI Singletons
_detector = None
_recognizer = None


def get_detector() -> cv2.FaceDetectorYN:
    global _detector
    if _detector is None:
        ensure_models()
        _detector = cv2.FaceDetectorYN.create(
            model=str(YUNET_PATH),
            config="",
            input_size=(320, 320),
            score_threshold=0.4,
            nms_threshold=0.3,
            top_k=5000,
        )
        log.info("OpenCV YuNet Face Detector loaded.")
    return _detector


def get_recognizer() -> cv2.FaceRecognizerSF:
    global _recognizer
    if _recognizer is None:
        ensure_models()
        _recognizer = cv2.FaceRecognizerSF.create(
            model=str(SFACE_PATH),
            config="",
        )
        log.info("OpenCV SFace Face Recognizer loaded.")
    return _recognizer


# FastAPI
app = FastAPI(title="Face Recognition Service", version="3.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Schemas

class FaceRequest(BaseModel):
    image: str | None = Field(None, description="Base64-encoded image")
    images: list[str] | None = Field(None, description="Multiple base64-encoded images")
    user_id: str | None = None


class DetectResponse(BaseModel):
    detected: bool
    face_count: int
    quality_ok: bool
    liveness_ok: bool
    quality_score: float
    liveness_score: float
    bbox: list[float] | None = None
    message: str


class EnrollResponse(BaseModel):
    success: bool
    message: str
    enrolled_poses: int = 1


class VerifyResponse(BaseModel):
    verified: bool
    similarity: float
    user_id: str | None = None
    message: str


# Utilities

def decode_image(b64: str) -> np.ndarray:
    if "," in b64:
        b64 = b64.split(",", 1)[1]
    raw = base64.b64decode(b64 + "==")
    arr = np.frombuffer(raw, np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if img is None:
        raise ValueError("Cannot decode image bytes")
    return img


def resize_if_needed(img: np.ndarray, max_dim: int = 800) -> np.ndarray:
    """Resize image so the longer side is at most max_dim pixels."""
    h, w = img.shape[:2]
    if max(h, w) <= max_dim:
        return img
    scale = max_dim / max(h, w)
    new_w, new_h = int(w * scale), int(h * scale)
    return cv2.resize(img, (new_w, new_h), interpolation=cv2.INTER_AREA)


def blur_score(gray: np.ndarray) -> float:
    return float(cv2.Laplacian(gray, cv2.CV_64F).var())


def vector_to_str(emb: np.ndarray) -> str:
    return ",".join(f"{v:.6f}" for v in emb.flatten())


def str_to_vector(s: str) -> np.ndarray:
    return np.array([float(x) for x in s.split(",")], dtype=np.float32)


def multi_vector_to_str(embeddings: list[np.ndarray]) -> str:
    return ";".join(vector_to_str(e) for e in embeddings)


def str_to_multi_vectors(s: str) -> list[np.ndarray]:
    parts = [p.strip() for p in s.split(";") if p.strip()]
    return [str_to_vector(p) for p in parts]


def run_pipeline(img: np.ndarray):
    """
    Runs YuNet detection + SFace feature extraction.
    Returns: (face_box, feature_128d, quality_score, liveness_score)
    """
    img = resize_if_needed(img, 640)
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    liveness = blur_score(gray)

    h, w, _ = img.shape
    detector = get_detector()
    detector.setInputSize((w, h))
    _, faces = detector.detect(img)

    if faces is None or len(faces) == 0:
        return None, None, 0.0, liveness

    # Pick the largest face in case of multiple people
    face = max(faces, key=lambda f: float(f[2] * f[3]))
    x, y, fw, fh = int(face[0]), int(face[1]), int(face[2]), int(face[3])
    x, y = max(0, x), max(0, y)
    crop = gray[y : y + fh, x : x + fw]
    quality = blur_score(crop) if crop.size > 0 else liveness

    recognizer = get_recognizer()
    aligned_face = recognizer.alignCrop(img, face)
    feature = recognizer.feature(aligned_face)
    if feature is not None:
        feature = feature.flatten()

    bbox = [float(face[0]), float(face[1]), float(face[2]), float(face[3])]
    return bbox, feature, quality, liveness


# Endpoints
@app.get("/health")
def health():
    return {
        "status": "ok",
        "engine": "OpenCV YuNet + SFace",
        "python": "3.13 Ready",
        "threshold": SIMILARITY_THRESHOLD,
    }


@app.get("/face/enrolled/{user_id}")
def check_enrolled(user_id: str, db: DB):
    tmpl = db.get(FaceTemplate, user_id)
    return {"enrolled": tmpl is not None}


@app.post("/face/detect", response_model=DetectResponse)
def detect(req: FaceRequest):
    """Real-time live preview feedback."""
    if not req.image:
        raise HTTPException(status_code=400, detail="Image is required")

    try:
        img = decode_image(req.image)
        bbox, feature, quality, liveness = run_pipeline(img)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc))

    if bbox is None:
        return DetectResponse(
            detected=False,
            face_count=0,
            quality_ok=False,
            liveness_ok=False,
            quality_score=0.0,
            liveness_score=round(liveness, 2),
            message="Không tìm thấy khuôn mặt — vui lòng nhìn vào camera",
        )

    quality_ok = quality >= QUALITY_THRESHOLD
    liveness_ok = liveness >= LIVENESS_THRESHOLD
    msg = "Sẵn sàng nhận diện" if (quality_ok and liveness_ok) else (
        "Ảnh hơi mờ — giữ yên khuôn mặt" if not quality_ok else "Đang kiểm tra liveness"
    )

    return DetectResponse(
        detected=True,
        face_count=1,
        quality_ok=quality_ok,
        liveness_ok=liveness_ok,
        quality_score=round(quality, 2),
        liveness_score=round(liveness, 2),
        bbox=bbox,
        message=msg,
    )


@app.post("/face/enroll", response_model=EnrollResponse)
def enroll(req: FaceRequest, db: DB):
    """
    Enroll face template for user_id.
    Accepts single image or 3-pose images list (straight, left, right).
    """
    if not req.user_id:
        raise HTTPException(status_code=400, detail="user_id is required")

    raw_images = req.images if req.images and len(req.images) > 0 else ([req.image] if req.image else [])
    if not raw_images:
        raise HTTPException(status_code=400, detail="Ít nhất một ảnh khuôn mặt là bắt buộc")

    embeddings: list[np.ndarray] = []
    for idx, img_b64 in enumerate(raw_images):
        try:
            img = decode_image(img_b64)
            bbox, feature, quality, liveness = run_pipeline(img)
            log.info(
                "Frame %d: bbox=%s, quality=%.1f (need>=%.1f), liveness=%.1f (need>=%.1f), feature=%s",
                idx + 1, bbox, quality, QUALITY_THRESHOLD, liveness, LIVENESS_THRESHOLD,
                "OK" if feature is not None else "NONE",
            )
            if feature is not None:
                embeddings.append(feature)
                log.info("Frame %d enrolled successfully", idx + 1)
            else:
                log.warning("Frame %d: no face detected", idx + 1, bbox)
        except Exception as e:
            log.warning("Frame %d enrollment error: %s", idx + 1, e)

    if not embeddings:
        log.error("Enrollment FAILED for user=%s: 0/%d frames had detectable faces", req.user_id, len(raw_images))
        raise HTTPException(status_code=422, detail="Không nhận diện được khuôn mặt rõ nét trong ảnh chụp. Vui lòng đảm bảo ánh sáng tốt và khuôn mặt rõ ràng.")

    template_str = multi_vector_to_str(embeddings)
    existing = db.get(FaceTemplate, req.user_id)
    if existing:
        existing.embedding = template_str
    else:
        db.add(FaceTemplate(user_id=req.user_id, embedding=template_str))
    db.commit()

    log.info("Enrolled user=%s with %d poses into database", req.user_id, len(embeddings))
    return EnrollResponse(
        success=True,
        message=f"Đăng ký khuôn mặt thành công ({len(embeddings)} góc nhận diện)",
        enrolled_poses=len(embeddings),
    )


@app.post("/face/verify", response_model=VerifyResponse)
def verify(req: FaceRequest, db: DB):
    """
    Instant verify against multi-angle templates.
    Computes SFace cosine similarity across all stored angles and takes max(similarity).
    """
    if not req.image:
        raise HTTPException(status_code=400, detail="Image is required")

    try:
        t0 = time.time()
        img = decode_image(req.image)
        _, probe_feature, quality, liveness = run_pipeline(img)
        dt = (time.time() - t0) * 1000.0
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc))

    if probe_feature is None:
        return VerifyResponse(verified=False, similarity=0.0, message="Không nhận diện được khuôn mặt")

    recognizer = get_recognizer()
    probe_mat = probe_feature.reshape(1, -1)

    # 1:1 Verification
    if req.user_id:
        tmpl = db.get(FaceTemplate, req.user_id)
        if tmpl is None:
            raise HTTPException(status_code=404, detail="Người dùng chưa đăng ký Face ID")

        stored_vectors = str_to_multi_vectors(tmpl.embedding)
        similarities = [
            float(recognizer.match(probe_mat, sv.reshape(1, -1), cv2.FaceRecognizerSF_FR_COSINE))
            for sv in stored_vectors
        ]
        max_sim = max(similarities) if similarities else 0.0
        similarity = round(max(0.0, min(1.0, max_sim)), 4)
        verified = similarity >= SIMILARITY_THRESHOLD

        log.info(
            "Verify 1:1 user=%s in %.1fms | max_sim=%.1f%% across %d poses -> %s",
            req.user_id, dt, similarity * 100, len(stored_vectors), "PASS" if verified else "FAIL"
        )
        return VerifyResponse(
            verified=verified,
            similarity=similarity,
            user_id=req.user_id,
            message="Xác thực thành công" if verified else f"Khuôn mặt không khớp (độ tương đồng {similarity:.1%})",
        )

    # 1:N Identification (Automatic user detection)
    all_templates = db.query(FaceTemplate).all()
    if not all_templates:
        return VerifyResponse(verified=False, similarity=0.0, message="Chưa có dữ liệu Face ID trong hệ thống")

    best_user_id = None
    best_similarity = -1.0

    for tmpl in all_templates:
        stored_vectors = str_to_multi_vectors(tmpl.embedding)
        for sv in stored_vectors:
            sim = float(recognizer.match(probe_mat, sv.reshape(1, -1), cv2.FaceRecognizerSF_FR_COSINE))
            if sim > best_similarity:
                best_similarity = sim
                best_user_id = tmpl.user_id

    similarity = round(max(0.0, min(1.0, best_similarity)), 4)
    verified = similarity >= SIMILARITY_THRESHOLD

    log.info(
        "Verify 1:N in %.1fms | best_user=%s sim=%.1f%% -> %s",
        dt, best_user_id, similarity * 100, "PASS" if verified else "FAIL"
    )
    return VerifyResponse(
        verified=verified,
        similarity=similarity,
        user_id=best_user_id if verified else None,
        message="Xác thực thành công" if verified else "Không tìm thấy người dùng phù hợp",
    )
