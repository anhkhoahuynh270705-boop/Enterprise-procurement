import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  OnDestroy,
  OnInit,
  Output,
  ViewChild,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FaceAuthService } from '../services/face-auth.service';
import { ToastService } from '../../../core/services/toast.service';

type EnrollPhase =
  | 'detecting' // waiting for a clear face
  | 'countdown' // face detected – counting down before capture
  | 'captured' // photo taken – move to next step
  | 'saving' // uploading to backend
  | 'done'; // success

const STEP_LABELS = [
  'Nhìn thẳng vào camera',
  'Quay nhẹ mặt sang bên TRÁI',
  'Quay nhẹ mặt sang bên PHẢI',
];
const COUNTDOWN_SEC = 2; // seconds to count down before auto-capture
const SCAN_INTERVAL_MS = 250; // detect every 250ms

@Component({
  selector: 'app-face-enroll',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './face-enroll.html',
  styleUrl: './face-enroll.scss',
})
export class FaceEnrollComponent implements OnInit, OnDestroy {
  @ViewChild('videoElement')
  videoRef?: ElementRef<HTMLVideoElement>;

  @Input({ required: true })
  username!: string;

  @Output() closeDialog = new EventEmitter<void>();

  @Output() enrolledSuccess = new EventEmitter<void>();

