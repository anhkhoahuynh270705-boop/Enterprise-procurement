import { ToastService } from '../../../core/services/toast.service';
import {
  Component,
  OnInit,
  inject,
  ElementRef,
  ViewChild,
  AfterViewInit,
  PLATFORM_ID,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../services/auth.service';
@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login implements OnInit, AfterViewInit {
  @ViewChild('bgVideo') bgVideo?: ElementRef<HTMLVideoElement>;
  private readonly platform = inject(PLATFORM_ID);
  readonly auth = inject(AuthService);
  readonly failed = inject(ActivatedRoute).snapshot.queryParamMap.has('error');
  private readonly toast = inject(ToastService);
  ngOnInit(): void {
    if (this.failed) this.toast.error('Không thể hoàn tất đăng nhập. Vui lòng thử lại.');
  }
  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platform) && this.bgVideo?.nativeElement) {
      this.bgVideo.nativeElement.muted = true;
      void this.bgVideo.nativeElement.play().catch(() => {});
    }
  }
}
