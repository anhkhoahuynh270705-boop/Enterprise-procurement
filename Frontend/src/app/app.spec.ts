import { vi, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Login } from './features/auth/login/login';
import { App } from './app';

describe('App', () => {
  afterEach(() => vi.restoreAllMocks());
  it('creates the application shell', () => {
    TestBed.configureTestingModule({ imports: [App], providers: [provideRouter([])] });
    expect(TestBed.createComponent(App).componentInstance).toBeTruthy();
  });
  it('renders Keycloak sign-in without public registration or a local password form', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideRouter([{ path: 'login', component: Login }])] });
    vi.spyOn(HTMLMediaElement.prototype, 'play').mockResolvedValue();
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/login', Login);
    const page = harness.routeNativeElement!;
    expect(page.querySelector('a.btn-login')?.getAttribute('href')).toContain('/api/auth/authorize');
    expect(page.querySelector('a[href="/register"]')).toBeNull();
    expect(page.querySelector('input[type="password"]')).toBeNull();
  });
});