import { SensitiveValue } from '../../../shared/sensitive-value/sensitive-value';
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  PLATFORM_ID,
  ViewChild,
  inject,
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { DashboardFilters, DashboardSummaryItem } from '../model/dashboard.model';
import { DashboardService } from '../services/dashboard.service';
import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  ChartConfiguration,
  DoughnutController,
  Legend,
  LinearScale,
  Tooltip,
} from 'chart.js';
import { ProcurementStatus, ProcurementTicket } from '../../procurement/model/procurement';

Chart.register(
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  DoughnutController,
  Legend,
  LinearScale,
  Tooltip,
);

@Component({
  selector: 'app-dashboard',
  imports: [SensitiveValue, CommonModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit, AfterViewInit, OnDestroy {
  private readonly dashboardService = inject(DashboardService);
  private request?: Subscription;
  filters: DashboardFilters = {};
  currency = 'VND';
  currencies: string[] = ['VND'];
  readonly statusKeys = ['APPROVED', 'PENDING_APPROVAL', 'REJECTED', 'DRAFT', 'CANCELLED'];
  readonly filterNames: Record<string, string> = {
    supplier: 'Nhà cung cấp',
    product: 'Sản phẩm',
    department: 'Phòng ban',
    status: 'Trạng thái',
    month: 'Tháng',
  };
  get activeFilters() {
    return Object.entries(this.filters);
  }
  supplierSummary: DashboardSummaryItem[] = [];
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly charts: Chart[] = [];
  private viewReady = false;

  @ViewChild('supplierChart') supplierChart?: ElementRef<HTMLCanvasElement>;
  @ViewChild('statusChart') statusChart?: ElementRef<HTMLCanvasElement>;
  @ViewChild('monthlyChart') monthlyChart?: ElementRef<HTMLCanvasElement>;
  @ViewChild('departmentChart') departmentChart?: ElementRef<HTMLCanvasElement>;

  readonly currentDate = new Date();
  readonly statusLabels = ['Đã phê duyệt', 'Chờ phê duyệt', 'Từ chối', 'Bản nháp', 'Đã hủy'];

  totalUsers = 0;
  activeUsers = 0;
  totalTickets = 0;
  pendingTickets = 0;
  approvedTickets = 0;
  rejectedTickets = 0;
  totalProcurementAmount = 0;
  approvalRate = 0;
  statusCounts = [0, 0, 0, 0, 0];
  departmentSummary: DashboardSummaryItem[] = [];
  monthlySummary: DashboardSummaryItem[] = [];
  recentTickets: ProcurementTicket[] = [];
  isLoading = true;
  hasLoadError = false;

  ngOnInit(): void {
    this.loadStats();
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.renderCharts();
  }

  ngOnDestroy(): void {
    this.request?.unsubscribe();
    this.destroyCharts();
  }

  loadStats(): void {
    this.isLoading = true;
    this.hasLoadError = false;

    this.request?.unsubscribe();
    this.destroyCharts();
    this.request = this.dashboardService.getReport(this.filters, this.currency).subscribe({
      next: (report) => {
        Object.assign(this, report);
        this.isLoading = false;
        this.cdr.detectChanges();
        this.renderCharts();
      },
      error: () => {
        this.isLoading = false;
        this.hasLoadError = true;
        this.cdr.detectChanges();
      },
    });
  }

  selectFilter(field: string, value: string): void {
    const key = field as keyof DashboardFilters;
    this.filters = { ...this.filters };
    if (this.filters[key] === value) delete this.filters[key];
    else this.filters[key] = value;
    this.loadStats();
  }

  clearFilters(): void {
    this.filters = {};
    this.loadStats();
  }

  filterLabel(field: string, value: string): string {
    if (field === 'status') return this.getStatusLabel(value as ProcurementStatus);
    if (field === 'product') return value.replace(/^(code|name):/, '');
    return value;
  }

  changeCurrency(value: string): void {
    this.currency = value;
    this.loadStats();
  }

  productKey(item: { itemCode?: string; itemName: string }): string {
    return item.itemCode?.trim() ? 'code:' + item.itemCode.trim() : 'name:' + item.itemName.trim();
  }

  suppliers(ticket: ProcurementTicket): string[] {
    return [...new Set(ticket.items.map((item) => item.supplierName?.trim() || 'Chưa xác định'))];
  }

  getStatusLabel(status: ProcurementStatus): string {
    const labels: Record<ProcurementStatus, string> = {
      DRAFT: 'DRAFT',
      PENDING_APPROVAL: 'PENDING_APPROVAL',
      APPROVED: 'APPROVED',
      REJECTED: 'REJECTED',
      CANCELLED: 'CANCELLED',
    };
    return labels[status];
  }

  private renderCharts(): void {
    if (!this.isBrowser || !this.viewReady || this.isLoading || this.hasLoadError) return;
    this.destroyCharts();
    this.createStatusChart();
    this.createMonthlyChart();
    this.createDepartmentChart();
    this.createSupplierChart();
  }

  private createStatusChart(): void {
    if (!this.statusChart) return;
    this.charts.push(
      new Chart(this.statusChart.nativeElement, {
        type: 'doughnut',
        data: {
          labels: this.statusLabels,
          datasets: [
            {
              data: this.statusCounts,
              backgroundColor: ['#118dff', '#f2c80f', '#e66c37', '#6b7280', '#a5a5a5'],
              borderColor: '#ffffff',
              borderWidth: 3,
              hoverOffset: 5,
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          cutout: '68%',
          onClick: (_, elements) => {
            if (elements.length) this.selectFilter('status', this.statusKeys[elements[0].index]);
          },
          plugins: {
            legend: {
              position: 'bottom',
              labels: { usePointStyle: true, boxWidth: 8, padding: 14 },
            },
          },
        },
      }),
    );
  }

  private createMonthlyChart(): void {
    if (!this.monthlyChart) return;
    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels: this.monthlySummary.map((item) => item.label),
        datasets: [
          {
            label: 'Giá trị mua sắm',
            data: this.monthlySummary.map((item) => item.value),
            backgroundColor: '#118dff',
            borderRadius: 3,
            maxBarThickness: 54,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        onClick: (_, elements) => {
          if (elements.length)
            this.selectFilter('month', this.monthlySummary[elements[0].index].key);
        },
        scales: {
          x: { grid: { display: false }, border: { display: false } },
          y: {
            beginAtZero: true,
            border: { display: false },
            ticks: { callback: (value) => this.compactCurrency(Number(value)) },
          },
        },
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (context) => `${this.formatCurrency(context.parsed.y ?? 0)} ${this.currency}`,
            },
          },
        },
      },
    };
    this.charts.push(new Chart(this.monthlyChart.nativeElement, config));
  }

  private createDepartmentChart(): void {
    if (!this.departmentChart) return;
    this.charts.push(
      new Chart(this.departmentChart.nativeElement, {
        type: 'bar',
        data: {
          labels: this.departmentSummary.map((item) => item.label),
          datasets: [
            {
              label: 'Số phiếu',
              data: this.departmentSummary.map((item) => item.value),
              backgroundColor: '#4e79a7',
              borderRadius: 3,
              maxBarThickness: 28,
            },
          ],
        },
        options: {
          onClick: (_, elements) => {
            if (elements.length)
              this.selectFilter('department', this.departmentSummary[elements[0].index].key);
          },
          indexAxis: 'y',
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            x: {
              beginAtZero: true,
              ticks: {
                precision: 0,
              },
              border: {
                display: false,
              },
            },
            y: {
              grid: {
                display: false,
              },
              border: {
                display: false,
              },
            },
          },
          plugins: {
            legend: {
              display: false,
            },
          },
        },
      }),
    );
  }

  private createSupplierChart(): void {
    if (!this.supplierChart) return;
    this.charts.push(
      new Chart(this.supplierChart.nativeElement, {
        type: 'bar',
        data: {
          labels: this.supplierSummary.map((item) => item.label),
          datasets: [
            {
              label: this.currency,
              data: this.supplierSummary.map((item) => item.value),
              backgroundColor: '#0099bc',
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          indexAxis: 'y',
          onClick: (_, elements) => {
            if (elements.length)
              this.selectFilter('supplier', this.supplierSummary[elements[0].index].key);
          },
          scales: {
            x: {
              beginAtZero: true,
            },
          },
          plugins: {
            legend: {
              display: false,
            },
          },
        },
      }),
    );
  }

  private compactCurrency(value: number): string {
    return new Intl.NumberFormat('vi-VN', {
      notation: 'compact',
      maximumFractionDigits: 1,
    }).format(value);
  }

  private formatCurrency(value: number): string {
    return new Intl.NumberFormat('vi-VN', {
      maximumFractionDigits: 0,
    }).format(value);
  }

  private destroyCharts(): void {
    this.charts.splice(0).forEach((chart) => chart.destroy());
  }
}
