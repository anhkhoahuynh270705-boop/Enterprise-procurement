export interface FaceLoginRequest {
  username?: string;
  image: string;
}

export interface FaceEnrollRequest {
  username: string;
  image?: string;
  images?: string[];
}

export interface EnrolledStatusResponse {
  enrolled: boolean;
}

export interface DetectResponse {
  detected: boolean;
  face_count: number;
  quality_ok: boolean;
  liveness_ok: boolean;
  quality_score: number;
  liveness_score: number;
  bbox: number[] | null;
  message: string;
}