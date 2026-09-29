import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, of, switchMap } from 'rxjs';
import { AuthService } from '../../features/auth/services/auth.service';
import { AppRole } from '../../features/auth/model/auth';

export const roleGuard: CanActivateFn = route => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const allowed = route.data['roles'] as AppRole[] | undefined;
  return auth.restoreSession().pipe(
    switchMap(loggedIn => loggedIn ? auth.loadCurrentProfile() : of(null)),
    map(profile => {
      if (!profile?.role) 
        return router.createUrlTree(['/forbidden']);
      if (allowed?.includes(profile.role)) 
        return true;
      return router.createUrlTree(['/procurement']);
    })
  );
};
