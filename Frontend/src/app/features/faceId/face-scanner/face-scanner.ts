import {
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
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FaceAuthService } from '../services/face-auth.service';
import { ToastService } from '../../../core/services/toast.service';

type ScanPhase = 'scanning' | 'matched' | 'error';

/** Interval giữa các lần quét (ms) — tăng lên 400ms để giảm tải */
const SCAN_INTERVAL_MS = 400;

@Component({
  selector: 'app-face-scanner',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './face-scanner.html',
  styleUrl: './face-scanner.scss',
})
export class FaceScannerComponent implements OnInit, OnDestroy {
  @ViewChild('videoElement')
  videoRef?: ElementRef<HTMLVideoElement>;

  @Input()
  initialUsername = '';

  @Output()
  closeDialog = new EventEmitter<void>();

  @Output()
  loginSuccess = new EventEmitter<void>();

  private readonly faceAuth = inject(FaceAuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly zone = inject(NgZone);

  username = '';
  phase: ScanPhase = 'scanning';
  hintText = 'Hướng mặt vào camera';
  cameraError: string | null = null;
  /** Trạng thái khuôn mặt để hiện thị trên UI */
  faceDetected = false;

  private stream?: MediaStream;
  private scanInterval?: number;
  private isRequestInFlight = false;
  private currentFacingMode: 'user' | 'environment' = 'user';
  /** Timestamp each login to avoid spam 401 */
  private lastFailedAt = 0;
  private readonly FAIL_COOLDOWN_MS = 1500;

  get verified(): boolean {
    return this.phase === 'matched';
  }

  ngOnInit(): void {
    this.username = this.initialUsername || '';
    void this.initCamera();
  }

  ngOnDestroy(): void {
    this.cleanup();
  }

  async initCamera(): Promise<void> {
    this.cameraError = null;
    this.cleanup();
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        video: {
          width: { ideal: 640 },
          height: { ideal: 480 },
          facingMode: this.currentFacingMode,
        },
        audio: false,
      });
      if (this.videoRef?.nativeElement) {
        this.videoRef.nativeElement.srcObject = this.stream;
        await this.videoRef.nativeElement.play();
        this.startScanLoop();
      }
    } catch {
      this.cameraError = 'Không thể truy cập máy ảnh. Vui lòng cấp quyền trong trình duyệt.';
    }
  }

  private startScanLoop(): void {
    this.scanInterval = window.setInterval(() => this.scanTick(), SCAN_INTERVAL_MS);
  }

  private scanTick(): void {
    if (this.isRequestInFlight || this.phase !== 'scanning') return;
    if (Date.now() - this.lastFailedAt < this.FAIL_COOLDOWN_MS) return;

    const video = this.videoRef?.nativeElement;
    if (!video || video.readyState < 2) return;

    const frame = this.faceAuth.captureFrame(video, 360, 360);
    if (!frame) return;

    this.isRequestInFlight = true;

    // Bước 1: Kiểm tra chất lượng khuôn mặt qua Python FaceService
    this.faceAuth.detectFace(frame).subscribe({
      next: (res) => {
        const faceOk = res.detected && res.quality_ok && res.liveness_ok;
        this.zone.run(() => {
          this.faceDetected = faceOk;
        });

        if (!faceOk) {
          /* Face does not match quality requirements, show message and return */
          this.zone.run(() => {
            this.hintText = res.message || 'Hướng mặt vào camera';
            this.isRequestInFlight = false;
          });
          return;
        }
        this.zone.run(() => {
          this.hintText = 'Đang xác thực…';
        });

        this.faceAuth.loginWithFace(frame, this.username || undefined).subscribe({
          next: (profile) => {
            this.isRequestInFlight = false;
            if (profile) {
              this.zone.run(() => {
                this.phase = 'matched';
                this.cleanup();
                this.toast.success('Đăng nhập Face ID thành công!');
                setTimeout(() => {
                  this.zone.run(() => {
                    this.loginSuccess.emit();
                    void this.router.navigate(['/dashboard']);
                  });
                }, 400);
              });
            }
          },
          error: () => {
            this.isRequestInFlight = false;
            this.lastFailedAt = Date.now();
            this.zone.run(() => {
              this.faceDetected = false;
              this.hintText = 'Đang quét…';
            });
          },
        });
      },
      error: () => {
        this.isRequestInFlight = false;
        this.zone.run(() => {
          this.faceDetected = false;
        });
      },
    });
  }

  async switchCamera(): Promise<void> {
    this.currentFacingMode = this.currentFacingMode === 'user' ? 'environment' : 'user';
    this.phase = 'scanning';
    await this.initCamera();
  }

  close(): void {
    this.cleanup();
    this.closeDialog.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) this.close();
  }

  private cleanup(): void {
    if (this.scanInterval !== undefined) {
      clearInterval(this.scanInterval);
      this.scanInterval = undefined;
    }
    this.isRequestInFlight = false;
    if (this.stream) {
      this.stream.getTracks().forEach((t) => t.stop());
      this.stream = undefined;
    }
    if (this.videoRef?.nativeElement) {
      this.videoRef.nativeElement.srcObject = null;
    }
  }
}