  private readonly faceAuth = inject(FaceAuthService);
  private readonly toast = inject(ToastService);
  private readonly zone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);

  private stream?: MediaStream;
  private scanTimer?: number;
  private countdownTimer?: number;
  private isScanInFlight = false;

  currentStep = 0; // 0-indexed (0=straight, 1=left, 2=right)
  phase: EnrollPhase = 'detecting';
  countdownValue = COUNTDOWN_SEC;
  detectionMsg = 'Đang tìm khuôn mặt…';
  faceDetected = false;
  capturedImages: string[] = [];

  // Thumbnails for each step
  readonly thumbs: (string | null)[] = [null, null, null];

  get stepLabel(): string {
    return STEP_LABELS[this.currentStep] ?? '';
  }
  get isLastStep(): boolean {
    return this.currentStep === 2;
  }

  ngOnInit(): void {
    void this.initCamera();
  }
  ngOnDestroy(): void {
    this.cleanup();
  }

  async initCamera(): Promise<void> {
    this.cleanup();
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        video: { width: { ideal: 640 }, height: { ideal: 480 }, facingMode: 'user' },
        audio: false,
      });
      if (this.videoRef?.nativeElement) {
        this.videoRef.nativeElement.srcObject = this.stream;
        await this.videoRef.nativeElement.play();
        this.startScanLoop();
      }
    } catch {
      this.toast.error('Không thể truy cập máy ảnh. Vui lòng kiểm tra quyền thiết bị.');
    }
  }

  private startScanLoop(): void {
    if (this.scanTimer !== undefined) return;
    this.scanTimer = window.setInterval(
      () => this.zone.run(() => this.scanTick()),
      SCAN_INTERVAL_MS,
    );
  }

  private scanTick(): void {
    if (
      this.isScanInFlight ||
      this.phase === 'captured' ||
      this.phase === 'saving' ||
      this.phase === 'done'
    )
      return;
    const video = this.videoRef?.nativeElement;
    if (!video || video.readyState < 2) return;

    const frame = this.faceAuth.captureFrame(video, 360, 360);
    if (!frame) return;

    this.isScanInFlight = true;
    this.faceAuth.detectFace(frame).subscribe({
      next: (res) => {
        this.zone.run(() => {
          this.isScanInFlight = false;
          this.handleDetection(res.detected && res.quality_ok && res.liveness_ok, res.message);
        });
      },
      error: () => {
        this.zone.run(() => {
          this.isScanInFlight = false;
        });
      },
    });
  }

  private handleDetection(faceOk: boolean, msg: string): void {
    if (this.phase === 'captured' || this.phase === 'saving' || this.phase === 'done') return;

    this.faceDetected = faceOk;

    if (!faceOk) {
      if (this.phase === 'countdown') {
        this.cancelCountdown();
        this.phase = 'detecting';
      }
      this.detectionMsg = msg || 'Đang tìm khuôn mặt…';
      this.cdr.detectChanges();
      return;
    }
    this.detectionMsg = 'Khuôn mặt đã sẵn sàng!';
    if (this.phase === 'detecting') {
      this.phase = 'countdown';
      this.countdownValue = COUNTDOWN_SEC;
      this.runCountdown();
    }
    this.cdr.detectChanges();
  }

  private runCountdown(): void {
    this.countdownTimer = window.setInterval(() => {
      this.zone.run(() => {
        this.countdownValue--;
        this.cdr.detectChanges();
        if (this.countdownValue <= 0) {
          this.cancelCountdown();
          this.autoCaptureStep();
        }
      });
    }, 1000);
  }

  private cancelCountdown(): void {
    if (this.countdownTimer !== undefined) {
      clearInterval(this.countdownTimer);
      this.countdownTimer = undefined;
    }
  }

  private autoCaptureStep(): void {
    const video = this.videoRef?.nativeElement;
    if (!video) return;
    const frame = this.captureForEnroll(video);
    if (!frame) {
      this.phase = 'detecting';
      this.cdr.detectChanges();
      return;
    }

    this.thumbs[this.currentStep] = frame;
    this.capturedImages.push(frame);
    this.phase = 'captured';
    this.cdr.detectChanges();

    if (this.isLastStep) {
      this.stopScanLoop();
      this.cdr.detectChanges();
      setTimeout(() => this.zone.run(() => this.submitEnrollment()), 600);
    } else {
      setTimeout(() => {
        this.zone.run(() => {
          this.currentStep++;
          this.phase = 'detecting';
          this.faceDetected = false;
          this.detectionMsg = 'Đang tìm khuôn mặt…';
          this.cdr.detectChanges();
        });
      }, 1000);
    }
  }

  private stopScanLoop(): void {
    if (this.scanTimer !== undefined) {
      clearInterval(this.scanTimer);
      this.scanTimer = undefined;
    }
    this.isScanInFlight = false;
  }

  /**
   * Capture enrollment image: center-crop at 640×640
   * but larger resolution, so YuNet gets a clear face for feature extraction).
   */
  private captureForEnroll(video: HTMLVideoElement): string | null {
    if (!video || video.videoWidth === 0 || video.videoHeight === 0) 
      return null;
    const canvas = document.createElement('canvas');
    const size = 640;
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d');
    if (!ctx) 
      return null;
    const vw = video.videoWidth;
    const vh = video.videoHeight;
    const minDim = Math.min(vw, vh);
    ctx.drawImage(video, (vw - minDim) / 2, (vh - minDim) / 2, minDim, minDim, 0, 0, size, size);
    const dataUrl = canvas.toDataURL('image/jpeg', 0.88);
    console.log(
      `[FaceEnroll] Step ${this.currentStep + 1} image: ~${Math.round((dataUrl.length * 0.75) / 1024)}KB`,
    );
    return dataUrl;
  }

  private submitEnrollment(): void {
    this.phase = 'saving';
    this.cdr.detectChanges();
    this.faceAuth.enrollFace(this.username, this.capturedImages).subscribe({
      next: (resp) => {
        this.zone.run(() => {
          this.phase = 'done';
          this.cdr.detectChanges();
          this.toast.success(resp.message || 'Đăng ký Face ID thành công!');
          setTimeout(() => {
            this.zone.run(() => {
              this.cleanup();
              this.enrolledSuccess.emit();
              this.closeDialog.emit();
            });
          }, 800);
        });
      },
      error: (err) => {
        console.error('[FaceEnroll] enroll error:', err.status, err.error);
        this.zone.run(() => {
          this.phase = 'detecting';
          this.currentStep = 0;
          this.capturedImages = [];
          this.thumbs.fill(null);
          this.faceDetected = false;
          this.detectionMsg = 'Đang tìm khuôn mặt…';
          this.startScanLoop();
          const msg =
            err.error?.message ?? err.error?.detail ?? 'Đăng ký thất bại. Vui lòng thử lại.';
          this.toast.error(msg);
          this.cdr.detectChanges();
        });
      },
    });
  }

  resetAll(): void {
    this.cancelCountdown();
    this.currentStep = 0;
    this.phase = 'detecting';
    this.faceDetected = false;
    this.detectionMsg = 'Đang tìm khuôn mặt…';
    this.capturedImages = [];
    this.thumbs.fill(null);
  }

  close(): void {
    this.cleanup();
    this.closeDialog.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) 
      this.close();
  }

  private cleanup(): void {
    if (this.scanTimer !== undefined) {
      clearInterval(this.scanTimer);
      this.scanTimer = undefined;
    }
    this.cancelCountdown();
    this.isScanInFlight = false;
    if (this.stream) {
      this.stream.getTracks().forEach((t) => t.stop());
      this.stream = undefined;
    }
    if (this.videoRef?.nativeElement) 
      this.videoRef.nativeElement.srcObject = null;
  }
}
