import { roleGuard } from './core/guards/role.guard';
import { Forbidden } from './core/forbidden/forbidden';
import { Routes } from '@angular/router';
import { CreateUser } from './features/users/create-user/create-user';
import { UserList } from './features/users/user-list/user-list';
import { Login } from './features/auth/login/login';
import { AuthCallback } from './features/auth/callback/callback';
import { ForgotPassword } from './features/auth/forgot-password/forgot-password';
import { authGuard } from './core/guards/auth.guard';
import { EditUser } from './features/users/edit-user/edit-user';
import { ProcurementList } from './features/procurement/procurement-list/procurement-list';
import { ProcurementCreate } from './features/procurement/procurement-create/procurement-create';
import { ProcurementEdit } from './features/procurement/procurement-edit/procurement-edit';
import { ProcurementDetail } from './features/procurement/procurement-detail/procurement-detail';
import { PurchaseOrderList } from './features/purchase-orders/purchase-order-list/purchase-order-list';
import { Dashboard } from './features/dashboard/dashboard-view/dashboard';
import { AdminLayout } from './layout/admin-layout/admin-layout';
import { SupplierList } from './features/suppliers/supplier-list/supplier-list';
import { SupplierCreate } from './features/suppliers/supplier-create/supplier-create';
import { SupplierEdit } from './features/suppliers/supplier-edit/supplier-edit';
import { SupplierProposals } from './features/suppliers/supplier-proposals/supplier-proposals';
import { SupplierDocuments } from './features/suppliers/supplier-documents/supplier-documents';
import { CompanyDocuments } from './features/company/company-documents/company-documents';
import { ActivityLog } from './features/admin/activity-log/activity-log';
import { RfqPage } from './features/rfq/rfq-list/rfq-page';
import { SupplierRfqResponsePage } from './features/rfq/supplier-response/supplier-list-response/supplier-rfq-response';

export const routes: Routes = [
  { path: 'forbidden', component: Forbidden },
  {
    path: 'login',
    component: Login,
  },
  {
    path: 'auth/callback',
    component: AuthCallback,
  },
  {
    path: 'forgot-password',
    component: ForgotPassword,
  },
  {
    path: 'supplier/rfq/respond',
    component: SupplierRfqResponsePage,
  },

  {
    path: '',
    component: AdminLayout,
    canActivate: [authGuard],
    children: [
      {
        path: 'company-documents',
        component: CompanyDocuments,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'USER', 'CHECKER'] },
      },
      {
        path: 'dashboard',
        component: Dashboard,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
      },
      {
        path: 'users',
        component: UserList,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
      },
      {
        path: 'activity-logs',
        component: ActivityLog,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
      },
      {
        path: 'user/create',
        component: CreateUser,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
      },
      {
        path: 'user/edit/:id',
        component: EditUser,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
      },
      {
        path: 'purchase-orders',
        component: PurchaseOrderList,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'USER', 'CHECKER'] },
      },
      {
        path: 'procurement/approvals',
        component: ProcurementList,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'CHECKER'], view: 'approvals' },
      },
      {
        path: 'procurement/rfq',
        component: RfqPage,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'USER', 'CHECKER'] },
      },
      {
        path: 'procurement',
        component: ProcurementList,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'USER', 'CHECKER'] },
        pathMatch: 'full',
      },
      {
        path: 'procurement/create',
        component: ProcurementCreate,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'USER'] },
      },
      {
        path: 'procurement/edit/:id',
        component: ProcurementEdit,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'USER'] },
      },
      {
        path: 'procurement/:id',
        component: ProcurementDetail,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'USER', 'CHECKER'] },
      },
      {
        path: 'supplier-proposals',
        component: SupplierProposals,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'USER', 'CHECKER'] },
      },
      {
        path: 'suppliers',
        component: SupplierList,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
      },
      {
        path: 'suppliers/documents',
        component: SupplierDocuments,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'USER', 'CHECKER'] },
      },
      {
        path: 'suppliers/contracts',
        loadComponent: () =>
          import('./features/suppliers/supplier-contracts/supplier-contracts').then(
            (module) => module.SupplierContracts,
          ),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'CHECKER'] },
      },
      {
        path: 'supplier/create',
        component: SupplierCreate,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
      },
      {
        path: 'supplier/edit/:id',
        component: SupplierEdit,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full',
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
