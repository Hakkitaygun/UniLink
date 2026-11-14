#!/usr/bin/env python3
r"""UniLink ML - ADIM 6: FastAPI anomali skorlama servisi (hibrit skorun canlı ML tarafı).

Backend (Java) her (ip, dakika) penceresi için 10 özelliği JSON olarak gönderir; bu servis
kaydedilmiş kNN modeliyle ml_score (0-1) döndürür. Hibrit riski Java hesaplar:
    risk = min(100, kuralSkoru + 0.4 * ml_score * 100)
Servis erişilemezse Java yalnızca kural skoruna düşer (fallback) - bu yüzden servis
"durum bilgisiz" ve hızlıdır.

Özellik hesabını SERVİS DEĞİL Java yapar (canlı security_events'ten); böylece ML yüzeyi
tek bir sözleşmeye (10 sayı) indirgenir. Java/Python özellik tutarlılığı ayrı "altın test"
dosyasıyla doğrulanacak.

Çalıştırma (backend\ml içinden):
    python -m pip install fastapi uvicorn
    uvicorn ml_service:app --host 127.0.0.1 --port 8000
Model: models/knn_model.joblib (train_model.py üretir).

Uçlar:
    GET  /health              -> model yüklü mü, eşik ve özellik sırası
    POST /score   {özellikler} -> tek pencere skoru
    POST /score/batch [{..},..] -> çoklu pencere (altın test ve toplu işlem için)
"""
import os
from typing import List
import numpy as np
import joblib
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

MODEL_PATH = os.environ.get("UNILINK_ML_MODEL", "models/knn_model.joblib")

app = FastAPI(title="UniLink ML Scoring", version="1.0")
_model = None

def model():
    global _model
    if _model is None:
        if not os.path.exists(MODEL_PATH):
            raise HTTPException(503, f"Model yok: {MODEL_PATH}. Önce train_model.py çalıştır.")
        _model = joblib.load(MODEL_PATH)
    return _model

class Features(BaseModel):
    req_count: float = Field(ge=0)
    distinct_paths: float = Field(ge=0)
    err_4xx_ratio: float = Field(ge=0, le=1)
    c401: float = Field(ge=0)
    c403: float = Field(ge=0)
    c404: float = Field(ge=0)
    login_fail_count: float = Field(ge=0)
    distinct_login_ids: float = Field(ge=0)
    write_ratio: float = Field(ge=0, le=1)
    admin_path_hits: float = Field(ge=0)

def score_many(items: List[Features]):
    m = model()
    order = m["features"]
    X = np.array([[getattr(it, f) for f in order] for it in items], float)
    X[:, m["count_cols_log1p"]] = np.log1p(X[:, m["count_cols_log1p"]])
    dist = m["nn"].kneighbors(m["scaler"].transform(X))[0].mean(axis=1)
    thr, scale = m["threshold"], m["scale"]
    ml = np.clip((dist - thr) / scale, 0.0, 1.0)          # train_model.py ile aynı eşleme
    out = []
    for d, s in zip(dist, ml):
        out.append({
            "ml_score": round(float(s), 4),               # hibritte kullanılan [0,1] skor
            "distance": round(float(d), 4),
            "threshold": round(float(thr), 4),
            "is_anomaly": bool(d >= thr),
            "risk_contribution": round(float(0.4 * s * 100), 2),   # hibride katkı (0-40), bilgi amaçlı
        })
    return out

@app.get("/health")
def health():
    m = model()
    return {"status": "ok", "model": "knn", "threshold": round(float(m["threshold"]), 4),
            "features": m["features"]}

@app.post("/score")
def score(f: Features):
    return score_many([f])[0]

@app.post("/score/batch")
def score_batch(items: List[Features]):
    return score_many(items)