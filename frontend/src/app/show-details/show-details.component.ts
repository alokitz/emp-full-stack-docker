import { Component, OnInit } from '@angular/core';
import { EmployeeService } from '../employee.service';
import { ActivatedRoute } from '@angular/router';
import { Employee } from '../employee';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import * as bootstrap from 'bootstrap';

@Component({
  selector: 'app-show-details',
  templateUrl: './show-details.component.html',
  styleUrls: ['./show-details.component.css']
})
export class ShowDetailsComponent implements OnInit {
  employee!: Employee;
  safeResumeUrl!: SafeResourceUrl;
  baseURL = 'http://localhost:8080/api/v1/employees';

  // New: parsed JSON and jobId for re-score
  parsedJson: any = null;
  jobIdForScoring?: number;
  scoringInProgress = false;

  constructor(
    private route: ActivatedRoute,
    private employeeService: EmployeeService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.params['id']);
    if (!id) return;

    this.employeeService.getEmployeeById(id).subscribe({
      next: (data) => {
        this.employee = data;

        // resume url prepare (view inside iframe/modal)
        if (this.employee && this.employee.id) {
          const resumeUrl = `${this.baseURL}/${this.employee.id}/resume/view`;
          this.safeResumeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(resumeUrl);
        }

        // Try to read parsed JSON from known places
        // backend might return parsed data directly in employee.parsedData
        if (this.employee && (this.employee as any).parsedData) {
          this.parsedJson = (this.employee as any).parsedData;
        } else if ((this.employee as any).parsed) {
          this.parsedJson = (this.employee as any).parsed;
        } else if ((this.employee as any).breakdownJson) {
          // breakdownJson may be stored as string in DB
          try {
            this.parsedJson = JSON.parse((this.employee as any).breakdownJson);
          } catch (e) {
            // fallback: keep raw string
            this.parsedJson = (this.employee as any).breakdownJson;
          }
        } else {
          this.parsedJson = null;
        }
      },
      error: (err) => {
        console.error('Failed to load employee', err);
      }
    });
  }

  // ✅ Photo URL
  getPhotoUrl(): string {
    return this.employee?.id
      ? `${this.baseURL}/${this.employee.id}/photo`
      : 'assets/team1.jpg';
  }

  // ✅ Resume Download URL
  getResumeDownloadUrl(): string {
    return this.employee?.id
      ? `${this.baseURL}/${this.employee.id}/resume/download`
      : '#';
  }

  // Alternative resume view URL (if you need direct link)
  getResumeViewUrl(): string {
    return this.employee?.id
      ? `${this.baseURL}/${this.employee.id}/resume/view`
      : '#';
  }

  // ✅ Image error fallback
  onImgError(event: Event): void {
    (event.target as HTMLImageElement).src = 'assets/team1.jpg';
  }

  // ✅ Modal openers
  openPhotoModal(): void {
    const modal = new bootstrap.Modal(document.getElementById('photoModal')!);
    modal.show();
  }

  openResumeModal(): void {
    const modal = new bootstrap.Modal(document.getElementById('resumeModal')!);
    modal.show();
  }

  // -------------------------
  // Re-score (shortlist) action
  // -------------------------
  reScoreNow() {
    if (!this.employee || !this.employee.id) {
      alert('Employee not loaded');
      return;
    }
    if (!this.jobIdForScoring) {
      alert('Please enter Job ID to run scoring');
      return;
    }

    this.scoringInProgress = true;
    this.employeeService.reScoreForJob(this.jobIdForScoring, this.employee.id).subscribe({
      next: (resp) => {
        // backend probably updates employee.resumeScore and status — reload employee to get fresh data
        this.employeeService.getEmployeeById(this.employee.id).subscribe({
          next: (d) => {
            this.employee = d;
            // refresh parsedJson if available
            if ((d as any).parsedData) this.parsedJson = (d as any).parsedData;
            else if ((d as any).breakdownJson) {
              try { this.parsedJson = JSON.parse((d as any).breakdownJson); } catch { this.parsedJson = (d as any).breakdownJson; }
            }
            this.scoringInProgress = false;
            alert('Scoring completed and details refreshed.');
          },
          error: () => { this.scoringInProgress = false; alert('Scoring finished but failed to refresh employee.'); }
        });
      },
      error: (err) => {
        console.error('Re-score failed', err);
        this.scoringInProgress = false;
        alert('Re-score failed: ' + (err?.message || err));
      }
    });
  }
}
